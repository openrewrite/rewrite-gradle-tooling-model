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
package org.openrewrite.gradle.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.MavenRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Contains metadata about a Gradle Project. Queried from Gradle itself when the OpenRewrite build plugin runs.
 * Not automatically available on LSTs that aren't parsed through a Gradle plugin, so tests won't automatically have
 * access to this metadata.
 */
@Value
@With
public class GradleProject implements Marker, Serializable {
    UUID id;
    String name;
    String path;
    List<GradlePluginDescriptor> plugins;
    List<MavenRepository> mavenRepositories;
    Map<String, GradleDependencyConfiguration> nameToConfiguration;

    public GradleDependencyConfiguration getConfiguration(String name) {
        return nameToConfiguration.get(name);
    }

    public List<GradleDependencyConfiguration> getConfigurations() {
        return new ArrayList<>(nameToConfiguration.values());
    }

    @Nullable
    public Dependency findDependency(String configuration, String groupId, String artifactId) {
        return nameToConfiguration.get(configuration).getRequested().stream()
                .filter(d -> StringUtils.matchesGlob(d.getGav().getGroupId(), groupId) &&
                             StringUtils.matchesGlob(d.getGav().getArtifactId(), artifactId))
                .findFirst()
                .orElse(null);
    }

    public static GradleProject fromToolingModel(org.openrewrite.gradle.toolingapi.GradleProject project) {
        return new GradleProject(
                UUID.randomUUID(),
                project.getName(),
                project.getPath(),
                project.getPlugins().stream()
                        .map(GradlePluginDescriptor::fromToolingModel)
                        .collect(Collectors.toList()),
                project.getMavenRepositories().stream()
                        .map(GradleProject::fromToolingModel)
                        .collect(Collectors.toList()),
                project.getNameToConfiguration().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> GradleDependencyConfiguration.fromToolingModel(e.getValue())))
        );
    }

    @Nullable
    static MavenRepository fromToolingModel(@Nullable org.openrewrite.gradle.toolingapi.MavenRepository mavenRepository) {
        if (mavenRepository == null) {
            return null;
        }
        return new MavenRepository(
                mavenRepository.getId(),
                mavenRepository.getUri(),
                mavenRepository.getReleases(),
                mavenRepository.getSnapshots(),
                mavenRepository.isKnownToExist(),
                mavenRepository.getUsername(),
                mavenRepository.getPassword(),
                mavenRepository.getDeriveMetadataIfMissing()
        );
    }
}
