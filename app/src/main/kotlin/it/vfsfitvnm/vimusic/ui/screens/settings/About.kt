package it.vfsfitvnm.vimusic.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.github.GitHub
import it.vfsfitvnm.github.models.Release
import it.vfsfitvnm.github.requests.releases
import it.vfsfitvnm.vimusic.BuildConfig
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.ui.components.themed.CircularProgressIndicator
import it.vfsfitvnm.vimusic.ui.components.themed.DefaultDialog
import it.vfsfitvnm.vimusic.ui.components.themed.SecondaryTextButton
import it.vfsfitvnm.vimusic.ui.screens.Route
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.bold
import it.vfsfitvnm.vimusic.utils.center
import it.vfsfitvnm.vimusic.utils.semiBold
import it.vfsfitvnm.vimusic.utils.version
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val VERSION_NAME = BuildConfig.VERSION_NAME.substringBeforeLast("-")
private const val REPO_OWNER = "25huizengek1"
private const val REPO_NAME = "ViMusic"

@Route
@Composable
fun About() = SettingsCategoryScreen(
    title = stringResource(R.string.about),
    description = stringResource(
        R.string.format_version_credits,
        VERSION_NAME
    )
) {
    val (_, typography) = LocalAppearance.current

    val uriHandler = LocalUriHandler.current

    SettingsGroup(title = stringResource(R.string.social)) {
        SettingsEntry(
            title = stringResource(R.string.github),
            text = stringResource(R.string.view_source),
            onClick = {
                uriHandler.openUri("https://github.com/$REPO_OWNER/$REPO_NAME")
            }
        )
    }

    SettingsGroup(title = stringResource(R.string.contact)) {
        SettingsEntry(
            title = stringResource(R.string.report_bug),
            text = stringResource(R.string.report_bug_description),
            onClick = {
                uriHandler.openUri(
                    "https://github.com/$REPO_OWNER/$REPO_NAME/issues/new?assignees=&labels=bug&template=bug_report.yaml"
                )
            }
        )

        SettingsEntry(
            title = stringResource(R.string.request_feature),
            text = stringResource(R.string.request_feature_description),
            onClick = {
                uriHandler.openUri(
                    @Suppress("MaximumLineLength")
                    "https://github.com/25huizengek1/ViMusic/issues/new?assignees=&labels=enhancement&template=feature_request.md"
                )
            }
        )
    }

    var newVersionDialogOpened by rememberSaveable { mutableStateOf(false) }

    SettingsGroup(title = stringResource(R.string.version)) {
        SettingsEntry(
            title = stringResource(R.string.check_new_version),
            text = stringResource(R.string.current_version, VERSION_NAME),
            onClick = { newVersionDialogOpened = true }
        )
    }

    if (newVersionDialogOpened) DefaultDialog(onDismiss = { newVersionDialogOpened = false }) {
        var newVersion: Result<Release?>? by remember { mutableStateOf(null) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                newVersion = GitHub.releases(
                    owner = REPO_OWNER,
                    repo = REPO_NAME
                )?.mapCatching { releases ->
                    val currentVersion = VERSION_NAME.version

                    releases
                        .sortedByDescending { it.publishedAt }
                        .firstOrNull { release ->
                            !release.draft &&
                                    !release.preRelease &&
                                    release.tag.removePrefix("v").version > currentVersion &&
                                    release.assets.any {
                                        it.contentType == "application/vnd.android.package-archive" &&
                                                it.state == Release.Asset.State.Uploaded
                                    }
                        }
                }?.onFailure(Throwable::printStackTrace)
            }
        }

        newVersion?.getOrNull()?.let {
            BasicText(
                text = stringResource(R.string.new_version_available),
                style = typography.xs.semiBold.center
            )

            Spacer(modifier = Modifier.height(12.dp))

            BasicText(
                text = it.name ?: it.tag,
                style = typography.m.bold.center
            )

            Spacer(modifier = Modifier.height(16.dp))

            SecondaryTextButton(
                text = stringResource(R.string.more_information),
                onClick = { uriHandler.openUri(it.frontendUrl.toString()) }
            )
        } ?: newVersion?.exceptionOrNull()?.let {
            BasicText(
                text = stringResource(R.string.error_github),
                style = typography.xs.semiBold.center,
                modifier = Modifier.padding(all = 24.dp)
            )
        } ?: if (newVersion?.isSuccess == true) BasicText(
            text = stringResource(R.string.up_to_date),
            style = typography.xs.semiBold.center
        ) else CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}
