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

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.plugins.PluginManagerInternal;
import org.gradle.api.plugins.PluginManager;
import org.gradle.plugin.use.PluginId;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public final class GradleProjectBuilder {

    private GradleProjectBuilder() {
    }

    public static GradleProject gradleProject(Project project) {
        return new GradleProject(Tree.randomId(),
                project.getName(),
                project.getPath(),
                GradleProjectBuilder.pluginDescriptors(project.getPluginManager()),
                project.getRepositories().stream()
                        .filter(MavenArtifactRepository.class::isInstance)
                        .map(MavenArtifactRepository.class::cast)
                        .map(repo -> MavenRepository.builder()
                                .id(repo.getName())
                                .uri(repo.getUrl().toString())
                                .releases(true)
                                .snapshots(true)
                                .build())
                        .collect(toList()),
                GradleProjectBuilder.dependencyConfigurations(project.getConfigurations()));
    }

    public static List<GradlePluginDescriptor> pluginDescriptors(@Nullable PluginManager pluginManager) {
        if (pluginManager instanceof PluginManagerInternal) {
            return pluginDescriptors((PluginManagerInternal) pluginManager);
        }
        return emptyList();
    }

    public static List<GradlePluginDescriptor> pluginDescriptors(PluginManagerInternal pluginManager) {
        return pluginManager.getPluginContainer().stream()
                .map(plugin -> new GradlePluginDescriptor(
                        plugin.getClass().getName(),
                        pluginIdForClass(pluginManager, plugin.getClass())))
                .collect(toList());
    }

    @Nullable
    private static String pluginIdForClass(PluginManagerInternal pluginManager, Class<?> pluginClass) {
        try {
            Method findPluginIdForClass = PluginManagerInternal.class.getMethod("findPluginIdForClass", Class.class);
            //noinspection unchecked
            Optional<PluginId> maybePluginId = (Optional<PluginId>) findPluginIdForClass.invoke(pluginManager, pluginClass);
            return maybePluginId.map(PluginId::getId).orElse(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // On old versions of gradle that don't have this method, returning null is fine
        }
        return null;
    }

    private static final Map<GroupArtifactVersion, GroupArtifactVersion> groupArtifactVersionCache = new HashMap<>();

    private static GroupArtifactVersion groupArtifactVersion(Dependency dep) {
        return groupArtifactVersionCache.computeIfAbsent(
                new GroupArtifactVersion(dep.getGroup(), dep.getName(), dep.getVersion()),
                it -> it);
    }

    private static GroupArtifactVersion groupArtifactVersion(org.openrewrite.maven.tree.Dependency dep) {
        return groupArtifactVersionCache.computeIfAbsent(
                new GroupArtifactVersion(dep.getGroupId(), dep.getArtifactId(), dep.getVersion()),
                it -> it);
    }

    private static GroupArtifactVersion groupArtifactVersion(ResolvedDependency dep) {
        return groupArtifactVersionCache.computeIfAbsent(
                new GroupArtifactVersion(dep.getModuleGroup(), dep.getModuleName(), dep.getModuleVersion()),
                it -> it);
    }

    private static final Map<ResolvedGroupArtifactVersion, ResolvedGroupArtifactVersion> resolvedGroupArtifactVersionCache = new HashMap<>();

    private static ResolvedGroupArtifactVersion resolvedGroupArtifactVersion(ResolvedDependency dep) {
        return resolvedGroupArtifactVersionCache.computeIfAbsent(new ResolvedGroupArtifactVersion(
                        null, dep.getModuleGroup(), dep.getModuleName(), dep.getModuleVersion(), null),
                it -> it);
    }

    private static Map<String, GradleDependencyConfiguration> dependencyConfigurations(ConfigurationContainer configurationContainer) {
        Map<String, GradleDependencyConfiguration> results = new HashMap<>();
        List<Configuration> configurations = new ArrayList<>(configurationContainer);
        for (Configuration conf : configurations) {
            Map<GroupArtifactVersion, org.openrewrite.maven.tree.Dependency> requestedCache = new HashMap<>();
            List<org.openrewrite.maven.tree.Dependency> requested = conf.getDependencies().stream()
                    .map(dep -> dependency(requestedCache, dep, conf))
                    .collect(toList());

            try {
                List<org.openrewrite.maven.tree.ResolvedDependency> resolved;
                if (conf.isCanBeResolved()) {
                    ResolvedConfiguration resolvedConf = conf.getResolvedConfiguration();

                    Map<ResolvedGroupArtifactVersion, org.openrewrite.maven.tree.ResolvedDependency> resolvedCache = new HashMap<>();
                    Map<GroupArtifactVersion, org.openrewrite.maven.tree.Dependency> gavToRequested = requested.stream()
                            .collect(Collectors.toMap(GradleProjectBuilder::groupArtifactVersion, dep -> dep, (a, b) -> a));
                    Map<GroupArtifactVersion, ResolvedDependency> gavToResolved = resolvedConf.getFirstLevelModuleDependencies().stream()
                            .collect(Collectors.toMap(GradleProjectBuilder::groupArtifactVersion, dep -> dep, (a, b) -> a));
                    resolved = resolved(requestedCache, resolvedCache, gavToRequested, gavToResolved);
                } else {
                    resolved = emptyList();
                }

                GradleDependencyConfiguration dc = new GradleDependencyConfiguration(conf.getName(), conf.getDescription(),
                        conf.isTransitive(), conf.isCanBeConsumed(), conf.isCanBeResolved(), emptyList(), requested, resolved);
                results.put(conf.getName(), dc);
            } catch (Exception e) {
                // No need to fail constructing the marker if a configuration cannot be resolved
            }
        }

        // Record the relationships between dependency configurations
        for (Configuration conf : configurations) {
            if (conf.getExtendsFrom().isEmpty()) {
                continue;
            }
            GradleDependencyConfiguration dc = results.get(conf.getName());
            if (dc != null) {
                List<GradleDependencyConfiguration> extendsFrom = conf.getExtendsFrom().stream()
                        .map(it -> results.get(it.getName()))
                        .collect(toList());
                dc.unsafeSetExtendsFrom(extendsFrom);
            }
        }
        return results;
    }

    private static org.openrewrite.maven.tree.Dependency dependency(
            Map<GroupArtifactVersion, org.openrewrite.maven.tree.Dependency> requestedCache,
            Dependency dep,
            Configuration configuration
    ) {
        return requestedCache.computeIfAbsent(groupArtifactVersion(dep), gav ->
                org.openrewrite.maven.tree.Dependency.builder()
                        .gav(gav)
                        .type("jar")
                        .scope(configuration.getName())
                        .exclusions(emptyList())
                        .build());
    }

    private static List<org.openrewrite.maven.tree.ResolvedDependency> resolved(
            Map<GroupArtifactVersion, org.openrewrite.maven.tree.Dependency> requestedCache,
            Map<ResolvedGroupArtifactVersion, org.openrewrite.maven.tree.ResolvedDependency> resolvedCache,
            Map<GroupArtifactVersion, org.openrewrite.maven.tree.Dependency> gavToRequested,
            Map<GroupArtifactVersion, ResolvedDependency> gavToResolved
    ) {
        return gavToResolved.entrySet().stream()
                .map(entry -> {
                    GroupArtifactVersion gav = entry.getKey();
                    ResolvedDependency resolved = entry.getValue();
                    // There may not be a requested entry if a dependency substitution rule took effect
                    // the DependencyHandler has the substitution mapping buried inside it, but not exposed publicly
                    // Possible improvement to dig that out and use it
                    org.openrewrite.maven.tree.Dependency requested = gavToRequested.computeIfAbsent(gav, ignored -> dependency(requestedCache, resolved));
                    // Gradle knows which repository it got a dependency from, but haven't been able to find where that info lives
                    return resolvedCache.computeIfAbsent(resolvedGroupArtifactVersion(resolved), resolvedGav -> org.openrewrite.maven.tree.ResolvedDependency.builder()
                                    .gav(resolvedGav)
                                    .requested(requested)
                                    .dependencies(resolved.getChildren().stream()
                                            .map(child -> resolved(requestedCache, resolvedCache, child, 1))
                                            .collect(toList()))
                                    .licenses(emptyList())
                                    .depth(0)
                                    .build());
                })
                .collect(toList());
    }

    /**
     * When there is a resolved dependency that cannot be matched up with a requested dependency, construct a requested
     * dependency corresponding to the exact version which was resolved. This isn't strictly accurate, but there is no
     * obvious way to access the resolution of transitive dependencies to figure out what versions are requested during
     * the resolution process.
     */
    private static org.openrewrite.maven.tree.Dependency dependency(
            Map<GroupArtifactVersion, org.openrewrite.maven.tree.Dependency> requestedCache,
            ResolvedDependency dep
    ) {
        return requestedCache.computeIfAbsent(groupArtifactVersion(dep),
                gav ->org.openrewrite.maven.tree.Dependency.builder()
                        .gav(gav)
                        .type("jar")
                        .scope(dep.getConfiguration())
                        .exclusions(emptyList())
                        .build());
    }

    private static org.openrewrite.maven.tree.ResolvedDependency resolved(
            Map<GroupArtifactVersion, org.openrewrite.maven.tree.Dependency> requestedCache,
            Map<ResolvedGroupArtifactVersion, org.openrewrite.maven.tree.ResolvedDependency> resolvedCache,
            ResolvedDependency dep,
            int depth
    ) {
        return resolvedCache.computeIfAbsent(resolvedGroupArtifactVersion(dep),
                gav -> org.openrewrite.maven.tree.ResolvedDependency.builder()
                        .gav(gav)
                        .requested(dependency(requestedCache, dep))
                        .dependencies(dep.getChildren().stream()
                                .map(child -> resolved(requestedCache, resolvedCache, child, depth + 1))
                                .collect(toList()))
                        .licenses(emptyList())
                        .depth(0)
                        .build());
    }

    public static void clearCaches() {
        groupArtifactVersionCache.clear();
    }
}
