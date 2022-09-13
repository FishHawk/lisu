package me.fishhawk.lisu.source.ehentai

import com.tfowl.ktor.client.features.JsoupPlugin
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.*
import io.ktor.client.request.*
import org.jsoup.nodes.Document

class Api {
    private val client = HttpClient(Java) {
        install(JsoupPlugin)
        expectSuccess = true
    }

    suspend fun latest(
        page: Int,
        keywords: String,
        genres: Set<LatestGenre>,
        searchGalleryName: Boolean,
        searchGalleryTags: Boolean,
        searchGalleryDescription: Boolean,
        searchExpungedGalleries: Boolean,
        onlyShowGalleriesWithTorrents: Boolean,
        searchLowPowerTags: Boolean,
        searchDownvotedTags: Boolean,
        minimumRating: LatestRating,
        minimumPages: String,
        maximumPages: String,
    ) =
        client.get("https://e-hentai.org/") {
            parameter("page", page)
            if (keywords.isNotBlank()) parameter("f_search", keywords)
            if (genres.isNotEmpty()) parameter("f_cats", 1023 - genres.sumOf { it.mask })
            if (searchGalleryName) parameter("f_sname", "on")
            if (searchGalleryTags) parameter("f_stags", "on")
            if (searchGalleryDescription) parameter("f_sdesc", "on")
            if (searchExpungedGalleries) parameter("f_sh", "on")
            if (onlyShowGalleriesWithTorrents) parameter("f_sto", "on")
            if (searchLowPowerTags) parameter("f_sdt1", "on")
            if (searchDownvotedTags) parameter("f_sdt2", "on")
            if (minimumRating.star > 0) {
                parameter("f_sr", "on")
                parameter("f_srdd", minimumRating.star.toString())
            }
            minimumPages.toIntOrNull()?.let { parameter("f_spf", it) }
            maximumPages.toIntOrNull()?.let { parameter("f_spt", it) }
        }.body<Document>()

    suspend fun popular() =
        client.get("https://e-hentai.org/popular").body<Document>()

    suspend fun toplist(page: Int, type: ToplistType) =
        client.get("https://e-hentai.org/toplist.php") {
            parameter("p", page)
            parameter("tl", type.type)
        }.body<Document>()

    suspend fun getGallery(id: String, token: String, page: Int = 0) =
        client.get("https://e-hentai.org/g/$id/$token/") { parameter("p", page) }.body<Document>()

    suspend fun getPage(url: String) =
        client.get(url).body<Document>()

    suspend fun getImage(url: String) =
        client.get(url)

    companion object {
        data class ToplistType(val name: String, val type: String)

        val toplistTypes = arrayOf(
            ToplistType("All-Time", "11"),
            ToplistType("Past Year", "12"),
            ToplistType("Past Month", "13"),
            ToplistType("Yesterday", "15"),
        )

        data class LatestGenre(val name: String, val mask: Int)

        val latestGenres = arrayOf(
            LatestGenre("Doujinshi", 2),
            LatestGenre("Manga", 4),
            LatestGenre("Artist CG", 8),
            LatestGenre("Game CG", 16),
            LatestGenre("Western", 512),
            LatestGenre("Non-H", 256),
            LatestGenre("Image Set", 32),
            LatestGenre("Cosplay", 64),
            LatestGenre("Asian Porn", 128),
            LatestGenre("Misc", 1),
        )

        data class LatestRating(val name: String, val star: Int)

        val latestRatings = arrayOf(
            LatestRating("Any", 0),
            LatestRating("2 stars", 2),
            LatestRating("3 stars", 3),
            LatestRating("4 stars", 4),
            LatestRating("5 stars", 5),
        )
    }
}
