plugins {
    id("org.openrewrite.build.root") version("1.131.0-dev.36.uncommitted+8c96195")
}

allprojects {
    group = "org.openrewrite.gradle.tooling"
    description = "A model for extracting semantic information out of Gradle build files necessary for refactoring them."
}
