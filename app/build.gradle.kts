import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
}

android {
    namespace = "com.smu.daiary"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }
    defaultConfig {
        applicationId = "com.smu.daiary"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ANTHROPIC_API_KEY는 APK에 넣지 않는다(디컴파일로 추출 가능). 키는 Cloud Functions 프록시에만 있다.
        // local.properties의 값은 프롬프트 평가 도구(src/test/.../eval)만 직접 읽는다.

        val weatherApiKey = localProps["OPENWEATHER_API_KEY"] as String? ?: ""
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"$weatherApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        // 프롬프트 평가 도구(src/test/.../eval)가 AnthropicDataSource를 그대로 호출하는데,
        // 그 안의 android.util.Log는 유닛 테스트에서 스텁이라 예외를 던진다. 조용히 넘긴다.
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    // org.json은 android.jar에 들어 있어 유닛 테스트에선 스텁(모든 메서드가 null 반환)이 된다.
    // AnthropicDataSource가 요청/응답을 전부 org.json으로 다루므로 진짜 구현을 얹는다.
    testImplementation("org.json:json:20231013")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // Coroutines + Firebase Task 지원
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")

    // Lifecycle (Compose용)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Location (날씨 수집용 GPS)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.7")

    // EXIF (사진 메타데이터 추출용)
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Coil (이미지 로딩)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // AppCompat (다크모드 AppCompatDelegate, 언어 전환 setApplicationLocales)
    implementation("androidx.appcompat:appcompat:1.7.0")

    // HTTP client (Anthropic API 호출용)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Health Connect (건강 데이터 수집용 - 걸음 수, 수면 시간)
    implementation("androidx.health.connect:connect-client:1.1.0-alpha07")

    // WorkManager (백그라운드 날씨 수집 스케줄링)
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}