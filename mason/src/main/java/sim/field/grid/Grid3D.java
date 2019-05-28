/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.grid;
import sim.util.IntBag;
import java.util.Map;

/**
   Define basic neighborhood functions for 3D Grids.  The basic interface defines a width and a height
   (not all grids require a width and a height unless you're doing toroidal grids), and basic math for
   toroidal computation.
    
   <H3>Toroidal Computation</H3>
    
   <p>If you're using the Grid to define a toroidal (wrap-around) world, you can use the <b>tx</b>
   and <b>ty</b> and <b>tz</b> methods to simplify the math for you.  For example, to increment in the x direction,
   including wrap-around, you can do:  x = tx(x+1).
    
   <p>If you're sure that the values you'd pass into the toroidal functions would not wander off more than
   a grid dimension in either direction (height, width, length), you can use the slightly faster toroidal functions
   <b>stx</b> and <b>sty</b> and <b>stz</b> instead.  For example, to increment in the x direction,
   including wrap-around, you can do:  x = stx(x+1).  See the documentation on these functions for
   when they're appropriate to use.  Under most common situations, they're okay.

   <p>In HotSpot 1.4.1, stx, sty, and stz are inlined.  In Hotspot 1.3.1, they are not (they contain if-statements).

   <p>While this interface defines various methods common to many grids, you should endeavor not to call these grids casted into this interface: it's slow.  If you call the grids' methods directly by their class, their methods are almost certain to be inlined into your code, which is very fast.
*/

