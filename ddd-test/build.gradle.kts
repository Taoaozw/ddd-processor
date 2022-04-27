
plugins{
    id("com.google.devtools.ksp") version "1.6.21-1.0.5"
}
dependencies{
    implementation(project(":annotation"))
    ksp(project(":anno-ksp"))
    implementation(kotlin("reflect"))
    api("com.squareup:kotlinpoet:latest.release")
}


kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}