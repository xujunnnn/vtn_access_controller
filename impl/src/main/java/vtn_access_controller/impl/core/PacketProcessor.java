/*
 * Copyright © 2017 xujun and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package vtn_access_controller.impl.core;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.liblldp.BitBufferHelper;
import org.opendaylight.controller.liblldp.BufferException;
import org.opendaylight.controller.liblldp.HexEncode;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.packetfilter.rev150105.AclpacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.packetfilter.rev150105.PacketfilterListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.MacAcls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.mac.acl.config.AllowedHosts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.mac.acl.status.MappedHost;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.mac.acl.status.MappedHostBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.mac.acl.status.MappedHostKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.mac_acls.Vtn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.mac_acls.VtnKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.mac_acls.vtn.Vbridge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.mac_acls.vtn.VbridgeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.types.rev150209.vlan.host.desc.set.VlanHostDescList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import vtn_access_controller.impl.core.util.InstanceIdentifierUtils;
public class PacketProcessor implements PacketfilterListener{
	private final VtnRpcService vtnRpcService;
	private static String BROADCAST="ff:ff:ff:ff:ff:ff";
	private Set<NodeConnectorId> inner_ports;
	private Map<VbridgePath,Set<MappedHostConfig>> mac_acl_map;
	private final DataBroker dataBroker;
	private final ExecutorService service;
	public PacketProcessor(final DataBroker dataBroker,Map<VbridgePath,Set<MappedHostConfig>> mac_acl_map,ExecutorService service,Set<NodeConnectorId> inner_ports,final VtnRpcService vtnRpcService) {
		// TODO Auto-generated constructor stub
		this.dataBroker=dataBroker;
		this.mac_acl_map=mac_acl_map;
		this.service=service;
		this.inner_ports=inner_ports;
		this.vtnRpcService=vtnRpcService;
		
	}
	public void loadFromDataStore(){
		InstanceIdentifier<MacAcls> macacl=InstanceIdentifier.builder(MacAcls.class).build();
		ReadWriteTransaction rtx=dataBroker.newReadWriteTransaction();
		CheckedFuture<Optional<MacAcls>, ReadFailedException> future=rtx.read(LogicalDatastoreType.CONFIGURATION, macacl);
		try {
			Optional<MacAcls> optional=future.checkedGet(1000, TimeUnit.MILLISECONDS);
			if(optional.isPresent()){
				if(optional.get().getVtn()==null || optional.get().getVtn().size()==0)
					return;
				for(Vtn v:optional.get().getVtn()){
					String vtnpath=v.getVtn();
					if(v.getVbridge()==null || v.getVbridge().size()==0){
						continue;
					}
					for(Vbridge vbridge:v.getVbridge()){
						String vbridgepath=vbridge.getVbridge();
						VbridgePath path=new VbridgePath(vtnpath, vbridgepath);
						if(vbridge.getMappedHost()!=null){
							Set<MappedHostConfig> hostlist=new HashSet<>();
							for(MappedHost mappedHost:vbridge.getMappedHost()){
								MappedHostConfig mappedHostConfig=new MappedHostConfig();
								mappedHostConfig.set_macAddress(mappedHost.getMacAddress().getValue());
								if(mappedHost.getPortId()!=null){
									mappedHostConfig.set_portId(mappedHost.getPortId());
								}
								hostlist.add(mappedHostConfig);
							}
							mac_acl_map.put(path, hostlist);
						}
						
					}
				}
				
			}
		} catch (ReadFailedException | TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void onAclpacketReceived(AclpacketReceived packetReceived) {
		// TODO Auto-generated method stub
		if (packetReceived == null) {
            return;
        }
		 //if the packet comes from ths inner ports drop it
        NodeConnectorId inport=packetReceived.getIngress().getValue().firstKeyOf(NodeConnector.class).getId();
        if(inner_ports.contains(inport)){
        	return;
        }
        	
		try {
			//check whether this packet is a broadCast Packet,if so drop it;
		    MacAddress destMac=new MacAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(packetReceived.getPayload(), 0, 48)));
		    String destmac=destMac.getValue();
		    //ignore the arp packet
		    if(BROADCAST.equals(destmac)){
		    	return;
		    }
		    //if the src mac is in the white list
			MacAddress srcMac=new MacAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(packetReceived.getPayload(), 48, 48)));
		    String mac=srcMac.getValue();
	        MappedHostConfig mappedHost=new MappedHostConfig().set_macAddress(mac);
	        //querry the map 
	        for(VbridgePath path:mac_acl_map.keySet()){
	        	boolean find=false;
	        	if(mac_acl_map.get(path)!=null){
	        		Set<MappedHostConfig> set=mac_acl_map.get(path);
	        		if(!set.contains(mappedHost)){
	        			continue;
	        		}
	        		for(MappedHostConfig config:set){
	        			if(config.equals(mappedHost)){
	        				find=true;
	        				//the first time detected the packet  
	        				if(config.get_portId()==null){
	        					service.execute(setPortMapTask(path, packetReceived.getIngress(),srcMac));
	        					config.set_portId(inport);
	        					break;
	        				}
	        				// if the host moved to another port
	        				else if(!config.get_portId().equals(inport)){
								service.execute(changePortMapTask(path, packetReceived.getIngress(), srcMac,config.get_portId()));
								config.set_portId(inport);
								break;
							}
	        			}
	        		}
	        		
	        		if(find)
	        			break;
	        	}
	        }
		} catch (BufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    
		
	}
	
	

	
	private Runnable setPortMapTask(VbridgePath path,NodeConnectorRef port,MacAddress macAddress){
		return new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
						//if the port has been used,realease it
						NodeConnectorId connectorId=port.getValue().firstKeyOf(NodeConnector.class).getId();
						vtnRpcService.delVinterface(connectorId);
						if(!vtnRpcService.addVtn(path.getVtn())){
							return;
						}
						if(!vtnRpcService.addVbridge(path.getVtn(),path.getVbridge())){
							return;
						}
						if(!vtnRpcService.addVinterface(path.getVtn(), path.getVbridge(), InstanceIdentifierUtils.buildIfName(path, macAddress))){
							return;
						}
						if(!vtnRpcService.setPortMap(path.getVtn(), path.getVbridge(),InstanceIdentifierUtils.buildIfName(path, macAddress),port)){
							return;
						}
						InstanceIdentifier<MappedHost> identifier=InstanceIdentifier.builder(MacAcls.class)
																  .child(Vtn.class,new VtnKey(path.getVtn()))
																  .child(Vbridge.class,new VbridgeKey(path.getVbridge()))
																  .child(MappedHost.class,new MappedHostKey(macAddress))
																  .build();
						MappedHost host=new MappedHostBuilder().setMacAddress(macAddress).setKey(new MappedHostKey(macAddress)).setPortId(connectorId).build();
						ReadWriteTransaction rtx=dataBroker.newReadWriteTransaction();
						rtx.merge(LogicalDatastoreType.CONFIGURATION, identifier,host,true);
						rtx.submit();
			
			}
		};
		
		
	}
	
	
	private Runnable changePortMapTask(VbridgePath path,NodeConnectorRef port,MacAddress macAddress,NodeConnectorId rawPort){
		return new Runnable() {
			/**
			 * 
			 */
			@Override
			public void run() {
				// TODO Auto-generated method stub
				//querry the detailed information of the connector
				NodeConnectorId connectorId=port.getValue().firstKeyOf(NodeConnector.class).getId();
				vtnRpcService.delVinterface(connectorId);
				if(!vtnRpcService.addVinterface(path.getVtn(), path.getVbridge(), InstanceIdentifierUtils.buildIfName(path, macAddress))){
					return;
				}
				if(!vtnRpcService.setPortMap(path.getVtn(), path.getVbridge(),InstanceIdentifierUtils.buildIfName(path, macAddress),port)){
					return;
				}
				InstanceIdentifier<MappedHost> identifier=InstanceIdentifier.builder(MacAcls.class)
						  .child(Vtn.class,new VtnKey(path.getVtn()))
						  .child(Vbridge.class,new VbridgeKey(path.getVbridge()))
						  .child(MappedHost.class,new MappedHostKey(macAddress))
						  .build();
				MappedHost host=new MappedHostBuilder().setMacAddress(macAddress).setKey(new MappedHostKey(macAddress)).setPortId(connectorId).build();
				ReadWriteTransaction rtx=dataBroker.newReadWriteTransaction();
				rtx.merge(LogicalDatastoreType.CONFIGURATION, identifier,host,true);
				rtx.submit();
			}
				
		};
		
		
	}
	

	
	



	
	



	

}
