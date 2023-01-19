plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    implementation(project(":model"))
    implementation(gradleApi())
}
