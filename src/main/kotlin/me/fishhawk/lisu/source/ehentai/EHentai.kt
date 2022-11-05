package me.fishhawk.lisu.source.ehentai

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import me.fishhawk.lisu.library.model.Chapter
import me.fishhawk.lisu.library.model.Manga
import me.fishhawk.lisu.library.model.MangaDetail
import me.fishhawk.lisu.library.model.MangaPage
import me.fishhawk.lisu.source.*
import me.fishhawk.lisu.util.Image
import org.jsoup.nodes.Document

abstract class EHentaiBase(enableExHentai: Boolean) : Source() {
    override val lang: String = "en"

    override val boardModel: Map<BoardId, BoardModel> = mapOf(
        BoardId.Main to BoardModel(
            hasSearchBar = true,
            advance = mapOf(
                R.genres to FilterModel.MultipleSelect(Api.latestGenres.map { it.name }),
                R.searchGalleryName to FilterModel.Switch(true),
                R.searchGalleryTags to FilterModel.Switch(true),
                R.searchGalleryDescription to FilterModel.Switch(),
                R.searchExpungedGalleries to FilterModel.Switch(),
                R.onlyShowGalleriesWithTorrents to FilterModel.Switch(),
                R.searchLowPowerTags to FilterModel.Switch(),
                R.searchDownvotedTags to FilterModel.Switch(),
                R.minimumRating to FilterModel.Select(Api.latestRatings.map { it.name }),
                R.minimumPages to FilterModel.Text,
                R.maximumPages to FilterModel.Text,
            )
        ),
        BoardId.Rank to BoardModel(
            base = mapOf(
                "Type" to FilterModel.Select(listOf("Popular") + Api.toplistTypes.map { it.name })
            ),
        ),
    )

    protected val api = Api(
        enableExHentai = enableExHentai,
        cookiesStorage = cookiesStorage,
        client = client,
    )

    override val commentFeature = object : CommentFeature() {
        override suspend fun getCommentImpl(mangaId: String, page: Int): List<Comment> {
            if (page > 0) return emptyList()
            val (galleryId, galleryToken) = mangaId.split(".", limit = 2)
            return api.getGalleryWithComments(galleryId, galleryToken)
                .select("div.gm div.c1")
                .map { c1 ->
                    Comment(
                        username = c1.select("div.c3 a").text(),
                        content = c1.select("div.c6").text(),
                        createTime = c1.select("div.c3").text()
                            .removePrefix("Posted on ")
                            .substringBefore(" by:")
                            .asDateTime("dd MMMM yyyy, HH:mm"),
                        vote = c1.select("div.c5 span").first()?.text()?.toInt(),
                    )
                }
        }
    }

    override suspend fun getBoardImpl(boardId: BoardId, key: String, filters: Parameters): MangaPage {
        val page = if (key.isEmpty()) 0 else key.toInt()
        return when (boardId) {
            BoardId.Main -> api.latest(
                page,
                keywords = filters.keywords(),
                genres = filters.set(R.genres).map { Api.latestGenres[it] }.toSet(),
                searchGalleryName = filters.boolean(R.searchGalleryName, true),
                searchGalleryTags = filters.boolean(R.searchGalleryTags, true),
                searchGalleryDescription = filters.boolean(R.searchGalleryDescription),
                searchExpungedGalleries = filters.boolean(R.searchExpungedGalleries),
                onlyShowGalleriesWithTorrents = filters.boolean(R.onlyShowGalleriesWithTorrents),
                searchLowPowerTags = filters.boolean(R.searchLowPowerTags),
                searchDownvotedTags = filters.boolean(R.searchDownvotedTags),
                minimumRating = Api.latestRatings[filters.int(R.minimumRating)],
                minimumPages = filters.string(R.minimumPages),
                maximumPages = filters.string(R.maximumPages),
            )

            BoardId.Rank -> {
                val type = filters.int("Type")
                if (type == 0) {
                    if (page > 0) return MangaPage(list = emptyList())
                    else api.popular()
                } else {
                    api.toplist(page, Api.toplistTypes[type])
                }
            }

            else -> throw Error("board not support")
        }.let { doc ->
            MangaPage(
                list = doc.parseMangaList(),
                nextKey = (page + 1).toString(),
            )
        }
    }

