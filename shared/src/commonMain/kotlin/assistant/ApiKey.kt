package assistant

/**
 * Loads the OpenRouter API key from the platform configuration, or null when unset.
 *
 * JVM implementations check the OPENROUTER_API_KEY environment variable first
 */
expect fun loadOpenRouterApiKey(): String?
