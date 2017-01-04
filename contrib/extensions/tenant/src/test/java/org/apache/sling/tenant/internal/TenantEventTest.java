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
package org.apache.sling.tenant.internal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.ServiceUtil;
import org.apache.sling.tenant.Tenant;
import org.apache.sling.tenant.TenantManager;
import org.apache.sling.tenant.spi.TenantCustomizer;
import org.apache.sling.tenant.spi.TenantManagerHook;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.apache.sling.tenant.TenantConstants.*;
import static org.junit.Assert.assertTrue;

public class TenantEventTest {

    private static final String TENANT_ID_1 = "t1";

    private static final String KEY_1 = "key1";

    private static final Object VALUE_1 = new Object();

    private static final String KEY_2 = "key2";

    private static final String VALUE_2 = "value2";

    private static final Map<String, Object> CHANGE_K2 = Collections.<String, Object>singletonMap(KEY_2, VALUE_2);

    private List<Event> events;

    private TenantManager manager;

    @Before
    public void setup() throws Exception {
        ResourceResolverFactory factory = new MockResourceResolverFactory();
        TenantProviderImpl provider = new TenantProviderImpl();
        EventAdmin eventAdmin = new EventAdmin() {
            @Override
            public void postEvent(Event event) {
                events.add(event);
            }

            @Override
            public void sendEvent(Event event) {
                events.add(event);
            }
        };
        set(provider, "eventAdmin", eventAdmin);
        set(provider, "factory", factory);
        set(provider, "tenantRootPath", "/etc/tenants");
        manager = provider;
        events = new ArrayList<Event>();
    }

    @Test
    public void testEventUponCreateWithoutInitialProperties() {
        manager.create(TENANT_ID_1, Collections.<String, Object>emptyMap());
        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEvent(event, TOPIC_TENANT_CREATED, TENANT_ID_1);
    }

    @Test
    public void testEventUponCreateWithInitialProperties() {
        manager.create(TENANT_ID_1, Collections.<String, Object>singletonMap(KEY_1, VALUE_1));
        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEvent(event, TOPIC_TENANT_CREATED, TENANT_ID_1);
        Map<String, Object> added = getProperties(event, PROPERTIES_ADDED);
        assertEquals(1, added.size());
        assertEquals(VALUE_1, added.get(KEY_1));
    }

    @Test
    public void testEventsUponCreateWithHook() throws Exception {
        set(manager, "registeredHooks", registeredSetKeyTwoHook());
        manager.create(TENANT_ID_1, Collections.<String, Object>singletonMap(KEY_1, VALUE_1));
        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEvent(event, TOPIC_TENANT_CREATED, TENANT_ID_1);
        Map<String, Object> added = getProperties(event, PROPERTIES_ADDED);
        assertEquals(2, added.size());
        assertEquals(VALUE_1, added.get(KEY_1));
        assertEquals(VALUE_2, added.get(KEY_2));
    }

    @Test
    public void testEventsUponCreateWitCustomizer() throws Exception {
        set(manager, "registeredTenantHandlers", registeredSetKeyTwoCustomizer());
        manager.create(TENANT_ID_1, Collections.<String, Object>singletonMap(KEY_1, VALUE_1));
        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEvent(event, TOPIC_TENANT_CREATED, TENANT_ID_1);
        Map<String, Object> added = getProperties(event, PROPERTIES_ADDED);
        assertEquals(2, added.size());
        assertEquals(VALUE_1, added.get(KEY_1));
        assertEquals(VALUE_2, added.get(KEY_2));
    }

    @Test
    public void testEventUponRemoveWithoutProperties() {
        Tenant t1 = manager.create(TENANT_ID_1, Collections.<String, Object>emptyMap());
        events.clear();
        manager.remove(t1);
        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEvent(event, TOPIC_TENANT_REMOVED, TENANT_ID_1);
    }

    @Test
    public void testEventUponRemoveWithProperties() {
        Tenant t1 = manager.create(TENANT_ID_1, Collections.<String, Object>singletonMap(KEY_1, VALUE_1));
        events.clear();
        manager.remove(t1);
        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEvent(event, TOPIC_TENANT_REMOVED, TENANT_ID_1);
        Map<String, Object> removed = getProperties(event, PROPERTIES_REMOVED);
        assertEquals(1, removed.size());
        assertEquals(VALUE_1, removed.get(KEY_1));
    }

    @Test
    public void testEventUponRemovingNotSetProperty() {
        Tenant t1 = manager.create(TENANT_ID_1, Collections.<String, Object>singletonMap(KEY_1, VALUE_1));
        events.clear();
        manager.removeProperties(t1, "notExistingPropertyName");
        assertTrue(events.isEmpty());
    }

