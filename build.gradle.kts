plugins {
    id("org.openrewrite.build.root")
}

allprojects {
    group = "org.openrewrite.gradle.tooling"
    description = "A model for extracting semantic information out of Gradle build files necessary for refactoring them."
}
