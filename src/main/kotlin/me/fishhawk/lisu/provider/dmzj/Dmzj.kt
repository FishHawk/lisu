package me.fishhawk.lisu.provider.dmzj

import io.ktor.client.statement.*
import me.fishhawk.lisu.model.MangaDetailDto
import me.fishhawk.lisu.model.MangaDto
import me.fishhawk.lisu.provider.BoardModel
import me.fishhawk.lisu.provider.Provider
import java.net.URL

class Dmzj : Provider {
    override val id: String = "动漫之家"
    override val lang: String = "zh"
    override val icon: URL? = this::class.java.getResource("icon.png")
    override val boardModels: Map<String, BoardModel> = mapOf(
        "热门" to mapOf("type" to emptyList()),
        "最新" to mapOf("type" to emptyList()),
        "分类" to mapOf(
            "type" to emptyList(),
            "status" to emptyList()
        )
    )

    override suspend fun search(page: Int, keywords: String): List<MangaDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getBoard(boardId: String, page: Int, filters: Map<String, Int>): List<MangaDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getManga(mangaId: String): MangaDetailDto {
        TODO("Not yet implemented")
    }

    override suspend fun getContent(mangaId: String, collectionId: String, chapterId: String): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun getImage(url: String): HttpResponse {
        TODO("Not yet implemented")
    }
}