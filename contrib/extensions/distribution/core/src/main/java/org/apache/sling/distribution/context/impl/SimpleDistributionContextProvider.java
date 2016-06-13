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
package org.apache.sling.distribution.context.impl;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.distribution.context.DistributionContext;
import org.apache.sling.distribution.context.DistributionContextProvider;
import org.apache.sling.distribution.transport.impl.SimpleDistributionContext;

@Component(metatype = true,
        label = "Apache Sling Distribution Simple Transport Context Provider",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
@Service(value = DistributionContextProvider.class)
@Property(name="webconsole.configurationFactory.nameHint", value="Context provider name: {name}")
public class SimpleDistributionContextProvider implements DistributionContextProvider {

    @Nonnull
    @Override
    public DistributionContext getContext(@Nullable Map<String, Object> properties) {
        return new SimpleDistributionContext();
    }

    @Nonnull
    @Override
    public DistributionContext getContext() {
        return new SimpleDistributionContext();
    }
}
