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
package org.apache.sling.distribution.context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Map;

import aQute.bnd.annotation.ConsumerType;

/**
 * A provider for {@link DistributionContext} contexts.</p>
 *
 * Such providers can be implemented by distribution agents in order to
 * customize the initialization of {@link DistributionContext} distribution context.</p>
 *
 * @since 0.1.0
 *
 * @see org.apache.sling.distribution.context.DistributionContextProperties
 *      for the set of supported properties.
 */
@ConsumerType
public interface DistributionContextProvider {
    
    /**
     * Provide a {@link DistributionContext} distribution context instance
     * leveraging the provided properties.</p>
     *
     * @param properties the map of properties to be included in the context
     * @return a {@link DistributionContext} instance
     */
    @Nonnull
    DistributionContext getContext(@Nullable Map<String, Object> properties);

    /**
     * Provide a {@link DistributionContext} distribution context instance.</p>
     *
     * @return a {@link DistributionContext} instance
     */
    @Nonnull
    DistributionContext getContext();


}
