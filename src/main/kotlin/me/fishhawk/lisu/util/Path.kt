package me.fishhawk.lisu.util

import se.sawano.java.text.AlphanumericComparator
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.*
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.FileTime
import java.util.*
import kotlin.io.path.*

class IllegalChildPathException(dir: String, other: String) :
    FileSystemException(dir, other, "illegal child path") {
//    companion object {
//        private const val serialVersionUID = 3056667871802779003L
//    }
}

fun Any?.discard() = Unit

val Path.name: String
    get() = fileName?.toString().orEmpty()

val Path.nameWithoutExtension: String
    get() = fileName?.toString()?.substringBeforeLast(".") ?: ""

val Path.extension: String
    get() = fileName?.toString()?.substringAfterLast('.', "") ?: ""

fun Path.exists(vararg options: LinkOption): Boolean = Files.exists(this, *options)
fun Path.notExists(vararg options: LinkOption): Boolean = Files.notExists(this, *options)

fun Path.isRegularFile(vararg options: LinkOption): Boolean = Files.isRegularFile(this, *options)
fun Path.isDirectory(vararg options: LinkOption): Boolean = Files.isDirectory(this, *options)

private val imageExtensions = listOf("bmp", "jpeg", "jpg", "png", "gif", "webp")
fun Path.isImageFile() = isRegularFile() && extension.lowercase() in imageExtensions

fun Path.resolveChild(other: String) =
    if (
        other == "" ||
        other == "." ||
        other == ".." ||
        other.contains(File.separator)
    ) {
        Result.failure(IllegalChildPathException(this.toString(), other))
    } else {
        Result.success(resolve(other))
    }

fun Path.list(filter: (Path) -> Boolean) =
    runCatchingException { listDirectoryEntries().filter(filter) }

fun Path.listFiles() = list { it.isRegularFile() }
fun Path.listDirectory() = list { it.isDirectory() }
fun Path.listImageFiles() = list { it.isImageFile() }

fun Path.createFile(vararg attributes: FileAttribute<*>) =
    runCatchingException { Files.createFile(this, *attributes) }

fun Path.createDir(vararg attributes: FileAttribute<*>) =
    runCatchingException { Files.createDirectory(this, *attributes) }

fun Path.createDirAll(vararg attributes: FileAttribute<*>) =
    runCatchingException { Files.createDirectories(this, *attributes) }

fun Path.delete() =
    runCatchingException { Files.delete(this) }

fun Path.deleteDirAll() =
    runCatchingException {
        Files.walk(this)
            .sorted(Comparator.reverseOrder())
            .forEach { it.deleteExisting() }
    }

fun Path.inputStream(vararg options: OpenOption) =
    runCatchingException { Files.newInputStream(this, *options) }

fun Path.outputStream(vararg options: OpenOption) =
    runCatchingException { Files.newOutputStream(this, *options) }

fun Path.readText(charset: Charset = Charsets.UTF_8) =
    runCatchingException { reader(charset).use { it.readText() } }

fun Path.writeText(text: CharSequence, charset: Charset = Charsets.UTF_8, vararg options: OpenOption) =
    runCatchingException {
        Files.newOutputStream(this, *options)
            .writer(charset)
            .use { it.append(text) }
            .discard()
    }

fun Path.setDosHidden() =
    runCatchingException {
        setAttribute(
            "dos:hidden",
            true,
            LinkOption.NOFOLLOW_LINKS
        )
    }

fun Path.setFileDescription(title:String) =
    runCatchingException {
        setAttribute(
            "user:Doc.Title",
            ByteBuffer.wrap(title.toByteArray()),
            LinkOption.NOFOLLOW_LINKS
        )
    }

fun Path.setUserXorgComment(comment: String) =
    runCatchingException {
        setAttribute(
            "user:xdg.comment",
            ByteBuffer.wrap(comment.toByteArray()),
            LinkOption.NOFOLLOW_LINKS
        )
    }

fun Path.getLastModifiedTime(vararg options: LinkOption): FileTime =
    Files.getLastModifiedTime(this, *options)

fun Path.setLastModifiedTime(value: FileTime): Path =
    Files.setLastModifiedTime(this, value)

fun Iterable<Path>.sortedAlphanumeric() =
    sortedWith(alphanumericOrder())

fun <T> alphanumericOrder(): Comparator<T> = object : Comparator<T> {
    val comparator = AlphanumericComparator(Locale.getDefault())
    override fun compare(p0: T, p1: T): Int = comparator.compare(p0.toString(), p1.toString())
}
