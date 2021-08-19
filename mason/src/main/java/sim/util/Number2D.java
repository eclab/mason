package sim.util;

public abstract class Number2D extends NumberND{
	
	public Number2D add(int dx, int dy)
	{
	if (this instanceof Int2D)
		return ((Int2D)this).add(dx, dy);
	else if (this instanceof Double2D)
		return ((Double2D)this).add((double)dx, (double)dy);
	else return null;
	} 
	
	public Number2D add(Number2D offset)
	{
	if (this instanceof Int2D)
		return ((Int2D)this).add(offset);
	else if (this instanceof Double2D)
		return ((Double2D)this).add(offset);
	else return null;
	} 

}
