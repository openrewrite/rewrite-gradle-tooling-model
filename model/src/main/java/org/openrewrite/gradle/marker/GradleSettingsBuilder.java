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
import java.util.*;

import static org.openrewrite.gradle.marker.GradleProjectBuilder.mapRepositories;

public final class GradleSettingsBuilder {
    static MavenRepository GRADLE_PLUGIN_PORTAL = MavenRepository.builder()
            .id("Gradle Central Plugin Repository")
            .uri("https://plugins.gradle.org/m2")
            .releases(true)
            .snapshots(true)
            .build();

    private GradleSettingsBuilder() {
    }

    public static GradleSettings gradleSettings(Settings settings) {
        Set<MavenRepository> pluginRepositories = new HashSet<>();
        pluginRepositories.addAll(mapRepositories(settings.getPluginManagement().getRepositories()));
        pluginRepositories.addAll(mapRepositories(settings.getBuildscript().getRepositories()));
        if (pluginRepositories.isEmpty()) {
            pluginRepositories.add(GRADLE_PLUGIN_PORTAL);
        }

        return new GradleSettings(
                Tree.randomId(),
                new ArrayList<>(pluginRepositories),
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
