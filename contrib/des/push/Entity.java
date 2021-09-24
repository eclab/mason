import sim.engine.*;
import java.util.*;

public class Entity extends Resource
	{
	Object storage;
	
	public Object getStorage()
		{
		return storage;
		}
	
	public void setStorage(Object val)
		{
		storage = val;
		}
	
	public Entity(String name)
		{
		super(name);
		}
	
	/** 
		Returns a Entity of the same type, name, and amount as the provided resource.
		This is essentially a clone of the resource.
	*/
	public Entity(Entity other)
		{
		super();
		this.name = other.name;
		this.type = other.type;
		this.storage = other.storage;
		}
		
	public void compose(Resource[] resources)
		{
		storage = resources;
		}
	
	public void compose(ArrayList<Resource> resources)
		{
		storage = resources.toArray(new Resource[resources.size()]);
		}
	
	public Resource[] decompose()
		{
		return (Resource[]) storage;
		}

	public void clear()
		{
		storage = null;
		}
		
	public double getAmount()
		{
		return 1.0;
		}

	public Resource duplicate()
		{
		return new Entity(this);
		}

	/**
		Prints the resource out in a pleasing manner. 
	*/
	public String toString()
		{
		return "Entity[" + name + " (" + type + ")]";
		}

	/** 
		Returns true if the two objects are both Entitys with the same type and amount.
	*/
	public boolean equals(Object other)
		{
		if (other == this) return true;
		if (other == null) return false;
		if (other instanceof Resource)
			{
			Resource res = (Resource) other;
			return (res.type == type);
			}
		else return false;
		}
	}