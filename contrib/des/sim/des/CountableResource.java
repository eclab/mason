/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

/** 
    A CountableResource is a Resource which can be merged with resources of the same type.  CountabeResources
    have AMOUNTS, which must be integers >= 0.  CountableResources are thus finitely divisible.
    You can also split a CountableResource into a group of smaller CountableResources, and can 
    compare their amounts against each other.
        
    <p>Examples of CountableResources might be: cars, bricks, workers, and so on.  Money is also
    a countableResource as it is not infinitely divisible: but we have a special subclass for
    Money so it prints out in a cute way.
        
    <p>These amounts are stored as doubles for two bad reasons: first, to allow 
    UncountableResource to cleanly subclass from Resource, and second, because double has a 
    larger range of integers than int does.
        
    <p>CountableResource has a subclass called UncountableResource, which represents infinitely
    divisible resources (like water or gasoline).  This subclass arrangement may seem strange at
    first, but it makes sense given that UncountableResources can do everything CountableResources
    can do, plus some extra things.
    
*/

public class CountableResource extends Resource implements sim.util.Valuable
    {
    private static final long serialVersionUID = 1;

    /** This is the largest possible integer that can be held in a double without skipping integers */
    public static final double MAXIMUM_INTEGER = 9.007199254740992E15;

    double amount;
        
    public double doubleValue() { return amount; }
    
    /** Returns true if this is an instance of UncountableResource */
    public boolean isUncountable() { return false; }

    /** Returns true if this is NOT an instance of UncountableResource */
    public boolean isCountable() { return true; }

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

    void throwUnorderedException(double min, double max)
        {
        throw new RuntimeException("Min must be <= max.  Min was " + min + " and max was " + max);
        }

    static boolean isPositiveOrZeroNonNaN(double val)
        {
        return (val >= 0);
        }

    static boolean isInteger(double val)
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
        Returns a new kind of CountableResource with a given name, and an initial amount of 0.
        The name is informal: It's legal for two different kinds of countable resources 
        to have the same name.  CountableResource types are distinguishe internally using
        unique integers. 
    */
    public CountableResource(String name)
        {
        this(name, 0);
        }
        
    /** This is solely for the benefit of UncountableResource(UncountableResource) */
    protected CountableResource()
        {
        super();
        }
        
    /** 
        Returns a CountableResource of the same type, name, and amount as the provided CountableResource.
        This is essentially a clone of the CountableResource.
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
    */
    public CountableResource(CountableResource other, double amount)
        {
        this(other);
        setAmount(amount);
        }
                
    public String toString()
        {
        return "CountableResource[" + name + " (" + type + "), " + (long)amount + "]";
        }

    /** 
        Returns a CountableResource of the same type, name, and amount as the provided CountableResource.
        This is essentially a clone of the CountableResource.
    */
    public Resource duplicate()
        {
        return new CountableResource(this);
        }

    public double getAmount()
        {
        return amount;
        }

    /** 
        Sets the amount to zero.
    */
    public void clear()
        {
        amount = 0;
        }

    /** 
        Sets the amount.
    */
    public void setAmount(double val)
        {
        if (!isPositiveOrZeroNonNaN(val))                                     // negative or NaN
            throwInvalidNumberException(val);

        if (!isInteger(val))
            throwNonIntegerAmountException(val);

        amount = val;
        }

    /**
       Increments the amount by 1.0.
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
       Bounds the resource to be no more than max and no less than min.  
       It must be the case that max >= min >= 0.
    */
    public void bound(double min, double max)
        {
        if (!isPositiveOrZeroNonNaN(min))                                     // negative or NaN
            throwInvalidNumberException(min);
        if (!isPositiveOrZeroNonNaN(max))                                     // negative or NaN
            throwInvalidNumberException(max);
        if (min > max)
            throwUnorderedException(min, max);
        if (!isInteger(min))
            throwNonIntegerAmountException(min);
        if (!isInteger(max))
            throwNonIntegerAmountException(max);

        if (amount < min)
            {
            amount = min;
            }
                        
        if (amount > max)
            {
            amount = max;
            }
        }

    /**
       Bounds the resource to be no more than max and no less than 0.
    */
    public void bound(double max)
        {
        if (!isPositiveOrZeroNonNaN(max))                                     // negative or NaN
            throwInvalidNumberException(max);
        if (!isInteger(max))
            throwNonIntegerAmountException(max);

        if (amount > max)
            {
            amount = max;
            }
        }
        

    /**
       Increases the amount by the given value: if the value exceeds MAXIMUM_INTEGER, 
       nothing happens and FALSE is returned.  Else TRUE is returned.
    */
    public boolean increase(double val)
        {
        if (!isPositiveOrZeroNonNaN(val))                                     // negative or NaN
            throwInvalidNumberException(val);

        if (!isInteger(val))
            throwNonIntegerAmountException(val);

        double total = amount + val;

        if (total < 0 || !isInteger(total))             // FIXME: is it possible for total < 0 ?
            {
            return false;
            }       
        else
            {
            setAmount(total);                 // this does too many checks but whatever...
            return true;
            }
        }
        
    /**
       Decreases the amount by the given value: if the value drops
       to beneath 0, nothing happens and FALSE is returned.  Else TRUE is returned.
    */
    public boolean decrease(double val)
        {
        if (!isPositiveOrZeroNonNaN(val))                                     // negative or NaN
            throwInvalidNumberException(val);

        if (!isInteger(val))
            throwNonIntegerAmountException(val);

        double total = amount - val;

        if (total < 0 || !isInteger(total)) 
            {
            return false;
            }       
        else
            {
            setAmount(total);                 // this does too many checks but whatever...
            return true;
            }
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
        if (!isPositiveOrZeroNonNaN(atMost))                                  // negative or NaN
            throwInvalidNumberException(atLeast);
                
        if (!isPositiveOrZeroNonNaN(atLeast))                                 // negative or NaN
            throwInvalidNumberException(atMost);
                        
        if (!isInteger(atLeast))
            throwNonIntegerAmountException(atLeast);

        if (!isInteger(atMost))
            throwNonIntegerAmountException(atMost);

        if (amount < atLeast) return null;
        double sub = (amount <= atMost ? amount : atMost);
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

        if (!isPositiveOrZeroNonNaN(atMostThisMuch))                                  // negative or NaN
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
       Does comparison by value, as opposed to equals(...) which does comparison by pointer.
       Returns true if this CountableResource amount is greater than to the other.
       A NullPointerException is thrown if the other is null.
       A RuntimeException is thrown if the other is a CountableResource of a different type.
    */
    public boolean equalTo(CountableResource other)
        {
        if (other == this) return true;                
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
    public boolean greaterThanOrEqualTo(CountableResource other)
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
    public boolean lessThanOrEqualTo(CountableResource other)
        {
        if (other == this) return true;
                
        if (other == null) 
            throwNullPointerException();
        if (other.type != type) 
            throwUnequalTypeException(other);
                        
        return (other.amount >= amount);
        }
                
    /**
       Note: this class has a natural ordering that is inconsistent with equals, because equals(other)
       tests by pointer rather than value.  This is not a contract requirement of compareTo but it is unusual. 
    */
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
        return 0;               // never happens
        }
    }
