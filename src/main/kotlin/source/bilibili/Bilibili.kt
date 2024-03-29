package source.bilibili

import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import library.model.Chapter
import library.model.Manga
import library.model.MangaDetail
import library.model.MangaPage
import util.Image
import source.*
import source.asTimestamp
import source.jsonArrayOrNull
import source.jsonObjectOrNull

class Bilibili : Source() {
    override val id: String = "哔哩哔哩漫画"
    override val lang: String = "zh"

    override val boardModel: Map<BoardId, BoardModel> = mapOf(
        BoardId.Main to BoardModel(
            base = mapOf(
                "题材" to FilterModel.Select(Api.classStyle.map { it.first }),
                "地区" to FilterModel.Select(Api.classArea.map { it.first }),
                "进度" to FilterModel.Select(Api.classIsFinish.map { it.first }),
                "收费" to FilterModel.Select(Api.classIsFree.map { it.first }),
                "排序" to FilterModel.Select(Api.classOrder.map { it.first }),
            )
        ),
        BoardId.Rank to BoardModel(
            base = mapOf(
                "榜单" to FilterModel.Select(Api.homeHotType.map { it.first })
            )
        ),
        BoardId.Search to BoardModel(),
    )

    private val api = Api(
        cookiesStorage = cookiesStorage,
        client = client,
    )

    override val loginFeature = object : LoginFeature() {
        override suspend fun isLogged() = api.isLogged()
        override suspend fun logout() = api.logout()

        override val cookiesLogin = object : CookiesLogin {
            override val loginSite = Api.baseUrl
            override val cookieNames = listOf(Api.SESSDATA)
            override suspend fun login(cookies: Map<String, String>): Boolean {
                return cookies[Api.SESSDATA]?.let { api.login(it) } ?: false
            }
        }
    }

    override val commentFeature = object : CommentFeature() {
        override suspend fun getCommentImpl(mangaId: String, page: Int): List<Comment> {
            return api.getReply(mangaId, page = page + 1, sort = 2)
                .body<JsonObject>()
                .let { it["data"]!!.jsonObject }
                .let { obj ->
                    fun parseComment(reply: JsonObject): Comment {
                        return Comment(
                            username = reply["member"]!!.jsonObject["uname"]!!.jsonPrimitive.content,
                            content = reply["content"]!!.jsonObject["message"]!!.jsonPrimitive.content,
                            createTime = reply["ctime"]!!.jsonPrimitive.long.asTimestamp(),
                            vote = reply["like"]!!.jsonPrimitive.int,
                            subComments = reply["replies"]!!.jsonArrayOrNull?.obj?.map { parseComment(it) },
                        )
                    }

                    val upper = obj["upper"]!!.jsonObject["top"]!!.jsonObjectOrNull
                        ?.let { listOf(parseComment(it)) } ?: emptyList()
                    val replies = obj["replies"]!!.jsonArrayOrNull?.obj
                        ?.map { parseComment(it) } ?: emptyList()
                    upper + replies
                }
        }
    }

