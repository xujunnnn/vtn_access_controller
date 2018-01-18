/*
 * Copyright © 2017 xujun and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package vtn_access_controller.impl.core;

public class VbridgePath {
	
	private String Vtn;
	private String vbridge;
	public VbridgePath(String Vtn,String VBridge) {
		// TODO Auto-generated constructor stub
		this.Vtn=Vtn;
		this.vbridge=VBridge;
	}
	public String getVtn() {
		return Vtn;
	}
	public void setVtn(String vtn) {
		Vtn = vtn;
	}
	public String getVbridge() {
		return vbridge;
	}
	public void setVbridge(String vbridge) {
		this.vbridge = vbridge;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((Vtn == null) ? 0 : Vtn.hashCode());
		result = prime * result + ((vbridge == null) ? 0 : vbridge.hashCode());
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
		VbridgePath other = (VbridgePath) obj;
		if (Vtn == null) {
			if (other.Vtn != null)
				return false;
		} else if (!Vtn.equals(other.Vtn))
			return false;
		if (vbridge == null) {
			if (other.vbridge != null)
				return false;
		} else if (!vbridge.equals(other.vbridge))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "VbridgePath [Vtn=" + Vtn + ", vbridge=" + vbridge + "]";
	}
	
	

}
