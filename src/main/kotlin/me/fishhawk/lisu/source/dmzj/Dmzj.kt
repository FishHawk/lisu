package me.fishhawk.lisu.source.dmzj

//class Dmzj : Source() {
//    override val id: String = "动漫之家"
//    override val lang: String = "zh"
//
//    override val boardModels: Map<BoardId, BoardModel> = mapOf(
//        BoardId.Ranking to mapOf("type" to emptyList<String>()),
//        BoardId.Latest to mapOf("type" to emptyList()),
//        BoardId.Category to mapOf(
//            "type" to emptyList(),
//            "status" to emptyList()
//        )
//    )
//
//    override suspend fun searchImpl(page: Int, keywords: String): List<MangaDto> {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun getBoardImpl(boardId: BoardId, page: Int, filters: Map<String, Int>): List<MangaDto> {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun getMangaImpl(mangaId: String): MangaDetailDto {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun getContentImpl(mangaId: String, chapterId: String): List<String> {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun getImageImpl(url: String): Image {
//        TODO("Not yet implemented")
//    }
//}