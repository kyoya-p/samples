import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare


fun main(args: Array<String>) {
    val (serverName, userName, domain, shareName, folder) = args
    val password = System.getenv("PASSWORD") ?: ""

    val client = SMBClient()
    client.connect(serverName).use { connection ->
        val ac = AuthenticationContext(userName, password.toCharArray(), domain)
        val session = connection.authenticate(ac)
        (session.connectShare(shareName) as DiskShare?)?.use { share ->
            for (f in share.list(folder, "*.*")) {
                println("File : " + f.fileName)
            }
        }
    }
}

