package sim.app.geo.masoncsc.util;
import java.util.*;
import java.math.*;

import ec.util.MersenneTwisterFast;

public class MTFUtilities
    {
    /** Produces a synchronized java.util.Random subclassed object which internally uses
        the provided MersenneTwisterFast.  BE VERY CAREFUL WITH THIS METHOD.
        Though the provided object is synchronized, you can of course still access
        the underlying MersenneTwisterFast in an unsynchronized fashion.  Furthermore,
        if you call this method multiple times on the same MersenneTwisterFast
        instance, all the Random subclass objects will be holding the same 
        MersenneTwisterFast instance internally, but won't be synchronized
        with regard to one another.  This method should only be used when you have
        a library (such as MASON or ECJ) which uses MersenneTwisterFast but must
        communicate with an external library which uses java.util.Random (ugh)
        and you know better than to create and use two separate random number
        generators (a bad idea). */
    public static Random convertToRandom(MersenneTwisterFast mtf)
        {
        return new MTFWrapper(mtf);
        }
        
    /** Shuffles the list, see Collections.shuffle(list, random) */
    public static <T> void shuffle(List<T> list, MersenneTwisterFast mtf)
        {
        int numObjs = list.size();
        T obj;
        int rand;
        
        for(int x=numObjs-1; x >= 1 ; x--)
            {
            rand = mtf.nextInt(x+1);
            obj = list.get(x);
            list.set(x, list.get(rand));
            list.set(rand, obj);
            }
        }
                
    /** See the constructor BigInteger(bitLength, certainty, random). */
    public static BigInteger makeBigInteger(int bitLength, int certainty, MersenneTwisterFast mtf)
        {
        return new BigInteger(bitLength, certainty, convertToRandom(mtf));
        }

    /** See the constructor BigInteger(numBits, random) */
    public static BigInteger makeBigInteger(int numBits, MersenneTwisterFast mtf)
        {
        return new BigInteger(numBits, convertToRandom(mtf));
        }
    
    private static double calcHammingDistance(ArrayList<Integer> list) {
    	double distance = 0;
    	for (int i = 0; i < list.size(); i++)
    		distance += Math.abs(i - list.get(i));
    	
    	return distance;
    }

	public static void main(String[] args) {
		int n = 10;
		int numTests = 5000;
		MersenneTwisterFast random = new MersenneTwisterFast();
		ArrayList<Integer> sortedList = new ArrayList<Integer>();
		for (int i = 0; i < n; i++)
			sortedList.add(i);
		
		ArrayList<Integer> list = new ArrayList<Integer>(sortedList);
		
//		for (int i = 0; i < numTests; i++) {
//			list = new ArrayList<Integer>(sortedList);
//			MTFUtilities.shuffle(list, random);
////			Collections.shuffle(list);
//			System.out.format("%.0f\n", MTFUtilities.calcHammingDistance(list));
//		}
		
		ArrayList<String> stringList = new ArrayList<String>();
		stringList.add("A");
		stringList.add("B");
		stringList.add("C");
		MTFUtilities.shuffle(stringList, random);
		System.out.println(stringList.toString());
		
		// Conclusion: shuffle works with different types of lists
	}
    
    }
        
class MTFWrapper extends Random
    {
    MersenneTwisterFast rng;
                        
    public MTFWrapper(MersenneTwisterFast r) { rng = r; }
    public synchronized void setSeed(long seed) { rng.setSeed(seed); }
    public synchronized int nextInt() {  return rng.nextInt(); }
    public synchronized int nextInt(int n) { return rng.nextInt(n); }
    public synchronized long nextLong() { return rng.nextLong(); }
    public synchronized boolean nextBoolean() { return rng.nextBoolean(); }
    public synchronized float nextFloat() { return rng.nextFloat(); }
    public synchronized double nextDouble() { return rng.nextDouble(); }
    public synchronized void nextBytes(byte[] bytes) { rng.nextBytes(bytes); }
    public synchronized double nextGaussian()  { return rng.nextGaussian(); }
    }
