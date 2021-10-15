package me.fishhawk.lisu.library

import se.sawano.java.text.AlphanumericComparator
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.name


fun alphanumericOrder(): Comparator<Path> = object : Comparator<Path> {
    val comparator = AlphanumericComparator(Locale.getDefault())
    override fun compare(p0: Path, p1: Path): Int = comparator.compare(p0.name, p1.name)
}

class Chapter(private val path: Path) {
    fun getContent() = path.listImageFiles().sortedWith(alphanumericOrder()).map { it.name }

    fun getImage(name: String): File? {
        val imagePath = path.resolve(name)
        return if (imagePath.exists()) imagePath.toFile() else null
    }
}