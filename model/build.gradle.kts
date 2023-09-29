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
    testRuntimeOnly("org.openrewrite:rewrite-maven:latest.integration")
    testImplementation(platform("com.fasterxml.jackson:jackson-bom:latest.release"))
    testImplementation("com.fasterxml.jackson:jackson-core")
    testImplementation("com.fasterxml.jackson:jackson-databind")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-smile")
}
