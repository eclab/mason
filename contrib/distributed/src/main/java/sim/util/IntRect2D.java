package sim.util;

import java.io.Serializable;

public class IntRect2D implements Serializable
{
	private static final long serialVersionUID = 1L;
	//// SEAN FIXME: I am presuming that this rectangle is HALF-OPEN, that is br is
	//// out of bounds
//// IS THIS A VALID ASSUMPTION GIVEN THE HALO FIELD USAGE ETC.?
	Int2D ul, br;

//// SEAN FIXME: I changed this to >= from >, assuming you can't have zero-length rects. 
//// Zero-length rects are fine but they can't have a "center", which is one of the functions.
	void verifyValidSize(Int2D ul, Int2D br)
	{
		if (ul.x >= br.x) {
			System.out.println("ul x : "+ul.x+ " br.x "+br.x);
			throw new IllegalArgumentException("IntRect2D ul.x >= br.x");
		}
		if (ul.y >= br.y) {
			System.out.println("ul y : "+ul.y+ " br.y "+br.y);
			throw new IllegalArgumentException("IntRect2D ul.y >= br.y");
		}
	}

	public IntRect2D(Int2D ul, Int2D br)
	{
		verifyValidSize(ul, br);
		this.ul = ul;
		this.br = br;
	}

	public IntRect2D(int width, int height)
	{
		this(new Int2D(0, 0), new Int2D(width, height));
	}

	public Int2D ul()
	{
		return ul;
	}

	public Int2D br()
	{
		return br;
	}
	
	// Return the area of the hyper rectangle
	public int getArea()
	{
		return (br.x - ul.x) * (br.y - ul.y);
	}

	public int getHeight()
	{
		return br.y - ul.y;
	}

	public int getWidth()
	{
		return br.x - ul.x;
	}

	// Return whether the rect contains the given <x, y> point.
	// Noted that the rect is treated as half-inclusive (ul) and half-exclusive (br)
	public boolean contains(int x, int y)
	{
		return (ul.x <= x && ul.y <= y && br.x > x && br.y > y);
	}

	// Return whether the rect contains the given <x, y> point.
	// Noted that the rect is treated as half-inclusive (ul) and half-exclusive (br)
	public boolean contains(double x, double y)
	{
		return (ul.x <= x && ul.y <= y && br.x > x && br.y > y);
	}

	// Return whether the rect contains p
	// Noted that the rect is treated as half-inclusive (ul) and half-exclusive (br)
	public boolean contains(Int2D p)
	{
		return (ul.x <= p.x && ul.y <= p.y && br.x > p.x && br.y > p.y);
	}

	// Return whether the rect contains p
	// Noted that the rect is treated as half-inclusive (ul) and half-exclusive (br)
	public boolean contains(Double2D p)
	{
		return (ul.x <= p.x && ul.y <= p.y && br.x > p.x && br.y > p.y);
	}

//// SEAN FIXME: we should try not to require this
	public boolean contains(Number2D p)
	{
		double x = p.getVal(0);
		double y = p.getVal(1);
		return (ul.x <= x && ul.y <= y && br.x > x && br.y > y);
	}

//// SEAN FIXME: Verify that this is the case for half-open intervals
	// Return whether the given rect is inside this rectangle
	public boolean contains(IntRect2D that)
	{
		return (ul.x <= that.ul.x && ul.y <= that.ul.y && br.x >= that.br.x && br.y >= that.br.y);
	}

//// SEAN FIXME: rename to intersects(...)
//// SEAN FIXME: Verify that this is the case for half-open intervals
	// Return whether the given rect intersects with self
	public boolean intersects(IntRect2D that)
	{
		return (ul.x < that.br.x && ul.y < that.br.y && that.ul.x < br.x && that.ul.y < br.y);
	}

//// SEAN FIXME: Verify that this is the case for half-open intervals
	// Return the intersection of the given rect and self
	public IntRect2D getIntersection(IntRect2D that)
	{
		if (!intersects(that))
			throw new IllegalArgumentException(this + " does not intersect with " + that);
		return new IntRect2D(new Int2D(Math.max(ul.x, that.ul.x), Math.max(ul.y, that.ul.y)),
				new Int2D(Math.min(br.x, that.br.x), Math.min(br.y, that.br.y)));
	}

/*
	public IntRect2D resize(int left, int up, int right, int down)
	{
		Int2D newUL = new Int2D(ul.x + left, ul.y + up);
		Int2D newBR = new Int2D(br.x + right, br.y + down);
		return new IntRect2D(newUL, newBR);
	}
*/
/*
	public IntRect2D translate(int x, int y)
	{
		Int2D newUL = new Int2D(ul.x + x, ul.y + y);
		Int2D newBR = new Int2D(br.x + x, br.y + y);
		return new IntRect2D(newUL, newBR);
	}
*/

//// SEAN FIXME: I changed this to Double2D because it does / 2 which is almost certainly wrong
//// SEAN FIXME: I think the code is wrong in general anyway, as it rounds *up* because of half-open
	public Double2D getCenter()
	{
		// return new Double2D((br.x - ul.x + 1) / 2.0, (br.y - ul.y + 1) / 2.0);
		return new Double2D(((br.x - ul.x) / 2.0) + ul.x, ((br.y - ul.y) / 2.0) + ul.y);

		// return new Int2D(IntStream.range(0, getNd()).map(i -> (br.c(i) - ul.c(i)) / 2
		// + ul.c(i)).toArray());
	}

