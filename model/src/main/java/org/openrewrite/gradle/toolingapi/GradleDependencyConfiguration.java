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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public interface GradleDependencyConfiguration {
    String getName();

    String getDescription();

    boolean isTransitive();

    boolean isCanBeConsumed();

    boolean isCanBeResolved();

    boolean isCanBeDeclared();

    List<GradleDependencyConfiguration> getExtendsFrom();

    List<Dependency> getRequested();

    List<ResolvedDependency> getResolved();

    static Map<String, org.openrewrite.gradle.marker.GradleDependencyConfiguration> toMarkers(Collection<GradleDependencyConfiguration> configurations) {
        Map<String, org.openrewrite.gradle.marker.GradleDependencyConfiguration> results = new HashMap<>();
        for (org.openrewrite.gradle.toolingapi.GradleDependencyConfiguration config : configurations) {
            results.put(config.getName(), new org.openrewrite.gradle.marker.GradleDependencyConfiguration(
                    config.getName(),
                    config.getDescription(),
                    config.isTransitive(),
                    config.isCanBeResolved(),
                    config.isCanBeConsumed(),
                    config.isCanBeDeclared(),
                    emptyList(),
                    config.getRequested().stream().map(org.openrewrite.gradle.toolingapi.Dependency::toMarkers)
                            .collect(Collectors.toList()),
                    org.openrewrite.gradle.toolingapi.ResolvedDependency.toMarker(config.getResolved()),
                    null,
                    null
            ));
        }

        // Record the relationships between dependency configurations
        for (org.openrewrite.gradle.toolingapi.GradleDependencyConfiguration conf : configurations) {
            if (conf.getExtendsFrom().isEmpty()) {
                continue;
            }
            org.openrewrite.gradle.marker.GradleDependencyConfiguration dc = results.get(conf.getName());
            if (dc != null) {
                List<org.openrewrite.gradle.marker.GradleDependencyConfiguration> extendsFrom = conf.getExtendsFrom().stream()
                        .map(it -> results.get(it.getName()))
                        .collect(Collectors.toList());
                dc.unsafeSetExtendsFrom(extendsFrom);
            }
        }
        return results;
    }
}
