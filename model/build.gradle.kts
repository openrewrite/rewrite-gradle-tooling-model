plugins {
    id("org.openrewrite.build.language-library")
}

val rewriteVersion = if (project.hasProperty("releasing")) {
    "latest.release"
} else {
    "latest.integration"
}

dependencies {
    implementation(gradleApi())

    implementation("org.openrewrite:rewrite-maven:${rewriteVersion}")
}
