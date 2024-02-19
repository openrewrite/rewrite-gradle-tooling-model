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
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.plugins.PluginManagerInternal;
import org.gradle.api.plugins.PluginManager;
import org.gradle.invocation.DefaultGradle;
import org.gradle.plugin.use.PluginId;
import org.gradle.util.GradleVersion;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.gradle.marker.GradleDependencyConfiguration.resolveTransitiveDependencies;
import static org.openrewrite.gradle.marker.GradleSettingsBuilder.GRADLE_PLUGIN_PORTAL;

public final class GradleProjectBuilder {

    private GradleProjectBuilder() {
    }

    public static GradleProject gradleProject(Project project) {
        Set<MavenRepository> pluginRepositories = new HashSet<>();
        if (GradleVersion.current().compareTo(GradleVersion.version("4.4")) >= 0) {
            Settings settings = ((DefaultGradle) project.getGradle()).getSettings();
            pluginRepositories.addAll(mapRepositories(settings.getPluginManagement().getRepositories()));
            pluginRepositories.addAll(mapRepositories(settings.getBuildscript().getRepositories()));
        }
        List<ArtifactRepository> repositories = new ArrayList<>(project.getRepositories());
        if (GradleVersion.current().compareTo(GradleVersion.version("6.8")) >= 0) {
            Settings settings = ((DefaultGradle) project.getGradle()).getSettings();
            //noinspection UnstableApiUsage
            repositories.addAll(settings.getDependencyResolutionManagement().getRepositories());
        }
        pluginRepositories.addAll(mapRepositories(project.getBuildscript().getRepositories()));
        if (pluginRepositories.isEmpty()) {
            pluginRepositories.add(GRADLE_PLUGIN_PORTAL);
        }

        return new GradleProject(Tree.randomId(),
                project.getName(),
                project.getPath(),
                GradleProjectBuilder.pluginDescriptors(project.getPluginManager()),
                mapRepositories(repositories),
                new ArrayList<>(pluginRepositories),
                GradleProjectBuilder.dependencyConfigurations(project.getConfigurations()));
    }

