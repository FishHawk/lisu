package me.fishhawk.lisu.provider

import me.fishhawk.lisu.provider.manhuaren.Manhuaren

class ProviderManager {
    val providers: Map<String, Provider> =
        listOf(Manhuaren()).associateBy { it.id }
}