/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package vtn_access_controller.impl.rpcservice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.MacAcls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.SetMacMapAclInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.SetMacMapAclOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.SetMacMapAclOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.VtnAccessControllerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.mac.acl.config.AllowedHosts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.mac.acl.config.AllowedHostsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.mac.acl.status.MappedHost;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.mac.acl.status.MappedHostBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.mac.acl.status.MappedHostKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.mac_acls.Vtn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.mac_acls.VtnKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.mac_acls.vtn.Vbridge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vtn_access_controller.rev150907.mac_acls.vtn.VbridgeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.types.rev150209.VlanHostDesc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.types.rev150209.VtnUpdateOperationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.types.rev150209.VtnUpdateType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.types.rev150209.vlan.host.desc.set.VlanHostDescList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.types.rev150209.vlan.host.desc.set.VlanHostDescListBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import vtn_access_controller.impl.core.MappedHostConfig;
import vtn_access_controller.impl.core.VbridgePath;
import vtn_access_controller.impl.core.VtnRpcService;
import vtn_access_controller.impl.core.util.InstanceIdentifierUtils;

public class VtnAccessControllerServiceImpl implements VtnAccessControllerService{
	private Map<VbridgePath,Set<MappedHostConfig>> mac_acl_map;
	private final ExecutorService service;
	private final DataBroker databroker;
	private final VtnRpcService vtnRpcService;
    public VtnAccessControllerServiceImpl(final ExecutorService service,final DataBroker dataBroker,Map<VbridgePath,Set<MappedHostConfig>> mac_acl_map,final VtnRpcService vtnRpcService) {
		// TODO Auto-generated constructor stub
    	this.service=service;
    	this.databroker=dataBroker;
    	this.mac_acl_map=mac_acl_map;
    	this.vtnRpcService=vtnRpcService;
	}
	@Override
	public Future<RpcResult<SetMacMapAclOutput>> setMacMapAcl(SetMacMapAclInput input) {
		// TODO Auto-generated method stub
		SetMacMapAclOutputBuilder builder;
		VbridgePath path=new VbridgePath(input.getTenantName(), input.getBridgeName());	
		if(input.getOperation()==VtnUpdateOperationType.ADD){	
			Set<MappedHostConfig> hostlist;
			if(mac_acl_map.containsKey(path)){
				hostlist=mac_acl_map.get(path);
			}
			else {
				hostlist=new HashSet<>();
				mac_acl_map.put(path, hostlist);
			}
			for(VlanHostDesc host:input.getHosts()){
				String hostinfo[]=host.getValue().split("@");
				String macAddress=hostinfo[0];
				MappedHostConfig mappedHost=new MappedHostConfig().set_macAddress(macAddress);
				hostlist.add(mappedHost);
			}
			service.execute(addHost(input.getTenantName(), input.getBridgeName(), input.getHosts()));
		}
		else if(input.getOperation()==VtnUpdateOperationType.REMOVE){
			if(input.getTenantName()!=null && input.getBridgeName()!=null && input.getHosts()!=null){
				service.execute(removeHost(input.getTenantName(), input.getBridgeName(), input.getHosts()));
				
			}
			else if (input.getTenantName()!=null && input.getBridgeName()!=null) {
				mac_acl_map.remove(path);
				service.execute(removeVbridge(input.getTenantName(), input.getBridgeName()));
			}
			else if(input.getTenantName()!=null){
				Iterator<VbridgePath> iterator=mac_acl_map.keySet().iterator();
				while(iterator.hasNext()){
					VbridgePath delpath=iterator.next();
					if(input.getTenantName().equals(delpath.getVtn())){
						iterator.remove();
					}
				}
				service.execute(removeVtn(input.getTenantName()));
			}
				
				
		
		}
		builder=new SetMacMapAclOutputBuilder();
		builder.setStatus(VtnUpdateType.CHANGED);
		return RpcResultBuilder.success(builder.build()).buildFuture();
		
		
	}
	
