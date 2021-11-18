package sim.engine;

import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicInteger;
import sim.util.DRegistry;

/*
* Message used to exchange information between Remote Objects
* Has a specific atomic ID used for the register on DRegistry
*/

public class DistinguishedRemoteMessageObject implements DistinguishedRemoteMessage{
        
    /* Tag used to understand which method to use to fill the RemoteMessage */
    protected int message; 
    /* Optional argument that could be needed */
    protected Serializable arguments;
    /* Value requested, i.e., the answer to the request */
    protected Serializable value;
    /* id of the message used to register it on the DRegistry */
    protected String id;

    /*generate unique remote message id*/
    private static int idCounter = 0;
    private static final AtomicInteger threadSafeCounter = new AtomicInteger();
    private static int nextCounter()
    {
        if (DSimState.isMultiThreaded())
            return threadSafeCounter.getAndIncrement();
        else
            return idCounter++;
    }
    
    protected DistinguishedRemoteMessageObject(int message, Serializable arguments) {
        this.message = message;
        this.arguments = arguments;
        this.id = "" + DSimState.getPID() + nextCounter();
    }

    // set the value requested within the remoteMessage
    // i.e. fulfill the remoteMessage
    public void setValue(Serializable value) {
            this.value = value;
    }
    
    // take the value from the message and notify the DRegistry
    // to unregister it at the end of the step
    public Serializable getValue() throws RemoteException, NotBoundException{
        DRegistry.getInstance().lazyUnregisterObject(id);
        return value;
    }
    
    // required to get the remoteMessage from the DRegistry
    public String getId() throws RemoteException{
        return id;
    }
    
    // check if the message is been fulfilled
    public boolean isReady() throws RemoteException{
        return value != null;
    }
}