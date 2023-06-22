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
package org.openrewrite.gradle.marker;

import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.FeaturePreviews;
import org.gradle.initialization.DefaultSettings;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.internal.service.UnknownServiceException;
import org.gradle.util.GradleVersion;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.MavenRepository;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class GradleSettingsBuilder {
    private GradleSettingsBuilder() {
    }

    public static GradleSettings gradleSettings(Settings settings) {
        return new GradleSettings(
                Tree.randomId(),
                settings.getBuildscript().getRepositories().stream()
                        .filter(MavenArtifactRepository.class::isInstance)
                        .map(MavenArtifactRepository.class::cast)
                        .map(repo -> MavenRepository.builder()
                                .id(repo.getName())
                                .uri(repo.getUrl().toString())
                                .releases(true)
                                .snapshots(true)
                                .build())
                        .collect(Collectors.toList()),
                GradleProjectBuilder.pluginDescriptors(settings.getPluginManager()),
                featurePreviews((DefaultSettings) settings)
        );
    }

    private static Map<String, FeaturePreview> featurePreviews(DefaultSettings settings) {
        if (GradleVersion.current().compareTo(GradleVersion.version("4.6")) < 0) {
            return Collections.emptyMap();
        }

        Map<String, FeaturePreview> featurePreviews = new HashMap<>();
        FeaturePreviews gradleFeaturePreviews = getService(settings, FeaturePreviews.class);
        if (gradleFeaturePreviews != null) {
            FeaturePreviews.Feature[] gradleFeatures = FeaturePreviews.Feature.values();
            for (FeaturePreviews.Feature feature : gradleFeatures) {
                featurePreviews.put(feature.name(), new FeaturePreview(feature.name(), feature.isActive(), gradleFeaturePreviews.isFeatureEnabled(feature)));
            }
        }
        return featurePreviews;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <T> T getService(DefaultSettings settings, Class<T> serviceType) {
        try {
            Method services = settings.getClass().getDeclaredMethod("getServices");
            services.setAccessible(true);
            ServiceRegistry serviceRegistry = (ServiceRegistry) services.invoke(settings);
            return serviceRegistry.get(serviceType);
        } catch (UnknownServiceException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }
}
