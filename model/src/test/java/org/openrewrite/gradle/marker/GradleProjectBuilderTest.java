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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.gradle.toolingapi.OpenRewriteModel;
import org.openrewrite.gradle.toolingapi.OpenRewriteModelBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class GradleProjectBuilderTest {

    @Test
    void serializable(@TempDir Path dir) throws Exception {
        try(InputStream is = GradleProjectBuilderTest.class.getResourceAsStream("/build.gradle")) {
            Files.write(dir.resolve("build.gradle"), Objects.requireNonNull(is).readAllBytes());
        }
        try(InputStream is = GradleProjectBuilderTest.class.getResourceAsStream("/settings.gradle")) {
            Files.write(dir.resolve("settings.gradle"), Objects.requireNonNull(is).readAllBytes());
        }

        OpenRewriteModel model = OpenRewriteModelBuilder.forProjectDirectory(dir.toFile(), dir.resolve("build.gradle").toFile());
        org.openrewrite.gradle.marker.GradleProject gp = org.openrewrite.gradle.marker.GradleProject.fromToolingModel(model.gradleProject());
        ObjectMapper m = buildMapper();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        m.writeValue(baos, model.gradleProject());
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        org.openrewrite.gradle.toolingapi.GradleProject roundTripped = m.readValue(bais, org.openrewrite.gradle.toolingapi.GradleProject.class);
        assertThat(roundTripped).isEqualTo(gp);
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
