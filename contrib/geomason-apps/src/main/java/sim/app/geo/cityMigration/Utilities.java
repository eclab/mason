package sim.app.geo.cityMigration;

import java.util.Arrays;

import ec.util.MersenneTwisterFast;

public class Utilities
{
	
	/**
	 * Randomly returns an index into the array in proportion to the size of values in 
	 * the given array probs. The values do not need to be normalized.
	 */
	static public int chooseStochastically(double [] probs, MersenneTwisterFast rand) {
		double [] cumulProbs = new double[probs.length];
		double total = 0;
		
		for (int i = 0; i < probs.length; i++) {
			total += probs[i];
			cumulProbs[i] = total;
		}
		
		int val = Arrays.binarySearch(cumulProbs, rand.nextDouble() * total);
		if (val < 0)
			return -(val + 1);		// Arrays.binarySearch(...) returns (-(insertion point) - 1) if the key isn't found
		
		return val;
	}

	static public void testChooseStochastically(double [] probs, int numTests) {
		double [] counts = new double[probs.length];
		for (int i = 0; i < numTests; i++) 
			counts[chooseStochastically(probs, new MersenneTwisterFast(System.currentTimeMillis()))]++;

		System.out.format("probs:       ");
		for (int i = 0; i < probs.length; i++)
			System.out.format("%.2f ", probs[i]);
		System.out.println();
		
		System.out.format("rel. counts: ");
		for (int i = 0; i < probs.length; i++)
			System.out.format("%.2f ", counts[i] / numTests);
		System.out.println();
	}
	
	static public void testChooseStochastically() {
		double [] probs = { 0.5, 0.5 };

		testChooseStochastically(probs, 1000);
		testChooseStochastically(new double[] {0.25, 0.25, 0.5}, 1000);
		testChooseStochastically(new double[] {25, 25, 50}, 1000);
	}
}
