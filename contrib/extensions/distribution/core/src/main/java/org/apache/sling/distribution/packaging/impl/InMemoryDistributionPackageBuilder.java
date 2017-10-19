/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.distribution.packaging.impl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.serialization.DistributionExportFilter;
import org.apache.sling.distribution.serialization.DistributionExportOptions;
import org.apache.sling.distribution.serialization.impl.vlt.VltUtils;

public class InMemoryDistributionPackageBuilder extends AbstractDistributionPackageBuilder {

    private final Map<String, DistributionPackage> packages = new HashMap<String, DistributionPackage>();

    private final DistributionContentSerializer serializer;

    private final NavigableMap<String, List<String>> nodeFilters;

    private final NavigableMap<String, List<String>> propertyFilters;

    public InMemoryDistributionPackageBuilder(@Nonnull String type,
                                              @Nonnull DistributionContentSerializer serializer,
                                              @Nonnull String[] nodeFilters,
                                              @Nonnull String[] propertyFilters) {
        super(type);
        this.serializer = serializer;
        this.nodeFilters = VltUtils.parseFilters(nodeFilters);
        this.propertyFilters = VltUtils.parseFilters(propertyFilters);
    }

    @Override
    protected DistributionPackage createPackageForAdd(@Nonnull ResourceResolver resourceResolver,
                                                      @Nonnull DistributionRequest request)
            throws DistributionException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        export(resourceResolver, request, baos);

        String packageId = "dstrpck-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString();

        DistributionPackage pkg = new InMemoryDistributionPackage(packageId, getType(), baos.toByteArray(), this);
        packages.put(packageId, pkg);

        return pkg;
    }

    @Override
    protected DistributionPackage readPackageInternal(@Nonnull ResourceResolver resourceResolver,
                                                      @Nonnull InputStream stream)
            throws DistributionException {
        return null; // TODO
    }

    @Override
    protected boolean installPackageInternal(@Nonnull ResourceResolver resourceResolver,
                                             @Nonnull InputStream inputStream)
            throws DistributionException {
        try {
            serializer.importFromStream(resourceResolver, inputStream);
            return true;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    protected DistributionPackage getPackageInternal(@Nonnull ResourceResolver resourceResolver,
                                                     @Nonnull String id) {
        return packages.get(id);
    }

    protected void removePackage(String packageId) {
        packages.remove(packageId);
    }

    private void export(@Nonnull ResourceResolver resourceResolver,
                        @Nonnull final DistributionRequest request,
                        @Nonnull OutputStream outputStream)
            throws DistributionException {
        final DistributionExportFilter filter = serializer.isRequestFiltering() ? null : DistributionExportFilter.createFilter(request, nodeFilters, propertyFilters);
        DistributionExportOptions distributionExportOptions = new DistributionExportOptions(request, filter);
        serializer.exportToStream(resourceResolver, distributionExportOptions, outputStream);
    }
}
