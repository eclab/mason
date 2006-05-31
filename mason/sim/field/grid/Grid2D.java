/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.grid;
import sim.util.IntBag;


// (stars down the left are added there to keep the formatting correct)
/** 
 *    Define basic neighborhood functions for 2D Grids.  The basic interface defines a width and a height
 *    (not all grids require a width and a height unless you're doing toroidal grids), and basic math for
 *    toroidal computation, hex grid location, and triangular grid location.
 *    
 *    <H3>Toroidal Computation</H3>
 *    
 *    <p>If you're using the Grid to define a toroidal (wrap-around) world, you can use the <b>tx</b>
 *    and <b>ty</b> methods to simplify the math for you.  For example, to increment in the x direction,
 *    including wrap-around, you can do:  x = tx(x+1).
 *    
 *    <p>If you're sure that the values you'd pass into the toroidal functions would not wander off more than
 *    a grid dimension in either direction (height, width), you can use the slightly faster toroidal functions
 *    <b>stx</b> and <b>sty</b> instead.  For example, to increment in the x direction,
 *    including wrap-around, you can do:  x = stx(x+1).  See the documentation on these functions for
 *    when they're appropriate to use.  Under most common situations, they're okay.
 *
 *    <p>In HotSpot 1.4.1, stx, and sty are inlined.  In Hotspot 1.3.1, they are not (they contain if-statements).
 *    
 *    <H3>Hex Grid Computation</H3>
 *    Grids can be used for both squares and hex grids.  Hex grids are stored in an ordinary
 *    rectangular array and are defined as follows:
 *    
 *<pre>
 *<tt>
 *        (0,0)            (2,0)            (4,0)            (6,0)            ...
 *                (1,0)            (3,0)            (5,0)            (7,0)            ...
 *        (0,1)            (2,1)            (4,1)            (6,1)            ...
 *                (1,1)            (3,1)            (5,1)            (7,1)            ...
 *        (0,2)            (2,2)            (4,2)            (6,2)            ...
 *                (1,2)            (3,2)            (5,2)            (7,2)            ...
 *        ...              ...              ...              ...              ...
 *                ...              ...              ...              ...              ...
 *</tt>
 *</pre>    
 *    
 *<p>The rules moving from a hex location (at CENTER) to another one are as follows:
 *
 *<pre>
 *<tt>
 *                                                UP
 *                                                x
 *            UPLEFT                            y - 1                   UPRIGHT
 *            x - 1                                                     x + 1
 *            ((x % 2) == 0) ? y - 1 : y                CENTER                  ((x % 2) == 0) ? y - 1 : y
 *                                                x
 *            DOWNLEFT                            y                                             DOWNRIGHT
 *            x - 1                                                     x + 1
 *            ((x % 2) == 0) ? y : y + 1                DOWN                    ((x % 2) == 0) ? y : y + 1
 *                                                x
 *                                                                                              y + 1
 *
 *</tt>
 *</pre>
 *   NOTE: (x % 2 == 0), that is, "x is even", may be written instead in this faster way: ((x & 1) == 0)
 *
 * <p>Because the math is a little hairy, we've provided the math for the UPLEFT, UPRIGHT, DOWNLEFT,
 *  and DOWNRIGHT directions for you.  For example, the UPLEFT location from [x,y] is at
 *  [ulx(x,y) , uly(x,y)].  Additionally, the toroidal methods can be used in conjunction with the 
 *  hex methods to implement a toroidal hex grid.  Be sure to  <b>To use a toroidal hex grid properly,
 *   you must ensure that height of the grid is an even number</b>.  For example, the toroidal 
 *   UPLEFT X location is at tx(ulx(x,y)) and the UPLEFT Y location is at ty(uly(x,y)).  Similarly, 
 *   you can use stx and sty.
 *
 * <p>While this interface defines various methods common to many grids, you should endeavor not to 
 * call these grids casted into this interface: it's slow.  If you call the grids' methods directly 
 * by their class, their methods are almost certain to be inlined into your code, which is very fast.
 * 
 *     <H3>Triangular Grid Computation</H3>
 *    
 *    Grids can also be used for triangular grids instead of squares.  Triangular grids look like this:
 *
 *    <pre><tt>
 *    -------------------------
 *    \(0,0)/ \(0,2)/ \(0,4)/ \
 *     \   /   \   /   \   /   \    ...
 *      \ /(0,1)\ /(0,3)\ /(0,5)\
 *       -------------------------
 *      / \(1,1)/ \(1,3)/ \(1,5)/
 *     /   \   /   \   /   \   /    ...
 *    /(1,0)\ /(1,2)\ /(1,4)\ /
 *    -------------------------
 *    \(2,0)/ \(2,2)/ \(2,4)/ \
 *     \   /   \   /   \   /   \    ...
 *      \ /(2,1)\ /(2,3)\ /(2,5)\
 *       -------------------------
 *      / \(3,1)/ \(3,3)/ \(3,5)/
 *     /   \   /   \   /   \   /    ...
 *    /(3,0)\ /(3,2)\ /(3,4)\ /
 *    -------------------------
 *               .
 *               .
 *               .
 *    </tt></pre> 
 *
 *    <p>How do you get around such a beast?  Piece of cake!  Well, to go to your right or left 
 *    neighbor, you just add or subtract the X value.  To go to your up or down neighbor, all you 
 *    do is add or subtract the Y value.  All you need to know is if your triangle has an edge on 
 *    the top (so you can go up) or an edge on the bottom (so you can go down).  The functions TRB 
 *    (triangle with horizontal edge on 'bottom') and TRT (triangle with horizontal edge on 'top') 
 *    will tell you this.
 *
 *    <p>Like the others, the triangular grid can <i>also</i> be used in toroidal fashion, and the 
 *    toroidal functions should work properly with it. <b>To use a <i>toroidal</i> triangular grid, 
 *    you should ensure that your grid's length and width are <i>both</i> even numbers.</b>
 *
 *    <p>We'll provide a distance-measure function for triangular grids just as soon as we figure out
 *    what the heck one looks like.  :-)
 */

