public class Factory
	{
	public static final String[] NAMES = { "Beta", "Binomial", "Breit Wigner", "Burr 2", "Burr 3", "Burr 4", "Burr 5", "Burr 6", "Burr 7", "Burr 8", "Burr 9", "Burr 10", 
											"Burr 12", "Cauchy", "Chi-Square",  "Empirical", "Emperical (Walker)", "Erlang", "Exponential", "Exponential Power",
											"Gamma", "Geometric", "Hyperbolic", "Hypergeometric", 
											"Lambda", "Laplace", "Logarithmic", "Negative Binomial",  "Normal", "Poisson",  "Poisson (Slow)", "Pow Law", 
											"Student's T", "Triangular", "Uniform", "Von Mises", "Weibull", "Zeta", "Zipfian" };

	public static final String[] DISCRETE = { false, true, false, false, false, false, false, false, false, false, false, false, 
											false, false, false, false, true, false, false, false, false, false, true,
											false, false, false, true, false, true, true, false, false, false, 
											false, false, false, true, true };

	public static final int TYPE_BETA = 0;
	public static final int TYPE_BINOMIAL = 1;
	public static final int TYPE_BREIT_WIGNER = 2;
	public static final int TYPE_BURR_2 = 3;
	public static final int TYPE_BURR_3 = 4;
	public static final int TYPE_BURR_4 = 5;
	public static final int TYPE_BURR_5 = 6;
	public static final int TYPE_BURR_6 = 7;
	public static final int TYPE_BURR_7 = 8;
	public static final int TYPE_BURR_8 = 9;
	public static final int TYPE_BURR_9 = 10;
	public static final int TYPE_BURR_10 = 11;
	public static final int TYPE_BURR_12 = 12;
	public static final int TYPE_CAUCHY = 13;
	public static final int TYPE_CHI_SQUARE = 14;
	public static final int TYPE_EMPIRICAL = 15;
	public static final int TYPE_EMPIRICAL_WALKER = 16;
	public static final int TYPE_ERLANG = 17;
	public static final int TYPE_EXPONENTIAL = 18;
	public static final int TYPE_EXPONENTIAL_POWER = 19;
	public static final int TYPE_GAMMA = 20;
	public static final int TYPE_GEOMETRIC = 21;
	public static final int TYPE_HYPERBOLIC = 22;
	public static final int TYPE_HYPERGEOMETRIC = 23;
	public static final int TYPE_LAMBDA = 24;
	public static final int TYPE_LAPLACE = 25;
	public static final int TYPE_LOGARITHMIC = 26;
	public static final int TYPE_NEGATIVE_BINOMIAL = 27;
	public static final int TYPE_NORMAL = 28;
	public static final int TYPE_POISSON = 29;
	public static final int TYPE_POISSON_SLOW = 30;
	public static final int TYPE_POW_LAW = 31;
	public static final int TYPE_STUDENTS_T = 32;
	public static final int TYPE_TRIANGULAR = 33;
	public static final int TYPE_UNIFORM = 34;
	public static final int TYPE_VON_MISES = 35;
	public static final int TYPE_WEIBULL = 36;
	public static final int TYPE_ZETA = 37;
	public static final int TYPE_ZIPFIAN = 38;

	public static final double[] ARGUMENT_NAMES = { {"alpha", "beta"}, { "n", "p" }, ... };
	public static final double[] ARGUMENT_TYPES = { { Double.TYPE, Double.TYPE }, { Integer.TYPE, Double.TYPE }, ... };
		
	public AbstractDistribution build(int type, double arg1, double arg2, MersenneTwisterFast random) throws IllegalArgumentException
		{
		switch(type)
			{
			case TYPE_BETA:
				return new Beta(arg1, arg2, random);
			break;
			case TYPE_BINOMIAL:
				if (arg1 != (int)arg1) throw new IllegalArgumentException("Argument n must be an integer for Binomial Distribution");
				return new Binomial((int)arg1, arg2, random);
			break;
			....
			}
		}
	
	
	}