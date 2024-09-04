/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.gradle.toolingapi;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.test.RewriteTest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;

@Disabled
class AssertionsTest implements RewriteTest {

    @Test
    void withToolingApi() {
        rewriteRun(
          spec -> spec.beforeRecipe(Assertions.withToolingApi()),
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getMarkers().findFirst(GradleProject.class)).isPresent())
          )
        );
    }

    @Test
    void withCustomDistributionUri() {
        rewriteRun(
          spec -> spec.beforeRecipe(Assertions.withToolingApi(URI.create("https://artifactory.moderne.ninja/artifactory/gradle-distributions/gradle-8.6-bin.zip"))),
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getMarkers().findFirst(GradleProject.class)).isPresent())
          )
        );
    }

    @Test
    void customInitScript() {
        //language=groovy
        String alternateInit = """
          initscript{
              repositories{
                  mavenLocal()
                  maven{ url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
                  mavenCentral()
              }
          
              configurations.all{
                  resolutionStrategy{
                      cacheChangingModulesFor 0, 'seconds'
                      cacheDynamicVersionsFor 0, 'seconds'
                  }
              }
          
              dependencies{
                  classpath 'org.openrewrite.gradle.tooling:plugin:latest.integration'
                  classpath 'org.openrewrite:rewrite-maven:latest.integration'
              }
          }
          
          allprojects{
              apply plugin: org.openrewrite.gradle.toolingapi.ToolingApiOpenRewriteModelPlugin
          }
          
          """;
        GradleWrapper gradleWrapper = GradleWrapper.create(URI.create("https://services.gradle.org/distributions/gradle-8.6-bin.zip"), null);
        rewriteRun(
          spec -> spec.beforeRecipe(Assertions.withToolingApi(gradleWrapper, alternateInit)),
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getMarkers().findFirst(GradleProject.class)).isPresent())
          )
        );
    }
}
