package source

import source.bilibili.Bilibili
import source.ehentai.EHentai
import source.ehentai.ExHentai
import source.manhuaren.Manhuaren

class SourceManager {
    private val sources: Map<String, Source> =
        listOf(
            Manhuaren(),
            Bilibili(),
            EHentai(),
            ExHentai()
        ).associateBy { it.id }

    fun listSources() = sources.values

    fun getSource(id: String) = sources[id]

    fun hasSource(id: String) = sources.containsKey(id)
}