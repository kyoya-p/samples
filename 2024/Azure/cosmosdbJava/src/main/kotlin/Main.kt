fun main(args: Array<String>) {
    val connStr = System.getenv("CONNSTR")
    when (args[0]) {
        "countTenantDevice" -> countDocuments(connStr)
        else -> findDocuments(connStr, db = args[0], collName = args[1], filters = args.drop(2))
    }
}

