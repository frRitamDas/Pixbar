import org.gradle.api.tasks.testing.Test
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.sentry.android.gradle)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun localProperty(name: String): String? = localProperties.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }

val releaseAppVersionName = localProperty("PIXBAR_VERSION_NAME") ?: "1.1"
val releaseAppVersionCode = localProperty("PIXBAR_VERSION_CODE")?.toIntOrNull() ?: 123
val sentryMappingUploadEnabled = localProperty("SENTRY_UPLOAD_MAPPINGS")?.toBooleanStrictOrNull() ?: false
val sentryAuthToken = localProperty("SENTRY_AUTH_TOKEN")
val sentryOrg = localProperty("SENTRY_ORG")
val sentryProject = localProperty("SENTRY_PROJECT")

android {
    namespace = "com.nuvio.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.nuvio.app.Nuvio"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = releaseAppVersionCode
        versionName = releaseAppVersionName
    }

    signingConfigs {
        create("release") {
            val storeFilePath = localProperty("PIXBAR_RELEASE_STORE_FILE")
            val storePassword = localProperty("PIXBAR_RELEASE_STORE_PASSWORD")
            val keyAlias = localProperty("PIXBAR_RELEASE_KEY_ALIAS")
            val keyPassword = localProperty("PIXBAR_RELEASE_KEY_PASSWORD")
            if (storeFilePath != null) storeFile = file(storeFilePath)
            if (storePassword != null) this.storePassword = storePassword
            if (keyAlias != null) this.keyAlias = keyAlias
            if (keyPassword != null) this.keyPassword = keyPassword
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("full") {
            dimension = "distribution"
            applicationId = "com.nuvio.app.Nuvio"
        }
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/AL2.0",
            "META-INF/LGPL2.1",
            "META-INF/DEPENDENCIES",
        )
    }

    lint {
        abortOnError = false
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    sourceSets.getByName("main") {
        manifest.srcFile("src/main/AndroidManifest.xml")
    }

    applicationVariants.all {
        val variant = this
        if (variant.buildType.name == "debug") {
            variant.applicationId = "com.nuviodebug.com"
        }
    }
}

sentry {
    includeProguardMapping.set(true)
    autoUploadProguardMapping.set(sentryMappingUploadEnabled)
    uploadNativeSymbols.set(false)
    autoUploadNativeSymbols.set(false)
    includeNativeSources.set(false)
    includeSourceContext.set(false)
    includeDependenciesReport.set(false)
    telemetry.set(false)
    sentryAuthToken?.let(authToken::set)
    sentryOrg?.let(org::set)
    sentryProject?.let(projectName::set)
    ignoredBuildTypes.set(setOf("debug"))
    autoInstallation {
        enabled.set(false)
    }
    tracingInstrumentation {
        enabled.set(false)
    }
}

dependencies {
    implementation(project(":composeApp"))
    // The Android wrapper compiles generated/application Kotlin that references the
    // Compose runtime. Keep the runtime on this module's compile classpath; the
    // Compose compiler plugin itself belongs to composeApp, not this Android wrapper.
    implementation(libs.compose.runtime)
    implementation(libs.androidx.appcompat)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    debugImplementation(libs.compose.uiTooling)
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.androidx.activity.compose)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:${libs.versions.composeMultiplatform.get()}")
    debugImplementation("androidx.compose.ui:ui-test-manifest:${libs.versions.composeMultiplatform.get()}")
}

// Keep this module's JVM/Android test configuration explicit for Gradle 9.x.
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
