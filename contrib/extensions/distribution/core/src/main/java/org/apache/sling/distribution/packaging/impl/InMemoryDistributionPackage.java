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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;

import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;

public class InMemoryDistributionPackage implements DistributionPackage {

    private final String id;

    private final String type;

    private final long size;

    private final byte[] data;

    private final DistributionPackageInfo info;

    private final InMemoryDistributionPackageBuilder packageBuilder;

    public InMemoryDistributionPackage(String id, String type, byte[] data, InMemoryDistributionPackageBuilder packageBuilder) {
        this.id = id;
        this.type = type;
        this.data = data;
        this.size = data.length;
        this.info = new DistributionPackageInfo(type);
        this.packageBuilder = packageBuilder;
    }

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    @Nonnull
    @Override
    public String getType() {
        return type;
    }

    @Nonnull
    @Override
    public InputStream createInputStream() throws IOException {
        return new ByteArrayInputStream(data);
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public void close() {
    }

    @Override
    public void delete() {
        packageBuilder.removePackage(id);
    }

    @Nonnull
    @Override
    public DistributionPackageInfo getInfo() {
        return info;
    }
}
