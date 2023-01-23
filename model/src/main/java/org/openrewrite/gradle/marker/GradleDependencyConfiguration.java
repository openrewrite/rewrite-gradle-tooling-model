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
import lombok.experimental.NonFinal;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.*;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Value
@With
public class GradleDependencyConfiguration implements Serializable {

    /**
     * The name of the dependency configuration. Unique within a given project.
     */
    String name;

    @Nullable
    String description;

    boolean isTransitive;

    boolean isCanBeResolved;

    /**
     * The list of zero or more configurations this configuration extends from.
     * The extended configuration's dependencies are all requested as part of this configuration, but different versions
     * may be resolved.
     */
    @NonFinal
    List<GradleDependencyConfiguration> extendsFrom;

    List<Dependency> requested;
    List<ResolvedDependency> resolved;

    public static GradleDependencyConfiguration fromToolingModel(org.openrewrite.gradle.toolingapi.GradleDependencyConfiguration config) {
        return new GradleDependencyConfiguration(
                config.getName(),
                config.getDescription(),
                false,
                true,
                config.getExtendsFrom().stream()
                        .map(GradleDependencyConfiguration::fromToolingModel)
                        .collect(Collectors.toList()),
                config.getRequested().stream().map(GradleDependencyConfiguration::fromToolingModel)
                        .collect(Collectors.toList()),
                config.getResolved().stream().map(GradleDependencyConfiguration::fromToolingModel)
                        .collect(Collectors.toList())
        );
    }

    void unsafeSetExtendsFrom(List<GradleDependencyConfiguration> extendsFrom) {
        this.extendsFrom = extendsFrom;
    }

    private static ResolvedDependency fromToolingModel(org.openrewrite.gradle.toolingapi.ResolvedDependency dep) {
        return ResolvedDependency.builder()
                .repository(org.openrewrite.gradle.marker.GradleProject.fromToolingModel(dep.getRepository()))
                .gav(fromToolingModel(dep.getGav()))
                .requested(fromToolingModel(dep.getRequested()))
                .dependencies(dep.getDependencies().stream()
                        .map(GradleDependencyConfiguration::fromToolingModel)
                        .collect(Collectors.toList()))
                .licenses(emptyList())
                .depth(dep.getDepth())
                .build();
    }

    private static Dependency fromToolingModel(org.openrewrite.gradle.toolingapi.Dependency dep) {
        return Dependency.builder()
                .gav(fromToolingModel(dep.getGav()))
                .scope(dep.getScope())
                .type(dep.getType())
                .exclusions(dep.getExclusions().stream()
                        .map(GradleDependencyConfiguration::fromToolingModel)
                        .collect(Collectors.toList()))
                .optional(dep.getOptional())
                .build();
    }

    private static GroupArtifact fromToolingModel(org.openrewrite.gradle.toolingapi.GroupArtifact ga) {
        return new GroupArtifact(ga.getGroupId(), ga.getArtifactId());
    }

    private static GroupArtifactVersion fromToolingModel(org.openrewrite.gradle.toolingapi.GroupArtifactVersion gav) {
        return new GroupArtifactVersion(gav.getGroupId(), gav.getArtifactId(), gav.getVersion());
    }

    private static ResolvedGroupArtifactVersion fromToolingModel(org.openrewrite.gradle.toolingapi.ResolvedGroupArtifactVersion gav) {
        return new ResolvedGroupArtifactVersion(null, gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), gav.getDatedSnapshotVersion());
    }
}
