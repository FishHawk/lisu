package library

import kotlinx.serialization.Serializable

@Serializable
data class SearchEntry(
    val title: String? = null,
    val authors: List<String>? = null,
    val tags: Map<String, List<String>>? = null
)

class Filter(
    private val key: String?,
    private val value: String,
    private val isExclusionMode: Boolean,
    private val isExactMode: Boolean
) {
    private fun isKeyMatch(key: String) = this.key == null || this.key == key

    private fun isIvsMatched(ivs: List<String>) =
        if (this.isExactMode) !ivs.contains(this.value)
        else ivs.any { it.contains(this.value) }

    fun isPass(entry: SearchEntry): Boolean {
        val iv1 = if (key == null) (entry.authors.orEmpty() + entry.title).filterNotNull() else emptyList()
        val iv2 = entry.tags?.flatMap { if (isKeyMatch(it.key)) it.value else emptyList() }.orEmpty()
        return isIvsMatched(iv1 + iv2) != isExclusionMode
    }

    companion object {
        fun fromKeywords(keywords: String): List<Filter> {
            return keywords.split(';')
                .mapNotNull { fromToken(it.trim()) }
        }

        private fun fromToken(toke: String): Filter? {
            var token = toke

            if (token.isBlank()) return null

            val isExclusionMode = token.startsWith('-')
            if (isExclusionMode) token = token.substring(1)

            val isExactMode = token.endsWith('$')
            if (isExactMode) token = token.substring(0, token.length - 1)

            val splitPosition = token.indexOf(':')
            val key = if (splitPosition != -1) token.substring(0, splitPosition) else null
            val value = if (splitPosition != -1) token.substring(splitPosition + 1, token.length) else token
            return Filter(key, value, isExclusionMode, isExactMode)
        }
    }
}
