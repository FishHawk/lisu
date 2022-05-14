package me.fishhawk.lisu.library

import me.fishhawk.lisu.model.Image
import me.fishhawk.lisu.model.toImage
import me.fishhawk.lisu.then
import java.nio.file.Path
import kotlin.io.path.*

class Chapter(
    private val path: Path,
    private val depth: Depth
) {
    enum class Depth { Zero, One, Two }

    val id
        get() = if (depth == Depth.Zero) "" else path.name

    private val unfinishedFile
        get() = path / ".unfinished"

    fun isFinished(): Boolean {
        return !unfinishedFile.exists()
    }

    fun setUnfinished(): Result<Unit> {
        return unfinishedFile
            .createFile()
            .then(Path::setDosHidden)
            .map { it.discard() }
    }

    fun setFinished(): Result<Unit> {
        return unfinishedFile.delete()
    }

    fun getContent(): List<String>? {
        return path.listImageFiles()
            .getOrNull()
            ?.map { it.nameWithoutExtension }
            ?.distinct()
            ?.sortedWith(alphanumericOrder())
    }

    fun getImage(name: String): Image? {
        return path.listImageFiles()
            .getOrNull()
            ?.firstOrNull { it.nameWithoutExtension == name }
            ?.toImage()
    }

    fun setImage(name: String, image: Image): Result<Unit> {
        return path.resolveChild("$name.${image.ext}")
            .then(Path::outputStream)
            .map { image.stream.copyTo(it).discard() }
    }
}
