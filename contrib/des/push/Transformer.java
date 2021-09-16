import sim.engine.*;
import java.util.*;

public class Transformer extends Provider implements Receiver
    {

	// Provider[] providers[];
    // Resource[] input; // our stash of ingredients
    // Resource[] output; // our stash of product
    // Pair<Resource[], Resource[]> recipe;

    Resource typicalIn;
    Double ratio; // ratio = output / input

	
	public Transformer(SimState state, Resource typical, Resource typicalIn, Double ratio)
		{
            //typical being our output resource
		    super(state, typical);
            this.typicalIn = typicalIn;
            this.ratio = ratio;
		}

    public void accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
            double amt = amount.getAmount();
            Resource output = typical.duplicate();



            double leastOut = ratio * atLeast;
            double mostOut = ratio * atMost;
            output.setAmount(mostOut);
            // offerReceivers(output, leastOut, mostOut);
            offerReceivers(output);
            // This comes into problems when we get to ratios with discrete objects..
            // TODO: Fix this..
            double diff = mostOut - output.getAmount();
            if(diff == 0)
                {
                    amount.reduce(diff/ratio);
                }


        }
    public String getName()
        {
            return "";
        }

    }