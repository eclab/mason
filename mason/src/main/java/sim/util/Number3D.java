package sim.util;

public abstract class Number3D extends NumberND{

	public Number3D add(int dx, int dy, int dz)
	{
	if (this instanceof Int3D)
		return ((Int3D)this).add(dx, dy, dz);
	else if (this instanceof Double3D)
		return ((Double3D)this).add((double)dx, (double)dy, (double)dz);
	else return null;
	} 
	
	public Number3D add(Number3D offset)
	{
	if (this instanceof Int3D)
		return ((Int3D)this).add(offset);
	else if (this instanceof Double3D)
		return ((Double3D)this).add(offset);
	else return null;
	} 
	
}
