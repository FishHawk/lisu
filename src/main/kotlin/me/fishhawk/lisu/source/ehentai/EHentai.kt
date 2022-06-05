package me.fishhawk.lisu.source.ehentai

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.fishhawk.lisu.model.Image
import me.fishhawk.lisu.model.MangaDetailDto
import me.fishhawk.lisu.model.MangaDto
import me.fishhawk.lisu.source.*
import org.jsoup.nodes.Document

fun getId(url: String): String {
    val pathSegments =
        (if (url.startsWith("http")) Url(url).pathSegments
        else url.split('/'))
            .filterNot(String::isNullOrBlank)

    val galleryId = pathSegments[1]
    val galleryToken = pathSegments[2]
    return "$galleryId.$galleryToken"
}

class EHentai : Source() {
    override val id: String = "E-Hentai"
    override val lang: String = "en"

    override val boardModels: Map<String, BoardModel> = mapOf(
        Board.Popular.id to emptyMap(),
        Board.Latest.id to mapOf(),
        Board.Ranking.id to mapOf("type" to Api.toplistTypes.map { it.name }),
    )

    private val api = Api()

    override suspend fun searchImpl(page: Int, keywords: String): List<MangaDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getBoardImpl(boardId: String, page: Int, filters: Map<String, Int>): List<MangaDto> =
        when (boardId) {
            Board.Popular.id -> if (page > 0) emptyList() else api.popular().parseMangaList()
            Board.Latest.id -> api.latest(page).parseMangaList()
            Board.Ranking.id -> api.toplist(page, filters["type"]!!).parseMangaList()
            else -> throw Error("board not found")
        }

    override suspend fun getMangaImpl(mangaId: String): MangaDetailDto {
        val (galleryId, galleryToken) = mangaId.split(".", limit = 2)
        api.getGallery(galleryId, galleryToken)
            .let { doc ->
                val metadata = GalleryMetadata().apply {
                    cover = doc.select("#gd1 div").attr("style").ifBlank { null }?.let {
                        it.substring(it.indexOf('(') + 1 until it.lastIndexOf(')'))
                    }
                    title = doc.select("#gn").text().ifBlank { null }?.trim()
                    altTitle = doc.select("#gj").text().ifBlank { null }?.trim()
                    genre = doc.select("#gdc div").text().ifBlank { null }?.trim()?.lowercase()
                    uploader = doc.select("#gdn").text().ifBlank { null }?.trim()

                    doc.select("#gdd tr").forEach {
                        val key = it.select(".gdt1").text().ifBlank { null }?.trim() ?: return@forEach
                        val value = it.select(".gdt2").text().ifBlank { null }?.trim() ?: return@forEach
                        when (key.removeSuffix(":").lowercase()) {
                            "posted" -> posted = value.asDateTimeToEpochSecond("yyyy-MM-dd HH:mm")
                            "length" -> length = value.removeSuffix("pages").trim().ifBlank { null }?.toInt()
                            "favorited" -> favorites = value.removeSuffix("times").trim().ifBlank { null }?.toInt()
                        }
                    }

                    rating = doc.select("#rating_label").text()
                        .removePrefix("Average:").trim().ifBlank { null }?.toDouble()
                    ratingCount = doc.select("#rating_count").text().trim().ifBlank { null }?.toInt()

                    doc.select("#taglist tr").forEach {
                        val key = it.select(".tc").text().removeSuffix(":")
                        val values = it.select("div").map { element -> element.text().trim() }
                        tags[key] = values
                    }
                }

                return MangaDetailDto(
                    providerId = id,
                    id = mangaId,
                    cover = metadata.cover,
                    updateTime = metadata.posted,
                    title = metadata.title ?: metadata.altTitle,
                    authors = metadata.tags[TagKeyArtist]?.ifEmpty { null },
                    isFinished = true,
                    description = StringBuilder().apply {
                        metadata.altTitle?.let { append("Alternate Title: $it\n") }
                        metadata.length?.let { append("$it pages, ") }
                        metadata.favorites?.let { append("$it favorites\n") }
                        metadata.rating?.let {
                            append("Rating: $it")
                            metadata.ratingCount?.let { count -> append(" ($count)") }
                            append("\n")
                        }
                    }.toString().removeSuffix("\n").ifBlank { null },
                    tags = metadata.tags.filterKeys { it != TagKeyArtist },
                    preview = doc.parseImageUrls().ifEmpty { null },
                )
            }
    }

    override suspend fun getContentImpl(mangaId: String, chapterId: String): List<String> {
        val (galleryId, galleryToken) = mangaId.split(".", limit = 2)
        val doc = api.getGallery(galleryId, galleryToken, 0)
        val urls = doc.parseImageUrls().toMutableList()

        if (urls.isNotEmpty()) {
            val pageSize = doc.select("a[onclick=return false]").takeLast(2).first().text().toInt() - 1
            for (page in 1..pageSize) {
                urls += api.getGallery(galleryId, galleryToken, page).parseImageUrls()
            }
        }
        return urls
    }

    // strange, maybe related to https://youtrack.jetbrains.com/issue/KTOR-1305
    override suspend fun getImageImpl(url: String): Image = withContext(Dispatchers.IO) {
        val imageUrl = if (url.startsWith("https://e-hentai.org/s/")) {
            api.getPage(url).select("div[id=i3] img").attr("src")
        } else url
        api.getImage(imageUrl).let {
            Image(
                it.contentType(),
                it.body<ByteReadChannel>().toInputStream()
            )
        }
    }

    private fun Document.parseImageUrls(): List<String> =
        select(".gdtm a")
            .map { it.child(0).attr("alt").toInt() to it.attr("href") }
            .sortedBy(Pair<Int, String>::first)
            .map { it.second }

    private fun Document.parseMangaList(): List<MangaDto> =
        select("table.itg td.glname")
            .mapNotNull { it.parent() }
            .map { tr ->
                val a = tr.select("td.gl3c a")
                MangaDto(
                    providerId = id,
                    id = getId(a.attr("href")),
                    cover = tr.select(".glthumb img").let {
                        it.attr("data-src").ifBlank { null }
                            ?: it.attr("src").ifBlank { null }
                    },
                    updateTime = tr.select("td.gl2c div[onclick]").lastOrNull()?.text()
                        ?.asDateTimeToEpochSecond("yyyy-MM-dd HH:mm"),
                    title = a.select(".glink").text(),
                    authors = a.select(".gt")
                        .map { it.attr("title") }
                        .filter { it.startsWith("$TagKeyArtist:") }
                        .map { it.substringAfter("$TagKeyArtist:") }
                        .ifEmpty { null },
                    isFinished = true,
                )
            }
}