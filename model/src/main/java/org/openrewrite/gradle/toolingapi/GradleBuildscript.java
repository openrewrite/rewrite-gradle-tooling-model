package org.openrewrite.gradle.toolingapi;

import java.util.List;
import java.util.Map;

public interface GradleBuildscript {
    List<MavenRepository> getMavenRepositories();

    Map<String, GradleDependencyConfiguration> getNameToConfiguration();
}
