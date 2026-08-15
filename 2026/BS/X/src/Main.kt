package xtool

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

class XTool : CliktCommand(name = "x-tool") {
    override fun run() = Unit
}

fun main(args: Array<String>) = XTool()
    .subcommands(Login(), Search(), InstallBrowsers())
    .main(args)
