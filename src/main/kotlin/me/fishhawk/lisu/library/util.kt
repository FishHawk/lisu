package me.fishhawk.lisu.library

import kotlinx.serialization.Serializable
import se.sawano.java.text.AlphanumericComparator
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

// hack, see https://stackoverflow.com/questions/48448000/kotlins-extension-method-stream-tolist-is-missing
import kotlin.streams.toList

fun Path.listImageFiles(): List<Path> = Files.list(this).filter { it.isImageFile() }.toList()

fun Path.listDirectory(): List<Path> = Files.list(this).filter { it.isDirectory() }.toList()

fun Iterable<Path>.sortedAlphanumeric(): List<Path> {
    return sortedWith(alphanumericOrder())
}

fun alphanumericOrder(): Comparator<Path> = object : Comparator<Path> {
    val comparator = AlphanumericComparator(Locale.getDefault())
    override fun compare(p0: Path, p1: Path): Int = comparator.compare(p0.name, p1.name)
}

private val imageExtensions = listOf("bmp", "jpeg", "jpg", "png", "gif", "webp")
fun Path.isImageFile() = isRegularFile() && extension.lowercase() in imageExtensions

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

            val isExactMode = token.endsWith('$');
            if (isExactMode) token = token.substring(0, token.length - 1)

            val splitPosition = token.indexOf(':')
            val key = if (splitPosition != -1) token.substring(0, splitPosition) else null
            val value = if (splitPosition != -1) token.substring(splitPosition + 1, token.length) else token
            return Filter(key, value, isExclusionMode, isExactMode)
        }
    }
}