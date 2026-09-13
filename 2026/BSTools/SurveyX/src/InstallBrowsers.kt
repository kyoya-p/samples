package xtool

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.microsoft.playwright.CLI

class InstallBrowsers : CliktCommand(name = "install-browsers") {
    override fun help(context: Context) = "Playwright用のChromiumブラウザをインストールする"

    override fun run() {
        CLI.main(arrayOf("install", "chromium"))
    }
}
