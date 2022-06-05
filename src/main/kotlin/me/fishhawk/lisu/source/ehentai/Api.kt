package me.fishhawk.lisu.source.ehentai

import com.tfowl.ktor.client.features.JsoupPlugin
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import org.jsoup.nodes.Document

class Api {
    private val client = HttpClient(CIO) {
        install(JsoupPlugin)
        expectSuccess = true
    }
//    fun popular(page: Int) =
//        exGet("$baseUrl/?f_search=${languageTag()}&f_srdd=5&f_sr=on", page)

    suspend fun popular() =
        client.get("https://e-hentai.org/popular").body<Document>()

    suspend fun latest(page: Int) =
        client.get("https://e-hentai.org/") { parameter("page", page) }.body<Document>()

    suspend fun toplist(page: Int, type: Int) =
        client.get("https://e-hentai.org/toplist.php") {
            parameter("p", page)
            parameter("tl", toplistTypes[type].type)
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
    }
}
