plugins {
    id("org.openrewrite.build.language-library")
}

val latest = if (project.hasProperty("releasing")) {
    "latest.release"
} else {
    "latest.integration"
}

repositories {
    if (!project.hasProperty("releasing")) {
        mavenLocal {
            mavenContent {
                excludeVersionByRegex(".+", ".+", ".+-rc-?[0-9]*")
            }
        }

        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }
    }

    mavenCentral {
        mavenContent {
            excludeVersionByRegex(".+", ".+", ".+-rc-?[0-9]*")
        }
    }
}

dependencies {
    implementation(project(":model"))
    implementation(gradleApi())

    implementation(platform("org.openrewrite:rewrite-bom:$latest"))
    compileOnly("org.openrewrite:rewrite-core")
    compileOnly("org.openrewrite:rewrite-gradle")
    implementation("org.openrewrite.gradle.tooling:model:$latest")
    compileOnly("org.openrewrite:rewrite-groovy")
    compileOnly("org.openrewrite:rewrite-hcl")
    compileOnly("org.openrewrite:rewrite-java")
    compileOnly("org.openrewrite:rewrite-json")
    compileOnly("org.openrewrite:rewrite-kotlin:$latest")
    compileOnly("org.openrewrite:rewrite-properties")
    compileOnly("org.openrewrite:rewrite-protobuf")
    compileOnly("org.openrewrite:rewrite-xml")
    compileOnly("org.openrewrite:rewrite-yaml")
    implementation("org.openrewrite:rewrite-polyglot:$latest")
    @Suppress("VulnerableLibrariesLocal", "RedundantSuppression")
    compileOnly("com.puppycrawl.tools:checkstyle:9.3")

    testImplementation(platform("org.junit:junit-bom:latest.release"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")

    testImplementation("org.openrewrite:rewrite-test")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.assertj:assertj-core:latest.release")

    testImplementation("org.slf4j:slf4j-api:1.7.36")

    runtimeOnly("org.openrewrite:rewrite-java-21")
    runtimeOnly("org.openrewrite:rewrite-java-17")
    runtimeOnly("org.openrewrite:rewrite-java-11")
    runtimeOnly("org.openrewrite:rewrite-java-8")

}
