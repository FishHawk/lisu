package me.fishhawk.lisu.model

import kotlinx.serialization.Serializable
import me.fishhawk.lisu.library.Library
import me.fishhawk.lisu.source.BoardId
import me.fishhawk.lisu.source.BoardModel
import me.fishhawk.lisu.source.Source

@Serializable
data class ProviderDto(
    val id: String,
    val lang: String,
    val boardModels: Map<BoardId, BoardModel>,
    val isLogged: Boolean? = null,
    val cookiesLogin: CookiesLoginDto? = null,
    val passwordLogin: Boolean = false,
) {
    companion object {
        suspend fun fromSource(source: Source): ProviderDto {
            return ProviderDto(
                id = source.id,
                lang = source.lang,
                boardModels = source.boardModel,
                isLogged = source.loginFeature?.isLogged(),
                cookiesLogin = source.loginFeature?.cookiesLogin?.let { CookiesLoginDto.fromCookiesLogin(it) },
                passwordLogin = source.loginFeature?.passwordLogin != null,
            )
        }

        fun fromLibrary(library: Library): ProviderDto {
            return ProviderDto(
                id = library.id,
                lang = Library.lang,
                boardModels = Library.boardModel
            )
        }
    }
}

@Serializable
data class CookiesLoginDto(
    val loginSite: String,
    val cookieNames: List<String>,
) {
    companion object {
        fun fromCookiesLogin(obj: Source.LoginFeature.CookiesLogin): CookiesLoginDto {
            return CookiesLoginDto(obj.loginSite, obj.cookieNames)
        }
    }
}