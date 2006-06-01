/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.grid;
import sim.util.IntBag;

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

    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y), abs(z-Z) ) <= dist.  This region forms a
     * cube 2*dist+1 cells across, centered at (X,Y,Z).  If dist==1, this
     * is equivalent to the twenty-six neighbors surrounding (X,Y,Z), plus (X,Y) itself.  
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     */
    public void getNeighborsMaxDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos, IntBag zPos );

    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) + abs(z-Z) <= dist.  This region 
     * forms an <a href="http://images.google.com/images?q=octahedron">octohedron</a> 2*dist+1 cells from point
     * to opposite point inclusive, centered at (X,Y,Y).  If dist==1 this is
     * equivalent to the six neighbors  above, below, left, and right, front, and behind (X,Y,Z)),
     * plus (X,Y,Z) itself.
     * Places each x, y, and z value of these locations in the provided IntBags xPos, yPos, and zPos, clearing the bags first.
     */
    public void getNeighborsHamiltonianDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos, IntBag zPos );
    }

