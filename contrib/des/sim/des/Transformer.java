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
    Resource typicalIn;
    Resource output;
    double atLeastOut;
    double atMostOut;
    double ratioIn;
    double ratioOut;
        
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

    protected boolean offerReceiver(Receiver receiver)
        {
        return receiver.accept(this, output, atLeastOut, atMostOut);
        }

    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {       
        if (!typical.isSameType(amount)) throwUnequalTypeException(amount);

        if (isOffering()) throwCyclicOffers();  // cycle
        
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
        
    public String getName()
        {
        return "Transformer(" + typicalIn + " -> " + typical + ", " + ratioIn + "/" + ratioOut + ")";
        }

	public void step(SimState state)
		{
		// do nothing
		}
    }
