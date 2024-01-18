package it.vfsfitvnm.innertube.models

import io.ktor.client.request.headers
import io.ktor.http.HttpMessageBuilder
import io.ktor.http.userAgent
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class Context(
    val client: Client,
    val thirdParty: ThirdParty? = null
) {
    @Serializable
    data class Client(
        val clientName: String,
        val clientVersion: String,
        val platform: String,
        val hl: String = "en",
        val gl: String = "US",
        val visitorData: String = DEFAULT_VISITOR_DATA,
        val androidSdkVersion: Int? = null,
        val userAgent: String? = null,
        val referer: String? = null
    )

    @Serializable
    data class ThirdParty(
        val embedUrl: String
    )

    context(HttpMessageBuilder)
    fun apply() {
        client.userAgent?.let { userAgent(it) }

        headers {
            client.referer?.let { append("Referer", it) }
            append("X-Youtube-Bootstrap-Logged-In", "false")
            append("X-YouTube-Client-Name", client.clientName)
            append("X-YouTube-Client-Version", client.clientVersion)
        }
    }

    companion object {
        const val DEFAULT_VISITOR_DATA = "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D"

        val DefaultWeb get() = DefaultWebNoLang.let {
            it.copy(
                client = it.client.copy(
                    hl = Locale.getDefault().toLanguageTag(),
                    gl = Locale.getDefault().country
                )
            )
        }

        val DefaultWebNoLang = Context(
            client = Client(
                clientName = "WEB_REMIX",
                clientVersion = "1.20220606.03.00",
                platform = "DESKTOP",
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.157 Safari/537.36",
                referer = "https://music.youtube.com/"
            )
        )

        val DefaultAndroid = Context(
            client = Client(
                clientName = "ANDROID_MUSIC",
                clientVersion = "5.28.1",
                platform = "MOBILE",
                androidSdkVersion = 30,
                userAgent = "com.google.android.apps.youtube.music/5.28.1 (Linux; U; Android 11) gzip"
            )
        )

        val DefaultAgeRestrictionBypass = Context(
            client = Client(
                clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
                clientVersion = "2.0",
                platform = "TV",
                userAgent = "Mozilla/5.0 (PlayStation 4 5.55) AppleWebKit/601.2 (KHTML, like Gecko)"
            )
        )
    }
}
