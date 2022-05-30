package me.fishhawk.lisu.source.bilibili

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.date.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.ZonedDateTime

class Api {
    private val cookiesStorage = AcceptAllCookiesStorage()
    private val client = HttpClient(CIO) {
        install(HttpCookies) {
            storage = cookiesStorage
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
    }

    private var isLogged = false

    suspend fun isLogged() =
        isLogged && cookiesStorage.get(Url("https://manga.bilibili.com")).any { it.name == SESSDATA }

    suspend fun login(sessdata: String): Boolean {
        cookiesStorage.addCookie(
            "https://manga.bilibili.com",
            Cookie(
                name = SESSDATA,
                value = sessdata,
                expires = GMTDate(ZonedDateTime.now().plusDays(30 * 6).toInstant().toEpochMilli()),
                domain = ".bilibili.com",
                path = "/",
                httpOnly = true,
            )
        )
        val status = client.get("https://api.bilibili.com/x/web-interface/nav").status
        return if (status == HttpStatusCode.OK) {
            isLogged = true
            true
        } else {
            isLogged = false
            false
        }
    }

    private suspend fun post(url: String, bodyBuilder: () -> Any) =
        client.post("https://manga.bilibili.com/twirp/comic.v1.Comic$url?device=pc&platform=web") {
            contentType(ContentType.Application.Json)
            setBody(bodyBuilder())
        }

    /**
     * 按关键词搜索
     * @param page - 页数，从0开始
     * @param keywords - 关键词
     */
    suspend fun search(page: Int, keywords: String) = post("/Search") {
        @Serializable
        data class SearchBody(val key_word: String, val page_num: Int, val page_size: Int)
        SearchBody(keywords, page + 1, 9)
    }


    /**
     * 漫画排行
     * @param type - @see homeHotType
     */
    suspend fun getHomeHot(type: Int) = post("/HomeHot") {
        @Serializable
        data class HomeHotBody(val type: Int)
        HomeHotBody(homeHotType[type].second)
    }

    /**
     * 漫画排行
     * @param type - @see homeFansType
     */
    suspend fun getHomeFans(type: Int) = post("/HomeFans") {
        @Serializable
        data class HomeFansBody(val type: Int, val last_month_offset: Int = 0, val last_week_offset: Int = 0)
        HomeFansBody(homeFansType[type].second)
    }

    /**
     * 漫画推荐
     * @param page - 页数，从0开始
     */
    suspend fun getHomeRecommend(page: Int) = post("/HomeRecommend") {
        @Serializable
        data class HomeRecommendBody(val page_num: Int, val seed: Int)
        HomeRecommendBody(page + 1, 0)
    }

    /**
     * 每日推送
     * @param page - 页数，从0开始
     * @param date - 日期，例如：'2020-09-23'
     */
    suspend fun getDailyPush(page: Int, date: String) = post("/GetDailyPush") {
        @Serializable
        data class DailyPushBody(val date: String, val page_num: Int, val page_size: Int)
        DailyPushBody(date, page + 1, 8)
    }

    /**
     * 漫画分类
     * @param page - 页数，从1开始
     * @param style - @see classStyle
     * @param area - @see classArea
     * @param isFinish - @see classIsFinish
     * @param isFree - @see classIsFree
     * @param order - @see classOrder
     */
    suspend fun getClassPage(
        page: Int,
        style: Int,
        area: Int,
        isFinish: Int,
        isFree: Int,
        order: Int
    ) = post("/ClassPage") {
        @Serializable
        data class ClassPageBody(
            val style_id: Int,
            val area_id: Int,
            val is_finish: Int,
            val is_free: Int,
            val order: Int,
            val page_num: Int,
            val page_size: Int
        )
        ClassPageBody(
            style_id = classStyle[style].second,
            area_id = classArea[area].second,
            is_finish = classIsFinish[isFinish].second,
            is_free = classIsFree[isFree].second,
            order = classOrder[order].second,
            page_num = page + 1,
            page_size = 18
        )
    }

    /**
     * 获取漫画详情
     * @param comicId - 漫画id
     */
    suspend fun getComicDetail(comicId: String) = post("/ComicDetail") {
        @Serializable
        data class ComicDetailBody(val comic_id: String)
        ComicDetailBody(comicId)
    }

    /**
     * 获取章节图片token
     * @param chapterId - 章节id
     */
    suspend fun getImageIndex(chapterId: String) = post("/GetImageIndex") {
        @Serializable
        data class ChapterIndexBody(val ep_id: String)
        ChapterIndexBody(chapterId)
    }


    /**
     * 获取章节图片token
     * @param pics - 图片url
     */
    suspend fun getImageToken(pics: List<String>) = post("/ImageToken") {
        @Serializable
        data class ChapterImageTokenBody(val urls: String)
        ChapterImageTokenBody(
            Json.encodeToString(ListSerializer(String.serializer()), pics)
        )
    }

    /**
     * 获取图片
     * @param url - 图片url
     */
    suspend fun getImage(url: String) = client.get(url)

    companion object {
        val SESSDATA = "SESSDATA"

        val homeHotType = arrayOf(
            "免费榜" to 1, // 前7日人气最高的免费漫画作品排行
            "飙升榜" to 2, // 前7日新增追漫数最多的漫画作品排行
            "日漫榜" to 3, // 前7日人气最高的日漫作品排行
            "国漫榜" to 4, // 前7日人气最高的国漫作品排行
            "新作榜" to 5, // 前7日人气最高的三个月内上线漫画作品排行
            "韩漫榜" to 6, // 前7日人气最高的韩漫作品排行
            "宝藏榜" to 8, // 前7日人气最高的官方精选漫画作品排行
        )

        val homeFansType = arrayOf(
            "投喂榜" to 0, // 每自然月累计粉丝值最高的漫画作品排行
            "月票榜" to 1, // 每自然月累计月票最高的漫画作品排行
        )

        val classStyle = arrayOf(
            "全部" to -1,
            "正能量" to 1028,
            "冒险" to 1013,
            "热血" to 999,
            "搞笑" to 994,
            "恋爱" to 995,
            "少女" to 1026,
            "纯爱" to 1022,
            "日常" to 1020,
            "校园" to 1001,
            "运动" to 1010,
            "治愈" to 1007,
            "橘味" to 1006,
            "古风" to 997,
            "玄幻" to 1016,
            "奇幻" to 998,
            "后宫" to 1017,
            "惊奇" to 996,
            "悬疑" to 1023,
            "都市" to 1002,
            "剧情" to 1030,
            "总裁" to 1004,
        )

        val classArea = arrayOf(
            "全部" to -1,
            "大陆" to 1,
            "日本" to 2,
            "韩国" to 6,
            "其他" to 5,
        )

        val classIsFinish = arrayOf(
            "全部" to -1,
            "连载" to 0,
            "完结" to 1,
        )

        val classIsFree = arrayOf(
            "全部" to -1,
            "免费" to 1,
            "付费" to 2,
            "等就免费" to 3,
        )

        val classOrder = arrayOf(
            "人气推荐" to 0,
            "更新时间" to 1,
            "追漫人数" to 2,
            "上架时间" to 3,
        )
    }
}