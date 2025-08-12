plugins {
    id("java-library")
    id("org.openrewrite.build.metadata")
    id("org.openrewrite.build.publish")
    id("org.openrewrite.build.java8-text-blocks")
}

// It is intentional that these are declared explicitly
// org.openrewrite.gradle.RewriteDependencyRepositoriesPlugin in rewrite-build-gradle-plugin disallows snapshot repositories during release builds
// Uniquely amongst our repositories, this repository is supposed to be able to release built against rewrite-core snapshots
repositories {
    mavenLocal()
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
    mavenCentral()
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

    compileOnly("org.projectlombok:lombok:latest.release")
    testCompileOnly("org.projectlombok:lombok:latest.release")
    annotationProcessor("org.projectlombok:lombok:latest.release")
    testAnnotationProcessor("org.projectlombok:lombok:latest.release")
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
    // last version which supports java 8, which testGradle4 runs on
    testImplementation("org.assertj:assertj-core:3.+")
}

tasks.named<JavaCompile>("compileJava").configure {
    options.release.set(8)
    options.compilerArgs.add("-parameters")
    options.encoding = "UTF-8"
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
