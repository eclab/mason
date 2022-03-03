package sim.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import sim.display.VisualizationProcessor;

//Basically, a reconstructed reconstructed SimpleProperties class
//so DSimState on servers generate a Properties object, then a remote serializable transport object gets info, the this info is used
// to construct this object
// I am currently unclear on how calling these methods in the Inspector work, so I may need to change this!


public class RemoteSimpleProperties extends SimpleProperties{

    private static final long serialVersionUID = 1;
    

    VisualizationProcessor vp;
    
    public RemoteSimpleProperties(VisualizationProcessor vp) {
    	
    	
        super();
        this.vp = vp;
        
        

  
    }
    

    
    //My guess is use p to make remote requests here?
    public Object getValue(int index) {
    	
    	//remote request DSimState.getProperties().getValue(index) on the partition DSimState
    	//the Object returned here is what is monitored ??
    	//is this feasible?
    	try {
    	
    	//return vp.getProperties().getValue(index);
        return vp.getPropertiesValue(index);    	
    	}
    	
    	catch (Exception e) {
    		System.out.println(e);
    		System.exit(-1);
    	}
    	
    	return null;
    	
    }

    //My guess is use p to make remote requests here?
    public int numProperties() {
    	
    	//remote request DSimState.getProperties().getValue(index) on the partition DSimState
    	//the Object returned here is what is monitored ??
    	//is this feasible?
    	try {
    	
    	//return vp.getProperties().getValue(index);
    	//return vp.getPropRequester().numProperties();  //we don't want to bring Properties over, just the values!  Handle Properties on partition end
    	return vp.getPropertiesNumProperties();
    		
    	}
    	
    	catch (Exception e) {
    		System.out.println(e);
    		System.exit(-1);
    	}
    	
    	return 0;
    	
    }    
    
    


    public String getDescription(int index)
    {
    	try {
			return vp.getPropertiesDescription(index);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
    		System.exit(-1);
	
		}
    	
    	return null;

    }

    public Object getDomain(int index)
    {
    	try {
			return vp.getPropertiesDomain(index);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
    		System.exit(-1);

		}
    	
    	return null;

    }

    public boolean isHidden(int index) {
    	
    	try {
			return vp.propertiesIsHidden(index);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
    		System.exit(-1);

		}
    	
    	return false;
   	
    }
    
    public String getName(int index) {
    	
    	try {
			return vp.getPropertiesName(index);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
    		System.exit(-1);

		}
    	
    	return null;
    }

    
    public String toString() {
    	return "RemoteSimpleProperties";
    }
    
 
}
