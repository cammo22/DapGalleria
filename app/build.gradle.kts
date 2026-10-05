plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// La CI passa -PversionName=1.2.3 e -PversionCode=N partendo dal tag della release.
val appVersionName = providers.gradleProperty("versionName").orElse("1.0.0").get()
val appVersionCode = providers.gradleProperty("versionCode").orElse("1").get().toInt()

// La firma della release arriva solo da variabili d'ambiente (GitHub Secrets in CI).
// Nessuna chiave o password e' salvata nel repository.
val releaseKeystore: String? = System.getenv("DAP_KEYSTORE_FILE")

android {
    namespace = "com.dapprod.dapgalleria"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dapprod.dapgalleria"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("DAP_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("DAP_KEY_ALIAS")
                keyPassword = System.getenv("DAP_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Senza chiave di release si ripiega sulla chiave di debug (build locali).
            signingConfig = signingConfigs.getByName(if (releaseKeystore != null) "release" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    lint {
        abortOnError = false
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
}
