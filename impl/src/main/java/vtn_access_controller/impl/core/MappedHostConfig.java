/*
 * Copyright © 2017 xujun and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package vtn_access_controller.impl.core;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;

public class MappedHostConfig {
    private String _macAddress;
    private NodeConnectorId _portId;
    private int _vlanId;
	public String get_macAddress() {
		return _macAddress;
	}
	public MappedHostConfig set_macAddress(String _macAddress) {
		this._macAddress = _macAddress;
		return this;
	}
	public NodeConnectorId get_portId() {
		return _portId;
	}
	public MappedHostConfig set_portId(NodeConnectorId _portId) {
		this._portId = _portId;
		return this;
	}
	public int get_vlanId() {
		return _vlanId;
	}
	public MappedHostConfig set_vlanId(int _vlanId) {
		this._vlanId = _vlanId;
		return this;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_macAddress == null) ? 0 : _macAddress.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MappedHostConfig other = (MappedHostConfig) obj;
		if (_macAddress == null) {
			if (other._macAddress != null)
				return false;
		} else if (!_macAddress.equals(other._macAddress))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "MappedHostConfig [_macAddress=" + _macAddress + ", _portId=" + _portId + ", _vlanId=" + _vlanId + "]";
	}
	



}