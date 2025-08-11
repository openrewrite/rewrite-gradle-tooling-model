/*
 * Copyright 2022 the original author or authors.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.RecipeSerializer;

import java.io.IOException;
import java.io.UncheckedIOException;

@Value
public class OpenRewriteModel {

    private static final ObjectMapper mapper = new RecipeSerializer().getMapper();

    org.openrewrite.gradle.marker.GradleProject gradleProject;


    org.openrewrite.gradle.marker. @Nullable GradleSettings gradleSettings;

    public static OpenRewriteModel from(OpenRewriteModelProxy proxy) {
        try {
            org.openrewrite.gradle.marker.GradleProject project = mapper.readValue(proxy.getGradleProjectBytes(), org.openrewrite.gradle.marker.GradleProject.class);
            org.openrewrite.gradle.marker.GradleSettings settings = proxy.getGradleSettingsBytes() == null ? null : mapper.readValue(proxy.getGradleSettingsBytes(), org.openrewrite.gradle.marker.GradleSettings.class);
            return new OpenRewriteModel(project, settings);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
