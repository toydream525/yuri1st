plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val configuredBangumiUserAgent = providers.gradleProperty("BANGUMI_USER_AGENT")
    .orElse(providers.environmentVariable("BANGUMI_USER_AGENT"))

fun buildConfigString(value: String): String = "\"" +
    value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.yurishelf.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yurishelf.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            val userAgent = configuredBangumiUserAgent.orElse(
                "Yuri1stDev/yuri1st/0.3.0 (Android; local prototype)",
            ).get()
            buildConfigField("String", "BANGUMI_USER_AGENT", buildConfigString(userAgent))
        }
        release {
            val userAgent = configuredBangumiUserAgent.orElse("").get()
            buildConfigField("String", "BANGUMI_USER_AGENT", buildConfigString(userAgent))
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.configureEach {
    if (name == "preReleaseBuild") {
        doFirst {
            check(configuredBangumiUserAgent.orNull?.isNotBlank() == true) {
                "Release builds require -PBANGUMI_USER_AGENT='yuri1st/version (developer-id; project-url)'"
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
