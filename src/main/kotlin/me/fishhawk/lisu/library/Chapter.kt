package me.fishhawk.lisu.library

import me.fishhawk.lisu.model.Image
import me.fishhawk.lisu.model.toImage
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name

class Chapter(private val path: Path) {
    fun getContent() = path.listImageFiles().sortedAlphanumeric().map { it.name }

    fun getImage(name: String): Image? {
        val imagePath = path.resolve(name)
        return if (imagePath.exists()) imagePath.toImage() else null
    }
}