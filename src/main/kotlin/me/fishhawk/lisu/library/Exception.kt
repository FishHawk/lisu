package me.fishhawk.lisu.library

sealed class LibraryException(override val message: String) : Exception() {
    class LibraryIllegalId(id: String) :
        LibraryException("Library $id has illegal id.")

    class LibraryNotFound(id: String) :
        LibraryException("Library $id not found.")

    class MangaIllegalId(id: String) :
        LibraryException("Manga $id has illegal id.")

    class MangaNotFound(id: String) :
        LibraryException("Manga $id not found.")

    class ChapterIllegalId(collectionId: String, chapterId: String) :
        LibraryException("Chapter $collectionId/$chapterId has illegal id.")

    class ChapterNotFound(collectionId: String, chapterId: String) :
        LibraryException("Chapter $collectionId/$chapterId not found.")
}
