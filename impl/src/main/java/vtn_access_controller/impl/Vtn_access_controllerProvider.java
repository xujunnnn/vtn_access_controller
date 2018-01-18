/*
 * Copyright © 2017 xujun and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package vtn_access_controller.impl;



import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.MacAcls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.MacAclsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.VtnAccessControllerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.mac.acl.status.MappedHost;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import javassist.compiler.ast.NewExpr;
import vtn_access_controller.impl.core.MappedHostConfig;
import vtn_access_controller.impl.core.PacketProcessor;
import vtn_access_controller.impl.core.VbridgePath;
import vtn_access_controller.impl.core.VtnRpcService;
import vtn_access_controller.impl.core.VtnRpcServiceImpl;
import vtn_access_controller.impl.corr.topo.TopoHolder;
import vtn_access_controller.impl.rpcservice.VtnAccessControllerServiceImpl;



public class Vtn_access_controllerProvider {
	private static String TOPOID="flow:1";
	private Set<NodeConnectorId> inner_ports=new CopyOnWriteArraySet<>();
    private static final Logger LOG = LoggerFactory.getLogger(Vtn_access_controllerProvider.class);
    private RpcRegistration<VtnAccessControllerService> registration;
    private final DataBroker dataBroker;
    private final RpcProviderRegistry reg;
    private final ExecutorService service=Executors.newFixedThreadPool(20);
    private final NotificationProviderService notificationProviderService;
    private ListenerRegistration<DataChangeListener> listenerRegistration;
    private Map<VbridgePath,Set<MappedHostConfig>> mac_acl_map=new ConcurrentHashMap<>();
    public Vtn_access_controllerProvider(final DataBroker dataBroker,final RpcProviderRegistry reg,final NotificationProviderService notificationProviderService) {
        this.dataBroker = dataBroker;
        this.reg=reg;
        this.notificationProviderService=notificationProviderService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
    	TopoHolder topoHolder=new TopoHolder(dataBroker, inner_ports);
    	topoHolder.setTopologyId(TOPOID);
    	VtnRpcService vtnRpcService=new VtnRpcServiceImpl(reg, dataBroker);
    	listenerRegistration=topoHolder.registerAsDataChangeListener();
    	PacketProcessor processor=new PacketProcessor(dataBroker, mac_acl_map, service, inner_ports, vtnRpcService);
    	notificationProviderService.registerNotificationListener(processor);
    	processor.loadFromDataStore();
    	VtnAccessControllerServiceImpl rpcservice=new VtnAccessControllerServiceImpl(service,dataBroker,mac_acl_map,vtnRpcService);
    	registration=reg.addRpcImplementation(VtnAccessControllerService.class, rpcservice);   	
    	}
     
    	
    	 
       
    	
    	
        
        
    

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("Vtn_access_controllerProvider Closed");
        registration.close();
        listenerRegistration.close();
    }
    
/**
 *     private MacAcls buildMacAcls(){

    	MacAclsBuilder builder=new MacAclsBuilder();
    	
    	return builder.build();
    	
    	
    	
    }
 */
}