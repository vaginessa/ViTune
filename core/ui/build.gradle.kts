plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.library)
}

android {
    namespace = "it.vfsfitvnm.compose.core.ui"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    sourceSets.all {
        kotlin.srcDir("src/$name/kotlin")
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf("-Xcontext-receivers")
    }
}

dependencies {
    implementation(libs.bundles.compose)
    implementation(libs.compose.material3)
    implementation(libs.palette)

    implementation(projects.core.data)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}