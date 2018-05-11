package util;
import org.apache.commons.math3.random.RandomGenerator;

import ec.util.MersenneTwisterFast;


public class MersenneTwisterFastApache implements RandomGenerator
{
	private MersenneTwisterFast random;
	

	public MersenneTwisterFastApache(MersenneTwisterFast random) {
		super();
		this.random = random;
	}

	@Override
	public boolean nextBoolean() {
		return random.nextBoolean();
	}

	@Override
	public void nextBytes(byte[] bytes) {
		random.nextBytes(bytes);
	}

	@Override
	public double nextDouble() {
		return random.nextDouble();
	}

	@Override
	public float nextFloat() {
		return random.nextFloat();
	}

	@Override
	public double nextGaussian() {
		return random.nextGaussian();
	}

	@Override
	public int nextInt() {
		return random.nextInt();
	}

	@Override
	public int nextInt(int n) {
		return random.nextInt(n);
	}

	@Override
	public long nextLong() {
		return random.nextLong();
	}

	@Override
	public void setSeed(int seed) {
		random.setSeed(seed);
	}

	@Override
	public void setSeed(int[] array) {
		random.setSeed(array);
	}

	@Override
	public void setSeed(long seed) {
		random.setSeed(seed);
	}

}
