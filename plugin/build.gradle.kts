plugins {
    id("java-library")
    id("org.openrewrite.build.metadata")
    id("org.openrewrite.build.publish")
    id("org.openrewrite.build.java8-text-blocks")
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
    mavenCentral()
}

val latest = if (project.hasProperty("releasing")) {
    "latest.release"
} else {
    "latest.integration"
}

dependencies {
    implementation(project(":model"))
    implementation("org.openrewrite:rewrite-gradle:$latest")
    implementation(gradleApi())
}

tasks.withType<Test>().configureEach {
    dependsOn(tasks.named("publishToMavenLocal"))
}

tasks.named<JavaCompile>("compileJava").configure {
    options.release.set(8)
}
