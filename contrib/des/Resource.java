/** 
	A resource which can be merged with resources of the same type.  Resources
	have AMOUNTS, which must be integers >= 0.  These amounts are stored as doubles
	for two bad reasons: first, to allow UnResource to cleanly subclass from
	Resource, and second, because double has a large range of integers than int
	does.
*/

public class Resource 
	{
	static int lastType = -1;
	
	double amount;
	String name;
	int type;
    public static final double MAXIMUM_INTEGER = 9.007199254740992E15;

	protected int getNextType() { return ++lastType; }
	
	public boolean isUncountable() { return (this instanceof UncountableResource); }
	public boolean isCountable() { return !(isUncountable()); }

	void throwUncountableResourceException(Resource resource)
		{
		throw new RuntimeException("This resource is countable, but the other is uncountable:\n\t" + this + "\n\t" + resource);
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

	public static boolean isInteger(double val)
		{
		return (val <= MAXIMUM_INTEGER && val == (long) val);
		}

	/** 
		Returns a new kind of Resource with a given name, and initial amount.
		The name is informal: It's legal for two different kinds of resources 
		to have the same name.  Resource types are distinguishe internally using
		unique integers. 
	*/
	// This is a double rather than an int so we can have a larger integer space.
	// Do we need it?
	public Resource(String name, double initialAmount)
		{
		this.name = name;
		this.type = getNextType();
		setAmount(initialAmount);
		}
	
	/** 
		Returns a Resource of the same type, name, and amount as the provided resource.
		This is essentially a clone of the resource.
		Note that this changes the amount of the given resource available in the world.
	*/
	public Resource(Resource other)
		{
		if (other.isUncountable())
			throwUncountableResourceException(other);
		this.name = other.name;
		this.type = other.type;
		setAmount(other.amount);
		}

	/** 
		Returns a Resource of the same type, name, and amount as the provided resource.
		This is essentially a clone of the resource.
		Note that this changes the amount of the given resource available in the world.
	*/
	public Resource(Resource other, double amount)
		{
		this(other);
		setAmount(amount);
		}
		
	/**
		Prints the resource out in a pleasing manner. 
	*/
	public String toString()
		{
		return "Resource[" + name + " (" + type + "), " + amount + "]";
		}

	public double getAmount()
		{
		return amount;
		}

	/** 
		Sets the amount of the resource.
		Note that this changes the amount of the given resource available in the world.
	*/
	public void setAmount(double val)
		{
		if (!(val >= 0))					// negative or NaN
			throwInvalidNumberException(val);

		if (!isInteger(val))
			{
			throwNonIntegerAmountException(val);
			}

		amount = val;
		}

	/**
		Increments the amount by 1.0.
		Note that this changes the amount of the given resource available in the world.
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
		Note that this changes the amount of the given resource available in the world.
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
		Note that this changes the amount of the given resource available in the world.
	*/
	public boolean increase(double val)
		{
		if (!(val >= 0))					// negative or NaN
			throwInvalidNumberException(val);

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
		Note that this changes the amount of the given resource available in the world.
	*/
	public boolean decrease(double val)
		{
		return increase(0 - val);
		}

	/** 
		Subtracts an exact amount from this resource and puts it in a new Resource,
		returning that.  If we cannot do this, then null is returned instead.
	*/
	public Resource reduce(double byExactly)
		{
		return reduce(byExactly, byExactly);
		}

	/** 
		Subtracts at least a certain amount and at most a certain amount 
		from this resource and puts it in a new Resource,
		returning that.  If we cannot do this, then null is returned instead.
	*/
	public Resource reduce(double atLeast, double atMost)
		{
		if (!(atMost >= 0))					// negative or NaN
			{
			throwInvalidNumberException(atLeast);
			}
		
		if (!(atLeast >= 0))					// negative or NaN
			{
			throwInvalidNumberException(atMost);
			}
			
		if (!isInteger(atLeast))
			{
			throwNonIntegerAmountException(atLeast);
			}

		if (!isInteger(atMost))
			{
			throwNonIntegerAmountException(atMost);
			}

		if (amount < atLeast) return null;
		double sub = (amount >= atMost ? amount : atMost);
		amount -= sub;
		return new Resource(this, sub);
		}

	/**
		Adds the other resource amount into this one.  The other resource amount is set to 0.
		A NullPointerException is thrown if the other is null.
		A RuntimeException is thrown if the other is a Resource of a different type.
	*/
	public void add(Resource other)
		{
		if (other == null)
			throwNullPointerException();
		if (other.type != type) 
			throwUnequalTypeException(other);
			
		this.amount += other.amount;
		other.amount = 0;
		}

	/**
		Adds *at most* a certain amount of the other resource amount into this one.  
		The other resource amount is reduced by that amount.
		A NullPointerException is thrown if the other is null.
		A RuntimeException is thrown if the other is a Resource of a different type.
	*/
	public void add(Resource other, double atMostThisMuch)
		{
		if (other == null)
			throwNullPointerException();
		if (other.type != type) 
			throwUnequalTypeException(other);
		if (!isInteger(atMostThisMuch))
			{
			throwNonIntegerAmountException(atMostThisMuch);
			}
			
		if (atMostThisMuch > other.amount)
			atMostThisMuch = other.amount;
		this.amount += atMostThisMuch;
		other.amount -= atMostThisMuch;
		}

	/**
		Adds the other resource amounts into this one.  The other resource amounts are set to 0.
		A NullPointerException is thrown if any of the other resources, or the array, is null.
		A RuntimeException is thrown if any of the other resources are of a different type.
	*/
	public void add(Resource[] other)
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
		Returns true if this resource amount is greater than to the other.
		A NullPointerException is thrown if the other is null.
		A RuntimeException is thrown if the other is a Resource of a different type.
	*/
	public boolean greaterThan(Resource other)
		{
		if (other == this) return false;
		
		if (other == null) 
			throwNullPointerException();
		if (other.type != type) 
			throwUnequalTypeException(other);
			
		return (other.amount < amount);
		}

	/**
		Returns true if this resource amount is greater than or equal to the other.
		A NullPointerException is thrown if the other is null.
		A RuntimeException is thrown if the other is a Resource of a different type.
	*/
	public boolean greaterThanOrEquals(Resource other)
		{
		if (other == this) return false;
		
		if (other == null) 
			throwNullPointerException();
		if (other.type != type) 
			throwUnequalTypeException(other);
			
		return (other.amount <= amount);
		}

	/**
		Returns true if this resource amount is less than to the other.
		A NullPointerException is thrown if the other is null.
		A RuntimeException is thrown if the other is a Resource of a different type.
	*/
	public boolean lessThan(Resource other)
		{
		if (other == this) return false;
		
		if (other == null) 
			throwNullPointerException();
		if (other.type != type) 
			throwUnequalTypeException(other);
			
		return (other.amount > amount);
		}

	/**
		Returns true if this resource amount is less than or equal to the other.
		A NullPointerException is thrown if the other is null.
		A RuntimeException is thrown if the other is a Resource of a different type.
	*/
	public boolean lessThanOrEquals(Resource other)
		{
		if (other == this) return false;
		
		if (other == null) 
			throwNullPointerException();
		if (other.type != type) 
			throwUnequalTypeException(other);
			
		return (other.amount >= amount);
		}
		
	public Resource duplicate()
		{
		return new Resource(this);
		}

	/** 
		Returns true if the two objects are both Resources with the same type and amount.
	*/
	public boolean equals(Object other)
		{
		if (other == this) return true;
		if (other == null) return false;
		if (other instanceof Resource)
			{
			Resource resource = (Resource) other;
			return (resource.type == type);
			}
		else return false;
		}

		
	public int compareTo(Object other)
		{
		if (other == this) return 0;
		else if (other == null) throwNullPointerException();
		else if (!(other instanceof Resource)) throwNotResourceException(other);
		else 
			{
			Resource o = (Resource) other;
			if (o.type != type) throwUnequalTypeException(o);
			else if (amount == o.amount) return 0;
			else if (amount < o.amount) return -1;
			else return 1;
			}
		return 0;		// never happens
		}

	/** 
		Returns true if the two objects are both Resources with the same type.
	*/
	public boolean isSameType(Resource other)
		{
		if (other == this) return true;
		if (other == null) return false;
		if (other instanceof Resource)
			{
			Resource resource = (Resource) other;
			return (resource.type == type);
			}
		else return false;
		}
	
	/** 
		Exactly copies the resource TIMES times.  Returns an array of the new resources.
		Note that this changes the amount of the given resource available in the world.
	*/
	public Resource[] duplicate(int times)
		{
		Resource[] resources = new Resource[times];
		for(int i = 0; i < times; i++)
			{
			resources[i] = duplicate();
			}
		return resources;
		}
	
	/**
		Returns the assigned resource type.
	*/
	public int getType()
		{
		return type;
		}
		
	/**
		Returns the assigned resource name.
	*/
	public String getName()
		{
		return name;
		}
	}