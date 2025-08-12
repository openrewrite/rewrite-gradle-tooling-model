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

dependencies {
    implementation(project(":model"))
    implementation("org.openrewrite:rewrite-gradle:latest.integration")
    implementation(gradleApi())
}

tasks.withType<Test>().configureEach {
    dependsOn(tasks.named("publishToMavenLocal"))
}

tasks.named<JavaCompile>("compileJava").configure {
    options.release.set(8)
}
