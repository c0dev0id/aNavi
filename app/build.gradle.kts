plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

fun envOrProp(name: String): String =
    System.getenv(name) ?: project.findProperty(name) as? String ?: ""

android {
    namespace = "dev.anavi"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.anavi"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "MAPTILER_KEY", "\"${envOrProp("MAPTILER_KEY")}\"")
        buildConfigField("String", "GOOGLE_STT_KEY", "\"${envOrProp("GOOGLE_STT_KEY")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            val ks = file("release.keystore")
            if (ks.exists()) {
                storeFile = ks
                storePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.maplibre.sdk)
    testImplementation(libs.junit)
}
