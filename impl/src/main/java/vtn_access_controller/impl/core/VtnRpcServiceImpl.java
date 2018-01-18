/*
 * Copyright © 2017 xujun and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package vtn_access_controller.impl.core;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.crypto.Data;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.mapping.port.rev150907.RemovePortMapInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.mapping.port.rev150907.RemovePortMapInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.mapping.port.rev150907.RemovePortMapOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.mapping.port.rev150907.SetPortMapInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.mapping.port.rev150907.SetPortMapInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.mapping.port.rev150907.SetPortMapOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.mapping.port.rev150907.VtnPortMapService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.rev150328.RemoveVtnInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.rev150328.RemoveVtnInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.rev150328.UpdateVtnInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.rev150328.UpdateVtnInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.rev150328.UpdateVtnOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.rev150328.VtnService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.rev150328.Vtns;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.rev150328.vtns.Vtn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.rev150328.vtns.VtnKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.types.rev150209.VnodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.types.rev150209.VnodeUpdateMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.types.rev150209.VtnUpdateOperationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.vbridge.rev150907.RemoveVbridgeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.vbridge.rev150907.RemoveVbridgeInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.vbridge.rev150907.UpdateVbridgeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.vbridge.rev150907.UpdateVbridgeInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.vbridge.rev150907.UpdateVbridgeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.vbridge.rev150907.VtnVbridgeService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.vbridge.rev150907.vtn.vbridge.list.Vbridge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.vbridge.rev150907.vtn.vbridge.list.VbridgeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.vinterface.rev150907.RemoveVinterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.vinterface.rev150907.RemoveVinterfaceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.vinterface.rev150907.UpdateVinterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.vinterface.rev150907.UpdateVinterfaceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.vinterface.rev150907.UpdateVinterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.vinterface.rev150907.VtnVinterfaceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.vinterface.rev150907.vtn.mappable.vinterface.list.Vinterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.vinterface.rev150907.vtn.mappable.vinterface.list.VinterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class VtnRpcServiceImpl implements VtnRpcService{
	private final DataBroker dataBroker;
	private final VtnService vtnService;
	private final VtnVbridgeService vbridgeService;
	private final VtnVinterfaceService vinterfaceService;
	private final VtnPortMapService portMapService;
	private final RpcProviderRegistry registry;
	public VtnRpcServiceImpl(final RpcProviderRegistry registry,final DataBroker dataBroker) {
		// TODO Auto-generated constructor stub
		this.dataBroker=dataBroker;
		this.registry=registry;
		vtnService=registry.getRpcService(VtnService.class);
		vbridgeService=registry.getRpcService(VtnVbridgeService.class);
		vinterfaceService=registry.getRpcService(VtnVinterfaceService.class);
		portMapService=registry.getRpcService(VtnPortMapService.class);
	}

	@Override
	public boolean addVtn(String Vtn) {
		// TODO Auto-generated method stub
		
		InstanceIdentifier<Vtn> ii=InstanceIdentifier.builder(Vtns.class).child(Vtn.class, new VtnKey(new VnodeName(Vtn))).build();
		ReadOnlyTransaction rx=dataBroker.newReadOnlyTransaction();
		CheckedFuture<Optional<Vtn>, ReadFailedException> readfuture=rx.read(LogicalDatastoreType.OPERATIONAL, ii);
		try {
			if((readfuture.get(1000, TimeUnit.MILLISECONDS).isPresent())){
				return true;
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
				UpdateVtnInput input=new UpdateVtnInputBuilder().setDescription("creating "+Vtn)
							 .setHardTimeout(0)
							 .setIdleTimeout(3000)
							 .setTenantName(Vtn)
							 .setOperation(VtnUpdateOperationType.SET)
							 .setUpdateMode(VnodeUpdateMode.CREATE)
							 .build();
		Future<RpcResult<UpdateVtnOutput>> future=vtnService.updateVtn(input);
		try {
			
			if(future.get(1000, TimeUnit.MILLISECONDS).isSuccessful()){
				return true;
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
		
	}

	@Override
	public boolean RemoveVtn(String Vtn) {
		// TODO Auto-generated method stub
		InstanceIdentifier<Vtn> ii=InstanceIdentifier.builder(Vtns.class).child(Vtn.class, new VtnKey(new VnodeName(Vtn))).build();
		ReadOnlyTransaction rx=dataBroker.newReadOnlyTransaction();
		CheckedFuture<Optional<Vtn>, ReadFailedException>readFuture=rx.read(LogicalDatastoreType.OPERATIONAL, ii);
		try {
			if((!readFuture.get(1000, TimeUnit.MILLISECONDS).isPresent())){
				return true;
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		RemoveVtnInput input=new RemoveVtnInputBuilder().setTenantName(Vtn).build();
		Future<RpcResult<Void>> future=vtnService.removeVtn(input);
		try {
			
			if(future.get(1000, TimeUnit.MILLISECONDS).isSuccessful()){
				return true;
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}

	@Override
	public boolean addVbridge(String Vtn, String Vbridge) {
		// TODO Auto-generated method stub
		InstanceIdentifier<Vbridge> ii=InstanceIdentifier.builder(Vtns.class).
				child(Vtn.class,new VtnKey(new VnodeName(Vtn))).
				child(Vbridge.class, new VbridgeKey(new VnodeName(Vbridge))).
				build();
		ReadOnlyTransaction rx=dataBroker.newReadOnlyTransaction();
		CheckedFuture<Optional<Vbridge>, ReadFailedException>readFuture=rx.read(LogicalDatastoreType.OPERATIONAL, ii);
		try {
			if((readFuture.get(1000, TimeUnit.MILLISECONDS).isPresent())){
				return true;
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		UpdateVbridgeInput input=new UpdateVbridgeInputBuilder()
								 .setTenantName(Vtn)
								 .setBridgeName(Vbridge)
								 .setDescription("createing "+Vbridge)
								 .setUpdateMode(VnodeUpdateMode.CREATE)
								 .setOperation(VtnUpdateOperationType.SET)
								 .build();
		Future<RpcResult<UpdateVbridgeOutput>> future=vbridgeService.updateVbridge(input);
		try {
			if(future.get(1000, TimeUnit.MILLISECONDS).isSuccessful()){
				return true;
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
		
	}

	@Override
	public boolean removeVbridge(String Vtn, String Vbridg) {
		// TODO Auto-generated method stub
		InstanceIdentifier<Vbridge> ii=InstanceIdentifier.builder(Vtns.class).child(Vtn.class, new VtnKey(new VnodeName(Vtn))).child(Vbridge.class,new VbridgeKey(new VnodeName(Vbridg))).build();
		ReadOnlyTransaction rx=dataBroker.newReadOnlyTransaction();
		CheckedFuture<Optional<Vbridge>, ReadFailedException> readfuture=rx.read(LogicalDatastoreType.OPERATIONAL, ii);
		try {
			Optional<Vbridge> optional=readfuture.checkedGet(1000, TimeUnit.MILLISECONDS);
			if(!optional.isPresent()){
				return true;
			}
		} catch (ReadFailedException | TimeoutException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		RemoveVbridgeInput input=new RemoveVbridgeInputBuilder()
								 .setTenantName(Vtn)
								 .setBridgeName(Vbridg)
								 .build();
		Future<RpcResult<Void>> future=vbridgeService.removeVbridge(input);
		try {
			if(future.get(1000, TimeUnit.MILLISECONDS).isSuccessful()){
				return true;
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean addVinterface(String Vtn, String Vbridge, String Vinterface) {
		// TODO Auto-generated method stub
		InstanceIdentifier<Vinterface> ii=InstanceIdentifier.builder(Vtns.class).
										  child(Vtn.class,new VtnKey(new VnodeName(Vtn))).
										  child(Vbridge.class, new VbridgeKey(new VnodeName(Vbridge))).
										  child(Vinterface.class,new VinterfaceKey(new VnodeName(Vinterface))).
										  build();
		ReadOnlyTransaction rx=dataBroker.newReadOnlyTransaction();
		CheckedFuture<Optional<Vinterface>, ReadFailedException>readFuture=rx.read(LogicalDatastoreType.OPERATIONAL, ii);
		try {
			if((readFuture.get(1000, TimeUnit.MILLISECONDS).isPresent())){
				return true;
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		UpdateVinterfaceInput input=new UpdateVinterfaceInputBuilder()
									.setTenantName(Vtn)
									.setBridgeName(Vbridge)
									.setDescription("creating vinterface "+Vinterface)
									.setUpdateMode(VnodeUpdateMode.CREATE)
									.setOperation(VtnUpdateOperationType.SET)
									.setEnabled(true)
									.setInterfaceName(Vinterface)
		                            .build(); 
		Future<RpcResult<UpdateVinterfaceOutput>> future=vinterfaceService.updateVinterface(input);
		try {
			if(future.get(1000, TimeUnit.MILLISECONDS).isSuccessful()){
				return true;
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean removeVinterface(String Vtn, String Vbridge, String Vinterface) {
		// TODO Auto-generated method stub
		InstanceIdentifier<Vinterface> ii=InstanceIdentifier.builder(Vtns.class).
				  child(Vtn.class,new VtnKey(new VnodeName(Vtn))).
				  child(Vbridge.class, new VbridgeKey(new VnodeName(Vbridge))).
				  child(Vinterface.class,new VinterfaceKey(new VnodeName(Vinterface))).
				  build();
		ReadOnlyTransaction rx=dataBroker.newReadOnlyTransaction();
		CheckedFuture<Optional<Vinterface>, ReadFailedException>readFuture=rx.read(LogicalDatastoreType.OPERATIONAL, ii);
		try {
			if((!readFuture.get(1000, TimeUnit.MILLISECONDS).isPresent())){
				return true;
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		RemoveVinterfaceInput input=new RemoveVinterfaceInputBuilder()
									.setTenantName(Vtn)
									.setBridgeName(Vbridge)
									.setInterfaceName(Vinterface)
									.build();
		Future<RpcResult<Void>> future=vinterfaceService.removeVinterface(input);
		try {
			if(future.get(1000, TimeUnit.MILLISECONDS).isSuccessful()){
				return true;
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	@Override
	public boolean delVinterface(NodeConnectorId nodeConnector){
		InstanceIdentifier<Vtns> vIdentifier=InstanceIdentifier.builder(Vtns.class).build();
		ReadOnlyTransaction rx=dataBroker.newReadOnlyTransaction();
		CheckedFuture<Optional<Vtns>, ReadFailedException> checkedFuture=rx.read(LogicalDatastoreType.OPERATIONAL, vIdentifier);
		Optional<Vtns> result;
		try {
			result = checkedFuture.checkedGet(1000, TimeUnit.MILLISECONDS);
			if(result.isPresent()){
				Vtns vtns=result.get();
				if(vtns.getVtn()!=null){
					for(Vtn vtn:vtns.getVtn()){
						if(vtn.getVbridge()!=null){
							for(org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.vbridge.rev150907.vtn.vbridge.list.Vbridge vbridge:vtn.getVbridge()){
								if(vbridge.getVinterface()!=null){
									for(Vinterface vinterface:vbridge.getVinterface()){
										if(vinterface.getVinterfaceStatus()!=null && nodeConnector.equals(vinterface.getVinterfaceStatus().getMappedPort())){
											//delete the vinterface
											if( removePortMap(vtn.getName().getValue(), vbridge.getName().getValue(), vinterface.getName().getValue())){
												return true;
											}
																							
										
										}
									}

								}
						
							}
						}
					}
				}
			}
		} catch (ReadFailedException | TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	
		
	}

	@Override
	public boolean setPortMap(String Vtn, String Vbridge, String Vinterface, NodeConnectorRef nodeConnector) {
		// TODO Auto-generated method stub
		InstanceIdentifier<NodeConnector> ii=(InstanceIdentifier<NodeConnector>) nodeConnector.getValue();
		ReadWriteTransaction rtx=dataBroker.newReadWriteTransaction();
		CheckedFuture<Optional<NodeConnector>, ReadFailedException>future=rtx.read(LogicalDatastoreType.OPERATIONAL, ii);
		Optional<NodeConnector> optional;
		try {
			optional = future.checkedGet(1000, TimeUnit.MILLISECONDS);
			if(optional.isPresent()){
				NodeConnector connector=optional.get();
				FlowCapableNodeConnector flowCapableNodeConnector=connector.getAugmentation(FlowCapableNodeConnector.class);
				String portname=flowCapableNodeConnector.getName();
				String connectorinfo[]=connector.getId().getValue().split(":");
				NodeId nodeId=new NodeId(connectorinfo[0]+":"+connectorinfo[1]);
				SetPortMapInput input=new SetPortMapInputBuilder()
								  .setTenantName(Vtn)
								  .setBridgeName(Vbridge)
								  .setNode(nodeId)
								  .setPortName(portname)
								  .setInterfaceName(Vinterface)
								  .build();
				Future<RpcResult<SetPortMapOutput>> rpcfuture=portMapService.setPortMap(input);
				if(rpcfuture.get(1000, TimeUnit.MILLISECONDS).isSuccessful()){
					return true;
				}
			}
		} catch (ReadFailedException | TimeoutException | InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		return false;
	}

	@Override
	public boolean removePortMap(String Vtn, String Vbridge, String Vinterface) {
		// TODO Auto-generated method stub
		RemovePortMapInput input=new RemovePortMapInputBuilder().setTenantName(Vtn).setBridgeName(Vbridge).setInterfaceName(Vinterface).build();
		Future<RpcResult<RemovePortMapOutput>> future=portMapService.removePortMap(input);
		try {
			if(future.get(1000, TimeUnit.MILLISECONDS).isSuccessful()){
				return true;
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	
	}

}
