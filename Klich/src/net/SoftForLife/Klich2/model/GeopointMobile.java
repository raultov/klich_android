package net.SoftForLife.Klich2.model;

import net.softforlife.klich.model.Geopoint;

public class GeopointMobile extends Geopoint {
	/** Constante serialVersionUID. */
	private static final long serialVersionUID = 1L;
	
	private Boolean sent;

	
	
	public void setSent(Boolean sent) {
		this.sent = sent;
	}

	public Boolean getSent() {
		return sent;
	}
	
	
}
