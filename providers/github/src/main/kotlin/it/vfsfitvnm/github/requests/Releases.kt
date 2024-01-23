package it.vfsfitvnm.github.requests

import io.ktor.client.call.body
import io.ktor.client.request.get
import it.vfsfitvnm.extensions.runCatchingCancellable
import it.vfsfitvnm.github.GitHub
import it.vfsfitvnm.github.models.Release

suspend fun GitHub.releases(
    owner: String,
    repo: String,
    page: Int = 1,
    pageSize: Int = 30
) = runCatchingCancellable {
    httpClient.get("repos/$owner/$repo/releases") {
        withPagination(page = page, size = pageSize)
    }.body<List<Release>>()
}
