package org.eclipse.scanning.test.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EventListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.eclipse.dawnsci.analysis.api.persistence.IMarshallerService;
import org.eclipse.dawnsci.json.MarshallerService;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.alive.HeartbeatBean;
import org.eclipse.scanning.api.event.alive.HeartbeatEvent;
import org.eclipse.scanning.api.event.alive.IHeartbeatListener;
import org.eclipse.scanning.api.event.alive.KillBean;
import org.eclipse.scanning.api.event.alive.PauseBean;
import org.eclipse.scanning.api.event.bean.BeanEvent;
import org.eclipse.scanning.api.event.bean.IBeanListener;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IProcessCreator;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.ISubmitter;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.event.dry.DryRunCreator;
import org.eclipse.scanning.event.dry.FastRunCreator;
import org.eclipse.scanning.points.serialization.PointsModelMarshaller;
import org.junit.After;
import org.junit.Test;

public class AbstractConsumerTest {

	
	protected IEventService          eservice;
	protected ISubmitter<StatusBean> submitter;
	protected IConsumer<StatusBean>  consumer;

	
	@After
	public void dispose() throws EventException {
		submitter.disconnect();
		consumer.clearQueue(IEventService.SUBMISSION_QUEUE);
		consumer.clearQueue(IEventService.STATUS_SET);
		consumer.disconnect();
	}
	
    @Test
	public void testSimpleSubmission() throws Exception {
		
		StatusBean bean = doSubmit();
		
		// Manually take the submission from the list not using event service for isolated test
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(submitter.getUri());		
		Connection connection = connectionFactory.createConnection();
		
		try {
			Session   session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue queue = session.createQueue(IEventService.SUBMISSION_QUEUE);
	
			final MessageConsumer consumer = session.createConsumer(queue);
			connection.start();
	
			TextMessage msg = (TextMessage)consumer.receive(1000);
			
			IMarshallerService marshaller = new MarshallerService(new PointsModelMarshaller());
			StatusBean fromQ = marshaller.unmarshal(msg.getText(), StatusBean.class);
        	
        	if (!fromQ.equals(bean)) throw new Exception("The bean from the queue was not the same as that submitted! q="+fromQ+" submit="+bean);
        	
		} finally {
			connection.close();
		}
	}

    @Test
	public void testSimpleConsumer() throws Exception {
    	
		consumer.setRunner(new DryRunCreator<StatusBean>());
		consumer.start();
		
		StatusBean bean = doSubmit();
		 	
		Thread.sleep(14000); // 10000 to do the loop, 4000 for luck
		
		List<StatusBean> stati = consumer.getStatusSet();
		if (stati.size()!=1) throw new Exception("Unexpected status size in queue! Might not have status or have forgotten to clear at end of test!");
		
		StatusBean complete = stati.get(0);
		
       	if (complete.equals(bean)) {
       		throw new Exception("The bean from the status queue was the same as that submitted! It should have a different status. q="+complete+" submit="+bean);
       	}
        
       	if (complete.getStatus()!=Status.COMPLETE) {
       		throw new Exception("The bean in the queue is not complete!"+complete);
       	}
       	if (complete.getPercentComplete()<100) {
       		throw new Exception("The percent complete is less than 100!"+complete);
       	}
    }
    
    
    @Test
    public void testBeanClass() throws Exception {
    	
		IConsumer<StatusBean> fconsumer   = eservice.createConsumer(this.consumer.getUri(), IEventService.SUBMISSION_QUEUE, IEventService.STATUS_SET, IEventService.STATUS_TOPIC, IEventService.HEARTBEAT_TOPIC, IEventService.CMD_TOPIC);
		try {
			fconsumer.setRunner(new DryRunCreator<StatusBean>());
			fconsumer.start(); // No bean!
			
	     	FredStatusBean bean = new FredStatusBean();
			bean.setName("Frederick");
	       	
			dynamicBean(bean, fconsumer, 1);
			
		} finally {
			fconsumer.clearQueue(IEventService.SUBMISSION_QUEUE);
			fconsumer.clearQueue(IEventService.STATUS_SET);
			fconsumer.disconnect();
		}

    }

	@Test
    public void testBeanClass2Beans() throws Exception {
    	
		IConsumer<StatusBean> fconsumer   = eservice.createConsumer(this.consumer.getUri(), IEventService.SUBMISSION_QUEUE, IEventService.STATUS_SET, IEventService.STATUS_TOPIC, IEventService.HEARTBEAT_TOPIC, IEventService.CMD_TOPIC);
		try {
			fconsumer.setRunner(new DryRunCreator<StatusBean>());
			fconsumer.start();// It's going now, we can submit
			
	     	FredStatusBean fred = new FredStatusBean();
			fred.setName("Frederick");       	
			dynamicBean(fred, fconsumer, 1);
			
			BillStatusBean bill = new BillStatusBean();
			bill.setName("Bill");
			dynamicBean(bill, fconsumer, 2);

		} finally {
			fconsumer.clearQueue(IEventService.SUBMISSION_QUEUE);
			fconsumer.clearQueue(IEventService.STATUS_SET);
			fconsumer.disconnect();
		}

    }
    