    override suspend fun getBoardImpl(boardId: BoardId, key: String, filters: Parameters): MangaPage {
        val page = if (key.isEmpty()) 0 else key.toInt()
        return when (boardId) {
            BoardId.Main ->
                api.getClassPage(
                    page,
                    filters.int("题材"),
                    filters.int("地区"),
                    filters.int("进度"),
                    filters.int("收费"),
                    filters.int("排序"),
                ).body<JsonObject>()
                    .let { it["data"]!!.jsonArray.obj }
                    .map { obj ->
                        Manga(
                            id = obj["season_id"]!!.jsonPrimitive.content,
                            cover = obj["vertical_cover"]?.jsonPrimitive?.content,
                            title = obj["title"]?.jsonPrimitive?.content,
                            isFinished = obj["is_finish"]?.asMangaIsFinish(),
                        )
                    }

            BoardId.Rank ->
                if (page > 0) emptyList()
                else api.getHomeHot(filters.int("榜单"))
                    .body<JsonObject>()
                    .let { it["data"]!!.jsonArray.obj }
                    .map { obj ->
                        Manga(
                            id = obj["comic_id"]!!.jsonPrimitive.content,
                            cover = obj["vertical_cover"]?.jsonPrimitive?.content,
                            title = obj["title"]?.jsonPrimitive?.content,
                            authors = obj["author"]?.jsonArray?.string ?: emptyList(),
                            isFinished = obj["is_finish"]?.asMangaIsFinish()
                        )
                    }

            BoardId.Search -> {
                api.search(page, filters.keywords()).body<JsonObject>().let { json ->
                    json["data"]!!.jsonObject["list"]!!.jsonArray.map { it.jsonObject }.map { obj ->
                        Manga(
                            id = obj["id"]!!.jsonPrimitive.content,
                            cover = obj["vertical_cover"]?.jsonPrimitive?.content,
                            title = obj["org_title"]?.jsonPrimitive?.content,
                            authors = obj["author_name"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                            isFinished = obj["is_finish"]?.asMangaIsFinish()
                        )
                    }
                }
            }
        }.let {
            MangaPage(
                list = it,
                nextKey = (page + 1).toString(),
            )
        }
//            api.getDailyPush(
//                page,
//                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
//            ).body<JsonObject>()
//                .let { it["data"]!!.jsonObject["list"]!!.jsonArray.obj }
//                .map { obj ->
//                    Manga(
//                        providerId = id,
//                        id = obj["comic_id"]!!.jsonPrimitive.content,
//                        cover = obj["vertical_cover"]?.jsonPrimitive?.content,
//                        title = obj["title"]?.jsonPrimitive?.content,
//                    )
//                }
    }

    override suspend fun getMangaImpl(mangaId: String): MangaDetail {
        return api.getComicDetail(mangaId)
            .body<JsonObject>()
            .let { it["data"]!!.jsonObject }
            .let { obj ->
                MangaDetail(
                    id = obj["id"]!!.jsonPrimitive.content,

                    cover = obj["vertical_cover"]!!.jsonPrimitive.content,
                    title = obj["title"]!!.jsonPrimitive.content,
                    authors = obj["author_name"]!!.jsonArray.string,
                    isFinished = obj["is_finish"]!!.asMangaIsFinish(),

                    description = obj["evaluate"]!!.jsonPrimitive.content,
                    tags = mapOf("" to obj["styles"]!!.jsonArray.string),

                    collections = mapOf(
                        "" to obj["ep_list"]!!.jsonArray.obj.map {
                            Chapter(
                                id = it["id"]!!.jsonPrimitive.content,
                                name = it["short_title"]!!.jsonPrimitive.content,
                                title = it["title"]!!.jsonPrimitive.content,
                                isLocked = it["is_locked"]?.jsonPrimitive?.boolean ?: false,
                                updateTime = it["pub_time"]?.jsonPrimitive?.content?.asDateTime("yyyy-MM-dd HH:mm:ss")
                            )
                        }.reversed()
                    ),
                )
            }
    }

    override suspend fun getContentImpl(mangaId: String, chapterId: String): List<String> {
        return api.getImageIndex(chapterId)
            .body<JsonObject>()
            .let { it["data"]!!.jsonObject }
            .let { obj -> obj["images"]!!.jsonArray.obj.map { it["path"]!!.jsonPrimitive.content } }
            .let { images -> api.getImageToken(images) }
            .body<JsonObject>()
            .let { it["data"]!!.jsonArray.obj }
            .map {
                val url = it["url"]!!.jsonPrimitive.content
                val token = it["token"]!!.jsonPrimitive.content
                "$url?token=$token"
            }
    }


    override suspend fun getImageImpl(url: String) = withContext(Dispatchers.IO) {
        api.getImage(url).let {
            Image(
                it.contentType(),
                it.body<ByteReadChannel>().toInputStream()
            )
        }
    }
}

private fun JsonElement.asMangaIsFinish() =
    when (jsonPrimitive.int) {
        0 -> false
        1 -> true
        else -> null
    }