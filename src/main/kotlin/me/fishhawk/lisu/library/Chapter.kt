package me.fishhawk.lisu.library

import me.fishhawk.lisu.model.Image
import me.fishhawk.lisu.model.toImage
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name

class Chapter(
    val collectionId: String,
    val chapterId: String,
    private val path: Path
) {
    private val unfinishedFile = path.resolve(".unfinished").toFile()

    var unfinished
        get() = unfinishedFile.isFile
        set(value) {
            if (value) unfinishedFile.createNewFile()
            else unfinishedFile.delete()
        }

    fun getContent() = path.listImageFiles().sortedAlphanumeric().map { it.name }

    fun getImage(name: String): Image? {
        val imagePath = path.resolve(name)
        return if (imagePath.exists()) imagePath.toImage() else null
    }

    fun setImage(name: String, image: Image) {
        val imageFile = path.resolve("$name.${image.ext}").toFile()
        image.stream.copyTo(imageFile.outputStream())
    }
}