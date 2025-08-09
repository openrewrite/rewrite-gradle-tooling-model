/*
 * Copyright 2025 the original author or authors.
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
import java.util.List;
import java.util.stream.Collectors;

public interface GradleDependencyConstraint {
    String getGroupId();
    String getArtifactId();
    String getRequiredVersion();
    String getPreferredVersion();
    String getStrictVersion();
    String getBranch();
    String getReason();
    List<String> getRejectedVersions();

    static org.openrewrite.gradle.marker.GradleDependencyConstraint toMarker(GradleDependencyConstraint constraint) {
        return org.openrewrite.gradle.marker.GradleDependencyConstraint.builder()
                .groupId(constraint.getGroupId())
                .artifactId(constraint.getArtifactId())
                .requiredVersion(constraint.getRequiredVersion())
                .preferredVersion(constraint.getPreferredVersion())
                .strictVersion(constraint.getStrictVersion())
                .branch(constraint.getBranch())
                .reason(constraint.getReason())
                .rejectedVersions(constraint.getRejectedVersions())
                .build();
    }

    static List<org.openrewrite.gradle.marker.GradleDependencyConstraint> toMarker(Collection<GradleDependencyConstraint> constraints) {
        return constraints.stream()
                .map(GradleDependencyConstraint::toMarker)
                .collect(Collectors.toList());
    }
}
