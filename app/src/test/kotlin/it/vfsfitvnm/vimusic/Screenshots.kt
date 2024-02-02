package it.vfsfitvnm.vimusic

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import com.github.takahirom.roborazzi.captureRoboImage
import it.vfsfitvnm.vimusic.enums.ColorPaletteMode
import it.vfsfitvnm.vimusic.preferences.AppearancePreferences
import it.vfsfitvnm.vimusic.preferences.UIStatePreferences
import it.vfsfitvnm.vimusic.ui.screens.albumRoute
import it.vfsfitvnm.vimusic.ui.screens.artistRoute
import it.vfsfitvnm.vimusic.ui.screens.searchResultRoute
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.minutes

// TODO: all very cluttered to start off, should restructure in the future

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "sw320dp-w400dp-h712dp-normal-notlong-notround-any-560dpi-keyshidden-nonav",
    application = MainApplication::class
)
class Screenshots {
    private val timeoutMillis = 1.minutes.inWholeMilliseconds

    private var atomic = 1
        get() = field++

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun prepare() {
        System.setProperty("kotlinx.coroutines.test.default_timeout", "3m")
    }

    @Test
    @Config(qualifiers = "+en-rUS-night")
    fun takeScreenshotsEnglish() = runTest {
        composeTestRule.activity.shouldRebind = false
        takeAll("en-US")
    }

    @Suppress("SameParameterValue") // TEMP
    private suspend fun takeAll(tag: String) {
        Coil.setImageLoader(
            ImageLoader.Builder(composeTestRule.activity.applicationContext).build()
        )

        AppearancePreferences.colorPaletteMode = ColorPaletteMode.Dark
        take("dark", tag)

        atomic =
            1 // not multi-threadable, but not a concern right now as we only generate en-US for now
    }

    @OptIn(ExperimentalTestApi::class)
    private suspend fun take(prefix: String, tag: String) = with(composeTestRule) {
        UIStatePreferences.homeScreenTabIndex = 0 // Quick Picks
        waitUntilAtLeastOneExists(
            matcher = hasText(activity.getString(R.string.related_albums)),
            timeoutMillis = timeoutMillis
        )
        awaitIdle()
        createScreenshot("$prefix-quickpicks", tag)

        UIStatePreferences.homeScreenTabIndex = 1 // Discover
        waitUntilAtLeastOneExists(
            matcher = hasText(activity.getString(R.string.moods_and_genres)),
            timeoutMillis = timeoutMillis
        )
        awaitIdle()
        createScreenshot("$prefix-discover", tag)

        searchResultRoute.ensureGlobal("goose synrise")
        waitUntilAtLeastOneExists(hasText("GOOSE"), timeoutMillis)
        awaitIdle()
        createScreenshot("$prefix-searchresult", tag)

        UIStatePreferences.artistScreenTabIndex = 0 // Overview
        artistRoute.ensureGlobal("UChNB35QmqX81vSbDXDR3i4w") // Emancipator
        waitUntilAtLeastOneExists(hasText("Emancipator"), timeoutMillis)
        awaitIdle()
        createScreenshot("$prefix-artist", tag)

        UIStatePreferences.artistScreenTabIndex = 2 // Albums
        waitUntilAtLeastOneExists(hasText("A Thousand Clouds"), timeoutMillis)
        awaitIdle()
        createScreenshot("$prefix-artistalbums", tag)

        albumRoute.ensureGlobal("MPREb_G6wid9TwIs9") // Baralku - Emancipator
        waitUntilAtLeastOneExists(hasText("Baralku"), timeoutMillis)
        awaitIdle()
        createScreenshot("$prefix-album", tag)

        back()
        waitUntilAtLeastOneExists(hasText("A Thousand Clouds"))
        back()
        waitUntilAtLeastOneExists(hasText("GOOSE"))
        back()
    }

    private fun back() = composeTestRule.activity.onBackPressedDispatcher.onBackPressed()

    private fun createScreenshot(name: String, tag: String) =
        composeTestRule.onRoot().captureRoboImage(
            filePath = "../fastlane/metadata/android/$tag/images/phoneScreenshots/${
                atomic.toString().padStart(3, '0')
            }-$name.png"
        )
}
