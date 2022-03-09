package sim.engine;

import java.io.Serializable;

import sim.engine.rmi.RemotePromise;

/*
* Message used to exchange information between Remote Objects
* Has a specific atomic ID used for the register on DRegistry
*/

public class DistinguishedRemoteMessage 
{
    /* Tag used to understand which method to use to fill the RemoteMessage */
    int tag; 
    /* Optional argument that could be needed */
    Serializable arguments;
    Distinguished object;
    Promised callback;
    
    protected DistinguishedRemoteMessage(Distinguished object, int tag, Serializable arguments, Promised callback) 
        {
        this.tag = tag;
        this.arguments = arguments;
        this.object = object;
        this.callback = callback;
    	}
}