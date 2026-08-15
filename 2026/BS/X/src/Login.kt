package xtool

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright

private const val LOGIN_TIMEOUT_MS = 600_000L
private const val POLL_INTERVAL_MS = 1000.0

class Login : CliktCommand(name = "login") {
    override fun help(context: Context) = "手動ログインしてセッション(storageState)を保存する"

    val id by option("--id", help = "ログインID/メールアドレスを自動入力する（パスワード・2段階認証は手動）")

    override fun run() {
        AUTH_STATE_FILE.parentFile?.mkdirs()

        Playwright.create().use { playwright ->
            val browser = playwright.chromium().launch(
                BrowserType.LaunchOptions().setHeadless(false).setArgs(STEALTH_LAUNCH_ARGS)
            )
            val context = browser.newContext()
            context.applyStealth()
            val page = context.newPage()
            page.navigate("$BASE_URL/login")

            if (id != null) {
                page.fill("input[name=\"username_or_email\"]:visible", id!!)
                page.keyboard().press("Enter")
                echo("IDを自動入力しました。続けてパスワード・2段階認証を手動で入力してください。")
            }

            echo("ブラウザでログインを完了してください（2段階認証を含む）。")
            echo("ログイン後、認証が確認できるまで待機します...")

            val deadline = System.currentTimeMillis() + LOGIN_TIMEOUT_MS
            while (context.cookies().none { it.name == "auth_token" }) {
                if (System.currentTimeMillis() > deadline) {
                    error("ログインがタイムアウトしました（${LOGIN_TIMEOUT_MS / 1000}秒経過）。")
                }
                page.waitForTimeout(POLL_INTERVAL_MS)
            }

            context.storageState(BrowserContext.StorageStateOptions().setPath(AUTH_STATE_FILE.toPath()))
            echo("ログインセッションを保存しました: ${AUTH_STATE_FILE.path}")

            browser.close()
        }
    }
}
