package library

import util.*
import java.nio.file.Path

class ChapterAccessor(
    private val path: Path,
    private val depth: Depth
) {
    enum class Depth { Zero, One, Two }

    val id
        get() = if (depth == Depth.Zero) "" else path.name

    private val unfinishedFile
        get() = path.resolve("unfinished")

    fun isFinished(): Boolean {
        return !unfinishedFile.exists()
    }

    fun setUnfinished(): Result<Unit> {
        return unfinishedFile
            .createFile()
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
        return path.resolve("$name.${image.ext}")
            .outputStream()
            .map { image.stream.copyTo(it).discard() }
    }
}
