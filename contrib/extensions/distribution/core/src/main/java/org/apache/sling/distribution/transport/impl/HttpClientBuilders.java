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
package org.apache.sling.distribution.transport.impl;

import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.sling.distribution.context.DistributionContext;
import org.apache.sling.distribution.context.DistributionContextProperties;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientBuilders {

    /**
     * Default logger
     */
    private static final Logger log = LoggerFactory.getLogger(HttpClientBuilders.class);

    /**
     * HTTPS scheme
     */
    private static final String HTTPS = "https";

    /**
     * Configure a {@link HttpClientBuilder} instance based on
     * distribution context, secret and endpoint.
     *
     * @return a {@link HttpClientBuilder} instance
     */
    @Nonnull
    public static HttpClientBuilder syncClientBuilder(
            @Nonnull DistributionTransportSecretProvider secretProvider,
            @Nonnull DistributionContext transportContext,
            @Nonnull DistributionEndpoint endpoint) {

        log.debug("Build HTTP Client for endpoint: {}",
                endpoint.getUri());

        final HttpClientBuilder builder =
                HttpClientBuilder.create();

        CredentialsProvider credentialsProvider = getCredentialsProvider(secretProvider, endpoint);
        if (credentialsProvider != null) {
            builder.setDefaultCredentialsProvider(credentialsProvider);
        }

        if (HTTPS.equals(endpoint.getUri().getScheme())) {

            SSLContext sslContext = getSslContext(transportContext);
            if (sslContext != null) {
                builder.setSslcontext(sslContext);
            }
        }

        return builder;


    }

    /**
     * Configure a {@link HttpAsyncClientBuilder} instance based on
     *         distribution context, secret and endpoint.
     *
     * @return a {@link HttpAsyncClientBuilder} instance
     */
    @Nonnull
    public static HttpAsyncClientBuilder asyncClientBuilder(
            @Nonnull DistributionTransportSecretProvider secretProvider,
            @Nonnull DistributionContext transportContext,
            @Nonnull DistributionEndpoint endpoint) {

        log.debug("Build HTTP Async Client for endpoint: {}",
                endpoint.getUri());

        final HttpAsyncClientBuilder builder =
                HttpAsyncClients.custom();

        CredentialsProvider credentialsProvider = getCredentialsProvider(secretProvider, endpoint);
        if (credentialsProvider != null) {
            builder.setDefaultCredentialsProvider(credentialsProvider);
        }

        if (HTTPS.equals(endpoint.getUri().getScheme())) {

            SSLContext sslContext = getSslContext(transportContext);
            if (sslContext != null) {
                builder.setSSLContext(sslContext);
            }

        }

        return builder;

    }

    @CheckForNull
    private static CredentialsProvider getCredentialsProvider(
            @Nonnull DistributionTransportSecretProvider secretProvider,
            @Nonnull DistributionEndpoint endpoint) {

        final Map<String, String> credentialsMap = secretProvider.getSecret(
                endpoint.getUri()).asCredentialsMap();

        if (credentialsMap != null) {
            String username = credentialsMap.get("username");
            String password = credentialsMap.get("password");
            log.debug("Use BASIC authentication for user: {}", username);
            CredentialsProvider provider = new BasicCredentialsProvider();
            provider.setCredentials(new AuthScope(new HttpHost(endpoint.getUri().getHost(),
                    endpoint.getUri().getPort())), new UsernamePasswordCredentials(username, password));
        }

        return null;
    }

    @CheckForNull
    private static SSLContext getSslContext(@Nonnull DistributionContext transportContext) {
        SSLContext sslContext = transportContext.get(
                DistributionContextProperties.SSL_CONTEXT, SSLContext.class);
        if (sslContext != null) {
            log.debug("Use custom SSLContext {}", sslContext);
            return sslContext;
        }

        return null;
    }

}
