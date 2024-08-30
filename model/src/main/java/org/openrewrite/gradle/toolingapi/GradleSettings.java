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
package org.openrewrite.gradle.toolingapi;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public interface GradleSettings {
    List<MavenRepository> getPluginRepositories();

    List<GradlePluginDescriptor> getPlugins();

    Map<String, FeaturePreview> getFeaturePreviews();

    GradleBuildscript getBuildscript();

    static org.openrewrite.gradle.marker.GradleSettings toMarker(GradleSettings settings) {
        return new org.openrewrite.gradle.marker.GradleSettings(
                UUID.randomUUID(),
                settings.getPluginRepositories().stream()
                        .map(MavenRepository::toMarker)
                        .collect(Collectors.toList()),
                settings.getPlugins().stream()
                        .map(GradlePluginDescriptor::toMarker)
                        .collect(Collectors.toList()),
                settings.getFeaturePreviews().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> FeaturePreview.toMarker(e.getValue()))),
                new org.openrewrite.gradle.marker.GradleBuildscript(
                        UUID.randomUUID(),
                        settings.getBuildscript().getMavenRepositories().stream()
                                .map(MavenRepository::toMarker)
                                .collect(Collectors.toList()),
                        GradleDependencyConfiguration.toMarkers(settings.getBuildscript().getNameToConfiguration().values())
                )
        );
    }
}