public interface Grid2D extends java.io.Serializable
    {
    /** Returns the width of the field. */
    public int getWidth();
    
    /** Returns the width of the field. */
    public int getHeight();
    
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
    
    /** Simple [and fast] toroidal x.  Use this if the values you'd pass in never stray
        beyond (-width ... width * 2) not inclusive.  It's a bit faster than the full
        toroidal computation as it uses if statements rather than two modulos.
        The following definition:<br>
        { int width = this.width; if (x >= 0) { if (x < width) return x; return x - width; } return x + width; }
        ...produces the shortest code (24 bytes) and is inlined in Hotspot for 1.4.1.  However
        in most cases removing the int width = this.width; is likely to be a little faster if most
        objects are usually within the toroidal region. */
    public int stx(final int x);
    
    /** Simple [and fast] toroidal y.  Use this if the values you'd pass in never stray
        beyond (-height ... height * 2) not inclusive.  It's a bit faster than the full
        toroidal computation as it uses if statements rather than two modulos.
        The following definition:<br>
        { int height = this.height; if (y >= 0) { if (y < height) return y ; return y - height; } return y + height; }
        ...produces the shortest code (24 bytes) and is inlined in Hotspot for 1.4.1. However
        in most cases removing the int height = this.height; is likely to be a little faster if most
        objects are usually within the toroidal region. */
    public int sty(final int y);

    /** Hex upleft x. */
    public int ulx(final int x, final int y);
    /** Hex upleft y. */
    public int uly(final int x, final int y);

    /** Hex upright x.*/
    public int urx(final int x, final int y);
    /** Hex upright y.*/
    public int ury(final int x, final int y);
    
    /** Hex downleft x.*/
    public int dlx(final int x, final int y);
    /** Hex downleft y.*/
    public int dly(final int x, final int y);
    
    /** Hex downright x. */
    public int drx(final int x, final int y);
    /** Hex downright y. */
    public int dry(final int x, final int y);

    /** Hex up x. */
    public int upx(final int x, final int y);
    /** Hex up y. */
    public int upy(final int x, final int y);

    /** Hex down x. */
    public int downx(final int x, final int y);
    /** Hex down y. */
    public int downy(final int x, final int y);

    /** Horizontal edge is on the bottom for triangle.  True if x + y is odd.
        One definition of this is <tt>return ((x + y) & 1) == 1;</tt>*/
    public boolean trb(final int x, final int y);
    
    /** Horizontal edge is on the top for triangle.  True if x + y is even.
        One definition of this is <tt>return ((x + y) & 1) == 0;</tt>*/
    public boolean trt(final int x, final int y);

    /**
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y) ) <= dist.  This region forms a
     * square 2*dist+1 cells across, centered at (X,Y).  If dist==1, this
     * is equivalent to the so-called "Moore Neighborhood" (the eight neighbors surrounding (X,Y)), plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     */
    public void getNeighborsMaxDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos );

    /**
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) <= dist.  This region forms a diamond
     * 2*dist+1 cells from point to opposite point inclusive, centered at (X,Y).  If dist==1 this is
     * equivalent to the so-called "Von-Neumann Neighborhood" (the four neighbors above, below, left, and right of (X,Y)),
     * plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     */
    public void getNeighborsHamiltonianDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos );

    /**
     * Gets all neighbors located within the hexagon centered at (X,Y) and 2*dist+1 cells from point to opposite point 
     * inclusive.
     * If dist==1, this is equivalent to the six neighbors immediately surrounding (X,Y), 
     * plus (X,Y) itself.
     * Places each x and y value of these locations in the provided IntBags xPos and yPos, clearing the bags first.
     */
    public void getNeighborsHexagonalDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos );
    }
