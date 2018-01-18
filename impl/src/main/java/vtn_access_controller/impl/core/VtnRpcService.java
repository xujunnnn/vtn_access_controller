/*
 * Copyright © 2017 xujun and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package vtn_access_controller.impl.core;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;

public interface VtnRpcService {

	boolean addVtn(String Vtn);
	boolean RemoveVtn(String Vtn);
	boolean addVbridge(String Vtn,String Vbridge);
	boolean removeVbridge(String Vtn,String Vbridg);
	boolean addVinterface(String Vtn,String Vbridge,String Vinterface);
	boolean removeVinterface(String Vtn,String Vbridge,String Vinterface);
	boolean removePortMap(String Vtn,String Vbridge,String Vinterface);
	boolean setPortMap(String Vtn, String Vbridge, String Vinterface, NodeConnectorRef nodeConnector);
	boolean delVinterface(NodeConnectorId nodeConnector);

	

}
