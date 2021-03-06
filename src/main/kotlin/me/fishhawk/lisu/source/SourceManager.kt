package me.fishhawk.lisu.source

import me.fishhawk.lisu.source.bilibili.Bilibili
import me.fishhawk.lisu.source.ehentai.EHentai
import me.fishhawk.lisu.source.manhuaren.Manhuaren

class SourceManager {
    private val sources: Map<String, Source> =
        listOf(
            Manhuaren(),
            Bilibili(),
            EHentai(),
        ).associateBy { it.id }

    fun listSources() = sources.values

    fun getSource(id: String) = sources[id]

    fun hasSource(id: String) = sources.containsKey(id)
}