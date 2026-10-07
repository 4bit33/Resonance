import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/**
 * One permanent signing key for every build, so any build installs over the
 * previous one whichever machine made it. The key lives outside the repo
 * (~/.android-keys/crate.properties, or -PcrateSigning=<path>); without it
 * builds fall back to the machine's debug key.
 */
val crateSigning: Properties? = (findProperty("crateSigning") as String? ?: "${System.getProperty("user.home")}/.android-keys/crate.properties")
    .let(::file)
    .takeIf { it.isFile }
    ?.let { f -> Properties().apply { f.inputStream().use(::load) } }

/**
 * AcoustID application key (free, https://acoustid.org/new-application) for
 * "Recognise by sound". Kept out of the repo: acoustid.key in local.properties,
 * or -PacoustidKey=... / ACOUSTID_KEY. Without it that button explains it is off.
 */
val acoustidKey: String = (findProperty("acoustidKey") as String?)
    ?: System.getenv("ACOUSTID_KEY")
    ?: rootProject.file("local.properties").takeIf { it.isFile }
        ?.let { f -> Properties().apply { f.inputStream().use(::load) }.getProperty("acoustid.key") }
    ?: ""

/** Grows with every commit, so a newer build is always an update, never a downgrade. */
val gitCommitCount: Int = providers.exec { commandLine("git", "rev-list", "--count", "HEAD") }
    .standardOutput.asText.get().trim().toIntOrNull() ?: 1

android {
    namespace = "com.resonance.player"
    compileSdk = 36
    ndkVersion = "28.2.13676358"

    signingConfigs {
        if (crateSigning != null) {
            create("crate") {
                storeFile = file(crateSigning.getProperty("storeFile"))
                storePassword = crateSigning.getProperty("storePassword")
                keyAlias = crateSigning.getProperty("keyAlias")
                keyPassword = crateSigning.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        // New id (was com.resonance.player): builds signed with the old debug keys
        // could not be updated in place; this one installs next to them.
        applicationId = "app.crate.player"
        // ADR-002: minSdk 26 (notification channels, Media3 baseline). Scoped-storage
        // and media-permission branches are isolated in core.permissions.
        minSdk = 26
        targetSdk = 35
        versionCode = gitCommitCount
        versionName = "0.2.$gitCommitCount"
        buildConfigField("String", "ACOUSTID_KEY", "\"$acoustidKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_static"
            }
        }
    }

    // Chromaprint (audio fingerprints for AcoustID), see src/main/cpp.
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        debug {
            signingConfigs.findByName("crate")?.let { signingConfig = it }
        }
        release {
            signingConfigs.findByName("crate")?.let { signingConfig = it }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
    }
    // youtubedl-android runs its bundled Python/ffmpeg from extracted native libs.
    packaging {
        jniLibs.useLegacyPackaging = true
        // Zip archives named .so (yt-dlp's Python / ffmpeg): nothing to strip.
        jniLibs.keepDebugSymbols += "**/*.zip.so"
    }
    // One APK per CPU: the bundled Python + ffmpeg are large, a universal APK would carry all of them.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    // Cover thumbnails in the tag lookup results (ADR-013: only when the user searches).
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.documentfile)
    // Drag-to-reorder for lazy lists (Apache-2.0).
    implementation(libs.reorderable)
    // Shape morphing (what Material 3 MaterialShapes is built on).
    implementation(libs.graphics.shapes)

    // Import (ADR-013): yt-dlp + ffmpeg packaged for Android (GPL-3.0, same as Seal / YTDLnis).
    implementation(libs.youtubedl.android)
    implementation(libs.youtubedl.ffmpeg)

    // Room (local library database). KSP processor, no KAPT.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Media3 on the classpath for Phase 2 (playback service). No usage from UI.
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)

    testImplementation(libs.junit)
    // Android's org.json is a stub on the JVM; the real one for parser tests.
    testImplementation(libs.org.json)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
