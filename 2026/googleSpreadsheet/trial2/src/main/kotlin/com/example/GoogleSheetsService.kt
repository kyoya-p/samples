package com.example

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.ValueRange
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
    private val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
    private val SCOPES = listOf(SheetsScopes.SPREADSHEETS, SheetsScopes.DRIVE_FILE)
    
    private var accessToken: String? = null
    private var codeFuture = CompletableFuture<String>()
    private val signalFilePath = Paths.get(System.getProperty("java.io.tmpdir"), "sheetmaster_auth_signal")

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
        val inputStream = GoogleSheetsService::class.java.getResourceAsStream("/credentials.json")
            ?: throw Exception("credentials.json not found")
        val json = JsonParser.parseReader(InputStreamReader(inputStream)).asJsonObject
        return json.getAsJsonObject("installed").get("client_id").asString
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

    fun authorizeManual(): String {
        val clientId = getClientId()
        val scheme = getCustomScheme()
        val redirectUri = "$scheme:/oauth2callback"
        val verifier = generateVerifier()
        val challenge = generateChallenge(verifier)
        
        // Ensure protocol is registered
        registerWindowsProtocol()
        
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
        println("If the browser fails to redirect, you can manually paste the 'code' parameter here:")
        
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
                    val code = if (line.contains("code=")) line.substringAfter("code=").substringBefore("&") else line.trim()
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
        
        // Token Exchange
        val client = HttpClient.newHttpClient()
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
        return accessToken!!
    }

    fun getService(userId: String): Sheets {
        val token = accessToken ?: authorizeManual()
        return Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY) { request ->
            request.headers.authorization = "Bearer $token"
        }
            .setApplicationName(APPLICATION_NAME)
            .build()
    }

    fun createSpreadsheet(userId: String, title: String): String {
        val service = getService(userId)
        val spreadsheet = Spreadsheet().setProperties(SpreadsheetProperties().setTitle(title))
        val result = service.spreadsheets().create(spreadsheet).execute()
        return result.spreadsheetId
    }

    fun updateCell(userId: String, spreadsheetId: String, range: String, value: String) {
        val service = getService(userId)
        val body = ValueRange().setValues(listOf(listOf(value)))
        service.spreadsheets().values()
            .update(spreadsheetId, range, body)
            .setValueInputOption("RAW")
            .execute()
    }

    fun openInBrowser(spreadsheetId: String) {
        val url = "https://docs.google.com/spreadsheets/d/$spreadsheetId"
        println("Opening spreadsheet in browser: $url")
        val os = System.getProperty("os.name").lowercase()
        
        // On Windows, 'cmd /c start' is often more reliable than Desktop.browse
        if (os.contains("win")) {
            try {
                Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", url))
                println("Triggered 'cmd /c start' for Windows (Primary)")
                return 
            } catch (e: Exception) {
                println("Windows 'cmd /c start' failed, falling back to Desktop.browse: ${e.message}")
            }
        }

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
                println("Successfully called Desktop.browse")
            } else {
                println("Desktop.browse not supported, trying fallback...")
                if (os.contains("mac")) {
                    Runtime.getRuntime().exec(arrayOf("open", url))
                    println("Triggered 'open' fallback for macOS")
                } else {
                    println("No fallback available for $os")
                }
            }
        } catch (e: Exception) {
            println("Error opening browser: ${e.message}")
        }
    }
}
