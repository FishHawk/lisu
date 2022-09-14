package me.fishhawk.lisu.source.ehentai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.date.*
import org.jsoup.nodes.Document
import java.time.ZonedDateTime

class Api(
    enableExHentai: Boolean,
    private val cookiesStorage: CookiesStorage,
    private val client: HttpClient,
) {
    private val domain = if (enableExHentai) ".exhentai.org" else ".e-hentai.org"
    private val baseUrl = if (enableExHentai) exHentaiBaseUrl else eHentaiBaseUrl

    suspend fun isLogged() =
        cookiesStorage.get(Url(baseUrl)).any { it.name == ipb_pass_hash }

    suspend fun logout() {
        cookiesStorage.addCookie(baseUrl, Cookie(name = ipb_member_id, value = "", expires = GMTDate.START))
        cookiesStorage.addCookie(baseUrl, Cookie(name = ipb_pass_hash, value = "", expires = GMTDate.START))
        cookiesStorage.addCookie(baseUrl, Cookie(name = igneous, value = "", expires = GMTDate.START))
    }

    suspend fun login(
        ipb_member_id: String,
        ipb_pass_hash: String,
        igneous: String
    ): Boolean {
        listOf(
            Api.ipb_member_id to ipb_member_id,
            Api.ipb_pass_hash to ipb_pass_hash,
            Api.igneous to igneous,
        ).forEach { (name, value) ->
            cookiesStorage.addCookie(
                baseUrl,
                Cookie(
                    name = name,
                    value = value,
                    expires = GMTDate(ZonedDateTime.now().plusDays(30 * 12).toInstant().toEpochMilli()),
                    domain = domain,
                    path = "/",
                    httpOnly = true,
                )
            )
        }
        return true
//        val status = client.get(baseUrl).status
//        val isSuccess = status == HttpStatusCode.OK
//        if (!isSuccess) logout()
//        return isSuccess
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
        client.get(baseUrl) {
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
        client.get("${baseUrl}popular").body<Document>()

    suspend fun toplist(page: Int, type: ToplistType) =
        client.get("${eHentaiBaseUrl}toplist.php") {
            parameter("p", page)
            parameter("tl", type.type)
        }.body<Document>()

    suspend fun getGallery(id: String, token: String, page: Int = 0) =
        client.get("${baseUrl}g/$id/$token/") { parameter("p", page) }.body<Document>()

    suspend fun getGalleryWithComments(id: String, token: String) =
        client.get("${baseUrl}g/$id/$token/?hc=1#comments").body<Document>()

    suspend fun getPage(url: String) =
        client.get(url).body<Document>()

    suspend fun getImage(url: String) =
        client.get(url)

    companion object {
        const val eHentaiBaseUrl = "https://e-hentai.org/"
        const val exHentaiBaseUrl = "https://exhentai.org/"
        const val loginUrl = "https://forums.e-hentai.org/index.php?act=Login&CODE=00"
        const val ipb_member_id = "ipb_member_id"
        const val ipb_pass_hash = "ipb_pass_hash"
        const val igneous = "igneous"

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