	// Return whether two hyper rectangles equal (the two vertexs equal)
	public boolean equals(IntRect2D that)
	{
		return this.ul.equals(that.ul) && this.br.equals(that.br);
	}

//// SEAN FIXME: I deleted compareTo entirely because it makes arbitrary comparison orderings and Int2D is not comparable

	public static IntRect2D getBoundingRect(IntRect2D[] rects)
	{
		if (rects.length == 0)
			return null;

		Int2D ul = rects[0].ul, br = rects[0].br;
		for (IntRect2D rect : rects)
		{
			ul = (Int2D) ul.min(rect.ul);
			// br = (Int2D)br.max(rect.ul);
			br = (Int2D) br.max(rect.br);
		}

		return new IntRect2D(ul, br);
	}

	public String toString()
	{
		return new String("IntRect2D[(" + ul.x + ", " + ul.y + ") -> (" + br.x + ", " + br.y + ")]");
	}

/*
//// SEAN FIXME: I deleted toToroidal(rect) and split entirely for the time being

	// fix for bugs
	public Int2D toToroidal(Int2D p)
	{
		int x = p.x;
		int y = p.y;
		int width = br.x - ul.x;
		int height = br.y - ul.y;

		return new Int2D(tx(x, width), ty(y, height));
	}

	// slight revision for more efficiency
	final int tx(int x, int width)
	{
		if (x >= 0 && x < width)
			return x; // do clearest case first
		x = x % width;
		if (x < 0)
			x = x + width;
		return x;
	}

	// slight revision for more efficiency
	final int ty(int y, int height)
	{
		if (y >= 0 && y < height)
			return y; // do clearest case first
		y = y % height;
		if (y < 0)
			y = y + height;
		return y;
	}
*/

	// added by Raj Patel
	// returns a list of every Int2D point in IntRect2D
	public Int2D[] getPointList()
	{
		Int2D[] listOfPoints = new Int2D[this.getArea()];

		int listInd = 0;

		//int[] sizes = this.getSizes();
		int width = getWidth();
		int height = getHeight();
		
		for (int i = 0; i < width; i++)
		{
			for (int j = 0; j < height; j++)
			{
				listOfPoints[listInd] = new Int2D(ul.x + i, ul.y + j);
				listInd++;
			}
		}

		return listOfPoints;
	}

/*
	// remove? or add?
	public IntRect2D resize(Int2D vals)
	{
		return new IntRect2D(ul.subtract(vals), br.add(vals));
	}
*/

	public IntRect2D expand(int val)
	{
		//int[] vals = new int[] { val, val };
		//return resize(vals);
		
		/// SEAN -- I *think* this is right?
		return new IntRect2D(ul.add(-val, -val), br.add(val, val));
		//return new IntRect2D(ul.add(-val), br.add(val));		

	}

/*
	// maybe remove, maybe keep
	public IntRect2D add(int dim, int offset)
	{
		return new IntRect2D(ul.add(dim, offset), br.add(dim, offset));
	}
*/

	// maybe remove, maybe keep
	public IntRect2D add(Int2D offsets)
	{
		return new IntRect2D(ul.add(offsets), br.add(offsets));
	}

	// maybe remove, maybe keep
	public IntRect2D subtract(Int2D offsets)
	{
		return new IntRect2D(ul.subtract(offsets), br.subtract(offsets));
	}
}
