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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.invocation.DefaultGradle;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;
import org.gradle.util.GradleVersion;
import org.openrewrite.gradle.marker.GradleProjectBuilder;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.gradle.marker.GradleSettingsBuilder;

import javax.inject.Inject;
import java.io.File;

public class ToolingApiOpenRewriteModelPlugin implements Plugin<Project> {
    private final ToolingModelBuilderRegistry registry;

    @Inject
    public ToolingApiOpenRewriteModelPlugin(ToolingModelBuilderRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void apply(Project project) {
        registry.register(new OpenRewriteModelBuilder());
    }

    private static class OpenRewriteModelBuilder implements ToolingModelBuilder {

        @Override
        public boolean canBuild(String modelName) {
            return modelName.equals(OpenRewriteModel.class.getName());
        }

        @Override
        public Object buildAll(String modelName, Project project) {
            org.openrewrite.gradle.marker.GradleProject gradleProject = GradleProjectBuilder.gradleProject(project);
            GradleSettings gradleSettings = null;
            if (GradleVersion.current().compareTo(GradleVersion.version("4.4")) >= 0 &&
                (new File(project.getProjectDir(), "settings.gradle").exists() ||
                 new File(project.getProjectDir(), "settings.gradle.kts").exists())) {
                gradleSettings = GradleSettingsBuilder.gradleSettings(((DefaultGradle) project.getGradle()).getSettings());
            }
            return new OpenRewriteModelImpl(gradleProject, gradleSettings);
        }
    }
}
