package org.eclipse.scanning.api.event.queues.beans;

import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;

/**
 * Base class for all atoms/beans which will be handled by the 
 * {@link IQueueService}.
 * 
 * @author Michael Wharmby
 *
 */
public abstract class Queueable extends StatusBean implements IQueueable {
	
	protected long runTime;
	protected String beamline;

	protected Queueable() {
		super();
		setStatus(Status.NONE);
	}
	
	@Override
	public String getBeamline() {
		return beamline;
	}
	
	@Override
	public void setBeamline(String beamline) {
		this.beamline = beamline;
	}

	@Override
	public long getRunTime() {
		return runTime;
	}

	@Override
	public void setRunTime(long runTime) {
		this.runTime = runTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (int) (runTime ^ (runTime >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Queueable other = (Queueable) obj;
		if (runTime != other.runTime)
			return false;
		return true;
	}

}
