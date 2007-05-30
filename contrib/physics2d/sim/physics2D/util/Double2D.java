package sim.physics2D.util;

public final class Double2D implements java.io.Serializable
    {
    public final double x;
    public final double y;
    
    public Double2D() { x = 0.0; y = 0.0; }
    public Double2D(final double x, final double y) { this.x = x; this.y = y; }
    public final double getX() { return x; }
    public final double getY() { return y; }
    public String toString() { return "Double2D["+x+","+y+"]"; }
    public String toCoordinates() { return "(" + x + ", " + y + ")"; }
    
    ////////////////////////////////////////////////////
    // ADDED BY CHRSTIAN
    ////////////////////////////////////////////////////
    /** Add two Double2D's using vector addition */
    public final Double2D add(Double2D other)
        {
        return new Double2D(x + other.x, y + other.y);
        }

    /** Subtracts Double2D "other" from current Double2D using 
     * vector subtraction */
    public final Double2D subtract(Double2D other)
        {
        return new Double2D(x - other.x, y - other.y);
        }
        
    /** 2D version of the cross product. Rotates current
     * Vector2D 90 degrees and takes the dot product of 
     * the result and Double2D "other" */
    public double perpDot(Double2D other)
        {
        // this is the equivalent of multiplying by a 2x2 rotation
        // matrix since cos(90) = 0 and sin(90) = 1
        Double2D rotated90 = new Double2D(-this.y, this.x);
        return rotated90.dotProduct(other);
        }

    /** Returns the vector length of the Double2D */
    public final double length()
        {
        return Math.sqrt(x * x + y * y);
        }
        
    /** Multiplies each element by scalar "val" */
    public final Double2D scalarMult(double val)
        {
        return new Double2D(x * val, y * val);
        }

    /** Divides each element by scalar "val" */
    public final Double2D scalarDiv(double val)
        {
        return new Double2D(x / val, y / val);
        }

    /** Scales the vector to length "dist" */
    public final Double2D setLength(double dist)
        {
        if(dist == 0)
            return new Double2D(0, 0);
        if(x == 0 && y == 0)
            return new Double2D(0, 0);

        double temp = length();
        return new Double2D(x * dist / temp, y * dist / temp);
        }

    /** Normalizes the vector (sets it length to 1) */
    public final Double2D normalize()
        {
        double len = length();
        return new Double2D(x / len, y / len);
        } 

    /** Takes the dot product this Double2D with another */
    public final double dotProduct(Double2D other)
        {
        return other.x * x + other.y * y;
        }

    /** Rotates the Double2D by theta radians */
    public final Double2D rotate(double theta)
        {
        // Do the equivalent of multiplying by a 2D rotation
        // matrix without the overhead of converting the Double2D into
        // a matrix
        double sinTheta = Math.sin(theta);
        double cosTheta = Math.cos(theta);
                
        return new Double2D(cosTheta * this.x + -sinTheta * this.y, sinTheta * this.x + cosTheta * this.y);
        }
    ////////////////////////////////////////////////////
    // END STUFF ADDED BY CHRISTIAN
    ////////////////////////////////////////////////////
    
    }

        
