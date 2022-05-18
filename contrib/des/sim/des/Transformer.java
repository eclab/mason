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
    	return new ShapePortrayal2D(ShapePortrayal2D.POLY_POINTER_RIGHT, 
    		getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
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
        setName("Transformer");
        }

    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {       
    	if (getRefusesOffers()) { return false; }
        if (!typicalIn.isSameType(amount)) throwUnequalTypeException(amount);

        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (!(atLeast >= 0 && atMost >= atLeast))
        	throwInvalidAtLeastAtMost(atLeast, atMost);

		double _atLeast = (atLeast / ratioIn) * ratioOut;
		double _atMost = (atMost / ratioIn) * ratioOut;
		CountableResource _amount = (CountableResource)(typical.duplicate());
		double oldAmount = amount.getAmount() / ratioIn * ratioOut;
		_amount.setAmount(oldAmount);
		boolean retval = offerReceivers(_amount, _atLeast, _atMost);
		if (retval)
			{
			// modify original
			((CountableResource)amount).setAmount((oldAmount - amount.getAmount()) / ratioOut * ratioIn);
			}
		return retval;
        }
        
    public String toString()
        {
        return "Transformer@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + ", " + typicalIn + " -> " + typical + ", " + ratioIn + "/" + ratioOut + ")";
        }
    }