    private void dynamicBean(final StatusBean bean, IConsumer<StatusBean> fconsumer, int statusSize) throws Exception {
    	
    	// Hard code the service for the test
		ISubscriber<EventListener> sub = eservice.createSubscriber(fconsumer.getUri(), fconsumer.getStatusTopicName());
		sub.addListener(new IBeanListener<StatusBean>() {
			@Override
			public void beanChangePerformed(BeanEvent<StatusBean> evt) {
				if (!evt.getBean().getName().equals(bean.getName())) {
					System.out.println("This is not our bean! "+bean);
					Thread.dumpStack();
				}
			}
		});

		doSubmit(bean);
				
		Thread.sleep(14000); // 10000 to do the loop, 4000 for luck
		
		List<StatusBean> stati = fconsumer.getStatusSet();
		if (stati.size()!=statusSize) throw new Exception("Unexpected status size in queue! Might not have status or have forgotten to clear at end of test!");
		
		StatusBean complete = stati.get(0); // The queue is date sorted.
		
       	if (complete.equals(bean)) {
       		throw new Exception("The bean from the status queue was the same as that submitted! It should have a different status. q="+complete+" submit="+bean);
       	}
        
       	if (complete.getStatus()!=Status.COMPLETE) {
       		throw new Exception("The bean in the queue is not complete!"+complete);
       	}
       	if (complete.getPercentComplete()<100) {
       		throw new Exception("The percent complete is less than 100!"+complete);
       	}
       	
       	sub.disconnect();
	}

    @Test
	public void testConsumerStop() throws Exception {
        testStop(new DryRunCreator<StatusBean>());
    }
    @Test
	public void testConsumerStopNonBlockingProcess() throws Exception {
        testStop(new DryRunCreator<StatusBean>(false));
    }

    private void testStop(IProcessCreator<StatusBean> dryRunCreator) throws Exception {
    	
		consumer.setRunner(dryRunCreator);
		consumer.start();

		StatusBean bean = doSubmit();

		Thread.sleep(2000);
		
		consumer.stop();
		
		Thread.sleep(5000);
		checkTerminatedProcess(bean);

	}
    
	
    @Test
    public void testKillingAConsumer() throws Exception {
    	
		consumer.setRunner(new DryRunCreator<StatusBean>());
		consumer.start();

		StatusBean bean = doSubmit();

		Thread.sleep(2000);

		IPublisher<KillBean> killer = eservice.createPublisher(submitter.getUri(), IEventService.CMD_TOPIC);
		KillBean kbean = new KillBean();
		kbean.setConsumerId(consumer.getConsumerId());
		kbean.setExitProcess(false); // Or tests would exit!
		kbean.setDisconnect(false);  // Or we cannot ask for the list of what's left
		killer.broadcast(kbean);
		
		Thread.sleep(5000);
		checkTerminatedProcess(bean);
		
    }
    
    @Test
    public void testPausingAConsumerByID() throws Exception {
    	
		consumer.setRunner(new DryRunCreator<StatusBean>(false));
		consumer.start();

		StatusBean bean = doSubmit();

		Thread.sleep(2000);

		IPublisher<PauseBean> pauser = eservice.createPublisher(submitter.getUri(), IEventService.CMD_TOPIC);
		PauseBean pbean = new PauseBean();
		pbean.setConsumerId(consumer.getConsumerId());
		pauser.broadcast(pbean);
		
		Thread.sleep(2000);
		
		assertTrue(!consumer.isActive());
		
		pbean.setPause(false);
		pauser.broadcast(pbean);

		Thread.sleep(1000);
	
		assertTrue(consumer.isActive());
    }

    
    @Test
    public void testPausingAConsumerByQueueName() throws Exception {
    	
		consumer.setRunner(new DryRunCreator<StatusBean>(false));
		consumer.start();

		StatusBean bean = doSubmit();

		Thread.sleep(2000);

		IPublisher<PauseBean> pauser = eservice.createPublisher(submitter.getUri(), IEventService.CMD_TOPIC);
		PauseBean pbean = new PauseBean();
		pbean.setQueueName(consumer.getSubmitQueueName());
		pauser.broadcast(pbean);
		
		Thread.sleep(2000);
		
		assertTrue(!consumer.isActive());
		
		pbean.setPause(false);
		pauser.broadcast(pbean);

		Thread.sleep(1000);
	
		assertTrue(consumer.isActive());
    }

