import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.ksp) apply false
}

val clean by tasks.registering(Delete::class) {
    delete(rootProject.layout.buildDirectory.asFile)
}

subprojects {
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            if (project.findProperty("enableComposeCompilerReports") == "true")
                arrayOf("reports", "metrics").forEach {
                    @Suppress("DEPRECATION")
                    freeCompilerArgs = freeCompilerArgs + listOf(
                        "-P",
                        "plugin:androidx.compose.compiler.plugins.kotlin:${it}Destination=${project.buildDir.absolutePath}/compose_metrics"
                    )
                }
        }
    }
}
