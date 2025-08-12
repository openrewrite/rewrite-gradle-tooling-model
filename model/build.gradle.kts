plugins {
    id("org.openrewrite.build.language-library")
    id("org.openrewrite.build.java8-text-blocks")
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

tasks.named<JavaCompile>("compileJava").configure {
    options.release.set(8)
    options.compilerArgs.add("-parameters")
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "org.assertj" && requested.name == "assertj-core") {
                useVersion("3.+")
            }
        }
    }
}

val testGradle4 = tasks.register<Test>("testGradle4") {
    systemProperty("org.openrewrite.test.gradleVersion", "4.10")
    systemProperty("jarLocationForTest", tasks.named<Jar>("jar").get().archiveFile.get().asFile.absolutePath)
    // Gradle 4 predates support for Java 11
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    })
}

tasks.named("check").configure {
    dependsOn(testGradle4)
}
