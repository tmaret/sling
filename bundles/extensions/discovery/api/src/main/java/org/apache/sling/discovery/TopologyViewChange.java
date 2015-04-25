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
package org.apache.sling.discovery;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class allows to compute the difference between two {@code TopologyView} views by
 * filtering the set of {@code InstanceDescription} instances formed by the union of the
 * instances from the two views (called hereafter "union set").<p>
 *
 * The filtering is done in two phases, first a partition of the "union set" is created, then
 * the filters can be applied on the partition.<p>
 *
 * The partition is represented by a {@code Partition} object. Instances of this class are obtained
 * by either of the methods {@code all} (the "union set" partition), {@code added}, {@code removed} and
 * {@code retained}. The partitions split the "union set" in three groups with the property that no instance is
 * contained in more than one of those groups.<p>
 *
 * The comparison between {@code InstanceDescription} is done on the basis of the Sling identifier.
 * Two instances with the same Sling identifier are considered as equal.<p>
 */
public final class TopologyViewChange {

    /**
     * A filter for keeping instances with #InstanceDescription#isLocal set to {@code true}.
     */
    private static final InstanceFilter LOCAL_INSTANCE = new LocalInstanceFilter();

    /**
     * A filter for keeping instances with #InstanceDescription#isLocal set to {@code false}.
     */
    private static final InstanceFilter NOT_LOCAL_INSTANCE = new NotFilter(LOCAL_INSTANCE);

    /**
     * A filter for keeping instances with #InstanceDescription#isLeader set to {@code true}.
     */
    private static final InstanceFilter LEADER_INSTANCE = new LeaderInstanceFilter();

    /**
     * A filter for keeping instances with #InstanceDescription#isLeader set to {@code false}.
     */
    private static final InstanceFilter NOT_LEADER_INSTANCE = new NotFilter(LEADER_INSTANCE);


    private final Map<String, InstanceDescription> oldInstances;

    private final Map<String, InstanceDescription> newInstances;

    /**
     * @param event the event from which the old and new view are used for computing the change.
     *              If either of the views are {@code null}, then and empty view
     *              (containing no instance) is used as substitute.
     */
    public TopologyViewChange(TopologyEvent event) {
        this(event.getOldView(), event.getNewView());
    }

    /**
     * @param oldView the old topology view used for computing the change.
     *                If {@code null} then and empty view is used as substitute.
     * @param newView the new toplogy view used for computing the change.
     *                If {@code null} then and empty view is used as substitute.
     */
    public TopologyViewChange(TopologyView oldView, TopologyView newView) {
        newInstances = getInstances(newView);
        oldInstances = getInstances(oldView);
    }

    /**
     * Returns the "union set" partition which contains the instances from both views
     *
     * @param filterOnNewView {@code true} in order to keep the instances from the new view for filtering ;
     *                        {@code false} in order to keep the instances from the old view
     * @return the {@code Partition} instance containing the {@code InstanceDescription} instances from both views
     */
    public Partition all(boolean filterOnNewView) {
        return new Partition(partitionAll(filterOnNewView));
    }

    /**
     * Return the partition that contains the instances which are present in
     * the new topology view but not in the old one.
     *
     * @return the {@code Partition} instance containing the {@code InstanceDescription} instances in the new
     *         topology but not in the old one
     */
    public Partition added() {
        return new Partition(partitionAdded());
    }

    /**
     * Return the partition that contains the instances which are present in
     * the old topology view but not in the new one.
     *
     * @return {@code Partition} instance containing the {@code InstanceDescription} instances which are present in
     *         the old topology but not in the new one
     */
    public Partition removed() {
        return new Partition(partitionRemoved());
    }

