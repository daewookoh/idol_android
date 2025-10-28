plugins {
    id("kotlin")
    id("kotlin-kapt")
}

dependencies {
    implementation(libs.coroutines.core)

    implementation(libs.hilt.core)
    kapt(libs.hilt.compiler)
}