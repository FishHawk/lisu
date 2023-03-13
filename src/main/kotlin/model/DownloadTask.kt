package download.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MangaDownloadTask(
    val providerId: String,
    val mangaId: String,
    val cover: String?,
    val title: String?,
    val chapterTasks: MutableList<ChapterDownloadTask>,
) {
    override fun equals(other: Any?): Boolean {
        if (other !is MangaDownloadTask) return false
        return other.providerId == this.providerId && other.mangaId == this.mangaId
    }

    override fun hashCode(): Int {
        var result = providerId.hashCode()
        result = 31 * result + mangaId.hashCode()
        return result
    }
}

@Serializable
data class ChapterDownloadTask(
    val collectionId: String,
    val chapterId: String,
    val name: String? = null,
    val title: String? = null,
    var state: State = State.Waiting,
) {
    @Serializable
    sealed interface State {
        @Serializable
        @SerialName("waiting")
        object Waiting : State

        @Serializable
        @SerialName("downloading")
        data class Downloading(
            val downloadedPageNumber: Int?,
            val totalPageNumber: Int?,
        ) : State

        @Serializable
        @SerialName("failed")
        data class Failed(
            val downloadedPageNumber: Int?,
            val totalPageNumber: Int?,
            val errorMessage: String,
        ) : State
    }
}