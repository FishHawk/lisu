package me.fishhawk.lisu.source.ehentai

internal const val TagKeyArtist = "artist"

internal class GalleryMetadata {
    var cover: String? = null

    var title: String? = null
    var altTitle: String? = null
    var genre: String? = null
    var uploader: String? = null

    var posted: Long? = null
    var length: Int? = null
    var favorites: Int? = null
    var ratingCount: Int? = null
    var rating: Double? = null

    val tags: MutableMap<String, List<String>> = mutableMapOf()
}