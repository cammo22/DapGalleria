plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// La CI delle release passa -PversionName=1.2.3 e -PversionCode=N partendo dal tag.
// Senza, si usa il file VERSION (stessa formula della CI: MAJOR*10000+MINOR*100+PATCH): cosi' anche l'APK debug
// scaricato dalla CI ha la versione giusta e Android lo installa sopra quello gia' presente, senza disinstallare.
val versionFile: String = file("$rootDir/VERSION").takeIf { it.exists() }?.readText()?.trim().orEmpty().ifEmpty { "1.0.0" }
val fileVersionCode: Int = versionFile.substringBefore('-').split('.').let { p ->
    fun part(i: Int) = p.getOrNull(i)?.toIntOrNull() ?: 0
    part(0) * 10000 + part(1) * 100 + part(2)
}
val appVersionName = providers.gradleProperty("versionName").orElse(versionFile).get()
val appVersionCode = providers.gradleProperty("versionCode").map { it.toInt() }.orElse(fileVersionCode).get()

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
        debug {
            // stessa firma della release: debug e release si aggiornano a vicenda senza disinstallare
            signingConfig = signingConfigs.getByName("release")
        }
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
