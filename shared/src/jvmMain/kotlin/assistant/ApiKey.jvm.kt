package assistant

actual fun loadOpenRouterApiKey(): String? {
    System.getenv("OPENROUTER_API_KEY")
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }

    return null
}