    @Test
    public void testReorderingAPausedQueue() throws Exception {
    	
		consumer.setRunner(new FastRunCreator<StatusBean>(100, true));
		consumer.start();

		// Bung ten things on there.
		for (int i = 0; i < 10; i++) {
			StatusBean bean = new StatusBean();
			bean.setName("Submission"+i);
			bean.setStatus(Status.SUBMITTED);
			bean.setHostName(InetAddress.getLocalHost().getHostName());
			bean.setMessage("Hello World");
			bean.setUniqueId(UUID.randomUUID().toString());
			bean.setUserName(String.valueOf(i));
			submitter.submit(bean);
		}

		Thread.sleep(100);
		IPublisher<PauseBean> pauser = eservice.createPublisher(submitter.getUri(), IEventService.CMD_TOPIC);
		PauseBean pbean = new PauseBean();
		pbean.setQueueName(consumer.getSubmitQueueName());
		pauser.broadcast(pbean);
		
		// Now we are paused. Read the submission queue
		Thread.sleep(100);
		List<StatusBean> submitQ = consumer.getSubmissionQueue();
		assertEquals(9, submitQ.size());
	
		Thread.sleep(2000); // Wait for 0 to run and check again that nothing else is
		
		submitQ = consumer.getSubmissionQueue();
		assertEquals(9, submitQ.size()); // It really has paused has it?
		
		// Right then we will reorder it.
		consumer.cleanQueue(consumer.getSubmitQueueName());
		
		// Reverse sort
		Collections.sort(submitQ, new Comparator<StatusBean>() {
			@Override
			public int compare(StatusBean o1, StatusBean o2) {
				int y = Integer.valueOf(o1.getUserName());
				int x = Integer.valueOf(o2.getUserName());
				return (x < y) ? -1 : ((x == y) ? 0 : 1);
			}
		});
		
		// Start the consumer again
		pbean.setPause(false);
		pauser.broadcast(pbean);
		
		// Resubmit in new order 9-1
		for (StatusBean statusBean : submitQ) submitter.submit(statusBean);
		
		final Map<String, StatusBean> run = new LinkedHashMap<>(9); // Order important
		ISubscriber<EventListener> sub = eservice.createSubscriber(consumer.getUri(), consumer.getStatusTopicName());
		sub.addListener(new IBeanListener<StatusBean>() {
			@Override
			public void beanChangePerformed(BeanEvent<StatusBean> evt) {
				// Many events come through here but each scan is run in order
				StatusBean bean = evt.getBean();
				run.put(bean.getName(), bean);
			}
		});

		while(!consumer.getSubmissionQueue().isEmpty()) Thread.sleep(1000); // Wait for all to run
		
		Thread.sleep(200); // ensure last one is running or ran.
		
		List<StatusBean> ordered = new ArrayList<>(run.values());
		assertEquals(9, ordered.size());
		for (int i = 0; i < ordered.size(); i++) {
			int t = Integer.valueOf(ordered.get(i).getUserName());
			if ((9-i) != t) throw new Exception("The run order was not 9-1 after reordering!");
		}
    }


	@Test
	public void testAbortingAJobRemotely() throws Exception {

		consumer.setRunner(new DryRunCreator<StatusBean>());
		consumer.start();

		StatusBean bean = doSubmit();

		Thread.sleep(2000);
		
		IPublisher<StatusBean> terminator = eservice.createPublisher(submitter.getUri(), IEventService.STATUS_TOPIC);
        bean.setStatus(Status.REQUEST_TERMINATE);
        terminator.broadcast(bean);
        
        Thread.sleep(3000);
		checkTerminatedProcess(bean);
	}
	
	@Test
	public void testAbortingAJobRemotelyNoBeanClass() throws Exception {

		consumer.setRunner(new DryRunCreator<StatusBean>());
		consumer.start();

		StatusBean bean = doSubmit();

		Thread.sleep(2000);
		
		IPublisher<StatusBean> terminator = eservice.createPublisher(submitter.getUri(), IEventService.STATUS_TOPIC);
        bean.setStatus(Status.REQUEST_TERMINATE);
        terminator.broadcast(bean);
        
        Thread.sleep(3000);
		checkTerminatedProcess(bean);
	}

    
	private void checkTerminatedProcess(StatusBean bean) throws Exception {
		List<StatusBean> stati = consumer.getStatusSet();
		if (stati.size()!=1) throw new Exception("Unexpected status size in queue! Might not have status or have forgotten to clear at end of test!");
		
		StatusBean complete = stati.get(0);
		
       	if (complete.equals(bean)) {
       		throw new Exception("The bean from the status queue was the same as that submitted! It should have a different status. q="+complete+" submit="+bean);
       	}
        
       	if (complete.getStatus()!=Status.TERMINATED) {
       		throw new Exception("The bean in the queue should be terminated after a stop!"+complete);
       	}
       	if (complete.getPercentComplete()==100) {
       		throw new Exception("The percent complete should not be 100!"+complete);
       	}
	}
    
