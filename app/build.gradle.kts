import java.util.Properties
plugins {
    alias(libs.plugins.android.application)

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

// 네이버 지도 Client ID는 local.properties에서 읽는다
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
android {
    namespace = "com.example.ssuui"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.ssuui"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["NAVER_MAP_CLIENT_ID"] = localProps.getProperty("NAVER_MAP_CLIENT_ID", "")
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.naver.map.sdk)
    implementation(libs.print)      // QR 포스터 인쇄 (PrintHelper)
    implementation(libs.zxing.core) // QR 코드 만들기·읽기
    implementation(libs.camera.camera2)   // 손님 QR 스캔 (카메라 미리보기·프레임 분석)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))


    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")

    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-auth")

    // Add the dependency for the Cloud Storage library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-storage")

    implementation("com.google.firebase:firebase-firestore")

    // FirebaseUI Storage only
    implementation("com.firebaseui:firebase-ui-storage:9.0.0")

    // For @GlideModule
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
}