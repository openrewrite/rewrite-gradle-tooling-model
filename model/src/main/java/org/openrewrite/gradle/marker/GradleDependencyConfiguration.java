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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.*;

import java.io.Serializable;
import java.util.*;
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

    boolean isCanBeConsumed;

    /**
     * The list of zero or more configurations this configuration extends from.
     * The extended configuration's dependencies are all requested as part of this configuration, but different versions
     * may be resolved.
     */
    @NonFinal
    List<GradleDependencyConfiguration> extendsFrom;

    List<Dependency> requested;

    /**
     * The list of direct dependencies resolved for this configuration.
     */
    List<ResolvedDependency> directResolved;
    /**
     * The list of all dependencies resolved for this configuration, including transitive dependencies.
     */
    List<ResolvedDependency> resolved;

    /**
     * The type of exception thrown when attempting to resolve this configuration. null if no exception was thrown.
     */
    @Nullable
    String exceptionType;

    /**
     * The message of the exception thrown when attempting to resolve this configuration. null if no exception was thrown.
     */
    @Nullable
    String message;

    /**
     * List the configurations which are extended by the given configuration.
     * Assuming a hierarchy like:
     * <pre>
     *     implementation
     *     |> compileClasspath
     *     |> runtimeClasspath
     *     |> testImplementation
     *        |> testCompileClasspath
     *        |> testRuntimeClasspath
     * </pre>
     * <p>
     * When querying "testCompileClasspath" this function will return [testImplementation, implementation].
     */
    public List<GradleDependencyConfiguration> allExtendsFrom() {
        Set<GradleDependencyConfiguration> result = new LinkedHashSet<>();
        for (GradleDependencyConfiguration parentConfiguration : getExtendsFrom()) {
            result.add(parentConfiguration);
            result.addAll(parentConfiguration.allExtendsFrom());
        }
        return new ArrayList<>(result);
    }

    @Nullable
    public Dependency findRequestedDependency(String groupId, String artifactId) {
        return requested.stream()
                .filter(d -> StringUtils.matchesGlob(d.getGav().getGroupId(), groupId) &&
                             StringUtils.matchesGlob(d.getGav().getArtifactId(), artifactId))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public ResolvedDependency findResolvedDependency(String groupId, String artifactId) {
        return resolved.stream()
                .map(d -> d.findDependency(groupId, artifactId))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public static Map<String, GradleDependencyConfiguration> fromToolingModel(Map<String, org.openrewrite.gradle.toolingapi.GradleDependencyConfiguration> toolingConfigurations) {
        Map<String, GradleDependencyConfiguration> results = new HashMap<>();
        List<org.openrewrite.gradle.toolingapi.GradleDependencyConfiguration> configurations = new ArrayList<>(toolingConfigurations.values());
        for (org.openrewrite.gradle.toolingapi.GradleDependencyConfiguration toolingConfiguration : configurations) {
            GradleDependencyConfiguration configuration = fromToolingModel(toolingConfiguration);
            results.put(configuration.getName(), configuration);
        }

        // Record the relationships between dependency configurations
        for (org.openrewrite.gradle.toolingapi.GradleDependencyConfiguration conf : configurations) {
            if (conf.getExtendsFrom().isEmpty()) {
                continue;
            }
            GradleDependencyConfiguration dc = results.get(conf.getName());
            if (dc != null) {
                List<GradleDependencyConfiguration> extendsFrom = conf.getExtendsFrom().stream()
                        .map(it -> results.get(it.getName()))
                        .collect(Collectors.toList());
                dc.unsafeSetExtendsFrom(extendsFrom);
            }
        }
        return results;
    }

    private static GradleDependencyConfiguration fromToolingModel(org.openrewrite.gradle.toolingapi.GradleDependencyConfiguration config) {
        List<ResolvedDependency> direct = config.getResolved().stream().map(GradleDependencyConfiguration::fromToolingModel)
                .collect(Collectors.toList());
        List<ResolvedDependency> transitive = resolveTransitiveDependencies(direct, new LinkedHashSet<>());
        return new GradleDependencyConfiguration(
                config.getName(),
                config.getDescription(),
                config.isTransitive(),
                config.isCanBeResolved(),
                config.isCanBeConsumed(),
                emptyList(),
                config.getRequested().stream().map(GradleDependencyConfiguration::fromToolingModel)
                        .collect(Collectors.toList()),
                direct,
                transitive,
                null,
                null
        );
    }

    /**
     * Recursively resolve all transitive dependencies for the given list of resolved dependencies.
     * @param resolved The list of resolved dependencies to resolve transitive dependencies for.
     * @param alreadyResolved A set of dependencies that have already been resolved. This is used to prevent infinite recursion.
     * @return A list of all transitive dependencies for the given list of resolved dependencies.
     */
    static List<ResolvedDependency> resolveTransitiveDependencies(List<ResolvedDependency> resolved, Set<ResolvedDependency> alreadyResolved) {
        for (ResolvedDependency dependency : resolved) {
            if (alreadyResolved.add(dependency)) {
                alreadyResolved.addAll(resolveTransitiveDependencies(dependency.getDependencies(), alreadyResolved));
            }
        }
        return new ArrayList<>(alreadyResolved);
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
                // Setting depth to "0" everywhere isn't accurate, but this saves memory and
                // depth isn't meaningful for Gradle conflict resolution the way it is for Maven.
                .depth(0)
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
