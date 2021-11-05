package sim.util;

public abstract class Number3D extends NumberND
    {
    public abstract Number3D add(Int3D other);
    public abstract Number3D subtract(Int3D other);
    public abstract Double3D add(Double3D other);
    public abstract Double3D subtract(Double3D other);
    public abstract Double3D add(double x, double y, double z);

    public Number3D add(Number3D other)
        {
        if (other instanceof Int3D)
            {
            return add((Int3D) other);
            }
        else if (other instanceof Double3D)
            {
            return add((Double3D) other);
            }
        else    // uh...
            {
            return add(other.getVal(0), other.getVal(1), other.getVal(2));
            }
        }

    public Number3D subtract(Number3D other)
        {
        if (other instanceof Int3D)
            {
            return subtract((Int3D) other);
            }
        else if (other instanceof Double3D)
            {
            return subtract((Double3D) other);
            }
        else    // uh...
            {
            return add(0 - other.getVal(0), 0 - other.getVal(1), 0 - other.getVal(2));
            }
        }
    }
