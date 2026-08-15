// Lightweight compatibility stub for the original llama.cpp Android module.
// Remote-PC builds do not execute any on-device LLM inference. Keeping this
// module as a small Kotlin API shim lets the existing app compile without the
// llama.cpp git submodule, CMake, NDK, Vulkan, or native libraries.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.arm.aichat"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