    override suspend fun getMangaImpl(mangaId: String): MangaDetail {
        val (galleryId, galleryToken) = mangaId.split(".", limit = 2)
        return api.getGallery(galleryId, galleryToken)
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
                            "posted" -> posted = value.asDateTime("yyyy-MM-dd HH:mm")
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

                MangaDetail(
                    id = mangaId,

                    cover = metadata.cover,
                    updateTime = metadata.posted,
                    title = metadata.title ?: metadata.altTitle,
                    authors = metadata.tags[TagKeyArtist] ?: emptyList(),
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

                    collections = mapOf("" to listOf(Chapter(""))),
                    chapterPreviews = doc.parseImageUrls(),
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

    protected fun Document.parseMangaList(): List<Manga> =
        select("table.itg td.glname")
            .mapNotNull { it.parent() }
            .map { tr ->
                val a = tr.select("td.gl3c a")
                Manga(
                    id = getId(a.attr("href")),
                    cover = tr.select(".glthumb img").let {
                        it.attr("data-src").ifBlank { null }
                            ?: it.attr("src").ifBlank { null }
                    },
                    updateTime = tr.select("td.gl2c div[onclick]").lastOrNull()?.text()
                        ?.asDateTime("yyyy-MM-dd HH:mm"),
                    title = a.select(".glink").text(),
                    authors = a.select(".gt")
                        .map { it.attr("title") }
                        .filter { it.startsWith("$TagKeyArtist:") }
                        .map { it.substringAfter("$TagKeyArtist:") },
                    isFinished = true,
                )
            }

    companion object {
        private fun getId(url: String): String {
            val pathSegments =
                (if (url.startsWith("http")) Url(url).pathSegments
                else url.split('/'))
                    .filterNot(String::isNullOrBlank)

            val galleryId = pathSegments[1]
            val galleryToken = pathSegments[2]
            return "$galleryId.$galleryToken"
        }
    }
}

class EHentai : EHentaiBase(false) {
    override val id: String = "E-Hentai"
}

class ExHentai : EHentaiBase(true) {
    override val id: String = "ExHentai"

    override val boardModel: Map<BoardId, BoardModel> = super.boardModel.mapValues {
        if (it.key == BoardId.Main) {
            BoardModel(
                hasSearchBar = true,
                advance = mapOf(
                    R.genres to FilterModel.MultipleSelect(Api.latestGenres.map { it.name }),
                    R.browseExpungedGalleries to FilterModel.Switch(),
                    R.requireGalleryTorrent to FilterModel.Switch(),
                    R.searchLowPowerTags to FilterModel.Switch(),
                    R.minimumRating to FilterModel.Select(Api.latestRatings.map { it.name }),
                    R.minimumPages to FilterModel.Text,
                    R.maximumPages to FilterModel.Text,
                )
            )
        } else {
            it.value
        }
    }

    override val loginFeature = object : LoginFeature() {
        override suspend fun isLogged() = api.isLogged()
        override suspend fun logout() = api.logout()

        override val cookiesLogin = object : CookiesLogin {
            override val loginSite = Api.loginUrl
            override val cookieNames = listOf(Api.ipb_member_id, Api.ipb_pass_hash)
            override suspend fun login(cookies: Map<String, String>): Boolean {
                return api.loginByCookies(
                    ipb_member_id = cookies[Api.ipb_member_id]!!,
                    ipb_pass_hash = cookies[Api.ipb_pass_hash]!!,
                )
            }
        }
    }

    override suspend fun getBoardImpl(boardId: BoardId, key: String, filters: Parameters): MangaPage {
        return when (boardId) {
            BoardId.Main -> api.latestNewVersion(
                key,
                keywords = filters.keywords(),
                genres = filters.set(R.genres).map { Api.latestGenres[it] }.toSet(),
                browseExpungedGalleries = filters.boolean(R.browseExpungedGalleries),
                searchLowPowerTags = filters.boolean(R.searchLowPowerTags),
                requireGalleryTorrent = filters.boolean(R.requireGalleryTorrent),
                minimumRating = Api.latestRatings[filters.int(R.minimumRating)],
                minimumPages = filters.string(R.minimumPages),
                maximumPages = filters.string(R.maximumPages),
            ).let { doc ->
                val list = doc.parseMangaList()
                MangaPage(
                    list = list,
                    nextKey = list.lastOrNull()?.id?.substringBefore('.'),
                )
            }

            else -> super.getBoardImpl(boardId, key, filters)
        }
    }
}

private object R {
    const val genres = "Genres"
    const val searchGalleryName = "Search Gallery Name"
    const val searchGalleryTags = "Search Gallery Tags"
    const val searchGalleryDescription = "Search Gallery Description"
    const val searchExpungedGalleries = "Search Expunged Galleries"
    const val onlyShowGalleriesWithTorrents = "Only Show Galleries With Torrents"
    const val searchLowPowerTags = "Search Low-Power Tags"
    const val searchDownvotedTags = "Search Downvoted Tags"
    const val minimumRating = "Minimum Rating"
    const val minimumPages = "Minimum Pages"
    const val maximumPages = "Maximum Pages"

    // new api
    const val browseExpungedGalleries = "Browse Expunged Galleries"
    const val requireGalleryTorrent = "Require Gallery Torrent"
}
