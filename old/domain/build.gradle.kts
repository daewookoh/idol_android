plugins {
    id("kotlin")
    id("kotlin-kapt")
}

dependencies {
    implementation(project(":data-resource"))

    implementation(libs.coroutines.core)

    implementation(libs.hilt.core)
    kapt(libs.hilt.compiler)

    testImplementation(libs.google.truth)
    testImplementation(libs.mockk.core)
    testImplementation(libs.coroutine.test)
    testImplementation(libs.junit)
    testImplementation(libs.turbin)
}