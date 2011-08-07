package sim.field;
import sim.util.*;

public interface SparseField3D
    {
    /** Returns the width and height of the sparse field as a Double3D */
    public Double3D getDimensions();
        
    /** Returns the location of an object in the sparse field as a Double3D */
    public Double3D getObjectLocationAsDouble3D(Object obect);
    }

