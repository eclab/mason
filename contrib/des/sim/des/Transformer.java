/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import java.util.*;

public class Transformer extends Provider implements Receiver
    {
    private static final long serialVersionUID = 1;

    Resource typicalIn;
    Resource output;
    double atLeastOut;
    double atMostOut;
    double ratioIn;
    double ratioOut;
        
    public Resource getTypicalReceived() { return typical; }
	public boolean hideTypicalReceived() { return true; }

    public Transformer(SimState state, Resource typicalOut, Resource typicalIn, double ratioIn, double ratioOut)
        {
        // typical being our output CountableResource
        super(state, typicalOut);
        this.typicalIn = typicalIn.duplicate();
        this.ratioIn = ratioIn;
        this.ratioOut = ratioOut;
        output = typical.duplicate();
        }

    /** Returns false always and does nothing: Transformer is push-only. */
    public boolean provide(Receiver receiver)
        {
        return false;
        }

    protected boolean offerReceiver(Receiver receiver, double atMost)
        {
        return receiver.accept(this, output, Math.min(atLeastOut, atMost), Math.min(atMostOut, atMost));
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
                atLeastOut = 0;
                atMostOut = 0;
                output = (Entity)(output.duplicate());
                return offerReceivers();
                }
            else
                {
                atLeastOut = ratioOut / ratioIn;
                atMostOut = ratioOut / ratioIn;
                ((CountableResource)output).setAmount(ratioOut / ratioIn);
                return offerReceivers();
                }
            }
        else
            {
            // FIXME:
            // This comes into problems when we get to exchangeRates with discrete objects..
            atLeastOut = (atLeast / ratioIn) * ratioOut;
            atMostOut = (atMost / ratioIn) * ratioOut;
            ((CountableResource)output).setAmount(atMostOut);
            boolean retval = offerReceivers();
            if (retval)
                {
                ((CountableResource)amount).setAmount((((CountableResource)output).getAmount() * ratioIn) / ratioOut);                  // is this right?
                }
            return retval;
            }
        }
        
    public String toString()
        {
        return "Transformer@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + ", " + typicalIn + " -> " + typical + ", " + ratioIn + "/" + ratioOut + ")";
        }

    public void step(SimState state)
        {
        // do nothing
        }
        
    boolean refusesOffers = false;
	public void setRefusesOffers(boolean value) { refusesOffers = value; }
    public boolean getRefusesOffers() { return refusesOffers; }
    }
