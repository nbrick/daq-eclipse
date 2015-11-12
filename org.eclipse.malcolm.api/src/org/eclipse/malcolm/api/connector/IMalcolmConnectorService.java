package org.eclipse.malcolm.api.connector;

import java.net.URI;

import org.eclipse.malcolm.api.IMalcolmDevice;
import org.eclipse.malcolm.api.MalcolmDeviceException;
import org.eclipse.malcolm.api.event.IMalcolmListener;

/**
 * A connector service is used internally to abstract the 
 * provision of a connection to malcolm. It provides the transport
 * mechanism (for instance Zeromq) and the serialization mechanism
 * (for instance Jackson or gson).
 * 
 * @author fcp94556
 *
 */
public interface IMalcolmConnectorService<T> {

	/**
	 * Method to start the connection. In the case of ZeroMQ this will open the
	 * socket and throw an exception if the connection cannot be made.
	 * 
	 * @param malcolmUri
	 * @throws MalcolmDeviceException
	 */
	void connect(URI malcolmUri) throws MalcolmDeviceException;

	/**
	 * Call when the connection should be closed. In the case of ZeroMQ this will call 
	 * socket.close() for instance.
	 * 
	 * @throws MalcolmDeviceException
	 */
	void disconnect() throws MalcolmDeviceException;
	

	/**
	 * Send the message and get one back, blocking, same as send(device, T, true)
	 * @param message
	 * @return
	 * @throws MalcolmDeviceException
	 */
	T send(IMalcolmDevice device, T message) throws MalcolmDeviceException;


	/**
	 * Subscribe to a message, adding the listener to the list of listeners for this message
	 * @param message
	 * @return
	 * @throws MalcolmDeviceException
	 */
	public void subscribe(IMalcolmDevice device, T msg, IMalcolmListener<T> listener) throws MalcolmDeviceException;


	/**
	 * Unsubscribe to a message, if listeners is null all listeners will be unsubscribed, otherwise just those specified.
	 * @param message
	 * @return
	 * @throws MalcolmDeviceException
	 */
	public T unsubscribe(IMalcolmDevice device, T msg, IMalcolmListener<T>... listeners) throws MalcolmDeviceException;

	
	/**
	 * Creates the connection to Malcolm
	 * 
	 * @param class1
	 * @return
	 */
	MessageGenerator<T> createConnection();

	/**
	 * Creates the connection to the device in the protocol as defined with this connector service, for instance ZeroMQ.
	 * SerializedDeviceConnection encapsulates the encoding of objects sent down the connection, for instance using Jackson
	 * 
	 * @return the connection to a device talking the appropriately serialized objects.
	 * 
	 * @throws MalcolmDeviceException
	 */
	MessageGenerator<T> createDeviceConnection(IMalcolmDevice device) throws MalcolmDeviceException;

	
	/**
	 * May be used to use the serializer to marshal any object using its serilization routine.
	 * This can be used for objects other than T
	 * @param event
	 * @return
	 */
	String marshal(Object anyObject) throws Exception;

	/**
	 * May be used to get the serializer to unmarshal any object using its serialization routine.
	 * This can be used for objects other than T
	 * @param event
	 * @return
	 */
	<U> U unmarshal(String anyObject, Class<U> beanClass) throws Exception;

	/**
	 * Create a connection factory for sending events. This method 
	 * may return null or a class implementing javax.jms.ConnectionFactory
	 * or javax.jms.QueueConnectionFactory
	 * 
	 * @param uri
	 * @return
	 */
	Object createConnectionFactory(URI uri);


}