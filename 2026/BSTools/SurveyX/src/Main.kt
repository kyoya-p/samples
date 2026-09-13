package xtool

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

class SurveyXTool : CliktCommand(name = "survey-x") {
    override fun help(context: Context) = "Battle Spirits X(Twitter) 大会情報収集・メタゲーム分析ツール"
    override fun run() = Unit
}

fun main(args: Array<String>) = SurveyXTool()
    .subcommands(
        Login(),
        Search(),
        ExtractShopBattle(),
        Analyze(),
        ClearCache(),
        InstallBrowsers()
    )
    .main(args)
