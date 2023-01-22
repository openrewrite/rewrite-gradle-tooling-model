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

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class OpenRewriteModelBuilder {
    public static OpenRewriteModel forProjectDirectory(File projectDir) {
        GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(projectDir);
        try (ProjectConnection connection = connector.connect()) {
            ModelBuilder<OpenRewriteModel> customModelBuilder = connection.model(OpenRewriteModel.class);
            Path init = projectDir.toPath().resolve("openrewrite-tooling.gradle");
            try (InputStream is = OpenRewriteModel.class.getResourceAsStream("/init.gradle")) {
                if(is == null) {
                    throw new IllegalStateException("Expected to find init.gradle on the classpath");
                }
                Files.copy(is, init, StandardCopyOption.REPLACE_EXISTING);
                customModelBuilder.withArguments("--init-script", "openrewrite-tooling.gradle");
                return customModelBuilder.get();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                try {
                    Files.delete(init);
                } catch (IOException e) {
                    //noinspection ThrowFromFinallyBlock
                    throw new UncheckedIOException(e);
                }
            }
        }
    }
}