	/**
	 * private Runnable SetMacAclTask(SetMacMapAclInput input){
		
		InstanceIdentifier<AllowedHosts> path=InstanceIdentifier.builder(MacAcls.class)
				.child(Vtn.class, new VtnKey(input.getTenantName()))
				.child(Vbridge.class,new VbridgeKey(input.getBridgeName()))
				.child(AllowedHosts.class).build();
		List<VlanHostDescList> list=new ArrayList<>();
		for(VlanHostDesc vhd:input.getHosts()){
			list.add(new VlanHostDescListBuilder().setHost(vhd).build());
		}
		AllowedHosts allowedHosts=new AllowedHostsBuilder().setVlanHostDescList(list).build();
		if(input.getOperation()==VtnUpdateOperationType.ADD){
			return new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					ReadWriteTransaction rtx=databroker.newReadWriteTransaction();
					rtx.merge(LogicalDatastoreType.CONFIGURATION, path,allowedHosts,true);
					rtx.submit();
					
				}
			};
		}
		else if (input.getOperation()==VtnUpdateOperationType.REMOVE) {
			return new Runnable() {				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					ReadWriteTransaction rtx=databroker.newReadWriteTransaction();
					rtx.delete(LogicalDatastoreType.CONFIGURATION, path);
					rtx.submit();
					
				}
			};
		}
		else {
			return null;
		}
		
	}
	 */
	/**
	 * add new macs into the datestore
	 * @param vtn
	 * @param vbridge
	 * @param hosts
	 * @return
	 */
	private Runnable addHost(String vtn,String vbridge,List<VlanHostDesc> hosts ){
		return new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				ReadWriteTransaction rtx=databroker.newReadWriteTransaction();
				for(VlanHostDesc vhd:hosts){
					MacAddress macAddress=new MacAddress(vhd.getValue().split("@")[0]);
					InstanceIdentifier<MappedHost> path=InstanceIdentifier.builder(MacAcls.class)
							.child(Vtn.class, new VtnKey(vtn))
							.child(Vbridge.class,new VbridgeKey(vbridge))
							.child(MappedHost.class,new MappedHostKey(macAddress))
							.build();
					
					MappedHost host=new MappedHostBuilder().setMacAddress(macAddress).setKey(new MappedHostKey(macAddress)).build();
					rtx.put(LogicalDatastoreType.CONFIGURATION, path,host,true);
				}
				rtx.submit();
				
			
				
			}
		};
		
		
	}
	/**
	 * delete the vtn
	 * @param vtn
	 * @return
	 */
	private Runnable removeVtn(String vtn){
		return new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if(vtnRpcService.RemoveVtn(vtn)){
					InstanceIdentifier<Vtn> ii=InstanceIdentifier.builder(MacAcls.class).child(Vtn.class, new VtnKey(vtn)).build();
					ReadWriteTransaction rtx=databroker.newReadWriteTransaction();
					rtx.delete(LogicalDatastoreType.CONFIGURATION, ii);
					rtx.submit();
				}
				
			}
		};
	}
	/**
	 * delete the vbridge
	 * @param vtn
	 * @param Vbridge
	 * @return
	 */
	private Runnable removeVbridge(String vtn,String Vbridge){
		return new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if(vtnRpcService.removeVbridge(vtn, Vbridge)){
					InstanceIdentifier<Vbridge> ii=InstanceIdentifier.builder(MacAcls.class).child(Vtn.class,new VtnKey(vtn)).child(Vbridge.class,new VbridgeKey(Vbridge)).build();
					ReadWriteTransaction rtx=databroker.newReadWriteTransaction();
					rtx.delete(LogicalDatastoreType.CONFIGURATION, ii);
					rtx.submit();
				}
				
			}
		};
		
	}
	
	private Runnable removeHost(String vtn,String Vbridge,List<VlanHostDesc> hosts){
		return new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if(mac_acl_map.get(new VbridgePath(vtn, Vbridge))!=null){
					Set<MappedHostConfig> set=mac_acl_map.get(new VbridgePath(vtn, Vbridge));
					for(VlanHostDesc host:hosts){
						for(MappedHostConfig hostConfig:set){
							if(host.getValue().split("@")[0].equals(hostConfig.get_macAddress())){
								if(hostConfig.get_portId()!=null){
									vtnRpcService.removeVinterface(vtn, Vbridge,InstanceIdentifierUtils.buildIfName(new VbridgePath(vtn, Vbridge), new MacAddress(hostConfig.get_macAddress())));
								}
							}
						}
					}
				}
				Set<MappedHostConfig> hostlist=mac_acl_map.get(new VbridgePath(vtn, Vbridge));
				for(VlanHostDesc host:hosts){
					String hostinfo[]=host.getValue().split("@");
					String macAddress=hostinfo[0];
					MappedHostConfig mappedHost=new MappedHostConfig().set_macAddress(macAddress);
					hostlist.remove(mappedHost);
				}
				ReadWriteTransaction rtx=databroker.newReadWriteTransaction();
				for(VlanHostDesc vhd:hosts){
					MacAddress macAddress=new MacAddress(vhd.getValue().split("@")[0]);
					InstanceIdentifier<MappedHost> path=InstanceIdentifier.builder(MacAcls.class)
							.child(Vtn.class, new VtnKey(vtn))
							.child(Vbridge.class,new VbridgeKey(Vbridge))
							.child(MappedHost.class,new MappedHostKey(macAddress))
							.build();
					rtx.delete(LogicalDatastoreType.CONFIGURATION, path);
					
				}
				rtx.submit();
		
				
			}
		};
		
		
		
	}
	
	
	
	

}