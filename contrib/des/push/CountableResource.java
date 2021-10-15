/** 
	A CountableResource is an entity which can be merged with resources of the same type.  Resources
	have AMOUNTS, which must be integers >= 0.  These amounts are stored as doubles
	for two bad reasons: first, to allow UnResource to cleanly subclass from
	Resource, and second, because double has a large range of integers than int
	does.
*/

public class CountableResource extends Resource
	{
	/** This is the largest possible integer that can be held in a double without skipping integers */
    public static final double MAXIMUM_INTEGER = 9.007199254740992E15;

	double amount;
	
	/** Returns true if this is an instance of UncountableResource */
	public boolean isUncountable() { return false; }

	/** Returns true if this is NOT an instance of UncountableResource */
	public boolean isCountable() { return false; }

	void throwNotCountableResourceException(Object resource)
		{
		throw new RuntimeException("This provided object is not a CountableResource:\n\t" + this + "\n\t" + resource);
		}

	void throwUnequalTypeException(Resource resource)
		{
		throw new RuntimeException("Two resources do not have the same type:\n\t" + this + "\n\t" + resource);
		}

	void throwNotResourceException(Object object)
		{
		throw new RuntimeException("Cannot compare to non-Resource object " + object);
		}

	void throwNullPointerException()
		{
		throw new NullPointerException();
		}

	void throwNonIntegerAmountException(double amount)
		{
		throw new RuntimeException("Resource requires integer amounts.  Provided amount is not an integer:  " + amount);
		}

	void throwInvalidNumberException(double amount)
		{
		throw new RuntimeException("Amounts may not be negative or NaN.  Amount provided was: " + amount);
		}

	public static boolean isPositiveNonNaN(double val)
		{
		return (val >= 0);
		}

	public static boolean isInteger(double val)
		{
		return (val <= MAXIMUM_INTEGER && val == (long) val);
		}

	/** 
		Returns a new kind of CountableResource with a given name, and initial amount.
		The name is informal: It's legal for two different kinds of countable resources 
		to have the same name.  CountableResource types are distinguishe internally using
		unique integers. 
	*/
	public CountableResource(String name, double initialAmount)
		{
		super(name);
		setAmount(initialAmount);
		}
	
	/** 
		Returns a CountableResource of the same type, name, and amount as the provided CountableResource.
		This is essentially a clone of the CountableResource.
		Note that this changes the amount of the given countable resource available in the world.
	*/
	public CountableResource(CountableResource other)
		{
		if (!other.isCountable())
			throwNotCountableResourceException(other);
		this.name = other.name;
		this.type = other.type;
		setAmount(other.amount);
		}

	/** 
		Returns a CountableResource of the same type, name, as the provided CountableResource, but a different amount.
		Note that this changes the amount of the given countable resource available in the world.
	*/
	public CountableResource(CountableResource other, double amount)
		{
		this(other);
		setAmount(amount);
		}
		
	public String toString()
		{
		return "CountableResource[" + name + " (" + type + "), " + amount + "]";
		}

	public Resource duplicate()
		{
		return new CountableResource(this);
		}

	public CountableResource duplicate0()
		{
		CountableResource ret = new CountableResource(this);
		ret.clear();
		return ret;
		}
	

	public double getAmount()
		{
		return amount;
		}

	/** 
		Sets the amount to zero.
		Note that this changes the total amount of the given countable resource available in the world.
	*/
	public void clear()
		{
		amount = 0;
		}

	/** 
		Sets the amount.
		Note that this changes the total amount of the given countable resource available in the world.
	*/
	public void setAmount(double val)
		{
		if (!isPositiveNonNaN(val))					// negative or NaN
			throwInvalidNumberException(val);

		if (!isInteger(val))
			throwNonIntegerAmountException(val);

		amount = val;
		}

	/**
		Increments the amount by 1.0.
		Note that this changes the amount of the given countable resource available in the world.
	*/
	public boolean increment()
		{
		if (amount + 1 > MAXIMUM_INTEGER) 
			return false;
		amount++;
		return true;
		}

	/**
		Decrements the amount by 1.0: if the value drops
		to beneath 0, it is set to 0 and FALSE is returned.  Else TRUE is returned.
		Note that this changes the amount of the given countable resource available in the world.
	*/
	public boolean decrement()
		{
		if (amount > 0)
			{
			amount--;
			return true;
			}
		else return false;
		}

	/**
		Increases the amount by the given value: if the value drops
		to beneath 0, it is set to 0 and FALSE is returned.  Else TRUE is returned.
		Note that this changes the amount of the given countable resource available in the world.
	*/
	public boolean increase(double val)
		{
		if (!isPositiveNonNaN(val))					// negative or NaN
			throwInvalidNumberException(val);

		if (!isInteger(val))
			throwNonIntegerAmountException(val);

		double total = amount + val;

		if (total < 0 || !isInteger(total)) 
			{
			return false;
			}	
		else
			{
			setAmount(val);			// this does too many checks but whatever...
			return true;
			}
		}
	
	/**
		Decreases the amount by the given value: if the value drops
		to beneath 0, it is set to 0 and FALSE is returned.  Else TRUE is returned.
		Note that this changes the amount of the given countable resource available in the world.
	*/
	public boolean decrease(double val)
		{
		return increase(0 - val);
		}

	/** 
		Subtracts an exact amount from this resource and puts it in a new CountableResource,
		returning that.  If we cannot do this, then null is returned instead.
	*/
	public CountableResource reduce(double byExactly)
		{
		return reduce(byExactly, byExactly);
		}

	/** 
		Subtracts at least a certain amount and at most a certain amount 
		from this resource and puts it in a new Resource,
		returning that.  If we cannot do this, then null is returned instead.
	*/
	public CountableResource reduce(double atLeast, double atMost)
		{
		if (!isPositiveNonNaN(atMost))					// negative or NaN
			throwInvalidNumberException(atLeast);
		
		if (!isPositiveNonNaN(atLeast))					// negative or NaN
			throwInvalidNumberException(atMost);
			
		if (!isInteger(atLeast))
			throwNonIntegerAmountException(atLeast);

		if (!isInteger(atMost))
			throwNonIntegerAmountException(atMost);

		if (amount < atLeast) return null;
		double sub = (amount >= atMost ? amount : atMost);
		amount -= sub;
		return new CountableResource(this, sub);
		}

	/**
		Adds the other CountableResource amount into this one.  The other CountableResource amount is set to 0.
		A NullPointerException is thrown if the other is null.
		A RuntimeException is thrown if the other is a CountableResource of a different type.
	*/
	public void add(CountableResource other)
		{
		if (other == null)
			throwNullPointerException();
		if (other.type != type) 
			throwUnequalTypeException(other);
			
		this.amount += other.amount;
		other.amount = 0;
		}

	/**
		Adds *at most* a certain amount of the other CountableResource amount into this one.  
		The other CountableResource amount is reduced by that amount.
		A NullPointerException is thrown if the other is null.
		A RuntimeException is thrown if the other is a CountableResource of a different type.
	*/
	public void add(CountableResource other, double atMostThisMuch)
		{
		if (other == null)
			throwNullPointerException();
			
		if (other.type != type) 
			throwUnequalTypeException(other);

		if (!isPositiveNonNaN(atMostThisMuch))					// negative or NaN
			throwInvalidNumberException(atMostThisMuch);
		
		if (!isInteger(atMostThisMuch))
			throwNonIntegerAmountException(atMostThisMuch);
			
		if (atMostThisMuch > other.amount)
			atMostThisMuch = other.amount;
		this.amount += atMostThisMuch;
		other.amount -= atMostThisMuch;
		}

	/**
		Adds the other CountableResource amounts into this one.  The other CountableResource amounts are set to 0.
		A NullPointerException is thrown if any of the other CountableResources, or the array, is null.
		A RuntimeException is thrown if any of the other CountableResources are of a different type.
	*/
	public void add(CountableResource[] other)
		{
		for(int i = 0; i < other.length; i++)
			{
			if (other[i] == null) throwNullPointerException();
			if (other[i].type != type) 
				throwUnequalTypeException(other[i]);
			add(other[i], other[i].amount);
			}
		}

	/**
		Returns true if this CountableResource amount is greater than to the other.
		A NullPointerException is thrown if the other is null.
		A RuntimeException is thrown if the other is a CountableResource of a different type.
	*/
	public boolean equals(Object other)
		{
		if (other == this) return false;		
		if (other == null) return false;
		if (!(other instanceof CountableResource)) return false;
		CountableResource c = (CountableResource) other;
		if (c.type != type) return false;			
		return (c.amount == amount);
		}


	/**
		Returns true if this CountableResource amount is greater than to the other.
		A NullPointerException is thrown if the other is null.
		A RuntimeException is thrown if the other is a CountableResource of a different type.
	*/
	public boolean greaterThan(CountableResource other)
		{
		if (other == this) return false;
		
		if (other == null) 
			throwNullPointerException();
		if (other.type != type) 
			throwUnequalTypeException(other);
			
		return (other.amount < amount);
		}

	/**
		Returns true if this CountableResource amount is greater than or equal to the other.
		A NullPointerException is thrown if the other is null.
		A RuntimeException is thrown if the other is a CountableResource of a different type.
	*/
	public boolean greaterThanOrEquals(CountableResource other)
		{
		if (other == this) return true;
		
		if (other == null) 
			throwNullPointerException();
		if (other.type != type) 
			throwUnequalTypeException(other);
			
		return (other.amount <= amount);
		}

	/**
		Returns true if this CountableResource amount is less than to the other.
		A NullPointerException is thrown if the other is null.
		A RuntimeException is thrown if the other is a CountableResource of a different type.
	*/
	public boolean lessThan(CountableResource other)
		{
		if (other == this) return false;
		
		if (other == null) 
			throwNullPointerException();
		if (other.type != type) 
			throwUnequalTypeException(other);
			
		return (other.amount > amount);
		}

	/**
		Returns true if this CountableResource amount is less than or equal to the other.
		A NullPointerException is thrown if the other is null.
		A RuntimeException is thrown if the other is a CountableResource of a different type.
	*/
	public boolean lessThanOrEquals(CountableResource other)
		{
		if (other == this) return true;
		
		if (other == null) 
			throwNullPointerException();
		if (other.type != type) 
			throwUnequalTypeException(other);
			
		return (other.amount >= amount);
		}
		
	public int compareTo(Object other)
		{
		if (other == this) return 0;
		else if (other == null) throwNullPointerException();
		else if (!(other instanceof CountableResource)) throwNotResourceException(other);
		else 
			{
			CountableResource o = (CountableResource) other;
			if (o.type != type) throwUnequalTypeException(o);
			else if (amount == o.amount) return 0;
			else if (amount < o.amount) return -1;
			else return 1;
			}
		return 0;		// never happens
		}
	}