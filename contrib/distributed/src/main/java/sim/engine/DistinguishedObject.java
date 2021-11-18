package sim.engine;

import java.rmi.RemoteException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.Serializable;

import sim.util.DRegistry;

/*
 * Wrapper of the Remote Object containing
 * the real object
 * the queue of remote message that has to be fulfilled
 * and the corresponding methods
*/

public class DistinguishedObject implements DistinguishedRemote {
	
	// real object within the field
	protected Distinguished object;

	// queue of remoteMessage
	private Queue<DistinguishedRemoteMessageObject> queue = new ConcurrentLinkedQueue<DistinguishedRemoteMessageObject>();

	public DistinguishedObject(Distinguished object) {
		this.object = object;
	}

	// read all the messages in the queue and fulfill them using the remoteMessage()
	// implemented by the modeler
	// used in DSimState in the preSchedule()
	protected void parseQueueMessage() throws RemoteException{
		while (!queue.isEmpty()) {
			DistinguishedRemoteMessageObject remoteMessage = queue.remove();
			remoteMessage.setValue(this.object.remoteMessage(remoteMessage.message, remoteMessage.arguments));
		}
	}

	// create a remoteMessage, put it in the queue, and register it on the DRegistry
	// returns the id of the message in order to retrieve it from the DRegistry
	public String remoteMessage(int message, Serializable arguments) throws RemoteException {
		DistinguishedRemoteMessageObject remoteMessage = new DistinguishedRemoteMessageObject(message, arguments);
        this.queue.add(remoteMessage);
        DRegistry.getInstance().registerObject(remoteMessage.getId(), remoteMessage);
		return remoteMessage.getId();
	}


}