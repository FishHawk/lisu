package me.fishhawk.lisu.source.manhuaren

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Api {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
        defaultRequest {
            header("Referer", "http://www.dm5.com/dm5api/")
            header("clubReferer", "http://mangaapi.manhuaren.com/")
            header("X-Yq-Yqci", "{\"le\": \"zh\"}")
            header("User-Agent", "okhttp/3.11.0")
        }
    }

    /**
     * 按关键词搜索
     * @param page - 页数，从0开始
     * @param keywords - 关键词
     */
    suspend fun getSearchManga(page: Int, keywords: String) = get(
        "/v1/search/getSearchManga",
        createPageParam(page) + ("keywords" to keywords)
    )

    /**
     * 漫画排行
     * @param page - 页数，从0开始
     * @param type - @see rankType
     */
    suspend fun getRank(page: Int, type: Int) = get(
        "/v1/manga/getRank",
        createPageParam(page) + ("sortType" to rankTypes[type].sortType)
    )

    /**
     * 最新更新
     * @param page - 页数，从0开始
     */
    suspend fun getUpdate(page: Int) = get(
        "/v1/manga/getUpdate",
        createPageParam(page)
    )

    /**
     * 最新发布
     * @param page - 页数，从0开始
     */
    suspend fun getRelease(page: Int) = get(
        "/v1/manga/getRelease",
        createPageParam(page)
    )

    /**
     * 漫画分类
     * @param page - 页数，从0开始
     * @param type - @see categoryType
     * @param status - @see categorySort
     */
    suspend fun getCategoryMangas(page: Int, type: Int, status: Int) = get(
        "/v2/manga/getCategoryMangas",
        createPageParam(page) + mapOf(
            "subCategoryId" to categoryTypes[type].subId,
            "subCategoryType" to categoryTypes[type].subType,
            "sort" to categoryStatuses[status].sort
        )
    )

    /**
     * 获取漫画详情
     * @param mangaId - 漫画id
     */
    suspend fun getDetail(mangaId: String) = get(
        "/v1/manga/getDetail", mapOf("mangaId" to mangaId)
    )

    /**
     * 获取章节内容
     * @param chapterId - 章节id
     */
    suspend fun getRead(chapterId: String) = get(
        "/v1/manga/getRead",
        mapOf(
            "mangaSectionId" to chapterId,
            "netType" to "4",
            "loadreal" to "1",
            "imageQuality" to "2",
        )
    )

    /**
     * 获取图片
     * @param url - 图片url
     */
    suspend fun getImage(url: String) = client.get(url)

    private suspend fun get(url: String, parameters: Map<String, String> = emptyMap()) =
        client.get("http://mangaapi.manhuaren.com$url") {
            parameters.addExtraParam().forEach { (key, value) -> parameter(key, value) }
        }

    private fun Map<String, String>.generateGSNHash(): String {
        val c = "4e0a48e1c0b54041bce9c8f0e036124d"
        var s = c + "GET"
        toSortedMap().forEach { (key, value) ->
            if (key != "gsn") {
                s += key
                s += URLEncoder.encode(value, "UTF-8")
                    .replace("+", "%20")
                    .replace("%7E", "~")
                    .replace("*", "%2A")
            }
        }
        s += c

        return MessageDigest
            .getInstance("MD5")
            .digest(s.toByteArray())
            .joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
    }

    private fun Map<String, String>.addExtraParam(): Map<String, String> {
        val params = this + mapOf(
            "gsm" to "md5",
            "gft" to "json",
            "gts" to LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYY-MM-DD+HH:mm:ss")),
            "gak" to "android_manhuaren2",
            "gat" to "",
            "gaui" to "191909801",
            "gui" to "191909801",
            "gut" to "0"
        )
        val gsn = params.generateGSNHash()
        return params + ("gsn" to gsn)
    }

    private fun createPageParam(page: Int): MutableMap<String, String> {
        val pageSize = 20
        return mutableMapOf(
            "start" to (pageSize * page).toString(),
            "limit" to pageSize.toString()
        )
    }

    companion object {
        data class RankType(val name: String, val sortType: String)

        val rankTypes = arrayOf(
            RankType("人气榜", "0"),
            RankType("新番榜", "1"),
            RankType("收藏榜", "2"),
            RankType("吐槽榜", "3"),
        )

        data class CategoryType(val name: String, val subType: String, val subId: String)

        val categoryTypes = arrayOf(
            CategoryType("全部", "0", "0"),
            CategoryType("热血", "0", "31"),
            CategoryType("恋爱", "0", "26"),
            CategoryType("校园", "0", "1"),
            CategoryType("百合", "0", "3"),
            CategoryType("耽美", "0", "27"),
            CategoryType("伪娘", "0", "5"),
            CategoryType("冒险", "0", "2"),
            CategoryType("职场", "0", "6"),
            CategoryType("后宫", "0", "8"),
            CategoryType("治愈", "0", "9"),
            CategoryType("科幻", "0", "25"),
            CategoryType("励志", "0", "10"),
            CategoryType("生活", "0", "11"),
            CategoryType("战争", "0", "12"),
            CategoryType("悬疑", "0", "17"),
            CategoryType("推理", "0", "33"),
            CategoryType("搞笑", "0", "37"),
            CategoryType("奇幻", "0", "14"),
            CategoryType("魔法", "0", "15"),
            CategoryType("恐怖", "0", "29"),
            CategoryType("神鬼", "0", "20"),
            CategoryType("萌系", "0", "21"),
            CategoryType("历史", "0", "4"),
            CategoryType("美食", "0", "7"),
            CategoryType("同人", "0", "30"),
            CategoryType("运动", "0", "34"),
            CategoryType("绅士", "0", "36"),
            CategoryType("机甲", "0", "40"),
            CategoryType("限制级", "0", "61"),
            CategoryType("少年向", "1", "1"),
            CategoryType("少女向", "1", "2"),
            CategoryType("青年向", "1", "3"),
            CategoryType("港台", "2", "35"),
            CategoryType("日韩", "2", "36"),
            CategoryType("大陆", "2", "37"),
            CategoryType("欧美", "2", "52"),
        )

        data class CategoryStatus(val name: String, val sort: String)

        val categoryStatuses = arrayOf(
            CategoryStatus("热门", "0"),
            CategoryStatus("更新", "1"),
            CategoryStatus("新作", "2"),
            CategoryStatus("完结", "3"),
        )
    }
}