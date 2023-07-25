/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import java.util.*;
import sim.portrayal.simple.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import sim.portrayal.*;
import sim.display.*;
import javax.swing.*;
import sim.des.portrayal.*;

/**
   A Sink accepts all incoming offers of resources matching a given type, then throws them away.
*/

public class Sink extends DESPortrayal implements Receiver, ProvidesBarData, Parented
    {
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
        {
        return new ShapePortrayal2D(ShapePortrayal2D.POLY_POINTER_DOWN, 
            getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
        }


    public boolean hideDrawState() { return true; }
    public boolean getDrawState() { return false; }
    public boolean hideLabel() { return true; }

    private static final long serialVersionUID = 1;

    protected SimState state;

    public SimState getState()
        {
        return state;
        }

    Resource typicalReceived;
        
    protected double totalReceivedResource;
    public double getTotalReceivedResource() { return totalReceivedResource; }
    public double getReceiverResourceRate() { double time = state.schedule.getTime(); if (time <= 0) return 0; else return totalReceivedResource / time; }
        
    public Resource getTypicalReceived() { return typicalReceived; }
    //public boolean hideTypicalReceived() { return true; }

    void throwUnequalTypeException(Resource resource)
        {
        throw new RuntimeException("Expected resource type " + this.getTypicalReceived().getName() + "(" + this.getTypicalReceived().getType() + ")" +
            " but got resource type " + resource.getName() + "(" + resource.getType() + ")" );
        }

    /** Throws an exception indicating that atLeast and atMost are out of legal bounds. */
    protected void throwInvalidAtLeastAtMost(double atLeast, double atMost, Resource amount)
        {
        if (atMost <= 0)
            throw new RuntimeException("Requested resource may not be at most 0.");
        else if (atMost >= amount.getAmount())
            throw new RuntimeException("Requested resource " + atMost + " may not be larger than actual resource amount: " + amount);
        else
            throw new RuntimeException("Requested resource amounts are between " + atLeast + " and " + atMost + ", which is out of bounds.");
        }

    public Sink(SimState state, Resource typicalReceived)
        {
        this.state = state;
        this.typicalReceived = typicalReceived;
        setName("Sink");
        }

    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (getRefusesOffers()) { return false; }
        if (!getTypicalReceived().isSameType(amount)) throwUnequalTypeException(amount);
        
        if (!(atLeast >= 0 && atMost >= atLeast && atMost > 0 && atMost <= amount.getAmount()))
            throwInvalidAtLeastAtMost(atLeast, atMost, amount);

        if (amount instanceof CountableResource) 
            {
            totalReceivedResource += atMost;
            ((CountableResource) amount).reduce(atMost);
            return true;
            }
        else
            {
            totalReceivedResource += 1.0;
            return true;
            }
        }

    public String toString()
        {
        return "Sink@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : (getName() + ": ")) + getTypicalReceived().getName() + ")";
        }               

    public void step(SimState state)
        {
        // do nothing
        }

    String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean hideName() { return true; }
    Object parent;
    public Object getParent() { return parent; }
    public void setParent(Object parent) { this.parent = parent; }    

    public void reset(SimState state) { totalReceivedResource = 0; }
        
    boolean refusesOffers = false;
    public void setRefusesOffers(boolean value) { refusesOffers = value; }
    public boolean getRefusesOffers() { return refusesOffers; }

    public boolean hideDataBars() { return true; }
    public double[] getDataBars() 
        {
        return new double[0];
        }
    public boolean hideDataValues() { return true; }
    public String[] getDataValues() 
        {
        return new String[0];
        }
    public boolean hideDataLabels() { return true; }
    public String[] getDataLabels()
        {
        return new String[0];
        }
    }
