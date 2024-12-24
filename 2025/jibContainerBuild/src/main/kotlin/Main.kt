package org.example

//TIP コードを<b>実行</b>するには、<shortcut actionId="Run"/> を押すか
// ガターの <icon src="AllIcons.Actions.Execute"/> アイコンをクリックします。
fun main() {
    val name = "Kotlin"
    //TIP ハイライトされたテキストにキャレットがある状態で <shortcut actionId="ShowIntentionActions"/> を押すと
    // IntelliJ IDEA によるその修正案を確認できます。
    println("Hello, " + name + "!")

    for (i in 1..5) {
        //TIP <shortcut actionId="Debug"/> を押してコードのデバッグを開始します。<icon src="AllIcons.Debugger.Db_set_breakpoint"/> ブレークポイントを 1 つ設定しましたが、
        // <shortcut actionId="ToggleLineBreakpoint"/> を押すといつでも他のブレークポイントを追加できます。
        println("i = $i")
    }
}