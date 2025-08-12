plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    implementation(project(":model"))
    implementation("org.openrewrite:rewrite-gradle:latest.integration")
    implementation(gradleApi())
}

tasks.withType<Test>().configureEach {
    enabled = false
}
