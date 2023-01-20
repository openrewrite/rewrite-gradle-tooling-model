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

import org.openrewrite.internal.lang.Nullable;

import java.net.URI;

public interface MavenRepository {

    @Nullable
    String getId();

    /**
     * Not a {@link URI} because this could be a property reference.
     */
    String getUri();

    @Nullable
    String getReleases();

    @Nullable
    String getSnapshots();

    boolean isKnownToExist();

    @Nullable
    String getUsername();

    @Nullable
    String getPassword();

    @Nullable
    Boolean getDeriveMetadataIfMissing();
}
