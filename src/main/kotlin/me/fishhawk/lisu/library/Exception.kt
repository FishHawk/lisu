package me.fishhawk.lisu.library

sealed class LibraryException(override val message: String) : Exception() {
    class LibraryIllegalId(id: String) :
        LibraryException("Library $id has illegal id.")

    class LibraryNotFound(id: String) :
        LibraryException("Library $id not found.")

    class LibraryCanNotCreate(id: String, cause: Throwable) :
        LibraryException("Can not create library $id because: ${cause.message}")

    class MangaIllegalId(id: String) :
        LibraryException("Manga $id has illegal id.")

    class MangaNotFound(id: String) :
        LibraryException("Manga $id not found.")

    class MangaCanNotCreate(id: String, cause: Throwable) :
        LibraryException("Can not create manga $id because: ${cause.message}")

    class ChapterIllegalId(collectionId: String, chapterId: String) :
        LibraryException("Chapter $collectionId/$chapterId has illegal id.")

    class ChapterNotFound(collectionId: String, chapterId: String) :
        LibraryException("Chapter $collectionId/$chapterId not found.")

    class ChapterCanNotCreate(collectionId: String, chapterId: String, cause: Throwable) :
        LibraryException("Can not create chapter $collectionId/$chapterId because: ${cause.message}")
}