    /**
     * Return the partition that contains the instances which are present in
     * both the old topology view and the new one.<p>
     *
     * The partitioning only considers the Sling identifier for comparing instances.
     * The filters may require either the new or old instance in order to be applied.
     * The flag #filterOnNewView allows to specify whether the new or old instances must
     * be kept for further filtering.<p>
     *
     * @param filterOnNewView {@code true} in order to keep the instances from the new view for filtering ;
     *                        {@code false} in order to keep the instances from the old view
     * @return {@code Partition} instance containing the {@code InstanceDescription} instances which are present
     *         in both views. The reference (old or new) depends on the value of the #filterOnNewView.
     */
    public Partition retained(boolean filterOnNewView) {
        return new Partition(partitionRetained(filterOnNewView));
    }

    /**
     * Return the partition that contains the instances which are present in
     * both the old topology view and the new one.<p>
     *
     * The partitioning only considers the Sling identifier for comparing instances.
     * The filters may require either the new or old instance in order to be applied.
     * The flag #filterOnNewView allows to specify whether the new or old instances must
     * be kept for further filtering.<p>
     *
     * @param filterOnNewView {@code true} in order to keep the instances from the new view for filtering ;
     *                        {@code false} in order to keep the instances from the old view
     * @param propertyChanged {@code true} in order to return only the instances which
     *                        properties have not changed ; {@code false} in order to
     *                        return only the instances which properties have changed.
     * @return {@code Partition} instance containing the {@code InstanceDescription} instances which are present
     *         in both views. The reference (old or new) depends on the value of the #filterOnNewView.
     */
    public Partition retained(boolean filterOnNewView, boolean propertyChanged) {
        return new Partition(partitionRetained(filterOnNewView, propertyChanged));
    }

    //

    private Map<String, InstanceDescription> partitionAll(boolean filterOnNewView) {
        Map<String, InstanceDescription> partition = new HashMap<String, InstanceDescription>();
        if (filterOnNewView) {
            partition.putAll(newInstances);
            partition.putAll(oldInstances);
        } else {
            partition.putAll(oldInstances);
            partition.putAll(newInstances);
        }
        return partition;
    }

    private Map<String, InstanceDescription> partitionRemoved() {
        Map<String, InstanceDescription> partition = new HashMap<String, InstanceDescription>(oldInstances.size());
        partition.keySet().removeAll(newInstances.keySet());
        return partition;
    }

    private Map<String, InstanceDescription> partitionAdded() {
        Map<String, InstanceDescription> partition = new HashMap<String, InstanceDescription>(newInstances.size());
        partition.keySet().removeAll(oldInstances.keySet());
        return partition;
    }

    private Map<String, InstanceDescription> partitionRetained(boolean filterOnNewView, boolean propertyChanged) {
        Map<String, InstanceDescription> partition = new HashMap<String, InstanceDescription>();
        for (Map.Entry<String, InstanceDescription> oldEntry : oldInstances.entrySet()) {
            String slingId = oldEntry.getKey();
            InstanceDescription newDescription = newInstances.get(slingId);
            if(newDescription != null) {
                InstanceDescription oldDescription = oldEntry.getValue();
                boolean propertiesSame = newDescription.getProperties().equals(oldDescription.getProperties());
                if ((propertiesSame && ! propertyChanged) || (! propertiesSame && propertyChanged)) {
                    partition.put(slingId, filterOnNewView ? newDescription : oldDescription);
                }
            }
        }
        return partition;
    }

    private Map<String, InstanceDescription> partitionRetained(boolean filterOnNewView) {
        Map<String, InstanceDescription> partition = new HashMap<String, InstanceDescription>();
        if (filterOnNewView) {
            partition.putAll(newInstances);
            partition.keySet().retainAll(oldInstances.keySet());
        } else {
            partition.putAll(oldInstances);
            partition.keySet().retainAll(newInstances.keySet());
        }
        return partition;
    }

    private static Map<String, InstanceDescription> getInstances(TopologyView topologyView) {
        if (topologyView != null) {
            Set<InstanceDescription> descriptions = topologyView.getInstances();
            Map<String, InstanceDescription> instances = new HashMap<String, InstanceDescription>(descriptions.size());
            for (InstanceDescription description : descriptions) {
                String slingId = description.getSlingId();
                if (slingId != null) {
                    instances.put(description.getSlingId(), description);
                }
            }
            return instances;
        }
        return Collections.<String, InstanceDescription>emptyMap();
    }

