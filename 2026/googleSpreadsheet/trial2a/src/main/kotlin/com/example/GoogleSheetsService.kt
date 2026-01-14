package com.example

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.awt.Desktop
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object GoogleSheetsService {
    private const val APPLICATION_NAME = "SheetMaster Desktop"
    private val JSON_FACTORY = GsonFactory.getDefaultInstance()
    private val HTTP_TRANSPORT: com.google.api.client.http.HttpTransport
    private val SCOPES = listOf(SheetsScopes.SPREADSHEETS, SheetsScopes.DRIVE_FILE)

    private val PROXY_HOST: String?
    private val PROXY_PORT: Int?
    private val PROXY_USER: String?
    private val PROXY_PASS: String?

    // Token Management
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var expirationTimeMillis: Long = 0
    private var codeFuture = CompletableFuture<String>()
    
    private val tokenFilePath = Paths.get(System.getProperty("user.home"), ".sheetmaster_tokens.json")
    private val signalFilePath = Paths.get(System.getProperty("java.io.tmpdir"), "sheetmaster_auth_signal")

    init {
        // Parse Proxy from environment variable
        val proxyUrl = System.getenv("https_proxy") ?: System.getenv("http_proxy")
        if (proxyUrl != null) {
            println("Detected proxy configuration: $proxyUrl")
            val uri = URI(proxyUrl)
            PROXY_HOST = uri.host
            PROXY_PORT = if (uri.port != -1) uri.port else 8080
            val userInfo = uri.userInfo
            if (userInfo != null && userInfo.contains(":")) {
                val parts = userInfo.split(":")
                PROXY_USER = parts[0]
                PROXY_PASS = parts[1]
            } else {
                PROXY_USER = null
                PROXY_PASS = null
            }
            
            // Set System Properties for other libraries that might depend on them
            System.setProperty("https.proxyHost", PROXY_HOST)
            System.setProperty("https.proxyPort", PROXY_PORT.toString())
            System.setProperty("http.proxyHost", PROXY_HOST)
            System.setProperty("http.proxyPort", PROXY_PORT.toString())
            
            if (PROXY_USER != null && PROXY_PASS != null) {
                java.net.Authenticator.setDefault(object : java.net.Authenticator() {
                    override fun getPasswordAuthentication(): java.net.PasswordAuthentication {
                        return java.net.PasswordAuthentication(PROXY_USER, PROXY_PASS!!.toCharArray())
                    }
                })
                System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "")
            }

            // Configure Google API Transport with Proxy
            val proxy = java.net.Proxy(java.net.Proxy.Type.HTTP, java.net.InetSocketAddress(PROXY_HOST, PROXY_PORT!!))
            HTTP_TRANSPORT = com.google.api.client.http.javanet.NetHttpTransport.Builder()
                .setProxy(proxy)
                .build() // Note: TrustedTransport doesn't easily allow manual proxy setting in older versions, using NetHttpTransport
        } else {
            PROXY_HOST = null
            PROXY_PORT = null
            PROXY_USER = null
            PROXY_PASS = null
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
        }
        
        loadTokens()
    }

    private fun saveTokens() {
        if (accessToken == null) return
        val json = JsonObject()
        json.addProperty("access_token", accessToken)
        json.addProperty("refresh_token", refreshToken)
        json.addProperty("expiration_time", expirationTimeMillis)
        Files.writeString(tokenFilePath, json.toString())
    }

    private fun loadTokens() {
        if (Files.exists(tokenFilePath)) {
            try {
                val json = JsonParser.parseString(Files.readString(tokenFilePath)).asJsonObject
                accessToken = json.get("access_token")?.asString
                refreshToken = json.get("refresh_token")?.asString
                expirationTimeMillis = json.get("expiration_time")?.asLong ?: 0
                println("Loaded tokens from local storage.")
            } catch (e: Exception) {
                println("Failed to load tokens: ${e.message}")
            }
        }
    }

    // PKCE Helpers
    private fun generateVerifier(): String {
        val sr = SecureRandom()
        val code = ByteArray(32)
        sr.nextBytes(code)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(code)
    }

    private fun generateChallenge(verifier: String): String {
        val bytes = verifier.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    fun getClientId(): String {
        // Look for credentials.json in current directory first, then fallback to resources
        val file = File("credentials.json")
        val inputStream = if (file.exists()) {
            println("Loading credentials from ${file.absolutePath}")
            file.inputStream()
        } else {
            GoogleSheetsService::class.java.getResourceAsStream("/credentials.json")
        } ?: throw Exception("credentials.json not found in current directory or resources")

        val json = JsonParser.parseReader(InputStreamReader(inputStream)).asJsonObject
        
        // Use "installed" or "web" but we prefer secret-less client ID types (like iOS/Android/Desktop)
        return when {
            json.has("installed") -> json.getAsJsonObject("installed").get("client_id").asString
            json.has("web") -> json.getAsJsonObject("web").get("client_id").asString
            else -> throw Exception("Unsupported credentials.json format")
        }
    }

    fun getCustomScheme(): String {
        val clientId = getClientId()
        val prefix = clientId.substringBefore(".apps.googleusercontent.com")
        return "com.googleusercontent.apps.$prefix"
    }

    /**
     * Reisters the custom URI scheme in Windows Registry.
     * This allows the browser to redirect to our app using the custom scheme.
     */
    fun registerWindowsProtocol() {
        val scheme = getCustomScheme()
        val javaPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe"
        val classpath = System.getProperty("java.class.path")
        val mainClass = "com.example.MainKt"
        
        // Command to run the app with the URL as the first argument
        val command = "\"$javaPath\" -cp \"$classpath\" $mainClass \"%1\""
        
        // We use reg.exe to modify the registry for current user
        val baseKey = "HKCU\\Software\\Classes\\$scheme"
        println("Registering protocol with command: $command")
        
        Runtime.getRuntime().exec(arrayOf("reg", "add", baseKey, "/ve", "/t", "REG_SZ", "/d", "URL:Google Auth Protocol", "/f")).waitFor()
        Runtime.getRuntime().exec(arrayOf("reg", "add", baseKey, "/v", "URL Protocol", "/t", "REG_SZ", "/d", "", "/f")).waitFor()
        Runtime.getRuntime().exec(arrayOf("reg", "add", "$baseKey\\shell\\open\\command", "/ve", "/t", "REG_SZ", "/d", command, "/f")).waitFor()
        
        println("Successfully registered protocol in registry.")
    }

    /**
     * Sets the auth code received from a deep link.
     * If this is a second instance, it writes to a signal file.
     */
    fun onAuthCodeReceived(code: String) {
        Files.writeString(signalFilePath, code)
        codeFuture.complete(code)
    }

    fun authorizeManual(testRedirectUri: String? = null): String {
        val clientId = getClientId()
        val scheme = getCustomScheme()
        val redirectUri = testRedirectUri ?: "$scheme:/oauth2callback"
        val verifier = generateVerifier()
        val challenge = generateChallenge(verifier)
        
        // Reset code future to avoid using old codes from previous attempts
        codeFuture = CompletableFuture<String>()
        
        // Ensure protocol is registered if using custom scheme
        if (testRedirectUri == null) {
            registerWindowsProtocol()
        }
        
        // Clean up old signal file
        Files.deleteIfExists(signalFilePath)

        val authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=$clientId&" +
                "redirect_uri=${URLEncoder.encode(redirectUri, "UTF-8")}&" +
                "response_type=code&" +
                "scope=${URLEncoder.encode(SCOPES.joinToString(" "), "UTF-8")}&" +
                "code_challenge=$challenge&" +
                "code_challenge_method=S256&" +
                "access_type=offline"

        println("Please open this URL: $authUrl")
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(authUrl))
        }

        println("Waiting for authorization code from browser (custom scheme redirect)...")
        println("If the browser fails to redirect, you can manually paste the 'code' parameter (or the FULL URL) here:")
        
        // Concurrent manual input and signal file polling
        var authCode: String? = null
        val startTime = System.currentTimeMillis()
        val timeout = TimeUnit.MINUTES.toMillis(30)

        // Start a thread to read from console as a fallback
        val consoleReader = Thread {
            val sc = Scanner(System.`in`)
            if (sc.hasNextLine()) {
                val line = sc.nextLine()
                if (line.isNotBlank()) {
                    // Extract code from full URL if present
                    val input = line.trim()
                    val code = if (input.contains("code=")) {
                        input.substringAfter("code=").substringBefore("&")
                    } else {
                        input
                    }
                    onAuthCodeReceived(code)
                }
            }
        }
        consoleReader.isDaemon = true
        consoleReader.start()
        
        while (System.currentTimeMillis() - startTime < timeout) {
            if (Files.exists(signalFilePath)) {
                authCode = Files.readString(signalFilePath).trim()
                Files.deleteIfExists(signalFilePath)
                break
            }
            if (codeFuture.isDone) {
                authCode = codeFuture.get()
                break
            }
            Thread.sleep(1000)
        }

        if (authCode == null) {
            throw Exception("Authorization timed out or failed.")
        }
        
        // Token Exchange (Secret-less PKCE)
        val clientBuilder = HttpClient.newBuilder()
        if (PROXY_HOST != null && PROXY_PORT != null) {
            clientBuilder.proxy(java.net.ProxySelector.of(java.net.InetSocketAddress(PROXY_HOST, PROXY_PORT)))
        }
        val client = clientBuilder.build()
        
        val requestBody = "code=$authCode&" +
                "client_id=$clientId&" +
                "redirect_uri=${URLEncoder.encode(redirectUri, "UTF-8")}&" +
                "grant_type=authorization_code&" +
                "code_verifier=$verifier"
        
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://oauth2.googleapis.com/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val tokenResponse = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (tokenResponse.statusCode() != 200) {
            throw Exception("Token exchange failed: ${tokenResponse.body()}")
        }

        val tokenJson = JsonParser.parseString(tokenResponse.body()).asJsonObject
        accessToken = tokenJson.get("access_token").asString
        refreshToken = tokenJson.get("refresh_token")?.asString ?: refreshToken
        val expiresIn = tokenJson.get("expires_in").asLong
        expirationTimeMillis = System.currentTimeMillis() + (expiresIn * 1000)
        
        saveTokens()
        return accessToken!!
    }

    private fun refreshAccessToken(): String {
        println("Refreshing access token...")
        val clientId = getClientId()
        
        val clientBuilder = HttpClient.newBuilder()
        if (PROXY_HOST != null && PROXY_PORT != null) {
            clientBuilder.proxy(java.net.ProxySelector.of(java.net.InetSocketAddress(PROXY_HOST, PROXY_PORT)))
        }
        val client = clientBuilder.build()

        val requestBody = "client_id=$clientId&" +
                "refresh_token=$refreshToken&" +
                "grant_type=refresh_token"

        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://oauth2.googleapis.com/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val tokenResponse = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (tokenResponse.statusCode() != 200) {
            println("Token refresh failed: ${tokenResponse.body()}")
            // If refresh fails, we might need to re-authorize manually
            accessToken = null
            refreshToken = null
            Files.deleteIfExists(tokenFilePath)
            return authorizeManual()
        }

        val tokenJson = JsonParser.parseString(tokenResponse.body()).asJsonObject
        accessToken = tokenJson.get("access_token").asString
        // Some providers might not return a new refresh token if it's still valid
        if (tokenJson.has("refresh_token")) {
            refreshToken = tokenJson.get("refresh_token").asString
        }
        val expiresIn = tokenJson.get("expires_in").asLong
        expirationTimeMillis = System.currentTimeMillis() + (expiresIn * 1000)
        
        saveTokens()
        return accessToken!!
    }

    fun getService(): Sheets {
        val token = when {
            accessToken != null && System.currentTimeMillis() < expirationTimeMillis - 60000 -> {
                println("Using cached access token.")
                accessToken!!
            }
            refreshToken != null -> refreshAccessToken()
            else -> authorizeManual()
        }
        
        return Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY) { request ->
            request.headers.authorization = "Bearer $token"
        }
            .setApplicationName(APPLICATION_NAME)
            .build()
    }

    fun createSpreadsheet(title: String): String {
        val service = getService()
        val spreadsheet = Spreadsheet().setProperties(SpreadsheetProperties().setTitle(title))
        val result = service.spreadsheets().create(spreadsheet).execute()
        return result.spreadsheetId
    }

    fun updateCell(spreadsheetId: String, range: String, value: String) {
        val service = getService()
        val body = ValueRange().setValues(listOf(listOf(value)))
        service.spreadsheets().values()
            .update(spreadsheetId, range, body)
            .setValueInputOption("RAW")
            .execute()
    }

    /**
     * Internal method to set tokens directly (useful for tests)
     */
    fun setTokens(access: String, refresh: String?, expiresAt: Long) {
        this.accessToken = access
        this.refreshToken = refresh
        this.expirationTimeMillis = expiresAt
        saveTokens()
    }

    fun openUrl(url: String) {
        println("Attempting to open URL: $url")
        val os = System.getProperty("os.name").lowercase()
        
        // Method 1: Desktop.browse (Standard Java)
        // This is usually the best way if supported.
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
                println("Opened via Desktop.browse")
                return
            }
        } catch (e: Exception) {
            println("Desktop.browse failed: ${e.message}, attempting fallbacks...")
        }

        // Method 2: Windows 'start' command
        // Note: 'start "" "url"' is used to prevent the url from being interpreted as a window title
        if (os.contains("win")) {
            try {
                ProcessBuilder("cmd", "/c", "start", "", url).start()
                println("Opened via cmd /c start")
                return
            } catch (e: Exception) {
                println("cmd /c start failed: ${e.message}")
            }
        }
        
        // Method 3: macOS 'open'
        if (os.contains("mac")) {
            try {
                ProcessBuilder("open", url).start()
                println("Opened via open (macOS)")
                return
            } catch (e: Exception) {
                 println("mac open failed: ${e.message}")
            }
        }
        
        // Method 4: Linux 'xdg-open'
        if (os.contains("nix") || os.contains("nux")) {
             try {
                ProcessBuilder("xdg-open", url).start()
                println("Opened via xdg-open")
                return
            } catch (e: Exception) {
                 println("xdg-open failed: ${e.message}")
            }
        }
        
        println("ERROR: Could not open URL. No supported method found.")
    }

    fun openInBrowser(spreadsheetId: String) {
        val url = "https://docs.google.com/spreadsheets/d/$spreadsheetId"
        openUrl(url)
    }
}
