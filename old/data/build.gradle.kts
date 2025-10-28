plugins {
    id("kotlin")
    id("kotlin-kapt")
    id("kotlinx-serialization")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data-resource"))

    implementation(libs.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.core)
    kapt(libs.hilt.compiler)

    testImplementation(libs.google.truth)
    testImplementation(libs.mockk.core)
    testImplementation(libs.coroutine.test)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.turbin)
}