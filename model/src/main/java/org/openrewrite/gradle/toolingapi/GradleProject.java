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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public interface GradleProject {
    String getGroup();

    String getName();

    String getVersion();

    String getPath();

    List<GradlePluginDescriptor> getPlugins();

    List<MavenRepository> getMavenRepositories();

    List<MavenRepository> getMavenPluginRepositories();

    Map<String, GradleDependencyConfiguration> getNameToConfiguration();

    static org.openrewrite.gradle.marker.GradleProject toMarker(GradleProject project) {
        return new org.openrewrite.gradle.marker.GradleProject(
                UUID.randomUUID(),
                project.getGroup(),
                project.getName(),
                project.getVersion(),
                project.getPath(),
                project.getPlugins().stream()
                        .map(GradlePluginDescriptor::toMarker)
                        .collect(Collectors.toList()),
                project.getMavenRepositories().stream()
                        .map(MavenRepository::toMarker)
                        .collect(Collectors.toList()),
                project.getMavenPluginRepositories().stream()
                        .map(MavenRepository::toMarker)
                        .collect(Collectors.toList()),
                GradleDependencyConfiguration.toMarkers(project.getNameToConfiguration().values())
        );
    }
}