public interface Grid3D extends java.io.Serializable
    {
    /** Get the width */
    public int getWidth();
    
    /** Get the height */
    public int getHeight();
    
    /** Get the length */
    public int getLength();

    /** Toroidal x. The following definition:<br><br>
        final int length = this.length; <br>
        if (z >= 0) return (z % length); <br>
        final int length2 = (z % length) + length;<br>
        if (length2 &lt; length) return length2;<br>
        return 0;<br><br>
        ... produces the correct code and is 27 bytes, so it's likely to be inlined in Hotspot for 1.4.1.
    */
    public int tx(final int x);

    /** Toroidal y.  The following definition:<br><br>
        final int length = this.length;  <br>
        if (z >= 0) return (z % length);  <br>
        final int length2 = (z % length) + length; <br>
        if (length2 < length) return length2; <br>
        return 0; <br><br>
        ... produces the correct code and is 27 bytes, so it's likely to be inlined in Hotspot for 1.4.1.
    */
    public int ty(final int y);
    
    /** Toroidal z.  The following definition:<br><br>
        final int length = this.length; <br>
        if (z >= 0) return (z % length); <br>
        final int length2 = (z % length) + length;<br>
        if (length2 < length) return length2;<br>
        return 0;<br><br>
        ... produces the correct code and is 27 bytes, so it's likely to be inlined in Hotspot for 1.4.1.
    */
    public int tz(final int z);

    /** Simple [and fast] toroidal x.  Use this if the values you'd pass in never stray
        beyond (-width ... width * 2) not inclusive.  It's a bit faster than the full
        toroidal computation as it uses if statements rather than two modulos.
        The following definition:<br>
        { int width = this.width; if (x >= 0) { if (x < width) return x; return x - width; } return x + width; }<br><br>
        ...produces the shortest code (24 bytes) and is inlined in Hotspot for 1.4.1.  However
        in most cases removing the int width = this.width; is likely to be a little faster if most
        objects are usually within the toroidal region. */
    public int stx(final int x);
    
    /** Simple [and fast] toroidal y.  Use this if the values you'd pass in never stray
        beyond (-height ... height * 2) not inclusive.  It's a bit faster than the full
        toroidal computation as it uses if statements rather than two modulos.
        The following definition:<br>
        { int height = this.height; if (y >= 0) { if (y < height) return y ; return y - height; } return y + height; }<br><br>
        ...produces the shortest code (24 bytes) and is inlined in Hotspot for 1.4.1. However
        in most cases removing the int height = this.height; is likely to be a little faster if most
        objects are usually within the toroidal region. */
    public int sty(final int y);

    /** Simple [and fast] toroidal z.  Use this if the values you'd pass in never stray
        beyond (-length ... length * 2) not inclusive.  It's a bit faster than the full
        toroidal computation as it uses if statements rather than two modulos.
        The following definition:<br>
        { int length = this.length; if (z >= 0) { if (z < length) return z ; return z - length; } return z + length; }<br><br>
        ...produces the shortest code (24 bytes) and is inlined in Hotspot for 1.4.1. However
        in most cases removing the int length = this.length; is likely to be a little faster if most
        objects are usually within the toroidal region. */
    public int stz(final int z);

    /** Bounded Mode for neighborhood lookup.  Indicates that the Grid3D in question
        is being used in a way that assumes that it 
        has no valid locations outside of the rectangle starting at (0,0) and
        ending at (width-1, height-1) inclusive. */
    public static int BOUNDED = 0;

    /** Bounded Mode for neighborhood lookup.  Indicates that the Grid3D in question
        is being used in a way that assumes that any numerical location is a valid
        location.  Note that Grid3D subclasses based on arrays, such as DoubleGrid3D,
        IntGrid3D, ObjectGrid3D, and DenseGrid3D, <b>cannot be used</b> in an unbounded
        fashion. */
    public static int UNBOUNDED = 1;

    /** Bounded Mode for toroidal lookup.  Indicates that the Grid3D in question
        is being used in a way that assumes that it is bounded, but wrap-around: for
        example, (0,0) is located one away diagonally from (width-1, height-1).
    */
    public static int TOROIDAL = 2;

    /** Center measurement rule for raidal neighborhood lookup.  Indicates that
        radial lookup will include locations whose grid cell centers overlap with 
        the neighborhood region.  */
    public static int CENTER = 1024;

    /** "All" measurement rule for raidal neighborhood lookup.  Indicates that
        radial lookup will include locations whose grid cells are entirely within 
        the neighborhood region.  */
    public static int ALL = 1025;

    /** "Any" measurement rule for raidal neighborhood lookup.  Indicates that
        radial lookup will include locations whose grid cells have any overlap
        at all with the neighborhood region.  */
    public static int ANY = 1026;

    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y), abs(z-Z) ) <= dist.  This region forms a
     * cube 2*dist+1 cells across, centered at (X,Y,Z).  If dist==1, this
     * is equivalent to the twenty-six neighbors surrounding (X,Y,Z), plus (X,Y) itself.  
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p> This function may only run in two modes: toroidal or bounded.  Unbounded lookup is not permitted, and so
     * this function is deprecated: instead you should use the other version of this function which has more functionality.
     * If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0,0) to (width, height,length), 
     * that is, the width and height of the grid.   if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     *
     * <p>The origin -- that is, the (x,y,z) point at the center of the neighborhood -- is always included in the results.
     *
     * <p>This function is equivalent to: <tt>getNeighborsMaxDistance(x,y,dist,toroidal ? Grid3D.TOROIDAL : Grid3D.BOUNDED, true, xPos, yPos, zPos);</tt>
     * 
     * @deprecated
     */
    public void getNeighborsMaxDistance( final int x, final int z, final int y, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos, IntBag zPos);

    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y), abs(z-Z) ) <= dist.  This region forms a
     * cube 2*dist+1 cells across, centered at (X,Y,Z).  If dist==1, this
     * is equivalent to the twenty-six neighbors surrounding (X,Y,Z), plus (X,Y) itself.  
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>This function may be run in one of three modes: Grid3D.BOUNDED, Grid3D.UNBOUNDED, and GrideD.TOROIDAL.  If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0,0) to (width, height,length), 
     * that is, the width and height of the grid.  If "unbounded", then the neighbors are not so restricted.  Note that unbounded
     * neighborhood lookup only makes sense if your grid allows locations to actually <i>be</i> outside this box.  For example,
     * SparseGrid3D permits this but ObjectGrid3D and DoubleGrid3D and IntGrid3D and DenseGrid3D do not.  Finally if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     *
     * <p>You can also opt to include the origin -- that is, the (x,y,z) point at the center of the neighborhood -- in the neighborhood results.
     */
    public void getMooreLocations( final int x, final int y, int z, final int dist, int mode, boolean includeOrigin, IntBag xPos, IntBag yPos, IntBag zPos );


    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) + abs(z-Z) <= dist.  This region 
     * forms an <a href="http://images.google.com/images?q=octahedron">octohedron</a> 2*dist+1 cells from point
     * to opposite point inclusive, centered at (X,Y,Y).  If dist==1 this is
     * equivalent to the six neighbors  above, below, left, and right, front, and behind (X,Y,Z)),
     * plus (X,Y,Z) itself.
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p> This function may only run in two modes: toroidal or bounded.  Unbounded lookup is not permitted, and so
     * this function is deprecated: instead you should use the other version of this function which has more functionality.
     * If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0,0) to (width, height,length), 
     * that is, the width and height of the grid.   if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     *
     * <p>The origin -- that is, the (x,y,z) point at the center of the neighborhood -- is always included in the results.
     *
     * <p>This function is equivalent to: <tt>getNeighborsHamiltonianDistance(x,y,dist,toroidal ? Grid3D.TOROIDAL : Grid3D.BOUNDED, true, xPos, yPos, zPos);</tt>
     * 
     * @deprecated
     */
    public void getNeighborsHamiltonianDistance( final int x, final int y, int z, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos, IntBag zPos );

    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) + abs(z-Z) <= dist.  This region 
     * forms an <a href="http://images.google.com/images?q=octahedron">octohedron</a> 2*dist+1 cells from point
     * to opposite point inclusive, centered at (X,Y,Y).  If dist==1 this is
     * equivalent to the six neighbors  above, below, left, and right, front, and behind (X,Y,Z)),
     * plus (X,Y,Z) itself.
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     * null may be passed in for the various bags, though it is more efficient to pass in a 'scratch bag' for
     * each one.
     *
     * <p>This function may be run in one of three modes: Grid3D.BOUNDED, Grid3D.UNBOUNDED, and GrideD.TOROIDAL.  If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0,0) to (width, height,length), 
     * that is, the width and height of the grid.  If "unbounded", then the neighbors are not so restricted.  Note that unbounded
     * neighborhood lookup only makes sense if your grid allows locations to actually <i>be</i> outside this box.  For example,
     * SparseGrid3D permits this but ObjectGrid3D and DoubleGrid3D and IntGrid3D and DenseGrid3D do not.  Finally if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     *
     * <p>You can also opt to include the origin -- that is, the (x,y,z) point at the center of the neighborhood -- in the neighborhood results.
     */
    public void getVonNeumannLocations( final int x, final int y, int z, final int dist, int mode, boolean includeOrigin, IntBag xPos, IntBag yPos, IntBag zPos );
    
    /**
     * Gets all neighbors overlapping with a spherical region centered at (X,Y,Z) and with a radius of dist.
     * The measurement rule is Grid3D.ANY, meaning those cells which overlap at all with the region.  
     * The region is closed, meaning that that points which touch on the outer surface of the sphere will be 
     * considered members of the region.
     *
     * <p>Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     *
     * <p>This function may be run in one of three modes: Grid3D.BOUNDED, Grid3D.UNBOUNDED, and GrideD.TOROIDAL.  If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0,0) to (width, height,length), 
     * that is, the width and height of the grid.  If "unbounded", then the neighbors are not so restricted.  Note that unbounded
     * neighborhood lookup only makes sense if your grid allows locations to actually <i>be</i> outside this box.  For example,
     * SparseGrid3D permits this but ObjectGrid3D and DoubleGrid3D and IntGrid3D and DenseGrid3D do not.  Finally if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     *
     * <p>You can also opt to include the origin -- that is, the (x,y,z) point at the center of the neighborhood -- in the neighborhood results.
     */
    public void getRadialLocations( final int x, final int y, final int z, final double dist, int mode, boolean includeOrigin, IntBag xPos, IntBag yPos, IntBag zPos );


    /**
     * Gets all neighbors overlapping with a spherical region centered at (X,Y,Z) and with a radius of dist.
     * If measurementRule is Grid3D.CENTER, then the measurement rule will be those cells whose centers
     * overlap with the region.  If measurementRule is Grid3D.ALL, then the measurement rule will be those
     * cells which entirely overlap with the region.  If measurementrule is Grid3D.ANY, then the measurement
     * rule will be those cells which overlap at all with the region.  If closed is true, then the region will
     * be considered "closed", that is, that points which touch on the outer surface of the circle will be 
     * considered members of the region.  If closed is open, then the region will be considered "open", that is,
     * that points which touch on the outer surface of the circle will NOT be considered members of the region.
     *
     * <p>Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     *
     * <p>This function may be run in one of three modes: Grid3D.BOUNDED, Grid3D.UNBOUNDED, and GrideD.TOROIDAL.  If "bounded",
     * then the neighbors are restricted to be only those which lie within the box ranging from (0,0,0) to (width, height,length), 
     * that is, the width and height of the grid.  If "unbounded", then the neighbors are not so restricted.  Note that unbounded
     * neighborhood lookup only makes sense if your grid allows locations to actually <i>be</i> outside this box.  For example,
     * SparseGrid3D permits this but ObjectGrid3D and DoubleGrid3D and IntGrid3D and DenseGrid3D do not.  Finally if "toroidal",
     * then the environment is assumed to be toroidal, that is, wrap-around, and neighbors are computed in this fashion.  Toroidal
     * locations will not appear multiple times: specifically, if the neighborhood distance is so large that it wraps completely around
     * the width or height of the box, neighbors will not be counted multiple times.  Note that to ensure this, subclasses may need to
     * resort to expensive duplicate removal, so it's not suggested you use so unreasonably large distances.
     *
     * <p>You can also opt to include the origin -- that is, the (x,y,z) point at the center of the neighborhood -- in the neighborhood results.
     */
    public void getRadialLocations( final int x, final int y, final int z, final double dist, int mode, boolean includeOrigin, int measurementRule, boolean closed, IntBag xPos, IntBag yPos, IntBag zPos );


    /** Pass this into buildMap to indicate that it should make a map of any size it likes. */
    public static final int ANY_SIZE = 0;
    /** Creates a Map which is a copy of another. By default, HashMap is used. */
    public Map buildMap(Map other);
    /** Creates a map of the provided size (or any size it likes if ANY_SIZE is passed in).  By default, HashMap is used. */
    public Map buildMap(int size);

    }

