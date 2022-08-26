package sim.util;

import java.io.Serializable;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.HashMap;

import sim.engine.DObject;
import sim.engine.Promise;

public class DRegistry_NEW {

        
    HashMap<String, Remote> registered_objects = new HashMap<>(); //id, proxy object?

    //should this be in DSimState instead?
    ArrayList queue = new ArrayList(); //what object should this be?  For resolving requests on the above?
        
    void registerObject(int id, DObject obj){
                
        //1) create proxy obj
        //2) delete old proxy obj if exists
        //3) add to registry
        }
        
    boolean deregisterObject(int id) {
                
        //remove from registered_objects
        //implement
        return false;
        }
        
    boolean migrateObject(int id) {
        //migrate to registry on different partitions?
        //implement
        return false;
        }
    
    //does this add to the request queue?  Should this be in DSimState?
    Promise sendMessageTo(int msg, Serializable s){
        //locally queue request
        
        //Later (when exactly?, where do we implement this)
        //look up obj on the registry
        //make rmi call on the object
        //build a promise from the result
        
        return null;
        }
        
    }
