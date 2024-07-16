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

import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.internal.lang.Nullable;

import java.io.Serializable;

public class OpenRewriteModelImpl implements Serializable {
    private final GradleProject gradleProject;
    private final GradleSettings gradleSettings;

    public OpenRewriteModelImpl(GradleProject gradleProject, @Nullable GradleSettings gradleSettings) {
        this.gradleProject = gradleProject;
        this.gradleSettings = gradleSettings;
    }

    public GradleProject gradleProject() {
        return gradleProject;
    }

    public @Nullable GradleSettings gradleSettings() {
        return gradleSettings;
    }
}
