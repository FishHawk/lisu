package me.fishhawk.lisu.source

import me.fishhawk.lisu.source.manhuaren.Manhuaren

class SourceManager {
    private val sources: Map<String, Source> =
        listOf(Manhuaren()).associateBy { it.id }

    fun listSources() = sources.values

    fun getSource(id: String) = sources[id]
}