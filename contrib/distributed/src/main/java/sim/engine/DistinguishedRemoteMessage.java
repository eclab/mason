package sim.engine;

import java.io.Serializable;

import sim.engine.rmi.RemotePromise;

/**
	The message placed on the DSimState's queue to be processed on behalf of a Distinguished object.
	It contains the TAG of the message, its ARGUMENTS (which many be null), the Distinguished object
	to be processed, and the promised CALLBACK to be filled out.
**/

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