    static List<MavenRepository> mapRepositories(List<ArtifactRepository> repositories) {
        return repositories.stream()
                .filter(MavenArtifactRepository.class::isInstance)
                .map(MavenArtifactRepository.class::cast)
                .map(repo -> MavenRepository.builder()
                        .id(repo.getName())
                        .uri(repo.getUrl().toString())
                        .releases(true)
                        .snapshots(true)
                        .build())
                .collect(toList());
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

    private static final Map<GroupArtifact, GroupArtifact> groupArtifactCache = new ConcurrentHashMap<>();

    private static GroupArtifact groupArtifact(org.openrewrite.maven.tree.Dependency dep) {
        //noinspection ConstantConditions
        return groupArtifactCache.computeIfAbsent(new GroupArtifact(dep.getGroupId(), dep.getArtifactId()), it -> it);
    }

    private static GroupArtifact groupArtifact(ResolvedDependency dep) {
        return groupArtifactCache.computeIfAbsent(new GroupArtifact(dep.getModuleGroup(), dep.getModuleName()), it -> it);
    }

    private static final Map<GroupArtifactVersion, GroupArtifactVersion> groupArtifactVersionCache = new ConcurrentHashMap<>();

    private static GroupArtifactVersion groupArtifactVersion(ResolvedDependency dep) {
        return groupArtifactVersionCache.computeIfAbsent(
                new GroupArtifactVersion(dep.getModuleGroup(), dep.getModuleName(), unspecifiedToNull(dep.getModuleVersion())),
                it -> it);
    }

    private static GroupArtifactVersion groupArtifactVersion(Dependency dep) {
        return groupArtifactVersionCache.computeIfAbsent(
                new GroupArtifactVersion(dep.getGroup(), dep.getName(), unspecifiedToNull(dep.getVersion())), it -> it);
    }

    private static final Map<ResolvedGroupArtifactVersion, ResolvedGroupArtifactVersion> resolvedGroupArtifactVersionCache = new ConcurrentHashMap<>();

    private static ResolvedGroupArtifactVersion resolvedGroupArtifactVersion(ResolvedDependency dep) {
        return resolvedGroupArtifactVersionCache.computeIfAbsent(new ResolvedGroupArtifactVersion(
                        null, dep.getModuleGroup(), dep.getModuleName(), dep.getModuleVersion(), null),
                it -> it);
    }

    /**
     * Some Gradle dependency functions will have the String "unspecified" to indicate a missing value.
     * Rewrite's dependency API represents these missing things as "null"
     */
    @Nullable
    private static String unspecifiedToNull(@Nullable String maybeUnspecified) {
        if ("unspecified".equals(maybeUnspecified)) {
            return null;
        }
        return maybeUnspecified;
    }

    private static Map<String, GradleDependencyConfiguration> dependencyConfigurations(ConfigurationContainer configurationContainer) {
        Map<String, GradleDependencyConfiguration> results = new HashMap<>();
        List<Configuration> configurations = new ArrayList<>(configurationContainer);
        for (Configuration conf : configurations) {
            try {
                List<org.openrewrite.maven.tree.Dependency> requested = conf.getAllDependencies().stream()
                        .map(dep -> dependency(dep, conf))
                        .collect(Collectors.toList());

                List<org.openrewrite.maven.tree.ResolvedDependency> resolved;
                Map<GroupArtifact, org.openrewrite.maven.tree.Dependency> gaToRequested = requested.stream()
                        .collect(Collectors.toMap(GradleProjectBuilder::groupArtifact, dep -> dep, (a, b) -> a));
                String exceptionType = null;
                String exceptionMessage = null;
                // Archives and default are redundant with other configurations
                // Newer versions of gradle display warnings with long stack traces when attempting to resolve them
                // Some Scala plugin we don't care about creates configurations that, for some unknown reason, are difficult to resolve
                if (conf.isCanBeResolved() && !"archives".equals(conf.getName()) && !"default".equals(conf.getName()) && !conf.getName().startsWith("incrementalScalaAnalysis")) {
                    ResolvedConfiguration resolvedConf = conf.getResolvedConfiguration();
                    if (resolvedConf.hasError()) {
                        try {
                            resolvedConf.rethrowFailure();
                        } catch (ResolveException e) {
                            exceptionType = e.getClass().getName();
                            exceptionMessage = e.getMessage();
                        }
                    }
                    Map<GroupArtifact, ResolvedDependency> gaToResolved = resolvedConf.getFirstLevelModuleDependencies().stream()
                            .collect(Collectors.toMap(GradleProjectBuilder::groupArtifact, dep -> dep, (a, b) -> a));
                    resolved = resolved(gaToRequested, gaToResolved);
                } else {
                    resolved = emptyList();
                }
                List<org.openrewrite.maven.tree.ResolvedDependency> transitive = resolveTransitiveDependencies(resolved, new LinkedHashSet<>());
                GradleDependencyConfiguration dc = new GradleDependencyConfiguration(conf.getName(), conf.getDescription(),
                        conf.isTransitive(), conf.isCanBeResolved(), conf.isCanBeConsumed(), emptyList(), requested, resolved, transitive, exceptionType, exceptionMessage);
                results.put(conf.getName(), dc);
            } catch (Exception e) {
                GradleDependencyConfiguration dc = new GradleDependencyConfiguration(conf.getName(), conf.getDescription(),
                        conf.isTransitive(), conf.isCanBeResolved(), conf.isCanBeConsumed(), emptyList(), emptyList(), emptyList(), emptyList(), e.getClass().getName(), e.getMessage());
                results.put(conf.getName(), dc);
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
                        .collect(Collectors.toList());
                dc.unsafeSetExtendsFrom(extendsFrom);
            }
        }
        return results;
    }

    private static final Map<GroupArtifactVersion, org.openrewrite.maven.tree.Dependency>
            requestedCache = new ConcurrentHashMap<>();

    private static org.openrewrite.maven.tree.Dependency dependency(Dependency dep, Configuration configuration) {
        GroupArtifactVersion gav = groupArtifactVersion(dep);
        return requestedCache.computeIfAbsent(gav, it ->
                org.openrewrite.maven.tree.Dependency.builder()
                        .gav(gav)
                        .type("jar")
                        .scope(configuration.getName())
                        .exclusions(emptyList())
                        .build()
        );
    }


    private static List<org.openrewrite.maven.tree.ResolvedDependency> resolved(
            Map<GroupArtifact, org.openrewrite.maven.tree.Dependency> gaToRequested,
            Map<GroupArtifact, ResolvedDependency> gaToResolved) {
        Map<org.openrewrite.maven.tree.ResolvedGroupArtifactVersion, org.openrewrite.maven.tree.ResolvedDependency>
                resolvedCache = new HashMap<>();
        return gaToResolved.entrySet().stream()
                .map(entry -> {
                    GroupArtifact ga = entry.getKey();
                    ResolvedDependency resolved = entry.getValue();

                    // Gradle knows which repository it got a dependency from, but haven't been able to find where that info lives
                    ResolvedGroupArtifactVersion resolvedGav = resolvedGroupArtifactVersion(resolved);
                    org.openrewrite.maven.tree.ResolvedDependency resolvedDependency = resolvedCache.get(resolvedGav);
                    if (resolvedDependency == null) {
                        resolvedDependency = org.openrewrite.maven.tree.ResolvedDependency.builder()
                                .gav(resolvedGav)
                                // There may not be a requested entry if a dependency substitution rule took effect
                                // the DependencyHandler has the substitution mapping buried inside it, but not exposed publicly
                                // Possible improvement to dig that out and use it
                                .requested(gaToRequested.getOrDefault(ga, dependency(resolved)))
                                .dependencies(resolved.getChildren().stream()
                                        .map(child -> resolved(child, 1, resolvedCache))
                                        .collect(toList()))
                                .licenses(emptyList())
                                .depth(0)
                                .build();
                        resolvedCache.put(resolvedGav, resolvedDependency);
                    }
                    return resolvedDependency;
                })
                .collect(Collectors.toList());
    }

    /**
     * When there is a resolved dependency that cannot be matched up with a requested dependency, construct a requested
     * dependency corresponding to the exact version which was resolved. This isn't strictly accurate, but there is no
     * obvious way to access the resolution of transitive dependencies to figure out what versions are requested during
     * the resolution process.
     */
    private static org.openrewrite.maven.tree.Dependency dependency(ResolvedDependency dep) {
        GroupArtifactVersion gav = groupArtifactVersion(dep);
        return requestedCache.computeIfAbsent(gav, it ->
                org.openrewrite.maven.tree.Dependency.builder()
                        .gav(gav)
                        .type("jar")
                        .scope(dep.getConfiguration())
                        .exclusions(emptyList())
                        .build()
        );
    }

    private static org.openrewrite.maven.tree.ResolvedDependency resolved(
            ResolvedDependency dep, int depth,
            Map<org.openrewrite.maven.tree.ResolvedGroupArtifactVersion, org.openrewrite.maven.tree.ResolvedDependency> resolvedCache
    ) {
        ResolvedGroupArtifactVersion resolvedGav = resolvedGroupArtifactVersion(dep);
        org.openrewrite.maven.tree.ResolvedDependency resolvedDependency = resolvedCache.get(resolvedGav);
        if (resolvedDependency == null) {

            List<org.openrewrite.maven.tree.ResolvedDependency> dependencies = new ArrayList<>();

            resolvedDependency = org.openrewrite.maven.tree.ResolvedDependency.builder()
                    .gav(resolvedGav)
                    .requested(dependency(dep))
                    .dependencies(dependencies)
                    .licenses(emptyList())
                    .depth(depth).build();
            //we add a temporal resolved dependency in the cache to avoid stackoverflow with dependencies that have cycles
            resolvedCache.put(resolvedGav, resolvedDependency);
            dep.getChildren().forEach(child -> dependencies.add(resolved(child, depth + 1, resolvedCache)));
        }
        return resolvedDependency;
    }

    @SuppressWarnings("unused")
    public static void clearCaches() {
        requestedCache.clear();
        groupArtifactCache.clear();
        groupArtifactVersionCache.clear();
        resolvedGroupArtifactVersionCache.clear();
    }
}
