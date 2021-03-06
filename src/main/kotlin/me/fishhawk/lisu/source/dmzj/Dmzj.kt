package me.fishhawk.lisu.source.dmzj

import me.fishhawk.lisu.model.Image
import me.fishhawk.lisu.model.CommentDto
import me.fishhawk.lisu.model.MangaDetailDto
import me.fishhawk.lisu.model.MangaDto
import me.fishhawk.lisu.source.BoardModel
import me.fishhawk.lisu.source.Source

class Dmzj : Source() {
    override val id: String = "动漫之家"
    override val lang: String = "zh"
    override val boardModels: Map<String, BoardModel> = mapOf(
        "热门" to mapOf("type" to emptyList()),
        "最新" to mapOf("type" to emptyList()),
        "分类" to mapOf(
            "type" to emptyList(),
            "status" to emptyList()
        )
    )

    override suspend fun searchImpl(page: Int, keywords: String): List<MangaDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getBoardImpl(boardId: String, page: Int, filters: Map<String, Int>): List<MangaDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getMangaImpl(mangaId: String): MangaDetailDto {
        TODO("Not yet implemented")
    }

    override suspend fun getContentImpl(mangaId: String, chapterId: String): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun getImageImpl(url: String): Image {
        TODO("Not yet implemented")
    }
}