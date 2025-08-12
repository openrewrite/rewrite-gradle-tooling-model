plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    implementation(gradleApi())

    // NOTE: this is latest.integration because we need to be able to release
    // rewrite-gradle-tooling-model BEFORE rewrite but also need to depend on
    // changes to the ABI of rewrite-maven.
    compileOnly("org.openrewrite:rewrite-core:latest.integration")
    compileOnly("org.openrewrite:rewrite-maven:latest.integration")

    // These are for org.openrewrite.gradle.toolingapi.Assertions
    compileOnly("org.openrewrite:rewrite-test:latest.integration")
    compileOnly("org.openrewrite:rewrite-gradle:latest.integration")
    compileOnly("org.openrewrite:rewrite-groovy:latest.integration")
    compileOnly("org.openrewrite:rewrite-kotlin:latest.integration")
    compileOnly("org.openrewrite:rewrite-properties:latest.integration")
    compileOnly("org.openrewrite:rewrite-toml:latest.integration")

    testImplementation("org.openrewrite:rewrite-test:latest.integration")
    testImplementation("org.openrewrite:rewrite-gradle:latest.integration") {
        exclude(group = "org.openrewrite.gradle.tooling")
    }
    testImplementation("org.openrewrite:rewrite-core:latest.integration")
    testImplementation("org.openrewrite:rewrite-maven:latest.integration")
    testImplementation(platform("com.fasterxml.jackson:jackson-bom:2.17.+"))
    testImplementation("com.fasterxml.jackson.core:jackson-core")
    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-smile")
}
