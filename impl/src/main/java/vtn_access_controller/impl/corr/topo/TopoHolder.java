/*
 * Copyright © 2017 xujun and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package vtn_access_controller.impl.corr.topo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import vtn_access_controller.impl.core.util.InstanceIdentifierUtils;

public class TopoHolder implements DataChangeListener{
	//hold all the innerports

	private Set<NodeConnectorId> inner_ports;
	private static final String HOSTPREFIX="host";
	private static final String SWITCHPREFIX="openflow";
    private static final Logger LOG = LoggerFactory.getLogger(TopoHolder.class);
    private static final String DEFAULT_TOPOLOGY_ID = "flow:1";
    private static final long DEFAULT_GRAPH_REFRESH_DELAY = 1000;
    private final ScheduledExecutorService topologyDataChangeEventProcessor = Executors.newScheduledThreadPool(1);
    private boolean networkGraphRefreshScheduled = false;
    private boolean threadReschedule = false;
    private long graphRefreshDelay;
    private String topologyId;
    private final DataBroker dataBroker;
   public TopoHolder(final DataBroker dataBroker,Set<NodeConnectorId> inner_ports) {
	// TODO Auto-generated constructor stub
	   this.dataBroker=dataBroker;
	   this.inner_ports=inner_ports;
   }	

    public void setGraphRefreshDelay(long graphRefreshDelay) {
        if (graphRefreshDelay < 0) {
            this.graphRefreshDelay = DEFAULT_GRAPH_REFRESH_DELAY;
        } else
            this.graphRefreshDelay = graphRefreshDelay;
    }

    public void setTopologyId(String topologyId) {
        if (topologyId == null || topologyId.isEmpty()) {
            this.topologyId = DEFAULT_TOPOLOGY_ID;
        } else
            this.topologyId = topologyId;
    }

    /**
     * Registers as a data listener to receive changes done to
     * {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link}
     * under
     * {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology}
     * operation data root.
     */
    public ListenerRegistration<DataChangeListener> registerAsDataChangeListener() {
        InstanceIdentifier<Link> linkInstance = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(topologyId))).child(Link.class).build();
        return dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, linkInstance, this,
                DataChangeScope.BASE);
    }

    /**
     * Handler for onDataChanged events and schedules the building of the
     * network graph.
     *
     * @param dataChangeEvent
     *            The data change event to process.
     */
    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent) {
        if (dataChangeEvent == null) {
            return;
        }
        Map<InstanceIdentifier<?>, DataObject> createdData = dataChangeEvent.getCreatedData();
        Set<InstanceIdentifier<?>> removedPaths = dataChangeEvent.getRemovedPaths();
        Map<InstanceIdentifier<?>, DataObject> originalData = dataChangeEvent.getOriginalData();
        boolean isGraphUpdated = false;

        if (createdData != null && !createdData.isEmpty()) {
            Set<InstanceIdentifier<?>> linksIds = createdData.keySet();
            for (InstanceIdentifier<?> linkId : linksIds) {
                if (Link.class.isAssignableFrom(linkId.getTargetType())) {
                    Link link = (Link) createdData.get(linkId);
                    if (!(link.getLinkId().getValue().contains("host"))) {
                        isGraphUpdated = true;
                        LOG.debug("Graph is updated! Added Link {}", link.getLinkId().getValue());
                        break;
                    }
                }
            }
        }

        if (removedPaths != null && !removedPaths.isEmpty() && originalData != null && !originalData.isEmpty()) {
            for (InstanceIdentifier<?> instanceId : removedPaths) {
                if (Link.class.isAssignableFrom(instanceId.getTargetType())) {
                    Link link = (Link) originalData.get(instanceId);
                    if (!(link.getLinkId().getValue().contains("host"))) {
                        isGraphUpdated = true;
                        LOG.debug("Graph is updated! Removed Link {}", link.getLinkId().getValue());
                        break;
                    }
                }
            }
        }

        if (!isGraphUpdated) {
            return;
        }
        if (!networkGraphRefreshScheduled) {
            synchronized (this) {
                if (!networkGraphRefreshScheduled) {
                    topologyDataChangeEventProcessor.schedule(new TopologyDataChangeEventProcessor(), graphRefreshDelay,
                            TimeUnit.MILLISECONDS);
                    networkGraphRefreshScheduled = true;
                    LOG.debug("Scheduled Graph for refresh.");
                }
            }
        } else {
            LOG.debug("Already scheduled for network graph refresh.");
            threadReschedule = true;
        }
    }

    /**
     *
     */
    private class TopologyDataChangeEventProcessor implements Runnable {

        @Override
        public void run() {
            if (threadReschedule) {
                topologyDataChangeEventProcessor.schedule(this, graphRefreshDelay, TimeUnit.MILLISECONDS);
                threadReschedule = false;
                return;
            }
            LOG.debug("In network graph refresh thread.");
            networkGraphRefreshScheduled = false;
            updateInnerPorts();
            LOG.debug("Done with network graph refresh thread.");
        }

        private void updateInnerPorts(){
        	inner_ports.clear();
            InstanceIdentifier<Topology> topologyInstanceIdentifier = InstanceIdentifierUtils
                    .generateTopologyInstanceIdentifier(topologyId);
            Topology topology = null;
            ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
            try {
                Optional<Topology> topologyOptional = readOnlyTransaction
                        .read(LogicalDatastoreType.OPERATIONAL, topologyInstanceIdentifier).get();
                if (topologyOptional.isPresent()) {
                    topology = topologyOptional.get();
                }
            } catch (Exception e) {
                LOG.error("Error reading topology {}", topologyInstanceIdentifier);
                readOnlyTransaction.close();
                throw new RuntimeException(
                        "Error reading from operational store, topology : " + topologyInstanceIdentifier, e);
            }
            readOnlyTransaction.close();
            if (topology == null) {
                return;
            }
            List<Link> links = topology.getLink();
            if (links == null || links.isEmpty()) {
                return;
            }
            List<Link> internalLinks = new ArrayList<>();
            for (Link link : links) {
                if (isSwitch(link.getSource().getSourceTp()) && isSwitch(link.getDestination().getDestTp())) {
                    inner_ports.add(new NodeConnectorId(link.getSource().getSourceTp().getValue()));
                    inner_ports.add(new NodeConnectorId(link.getDestination().getDestTp().getValue()));
                }
            }
            
        }
        
        private boolean isHost(TpId tpId){
        	if(tpId.getValue()!=null && tpId.getValue().startsWith(HOSTPREFIX)){
        		return true;
        	}
        	return false;
        }
        
        private boolean isSwitch(TpId tpId){
        	if(tpId.getValue()!=null && tpId.getValue().startsWith(SWITCHPREFIX)){
        		return true;
        	}
        	return false;
        }
       

    }

}
