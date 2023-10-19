//import {  AzureFunction, Context,  Cookie,  HttpRequest,  HttpResponseSimple,} from "@azure/functions";


@JsModule("@azure/functions")
@JsNonModule
external class AzureFunction

fun AzureFunction.test()=println("AzureFunction.test()")
fun main() {
    val a = AzureFunction()
    a.test()
}

