/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.gradle.marker;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.gradle.toolingapi.OpenRewriteModel;
import org.openrewrite.gradle.toolingapi.OpenRewriteModelBuilder;
import org.openrewrite.maven.tree.ResolvedDependency;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

class GradleProjectTest {
    @TempDir
    Path dir;

    @Nested
    class singleProject {
        GradleProject gradleProject;

        //language=groovy
        String buildGradle = """
          plugins{
              id 'java'
          }

          repositories{
              mavenCentral()
          }

          dependencies{
              implementation platform('org.openrewrite:rewrite-bom:8.56.0')
              implementation 'org.openrewrite:rewrite-java'
          }
          """;

        //language=groovy
        String settingsGradle = """
          rootProject.name = "sample"
          """;

        @BeforeEach
        void gradleProject() throws IOException {
            try (InputStream is = new ByteArrayInputStream(buildGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(dir.resolve("build.gradle"), is.readAllBytes());
            }

            try (InputStream is = new ByteArrayInputStream(settingsGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(dir.resolve("settings.gradle"), is.readAllBytes());
            }

            OpenRewriteModel model = OpenRewriteModelBuilder.forProjectDirectory(dir.toFile(), dir.resolve("build.gradle").toFile());
            gradleProject = org.openrewrite.gradle.toolingapi.GradleProject.toMarker(model.gradleProject());
        }

        @Test
        void transitiveDependencies() {
            Map<Integer, Integer> dependenciesByDepth = new HashMap<>();
            for (GradleDependencyConfiguration configuration : gradleProject.getConfigurations()) {
                for (ResolvedDependency resolvedDependency : configuration.getResolved()) {
                    dependenciesByDepth.merge(resolvedDependency.getDepth(), 1, Integer::sum);
                }
            }
            assertThat(dependenciesByDepth).containsKeys(0, 1);
        }

        @Test
        void requestedCorrespondsDirectlyToResolved() {
            assertThat(requireNonNull(gradleProject.getConfiguration("compileClasspath")).getRequested())
              .hasSize(2);
            assertThat(requireNonNull(gradleProject.getConfiguration("compileClasspath")).getDirectResolved())
              .hasSize(2);
        }

        @Test
        void serializable() throws IOException {
            ObjectMapper m = buildMapper();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            m.writeValue(baos, gradleProject);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            GradleProject roundTripped = m.readValue(bais, GradleProject.class);
            assertThat(roundTripped).isEqualTo(gradleProject);
        }

        ObjectMapper buildMapper() {
            SmileFactory f = SmileFactory.builder()
              .enable(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES)
              .build();
            JsonMapper.Builder mBuilder = JsonMapper.builder(f);

            ObjectMapper m = mBuilder
              // to be able to construct classes that have @Data and a single field
              // see https://cowtowncoder.medium.com/jackson-2-12-most-wanted-3-5-246624e2d3d0
              .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
              .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
              .disable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES)
              .build()
              .registerModule(new ParameterNamesModule())
              .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
              .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
              .setSerializationInclusion(JsonInclude.Include.NON_NULL);

            // workaround for problems with JavaType.Method#defaultValue (should be temporary)
            m.coercionConfigFor(LogicalType.Collection)
              .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull)
              .setAcceptBlankAsEmpty(true);

            return m.setVisibility(m.getSerializationConfig().getDefaultVisibilityChecker()
              .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
              .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
              .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
              .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
        }

    }

    @Nested
    class multiProject {
        GradleProject rootGradleProject;
        GradleProject aGradleProject;
        GradleProject bGradleProject;

        //language=groovy
        String aBuildGradle = """
          plugins{
              id 'java'
          }

          repositories{
              mavenCentral()
          }

          dependencies{
              implementation platform('org.openrewrite:rewrite-bom:8.57.0')
              implementation 'org.openrewrite:rewrite-java'
              testImplementation("org.projectlombok:lombok:latest.release")
          }
          """;

        //language=groovy
        String bBuildGradle = """
          plugins{
              id 'java'
          }

          repositories{
              mavenCentral()
          }

          dependencies{
              implementation project(":a")
              implementation 'org.openrewrite:rewrite-java:8.56.0'
          }
          """;

        //language=groovy
        String settingsGradle = """
          rootProject.name = "sample"
          include("a")
          include("b")
          """;

        @BeforeEach
        void gradleProject() throws IOException {
            try (InputStream is = new ByteArrayInputStream("\n".getBytes(StandardCharsets.UTF_8))) {
                Files.write(dir.resolve("build.gradle"), is.readAllBytes());
            }

            try (InputStream is = new ByteArrayInputStream(settingsGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(dir.resolve("settings.gradle"), is.readAllBytes());
            }

            Path aDir = dir.resolve("a");
            Files.createDirectory(aDir);
            try (InputStream is = new ByteArrayInputStream(aBuildGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(aDir.resolve("build.gradle"), is.readAllBytes());
            }

            Path bDir = dir.resolve("b");
            Files.createDirectory(bDir);
            try (InputStream is = new ByteArrayInputStream(bBuildGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(bDir.resolve("build.gradle"), is.readAllBytes());
            }


            OpenRewriteModel rootModel = OpenRewriteModelBuilder.forProjectDirectory(dir.toFile(), dir.resolve("build.gradle").toFile());
            rootGradleProject = org.openrewrite.gradle.toolingapi.GradleProject.toMarker(rootModel.gradleProject());
            OpenRewriteModel aModel = OpenRewriteModelBuilder.forProjectDirectory(aDir.toFile(), aDir.resolve("build.gradle").toFile());
            aGradleProject = org.openrewrite.gradle.toolingapi.GradleProject.toMarker(aModel.gradleProject());
            OpenRewriteModel bModel = OpenRewriteModelBuilder.forProjectDirectory(bDir.toFile(), bDir.resolve("build.gradle").toFile());
            bGradleProject = org.openrewrite.gradle.toolingapi.GradleProject.toMarker(bModel.gradleProject());
        }
    }
}
