/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.gradle.parser;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.gradle.toolingapi.OpenRewriteModelBuilder;
import org.openrewrite.gradle.toolingapi.parser.GradleProjectData;
import org.openrewrite.style.NamedStyles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProjectParserTest {

    private static final Logger log = LoggerFactory.getLogger(ProjectParserTest.class);
    private static final ProjectParser.Options OPTIONS = new ProjectParser.Options() {
        @Override
        public List<String> getExclusions() {
            return Collections.emptyList();
        }

        @Override
        public boolean getLogCompilationWarningsAndErrors() {
            return false;
        }

        @Override
        public List<String> getPlainTextMasks() {
            return Collections.emptyList();
        }

        @Override
        public int getSizeThresholdMb() {
            return Integer.MAX_VALUE;
        }

        @Override
        public List<NamedStyles> getStyles() {
            return Collections.emptyList();
        }
    };

    @Test
    void sanity(@TempDir Path dir) throws Exception {
        try (InputStream is = ProjectParserTest.class.getResourceAsStream("/build.gradle")) {
            Files.write(dir.resolve("build.gradle"), Objects.requireNonNull(is).readAllBytes());
        }
        try (InputStream is = ProjectParserTest.class.getResourceAsStream("/settings.gradle")) {
            Files.write(dir.resolve("settings.gradle"), Objects.requireNonNull(is).readAllBytes());
        }

        GradleProjectData gp = OpenRewriteModelBuilder.forProjectDirectory(GradleProjectData.class, dir.toFile(), dir.resolve("build.gradle").toFile());
        List<SourceFile> sources = new ProjectParser(gp, OPTIONS, log).parse(new InMemoryExecutionContext(t -> Assertions.fail(t.getMessage()))).collect(Collectors.toList());
        assertEquals(sources.size(), 2);
    }

}
