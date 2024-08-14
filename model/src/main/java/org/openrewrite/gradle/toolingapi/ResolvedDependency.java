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

import org.jspecify.annotations.Nullable;

import java.util.*;

import static java.util.Collections.emptyList;

public interface ResolvedDependency {
    @Nullable
    MavenRepository getRepository();

    ResolvedGroupArtifactVersion getGav();

    Dependency getRequested();

    /**
     * Direct dependencies only that survived conflict resolution and exclusion.
     */
    List<ResolvedDependency> getDependencies();

    int getDepth();

    static List<org.openrewrite.maven.tree.ResolvedDependency> toMarker(Collection<ResolvedDependency> deps) {
        ResolvedDependencyMapper mapper = new ResolvedDependencyMapper();
        List<org.openrewrite.maven.tree.ResolvedDependency> resolved = new ArrayList<>(deps.size());
        for (org.openrewrite.gradle.toolingapi.ResolvedDependency dep : deps) {
            resolved.add(mapper.toMarker(dep, 0));
        }
        return resolved;
    }
}

class ResolvedDependencyMapper {
    private final Map<org.openrewrite.maven.tree.ResolvedGroupArtifactVersion, org.openrewrite.maven.tree.ResolvedDependency> resolvedCache = new HashMap<>();

    org.openrewrite.maven.tree.ResolvedDependency toMarker(ResolvedDependency dep, int depth) {
        org.openrewrite.maven.tree.ResolvedGroupArtifactVersion gav = new org.openrewrite.maven.tree.ResolvedGroupArtifactVersion(null,
                dep.getGav().getGroupId(), dep.getGav().getArtifactId(), dep.getGav().getVersion(), dep.getGav().getDatedSnapshotVersion());
        org.openrewrite.maven.tree.ResolvedDependency resolvedDependency = resolvedCache.get(gav);
        if (resolvedDependency != null) {
            return resolvedDependency;
        }
        List<org.openrewrite.maven.tree.ResolvedDependency> deps = new ArrayList<>(dep.getDependencies().size());
        resolvedDependency = resolvedCache.computeIfAbsent(
                gav,
                k -> org.openrewrite.maven.tree.ResolvedDependency.builder()
                        .repository(MavenRepository.toMarker(dep.getRepository()))
                        .gav(gav)
                        .requested(Dependency.toMarkers(dep.getRequested()))
                        .dependencies(deps)
                        .licenses(emptyList())
                        .depth(dep.getDepth())
                        .build()
        );
        dep.getDependencies().stream()
                .map(d -> toMarker(d, depth + 1))
                .forEach(deps::add);
        return resolvedDependency;
    }
}
