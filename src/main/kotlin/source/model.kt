package source

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import util.LocalDateTimeSerializer
import java.time.LocalDateTime

enum class BoardId { Main, Rank, Search }

@Serializable
sealed interface FilterModel {
    @Serializable
    @SerialName("Text")
    object Text : FilterModel

    @Serializable
    @SerialName("Switch")
    data class Switch(val default: Boolean = false) : FilterModel

    @Serializable
    @SerialName("Select")
    data class Select(val options: List<String>) : FilterModel

    @Serializable
    @SerialName("MultipleSelect")
    data class MultipleSelect(val options: List<String>) : FilterModel
}

@Serializable
data class BoardModel(
    val hasSearchBar: Boolean = false,
    val base: Map<String, FilterModel> = emptyMap(),
    val advance: Map<String, FilterModel> = emptyMap(),
)

@Serializable
data class Comment(
    val username: String,
    val content: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createTime: LocalDateTime? = null,
    val vote: Int? = null,
    val subComments: List<Comment>? = null,
)
