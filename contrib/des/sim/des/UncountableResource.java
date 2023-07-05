/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

/** 
    An UncountableResource represents an infinitely divisible and mergeable version of CountableResource.
    Examples of UncountableResources might be oxygen, water, gasoline, electricity, heat, and so on.
*/

public class UncountableResource extends CountableResource
    {
    private static final long serialVersionUID = 1;

    void throwNonPositiveIntegerException(int amount)
        {
        throw new RuntimeException("Provided integer must be positive:" + amount);
        }

    void throwInvalidScalingException(double amount)
        {
        throw new RuntimeException("Scaling may not be negative or NaN.  Scaling provided was: " + amount);
        }

    void throwInvalidScalingException(double amount, double scaling)
        {
        throw new RuntimeException("Scaling may not result in NaN.  Current amount is " + amount + " and scaling is " + scaling);
        }

    void throwNotUncountableResourceException(Object resource)
        {
        throw new RuntimeException("This provided object is not an UncountableResource:\n\t" + this + "\n\t" + resource);
        }

    /** Returns true if this is an instance of UncountableResource */
    public boolean isUncountable() { return true; }

    /** Returns true if this is NOT an instance of UncountableResource */
    public boolean isCountable() { return false; }

    /** 
        Returns a new kind of Resource with a given name, and initial amount.
        The name is informal: It's legal for two different kinds of resources 
        to have the same name.  Resource types are distinguishe internally using
        unique integers. 
    */
    public UncountableResource(String name, double initialAmount)
        {
        super(name, initialAmount);
        }
        
    /** 
        Returns a new kind of Resource with a given name, and an initial amount of 0.
        The name is informal: It's legal for two different kinds of resources 
        to have the same name.  Resource types are distinguishe internally using
        unique integers. 
    */
    public UncountableResource(String name)
        {
        this(name, 0);
        }
        
    /** 
        Returns a Resource of the same type, name, and amount as the provided resource.
        This is essentially a clone of the resource.
        Note that this changes the amount of the given resource available in the world.
    */
    public UncountableResource(UncountableResource other)
        {
        if (!other.isUncountable())
            throwNotUncountableResourceException(other);
        this.name = other.name;
        this.type = other.type;
        setAmount(other.amount);
        }
                
    /** 
        Returns a Resource of the same type, name, and amount as the provided resource.
        This is essentially a clone of the resource.
        Note that this changes the amount of the given resource available in the world.
    */
    public UncountableResource(UncountableResource other, double amount)
        {
        super(other, amount);
        }
                
    public String toString()
        {
        return "UncountableResource[" + name + " (" + type + "), " + amount + "]";
        }

    /** 
        Sets the amount of the resource.
        Note that this changes the amount of the given resource available in the world.
    */
    public void setAmount(double val)
        {
        if (!isPositiveOrZeroNonNaN(val))                                     // negative or NaN
            throwInvalidNumberException(val);
                        
        amount = val;
        }
                
    /** 
        Divides the resource into TIMES pieces, including the original.  Each
        piece gets 1/TIMES of the original resource amount.  Returns an array
        of all the new pieces, including the (modified) original.
    */
    public UncountableResource[] divide(int times)
        {
        if (times <= 0)
            throwNonPositiveIntegerException(times);
                        
        UncountableResource[] resources = new UncountableResource[times];
        double div = amount / times;
        for(int i = 0; i < times - 1; i++)
            {
            resources[i] = new UncountableResource(this, div);
            }
                        
        amount = amount - (div * (times - 1));          // we get the remainder
        resources[resources.length - 1] = this;
        return resources;
        }
        
    /** 
        Divides the resource into two pieces: the original resource and
        a new resource.  Each resource gets half the amount of the original
        resource.  The new resource is returned.
    */
    public UncountableResource halve()
        {
        double div = amount * 0.5;
        UncountableResource resource = new UncountableResource(this, div);
        amount = amount - div;                                                  // we get the remainder
        return resource;
        }

    /**
       Multiplies the amount by the given value.
       Note that this changes the amount of the given resource available in the world.
    */
    public void scale(double value)
        {
        if (!isPositiveOrZeroNonNaN(value))                                   // negative or NaN
            throwInvalidScalingException(value);

        double val = amount * value;

        // NaN can happen if amount = infinity and value = 0 for example
        if (!isPositiveOrZeroNonNaN(val))                                     // negative or NaN
            throwInvalidScalingException(amount, value);

        amount = val;
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

        if (amount > max)
            {
            amount = max;
            }
        }

    public boolean increase(double val)
        {
        if (!isPositiveOrZeroNonNaN(val))                                     // negative or NaN
            throwInvalidNumberException(val);

        double total = amount + val;

        if (total < 0)                                          // FIXME: this can never happen?
            {
            return false;
            }       
        else
            {
            setAmount(total);                 // this does too many checks but whatever...
            return true;
            }
        }

    public boolean decrease(double val)
        {
        if (!isPositiveOrZeroNonNaN(val))                                     // negative or NaN
            throwInvalidNumberException(val);

        double total = amount - val;

        if (total < 0)
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
        Subtracts an exact amount from this resource and puts it in a new Resource,
        returning that.  If we cannot do this, then null is returned instead.
    */
    public UncountableResource reduce(double byExactly)
        {
        return reduce(byExactly, byExactly);
        }

    /** 
        Subtracts at least a certain amount and at most a certain amount 
        from this resource and puts it in a new Resource,
        returning that.  If we cannot do this, then null is returned instead.
    */
    public UncountableResource reduce(double atLeast, double atMost)
        {
        if (!isPositiveOrZeroNonNaN(atMost))                                  // negative or NaN
            {
            throwInvalidNumberException(atLeast);
            }
                
        if (!isPositiveOrZeroNonNaN(atLeast))                                 // negative or NaN
            {
            throwInvalidNumberException(atMost);
            }
                        
        if (amount < atLeast) return null;
        double sub = (amount <= atMost ? amount : atMost);
        amount -= sub;
        return new UncountableResource(this, sub);
        }

    /**
       Adds *at most* a certain amount of the other resource amount into this one.  
       The other resource amount is reduced by that amount.
       A NullPointerException is thrown if the other is null.
       A RuntimeException is thrown if the other is a Resource of a different type.
    */
    public void add(UncountableResource other, double atMostThisMuch)
        {
        if (!isPositiveOrZeroNonNaN(atMostThisMuch))                                  // negative or NaN
            throwInvalidNumberException(atMostThisMuch);
                
        if (other == null)
            throwNullPointerException();
        if (other.type != type) 
            throwUnequalTypeException(other);
                        
        if (atMostThisMuch > other.amount)
            atMostThisMuch = other.amount;
        this.amount += atMostThisMuch;
        other.amount -= atMostThisMuch;
        }

    /** Makes an exact copy of this resource */
    public UncountableResource duplicate()
        {
        return new UncountableResource(this);
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
        if (!(other instanceof UncountableResource)) return false;
        UncountableResource c = (UncountableResource) other;
        if (c.type != type) return false;                       
        return (c.amount == amount);
        }

    }
