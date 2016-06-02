package org.eclipse.scanning.test.event.queues;

import org.eclipse.dawnsci.json.MarshallerService;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.event.queues.AtomQueueService;
import org.eclipse.scanning.event.queues.QueueServicesHolder;
import org.junit.Before;

import uk.ac.diamond.daq.activemq.connector.ActivemqConnectorService;

/**
 * Test of the concrete implementation {@link AtomQueueService} of the {@link IQueueService}.
 * Tests located in {@link AbstractQueueServiceTest}.
 * @author wnm24546
 *
 */
public class AtomQueueServiceDummyTest extends AbstractQueueServiceTest {
	
	@Before
	public void createService() throws Exception {
		//Set up IEventService //TODO Use EventInfrastructure
		ActivemqConnectorService.setJsonMarshaller(new MarshallerService());
		QueueServicesHolder.setEventService(new EventServiceImpl(new ActivemqConnectorService()));
		
		//Create QueueService with 2 argument constructor for tests
		qServ = new AtomQueueService(qRoot, uri);
		
		//All set? Let's go!
		qServ.init();
	}

}