    @Test
    public void testHeartbeat() throws Exception {
    	
		consumer.setRunner(new DryRunCreator<StatusBean>());
		consumer.start();
		
		ISubscriber<IHeartbeatListener> subscriber = eservice.createSubscriber(consumer.getUri(), IEventService.HEARTBEAT_TOPIC);
		final List<HeartbeatBean> gotBack = new ArrayList<>(3);
		subscriber.addListener(new IHeartbeatListener() {
			@Override
			public void heartbeatPerformed(HeartbeatEvent evt) {
				gotBack.add(evt.getBean());
				System.out.println("The heart beated at "+((new SimpleDateFormat()).format(new Date(evt.getBean().getPublishTime()))));
			}
		});
		
		Thread.sleep(3000);
		if (gotBack.size()<1) throw new Exception("No hearbeat the paitent might be dead!");

		doSubmit();
		Thread.sleep(5000);
		consumer.stop();  // Should also stop heartbeat within 2s
		Thread.sleep(3000);
		
		final int sizeBeforeSleep = gotBack.size();
		if (sizeBeforeSleep<2) throw new Exception("No hearbeat the paitent might be dead!");
		
		Thread.sleep(4000); // Should beat again if not dead
		
		final int sizeAfterSleep = gotBack.size();
		if (sizeAfterSleep!=sizeBeforeSleep) {
			throw new Exception("The pulse continues to beat after death. Ahhhhhh! Is it a vampir? Do we need the garlic?!");
		}

		subscriber.disconnect();
   }

   private StatusBean doSubmit() throws Exception {
	   return doSubmit("Test");
   }
   private StatusBean doSubmit(String name) throws Exception {

		StatusBean bean = new StatusBean();
		bean.setName(name);
		return doSubmit(bean);
   }
   private StatusBean doSubmit(StatusBean bean) throws Exception {

		bean.setStatus(Status.SUBMITTED);
		bean.setHostName(InetAddress.getLocalHost().getHostName());
		bean.setMessage("Hello World");
		bean.setUniqueId(UUID.randomUUID().toString());

		submitter.submit(bean);
		
		return bean;
	}
   
    @Test
    public void testMultipleSubmissions() throws Exception {
    	
		consumer.setRunner(new DryRunCreator<StatusBean>(false));
		consumer.start();
		
		List<StatusBean> submissions = new ArrayList<StatusBean>(10);
		for (int i = 0; i < 10; i++) {
			submissions.add(doSubmit("Test "+i));
			System.out.println("Submitted: Test "+i);
			Thread.sleep(10); // Guarantee that submission time cannot be same.
		}
		 	
		Thread.sleep(14000); // 10000 to do the loop, 4000 for luck
		
		checkStatus(submissions);
		
    }
    
    private void checkStatus(List<StatusBean> submissions) throws Exception {
    	
    	List<StatusBean> stati = consumer.getStatusSet();
		if (stati.size()!=10) throw new Exception("Unexpected status size in queue! Should be 10 size is "+stati.size());
		
		for (int i = 0; i < 10; i++) {
			
			StatusBean complete = stati.get(i);
			if (!complete.getName().equals("Test "+(9-i))) {
				throw new Exception("Unexpected run order detected! bean is named "+complete.getName()+" and should be 'Test "+(9-i)+"'");
			}
			
			StatusBean bean     = submissions.get(i);
	       	if (complete.equals(bean)) {
	       		throw new Exception("The bean from the status queue was the same as that submitted! It should have a different status. q="+complete+" submit="+bean);
	       	}
	        
	       	if (complete.getStatus()!=Status.COMPLETE) {
	       		throw new Exception("The bean in the queue is not complete!"+complete);
	       	}
	       	if (complete.getPercentComplete()<100) {
	       		throw new Exception("The percent complete is less than 100!"+complete);
	       	}
		}		
	}

	@Test
    public void testMultipleSubmissionsUsingThreads() throws Exception {
		
		consumer.setRunner(new DryRunCreator<StatusBean>(false));
		consumer.start();
		
		final List<StatusBean> submissions = new ArrayList<StatusBean>(10);
		for (int i = 0; i < 10; i++) {
			final int finalI = i;
			final Thread thread = new Thread(new Runnable() {
				public void run () {
					try {
						submissions.add(doSubmit("Test "+finalI));
						System.out.println("Submitted: Thread Test "+finalI);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			thread.setName("Thread "+i);
			thread.setDaemon(true);
			thread.start();
			
			Thread.sleep(100);
		}
		 	
		Thread.sleep(14000); // 10000 to do the loop, 4000 for luck
		
		checkStatus(submissions);

    }
    
    
}
