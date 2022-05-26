/*
  Copyright 2022 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/
        
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
import sim.engine.rmi.*;

/**
 REMOTESIMPLEPROPERTIES is a special subclass of SimpleProperties which
 allows property access of remote objects for purposes of model inspectors and charting. 
 */

public class RemoteSimpleProperties extends SimpleProperties
    {
    private static final long serialVersionUID = 1;

    RemoteProcessorRMI vp;
    
    public RemoteSimpleProperties(RemoteProcessorRMI vp) 
        {
        super();
        this.vp = vp;
        }
    
    //My guess is use p to make remote requests here?
    public Object getValue(int index) 
        {
        //remote request DSimState.getProperties().getValue(index) on the partition DSimState
        //the Object returned here is what is monitored ??
        //is this feasible?
        try 
            {
            //return vp.getProperties().getValue(index);
            return vp.getPropertyValue(index);            
            }
        catch (Exception e) 
            {
            System.out.println(e);
            System.exit(-1);
            }
        
        return null;
        }

    //My guess is use p to make remote requests here?
    public int numProperties() 
        {
        //remote request DSimState.getProperties().getValue(index) on the partition DSimState
        //the Object returned here is what is monitored ??
        //is this feasible?
        try 
            {
            //return vp.getProperties().getValue(index);
            //return vp.getPropRequester().numProperties();  //we don't want to bring Properties over, just the values!  Handle Properties on partition end
            return vp.getNumProperties();
            }
        catch (Exception e) 
            {
            System.out.println(e);
            System.exit(-1);
            }
        return 0;
        }    
    
    


    public String getDescription(int index)
        {
        try 
            {
            return vp.getPropertyDescription(index);
            } 
        catch (RemoteException e) 
            {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(-1);
            }
        
        return null;
        }

    public Object getDomain(int index)
        {
        try 
            {
            return vp.getPropertyDomain(index);
            } 
        catch (RemoteException e) 
            {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(-1);
            }
        return null;
        }

    public boolean isHidden(int index) 
        {
        try 
            {
            return vp.getPropertyIsHidden(index);
            } 
        catch (RemoteException e) 
            {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(-1);
            }
        
        return false;
        }
    
    public String getName(int index) 
        {
        try 
            {
            return vp.getPropertyName(index);
            } 
        catch (RemoteException e) 
            {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(-1);
            }
        
        return null;
        }

    
    public String toString() 
        {
        return "RemoteSimpleProperties";
        } 
    }
