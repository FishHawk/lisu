package me.fishhawk.lisu.source.dmzj

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.JsonObject

class Api {
    private val client = HttpClient(CIO) {
        Json {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                prettyPrint = true
                isLenient = true
            })
        }
        defaultRequest {
            header("Referer", "http://www.dmzj.com/")
            header(
                "User-Agent",
                "Mozilla/5.0 (X11; Linux x86_64) " +
                        "Mozilla/5.0 (Linux; Android 10) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/88.0.4324.93 " +
                        "Mobile Safari/537.36 " +
                        "Tachiyomi/1.0"
            )
        }
    }

    private suspend fun get(url: String, parameters: MutableMap<String, String>) =
        client.request<HttpResponse>("http://mangaapi.manhuaren.com$url").receive<JsonObject>()

    /**
     * 按关键词搜索
     * @param page - 页数，从0开始
     * @param keywords - 关键词
     */
    suspend fun search(page: Int, keywords: String): HttpResponse {
        return client.request("http://v3api.dmzj.com/search/show/0/${keywords}/${page}.json")
    }

    /**
     * 漫画排行
     * @param page - 页数，从0开始
     * @param type - @see rankType
     * @param range - @see rankRange
     */
    suspend fun getRank(page: Int, type: String, range: String) {
    }
}
