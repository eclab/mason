/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import java.util.*;
import sim.portrayal.*;
import sim.portrayal.simple.*;
import java.awt.*;

public class Transformer extends Filter
    {
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
    	{
    	return new ShapePortrayal2D(ShapePortrayal2D.POLY_POINTER_RIGHT, Color.GRAY, Color.BLACK, 1.0, scale);
    	}

    private static final long serialVersionUID = 1;

    CountableResource typicalIn;
    double ratioIn;
    double ratioOut;
        
    public Transformer(SimState state, CountableResource typicalOut, CountableResource typicalIn, double ratioIn, double ratioOut)
        {
        // typical being our _amount CountableResource
        super(state, typicalOut);
        this.typicalIn = (CountableResource)(typicalIn.duplicate());
        this.ratioIn = ratioIn;
        this.ratioOut = ratioOut;
        _amount = typical.duplicate();
        }

    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {       
    	if (getRefusesOffers()) { return false; }
        if (!typical.isSameType(amount)) throwUnequalTypeException(amount);

        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (!(atLeast >= 0 && atMost >= atLeast))
        	throwInvalidAtLeastAtMost(atLeast, atMost);

        if (amount instanceof Entity)
            {
            if (typical instanceof Entity)
                {
                _atLeast = 0;
                _atMost = 0;
                _amount = (Entity)(_amount.duplicate());
                return offerReceivers();
                }
            else
                {
                _atLeast = ratioOut / ratioIn;
                _atMost = ratioOut / ratioIn;
                ((CountableResource)_amount).setAmount(ratioOut / ratioIn);
                return offerReceivers();
                }
            }
        else
            {
            // FIXME:
            // This comes into problems when we get to exchangeRates with discrete objects..
            _atLeast = (atLeast / ratioIn) * ratioOut;
            _atMost = (atMost / ratioIn) * ratioOut;
            ((CountableResource)_amount).setAmount(_atMost);
            boolean retval = offerReceivers();
            if (retval)
                {
                ((CountableResource)amount).setAmount((((CountableResource)_amount).getAmount() * ratioIn) / ratioOut);                  // is this right?
                }
            return retval;
            }
        }
        
    public String toString()
        {
        return "Transformer@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + ", " + typicalIn + " -> " + typical + ", " + ratioIn + "/" + ratioOut + ")";
        }
    }
