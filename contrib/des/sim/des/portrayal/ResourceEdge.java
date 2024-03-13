/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/
/*
  Copyright 2022 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des.portrayal;

import sim.field.network.*;
import sim.util.*;
import sim.engine.*;
import java.util.*;
import sim.des.*;
import javax.swing.*;
import java.awt.Color;
import java.awt.*;
import sim.field.network.*;
import sim.portrayal.network.*;
import sim.portrayal.*;
import java.util.*;

/**
   A subclass of Edge which allows the display and weighting of resources which
   travel from a provider to a receiver.
**/

public class ResourceEdge extends Edge
    {
    private static final long serialVersionUID = 1;
    
    /// FROM is the object where the edge starts at
    /// TO is the object where the edge ends at
    /// PROVIDER is the object which represents the provider of the edge, for purposes of
    /// things like measuring edge thickness
    /// RECEIVER is the object which represents the receiver of the edge, for purposes of
    /// things like measuring edge thickness
    
    Provider provider;
    Receiver receiver;

    public Provider getProvider() { return provider; }
    public Receiver getReceiver() { return receiver; }

    public ResourceEdge(Object from, Provider provider, Receiver receiver, Object to)
        {
        super(from, to, null);          // we have a null info because we grab it from the provider
        this.provider = provider;
        this.receiver = receiver;
        }

    public ResourceEdge(Provider provider, Receiver receiver)
        {
        this(provider, provider, receiver, receiver);                
        }
                
    public Object getInfo() 
        { 
        Provider provider = getProvider();
        Receiver receiver = getReceiver();
        double offerTime = provider.getLastAcceptedOfferTime();
        if (offerTime > Schedule.BEFORE_SIMULATION)
            {
            ArrayList<Resource> offers = provider.getLastAcceptedOffers();
            ArrayList<Receiver> receivers = provider.getLastAcceptedOfferReceivers();
            int loc = receivers.indexOf(receiver);
            if (loc >= 0)
                {
                if (offerTime == provider.getState().schedule.getTime())
                    return "-->" + offers.get(loc);
                else return offers.get(loc);
                }
            else 
                {
                return null;
                }
            }
        else
            {
            return null;            // or maybe an empty object?
            }
        }
    }