    @Test
    public void testEventUponRemovingSingleProperty() {
        Tenant t1 = manager.create(TENANT_ID_1, Collections.<String, Object>singletonMap(KEY_1, VALUE_1));
        events.clear();
        manager.removeProperties(t1, KEY_1);
        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEvent(event, TOPIC_TENANT_UPDATED, TENANT_ID_1);
        Map<String, Object> removed = getProperties(event, PROPERTIES_REMOVED);
        assertEquals(1, removed.size());
        assertEquals(VALUE_1, removed.get(KEY_1));
    }

    @Test
    public void testEventUponRemovingMultipleProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(KEY_1, VALUE_1);
        properties.put(KEY_2, VALUE_2);
        Tenant t1 = manager.create(TENANT_ID_1, properties);
        events.clear();
        manager.removeProperties(t1, KEY_1, KEY_2);
        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEvent(event, TOPIC_TENANT_UPDATED, TENANT_ID_1);
        Map<String, Object> removed = getProperties(event, PROPERTIES_REMOVED);
        assertEquals(2, removed.size());
        assertEquals(VALUE_1, removed.get(KEY_1));
        assertEquals(VALUE_2, removed.get(KEY_2));
    }

    @Test
    public void testEventUponRemoveProperties() {
        Tenant t1 = manager.create(TENANT_ID_1, Collections.<String, Object>singletonMap(KEY_1, VALUE_1));
        events.clear();
        manager.removeProperties(t1, "notExistingPropertyName");
        assertTrue(events.isEmpty());
        manager.removeProperties(t1, KEY_1);
        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEvent(event, TOPIC_TENANT_UPDATED, TENANT_ID_1);
        Map<String, Object> removed = getProperties(event, PROPERTIES_REMOVED);
        assertEquals(1, removed.size());
        assertEquals(VALUE_1, removed.get(KEY_1));
    }

    @Test
    public void testEventUponUpdatingPropertyWithSameValue() {
        Tenant t1 = manager.create(TENANT_ID_1, Collections.<String, Object>singletonMap(KEY_1, VALUE_1));
        events.clear();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(KEY_1, VALUE_1);
        properties.put(KEY_2, VALUE_2);
        manager.setProperties(t1, properties);
        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEvent(event, TOPIC_TENANT_UPDATED, TENANT_ID_1);
        Map<String, Object> updated = getProperties(event, PROPERTIES_UPDATED);
        assertNull(updated);
    }

    @Test
    public void testEventUponSettingProperty() {
        Tenant t1 = manager.create(TENANT_ID_1, Collections.<String, Object>emptyMap());
        events.clear();
        manager.setProperty(t1, KEY_1, VALUE_1);
        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEvent(event, TOPIC_TENANT_UPDATED, TENANT_ID_1);
        Map<String, Object> added = getProperties(event, PROPERTIES_ADDED);
        assertEquals(1, added.size());
        assertEquals(VALUE_1, added.get(KEY_1));
    }

    @Test
    public void testEventUponUpdatingAndSettingProperties() {
        Tenant t1 = manager.create(TENANT_ID_1, Collections.<String, Object>singletonMap(KEY_1, VALUE_1));
        events.clear();
        String updatedValue1 = VALUE_1 + "-updated";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(KEY_1, updatedValue1);
        properties.put(KEY_2, VALUE_2);
        manager.setProperties(t1, properties);
        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEvent(event, TOPIC_TENANT_UPDATED, TENANT_ID_1);
        Map<String, Object> added = getProperties(event, PROPERTIES_ADDED);
        assertEquals(1, added.size());
        assertEquals(VALUE_2, added.get(KEY_2));
        Map<String, Object> updated = getProperties(event, PROPERTIES_UPDATED);
        assertEquals(1, updated.size());
        assertEquals(updatedValue1, updated.get(KEY_1));
    }

    @Test
    public void testEventUponSettingProperties() {
        Tenant t1 = manager.create(TENANT_ID_1, Collections.<String, Object>emptyMap());
        events.clear();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(KEY_1, VALUE_1);
        properties.put(KEY_2, VALUE_2);
        manager.setProperties(t1, properties);
        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEvent(event, TOPIC_TENANT_UPDATED, TENANT_ID_1);
        Map<String, Object> added = getProperties(event, PROPERTIES_ADDED);
        assertEquals(2, added.size());
        assertEquals(VALUE_1, added.get(KEY_1));
        assertEquals(VALUE_2, added.get(KEY_2));
    }

    @Test
    public void testEventsUponSettingPropertyWithHook() throws Exception {
        Tenant t1 = manager.create(TENANT_ID_1, Collections.<String, Object>emptyMap());
        events.clear();
        set(manager, "registeredHooks", registeredSetKeyTwoHook());
        manager.setProperty(t1, KEY_1, VALUE_1);
        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEvent(event, TOPIC_TENANT_UPDATED, TENANT_ID_1);
        Map<String, Object> added = getProperties(event, PROPERTIES_ADDED);
        assertEquals(2, added.size());
        assertEquals(VALUE_1, added.get(KEY_1));
        assertEquals(VALUE_2, added.get(KEY_2));
    }

    @Test
    public void testEventsUponUpdatingPropertyWithHook() throws Exception {
        String initValue2 = VALUE_2 + "-init";
        Tenant t1 = manager.create(TENANT_ID_1, Collections.<String, Object>singletonMap(KEY_2, initValue2));
        events.clear();
        set(manager, "registeredHooks", registeredSetKeyTwoHook());
        manager.setProperty(t1, KEY_1, VALUE_1);
        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEvent(event, TOPIC_TENANT_UPDATED, TENANT_ID_1);
        Map<String, Object> added = getProperties(event, PROPERTIES_ADDED);
        assertEquals(1, added.size());
        assertEquals(VALUE_1, added.get(KEY_1));
        Map<String, Object> updated = getProperties(event, PROPERTIES_UPDATED);
        assertEquals(1, updated.size());
        assertEquals(VALUE_2, updated.get(KEY_2));
    }

    private static void set(Object o, String name, Object value) throws Exception {
        final Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(o, value);
    }

    private void assertEvent(Event event, String topic, String tenantId) {
        assertEquals(topic, event.getTopic());
        assertEquals(tenantId, event.getProperty(PROPERTY_TENANT_ID));
        if (TOPIC_TENANT_CREATED.equals(event.getTopic())) {
            assertNotDefined(event, PROPERTIES_REMOVED, PROPERTIES_UPDATED);
        } else if (TOPIC_TENANT_REMOVED.equals(event.getTopic()))  {
            assertNotDefined(event, PROPERTIES_ADDED, PROPERTIES_UPDATED);
        } else if (TOPIC_TENANT_UPDATED.equals(event.getTopic())) {
            // TENANT_UPDATED events can contain all types of PROPERTIES change.
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void assertNotDefined(Event event, String ... notIncludedPropertyTypes) {
        for (String propertyType : notIncludedPropertyTypes) {
            assertNull(event.getProperty(propertyType));
        }
    }

    private Map<String, Object> getProperties(Event event, String propertyType) {
        return (Map<String, Object>) event.getProperty(propertyType);
    }

    private SortedMap<Comparable<Object>, TenantManagerHook> registeredSetKeyTwoHook() {
        TenantManagerHook setKeyTwo = new SetKeyTwoHook();
        SortedMap<Comparable<Object>, TenantManagerHook> hooks =
                new TreeMap<Comparable<Object>, TenantManagerHook>(Collections.reverseOrder());
        hooks.put(ServiceUtil.getComparableForServiceRanking(Collections.<String, Object>singletonMap(
                "service.id", 1004L)), setKeyTwo);
        return hooks;
    }

    private SortedMap<Comparable<Object>, TenantCustomizer> registeredSetKeyTwoCustomizer() {
        TenantCustomizer setKeyTwo = new SetKeyTwoCustomizer();
        SortedMap<Comparable<Object>, TenantCustomizer> customizers =
                new TreeMap<Comparable<Object>, TenantCustomizer>(Collections.reverseOrder());
        customizers.put(ServiceUtil.getComparableForServiceRanking(Collections.<String, Object>singletonMap(
                "service.id", 1926L)), setKeyTwo);
        return customizers;
    }

    private class SetKeyTwoCustomizer implements TenantCustomizer {

        @Override
        public Map<String, Object> setup(Tenant tenant, ResourceResolver resolver) {
            return CHANGE_K2;
        }

        @Override
        public void remove(Tenant tenant, ResourceResolver resolver) {

        }
    }

    private class SetKeyTwoHook implements TenantManagerHook {

        @Override
        public Map<String, Object> setup(Tenant tenant) {
            return CHANGE_K2;
        }

        @Override
        public Map<String, Object> change(Tenant tenant) {
            return CHANGE_K2;
        }

        @Override
        public void remove(Tenant tenant) {

        }
    };

}