    private static final class NotFilter implements InstanceFilter {

        final InstanceFilter filter;

        private NotFilter(InstanceFilter filter) {
            this.filter = filter;
        }

        public boolean accept(InstanceDescription instance) {
            return ! filter.accept(instance);
        }
    }

    private static final class LocalInstanceFilter implements InstanceFilter {

        public boolean accept(InstanceDescription instance) {
            return instance.isLocal();
        }
    }

    private static final class LeaderInstanceFilter implements InstanceFilter {

        public boolean accept(InstanceDescription instance) {
            return instance.isLeader();
        }
    }

    private static final class InClusterView implements InstanceFilter {

        private final ClusterView view;

        private InClusterView(ClusterView view) {
            this.view = view;
        }

        public boolean accept(InstanceDescription instance) {
            return view.getId().equals(instance.getClusterView().getId());
        }
    }

    /**
     * The {@code Partition} class allows to filter the partition per instance, using custom filter
     * (using {@code InstanceFilter}) or pre-defined ones. Filters can be appended ANDs each of their conditions.
     */
    public final class Partition {

        private final Map<String, InstanceDescription> partition;

        private final Set<InstanceFilter> filters = new HashSet<InstanceFilter>();

        /**
         * Filter the partition with a custom {@code InstanceFilter} filter.
         *
         * @param filter the filter to be applied on the partition
         * @return {@code this}
         */
        public Partition filterWith(InstanceFilter filter) {
            if (filter != null) {
                filters.add(filter);
            }
            return this;
        }

        /**
         * Keep only the local instance.
         *
         * @return {@code this}
         */
        public Partition isLocal() {
            filters.add(LOCAL_INSTANCE);
            return this;
        }

        /**
         * Keep only non local instances.
         *
         * @return {@code this}
         */
        public Partition isNotLocal() {
            filters.add(NOT_LOCAL_INSTANCE);
            return this;
        }

        /**
         * Keep only the leader instances
         *
         * @return {@code this}
         */
        public Partition isLeader() {
            filters.add(LEADER_INSTANCE);
            return this;
        }

        /**
         * Keep only the non leader instances.
         *
         * @return {@code this}
         */
        public Partition isNotLeader() {
            filters.add(NOT_LEADER_INSTANCE);
            return this;
        }

        /**
         * Keep only the instances that are contained in the same {@code ClusterView} cluster view
         * as the one provided.
         *
         * @param clusterView the cluster view used to filter the partition
         * @return {@code this}
         */
        public Partition isInClusterView(ClusterView clusterView) {
            filters.add(new InClusterView(clusterView));
            return this;
        }

        /**
         * Keep only the instances that are not contained in the same {@code ClusterView} cluster view
         * as the one provided.
         *
         * @param clusterView the cluster view used to filter the partition
         * @return {@code this}
         */
        public Partition isNotInClusterView(ClusterView clusterView) {
            filters.add(new NotFilter(new InClusterView(clusterView)));
            return this;
        }

        /**
         * Return the set of Sling identifiers that have not been filtered out from the partition.
         *
         * @return the unordered set of instance identifiers
         */
        public Set<String> get() {
            return filter();
        }

        //

        /**
         * Instances of this class can only be obtained through the {@code TopologyViewChange} class.
         * @param partition the partition to be filtered
         */
        private Partition(Map<String, InstanceDescription> partition) {
            this.partition = partition;
        }

        private Set<String> filter() {
            // apply filters
            Iterator<Map.Entry<String, InstanceDescription>> entries = partition.entrySet().iterator();
            for ( ; entries.hasNext() ; ) {
                Map.Entry<String, InstanceDescription> entry = entries.next();
                for (InstanceFilter filter : filters) {
                    if (! filter.accept(entry.getValue())) {
                        entries.remove();
                        break;
                    }
                }
            }
            return partition.keySet();
        }
    }
}
