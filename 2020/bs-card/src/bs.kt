package BSSim

fun main(args: Array<String>) {
    if(args.size<=3) {
        println("""
            syntax: java -jar bs.jar <deckFile> <logFile> [<shuffleSeed> <repeat>] 
                deckFile = [[ カード1, カード2, カード3,.... ], [ カード4, カード5,...],...]
            exmaple: java- jar bs.jar mydeck1.bs bs.log
                シャッフルしない
            exmaple: java -jar bs.jar mydeck1.bs 42 400 bs.log
                seed=40,41,42...でシャッフル、400回繰り返す

        """.trimIndent())
    }


}