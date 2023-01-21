plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    implementation(gradleApi())

    // NOTE: this is latest.integration because we need to be able to release
    // rewrite-gradle-tooling-model BEFORE rewrite but also need to depend on
    // changes to the ABI of rewrite-maven.
    compileOnly("org.openrewrite:rewrite-maven:latest.integration")
    testRuntimeOnly("org.openrewrite:rewrite-maven:latest.integration")
}
