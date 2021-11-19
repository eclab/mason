/** 
    A resource sink. 
*/

import sim.engine.*;
import java.util.*;


public class Sink implements Receiver
    {
    SimState state;
    Resource typical;
        
    void throwUnequalTypeException(Resource resource)
        {
        throw new RuntimeException("Expected resource type " + this.typical.getName() + "(" + this.typical.getType() + ")" +
            " but got resource type " + resource.getName() + "(" + resource.getType() + ")" );
        }

    public Sink(SimState state, Resource typical)
        {
        this.state = state;
        this.typical = typical;
        }

    public boolean accept(Provider provider, Resource resource, double atLeast, double atMost)
        {
        if (!typical.isSameType(resource)) throwUnequalTypeException(resource);
        
        if (resource instanceof CountableResource) 
            {
            ((CountableResource) resource).reduce(atMost);
            return true;
            }
        else
            {
            return true;
            }
        }

    public void step(SimState state)
        {
        // do nothing
        }

    public String getName()
        {
        return "Sink(" + typical.getName() + ")";
        }               
    }
