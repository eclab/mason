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
    
    HashMap<Integer, Paint> paintMap;

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
    
    public void putPaint(int resourceType, Paint paint)
        {
        if (paintMap == null) paintMap = new HashMap<>();
        paintMap.put(resourceType, paint);
        }
    
    public Paint getPaint(int resourceType)
        {
        if (paintMap == null) return fromPaint;
        Paint paint = paintMap.get(resourceType);
        if (paint == null) return fromPaint;
        else return paint;
        }

    protected double getPositiveWeight(Object edge, EdgeDrawInfo2D info)
        {
        ResourceEdge e = (ResourceEdge)edge;
        Provider provider = e.getProvider();
        Receiver receiver = e.getReceiver();
        if (provider == null || receiver == null) return 0.0;
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
    
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        if (!(object instanceof ResourceEdge))
            throw new RuntimeException("Expected this to be a ResourceEdge: " + object);
        ResourceEdge edge = (ResourceEdge)object;

        int resource = edge.getProvider().getTypicalProvided().getType();

        Paint f = fromPaint;
        fromPaint = getPaint(resource);
        super.draw(object, graphics, info);
        fromPaint = f;          // restore
        }

    public String getName(LocationWrapper wrapper)
        {
        if (!(wrapper.getLocation() instanceof ResourceEdge))
            throw new RuntimeException("Expected this to be a ResourceEdge: " + wrapper.getLocation());

        ResourceEdge edge = (ResourceEdge)(wrapper.getLocation());
        return "" + edge.getProvider().getTypicalProvided().getName() + ": " + edge.getProvider().getName() + " --> " + edge.getReceiver().getName();
        }
    }
        
        
