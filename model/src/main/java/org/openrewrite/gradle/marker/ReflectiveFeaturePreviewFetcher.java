package org.openrewrite.gradle.marker;

import org.gradle.api.internal.FeaturePreviews;
import org.gradle.initialization.DefaultSettings;
import org.gradle.internal.buildoption.FeatureFlags;

import java.util.Map;

/**
 * Avoid failure to load the class org.gradle.internal.buildoption.FeatureFlag on older versions of Gradle by accessing
 * this class reflectively.
 */
public class ReflectiveFeaturePreviewFetcher {

    public static Map<String, FeaturePreview> getPreviews(DefaultSettings settings) {
        Map<String, FeaturePreview> featurePreviews = new java.util.HashMap<>();
        FeatureFlags featureFlags = settings.getServices().get(FeatureFlags.class);
        FeaturePreviews.Feature[] gradleFeatures = FeaturePreviews.Feature.values();
        for (FeaturePreviews.Feature feature : gradleFeatures) {
            featurePreviews.put(feature.name(), new FeaturePreview(feature.name(), feature.isActive(), featureFlags.isEnabled(feature)));
        }
        return featurePreviews;
    }
}
