package org.eclipse.scanning.test.command;

import org.eclipse.dawnsci.json.MarshallerService;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.points.serialization.PointsModelMarshaller;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.command.Services;

import uk.ac.diamond.daq.activemq.connector.ActivemqConnectorService;


public class SubmissionTest extends AbstractSubmissionTest {

	@Override
	protected void setUpEventService() {
		ActivemqConnectorService.setJsonMarshaller(new MarshallerService(new PointsModelMarshaller()));
		IEventService eservice = new EventServiceImpl(new ActivemqConnectorService());
		Services.setEventService(eservice);
	}

}
