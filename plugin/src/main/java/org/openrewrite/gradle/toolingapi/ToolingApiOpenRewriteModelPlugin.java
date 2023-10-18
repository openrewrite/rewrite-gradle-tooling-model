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
import org.openrewrite.gradle.toolingapi.parser.GradleProjectData;

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
        registry.register(new GradleProjectDataBuilder());
    }

    private static class OpenRewriteModelBuilder implements ToolingModelBuilder {

        @Override
        public boolean canBuild(String modelName) {
            return modelName.equals(OpenRewriteModel.class.getName());
        }

        @Override
        public Object buildAll(String modelName, Project project) {
            GradleProject gradleProject = GradleToolingApiProjectBuilder.gradleProject(project);
            GradleSettings gradleSettings = null;
            if (GradleVersion.current().compareTo(GradleVersion.version("4.4")) >= 0 &&
                    (new File(project.getProjectDir(), "settings.gradle").exists() ||
                            new File(project.getProjectDir(), "settings.gradle.kts").exists())) {
                gradleSettings = GradleToolingApiSettingsBuilder.gradleSettings(((DefaultGradle)project.getGradle()).getSettings());
            }
            return new OpenRewriteModelImpl(gradleProject, gradleSettings);
        }
    }

    private static class GradleProjectDataBuilder implements ToolingModelBuilder {

        @Override
        public boolean canBuild(String modelName) {
            return modelName.equals(GradleProjectData.class.getName());
        }

        @Override
        public Object buildAll(String modelName, Project project) {
            return GradleProjectData.create(project);
        }
    }
}
