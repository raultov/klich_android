package net.SoftForLife.Klich2.model;

import net.softforlife.klich.model.Tuser;

public class TuserMobile extends Tuser {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private boolean registered = false;

	public boolean isRegistered() {
		return registered;
	}

	public void setRegistered(boolean registered) {
		this.registered = registered;
	}
	
	

}
