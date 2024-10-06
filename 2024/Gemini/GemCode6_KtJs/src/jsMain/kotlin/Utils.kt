actual fun getApiKey() = js("process.env.GOOGLE_API_KEY") as? String ?: throw IllegalArgumentException("No GOOGLE_API_KEY.")
