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

	protected boolean offerReceiver(Receiver receiver)
		{
		return receiver.accept(this, output, atLeastOut, atMostOut);
		}

    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {       
		if (!typical.isSameType(amount)) throwUnequalTypeException(amount);

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
            	((CountableResource)amount).setAmount((((CountableResource)output).getAmount() * ratioIn) / ratioOut);			// is this right?
            	}
            return retval;
			}
        }
        
    public String getName()
        {
		return "Transformer(" + typicalIn + " -> " + typical + ", " + ratioIn + "/" + ratioOut + ")";
        }

    }