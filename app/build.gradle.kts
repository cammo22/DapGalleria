plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// La CI passa -PversionName=1.2.3 e -PversionCode=N partendo dal tag della release.
val appVersionName = providers.gradleProperty("versionName").orElse("1.0.0").get()
val appVersionCode = providers.gradleProperty("versionCode").orElse("1").get().toInt()

// Firma della release.
// Di default si usa keystore/dapgalleria-public.jks, una chiave VOLUTAMENTE PUBBLICA inclusa nel repo
// (password nota): serve solo a far firmare ogni release con la stessa identita', cosi' gli
// aggiornamenti si installano sopra la versione precedente senza configurare nulla.
// Chi vuole una chiave privata imposta DAP_KEYSTORE_FILE / DAP_KEYSTORE_PASSWORD / DAP_KEY_ALIAS /
// DAP_KEY_PASSWORD (in CI arrivano dai GitHub Secrets).
val keystorePath: String = System.getenv("DAP_KEYSTORE_FILE") ?: "$rootDir/keystore/dapgalleria-public.jks"

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
        create("release") {
            storeFile = file(keystorePath)
            storePassword = System.getenv("DAP_KEYSTORE_PASSWORD") ?: "dapgalleria"
            keyAlias = System.getenv("DAP_KEY_ALIAS") ?: "dapgalleria"
            keyPassword = System.getenv("DAP_KEY_PASSWORD") ?: "dapgalleria"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
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
    implementation(libs.media3.transformer)
    implementation(libs.media3.effect)

    testImplementation(libs.junit)
}
