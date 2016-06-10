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

import aQute.bnd.annotation.ProviderType;

/**
 * This interface defines the properties (name and type) that could be referenced in
 * a {@link DistributionContext} distribution context.</p>
 *
 * @since 0.1.0
 */
@ProviderType
public interface DistributionContextProperties {

    /**
     * The name of the property referencing a transport ssl context.</p>
     *
     * The value referenced by this property must be of
     * type {@link javax.net.ssl.SSLContext}.
     */
    String SSL_CONTEXT = "sslContext";

    /**
     * The name of the property referencing the endpoint.</p>
     *
     * The value referenced by this property must be of
     * type {@link String}.
     */
    String ENDPOINT = "endpoint";

}
