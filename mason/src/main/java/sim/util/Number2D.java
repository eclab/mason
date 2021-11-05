package sim.util;

public abstract class Number2D extends NumberND
    {
    public abstract Number2D add(Int2D other);
    public abstract Number2D subtract(Int2D other);
    public abstract Double2D add(Double2D other);
    public abstract Double2D subtract(Double2D other);
    public abstract Double2D add(double x, double y);
        
    public Number2D add(Number2D other)
        {
        if (other instanceof Int2D)
            {
            return add((Int2D) other);
            }
        else if (other instanceof Double2D)
            {
            return add((Double2D) other);
            }
        else    // uh...
            {
            return add(other.getVal(0), other.getVal(1));
            }
        }


    public Number2D subtract(Number2D other)
        {
        if (other instanceof Int2D)
            {
            return subtract((Int2D) other);
            }
        else if (other instanceof Double2D)
            {
            return subtract((Double2D) other);
            }
        else    // uh...
            {
            return add(0 - other.getVal(0), 0 - other.getVal(1));
            }
        }

    }
