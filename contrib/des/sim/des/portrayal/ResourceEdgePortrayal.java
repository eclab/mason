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

/**
   A subclass of SimpleEdgePortrayal2D which scales the edges appropriately to the 
   receint offers accepted between nodes.
**/

public class ResourceEdgePortrayal extends SimpleEdgePortrayal2D
    {
    private static final long serialVersionUID = 1;
    
    double scale = 1.0;
    
    public ResourceEdgePortrayal()
        {
        this(1.0, false);
        }

    public ResourceEdgePortrayal(boolean triangle)
        {
        this(1.0, triangle);
        }

    public ResourceEdgePortrayal(double scale)
        {
        this(scale, false);
        }

    public ResourceEdgePortrayal(double scale, boolean triangle)
        {
        super(Color.BLUE, Color.RED, Color.BLACK, new Font("SansSerif", Font.PLAIN, 10));
        setShape(triangle ? SimpleEdgePortrayal2D.SHAPE_TRIANGLE : SimpleEdgePortrayal2D.SHAPE_LINE_ROUND_ENDS);
        setAdjustsThickness(true);
        setScaling(SimpleEdgePortrayal2D.ALWAYS_SCALE);
        setLabelScaling(SimpleEdgePortrayal2D.SCALE_WHEN_SMALLER);
        this.scale = scale;
        }
    
    protected double getPositiveWeight(Object edge, EdgeDrawInfo2D info)
        {
        ResourceEdge e = (ResourceEdge)edge;
        Provider provider = (Provider)(e.getFrom());
        Receiver receiver = (Receiver)(e.getTo());
        if (provider.getState().schedule.getTime() == provider.getLastAcceptedOfferTime())
            {
            ArrayList<Resource> offers = provider.getLastAcceptedOffers();
            ArrayList<Receiver> receivers = provider.getLastAcceptedOfferReceivers();
            int loc = receivers.indexOf(receiver);
            if (loc >= 0)
                {
                return (offers.get(loc).getAmount()) * scale;
                }
            else 
                {
                return 0.0;
                }
            }
        else
            {
            return 0.0;
            }
        }
    }
        
        
