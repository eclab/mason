
public abstract class Resource 
	{
	static int lastType = -1;
	int type;
	String name;
	
	/** Creates a new resource type.  Not threadsafe.  */
	protected int getNextType() { return ++lastType; }
	
	/** 
		Returns a new kind of Entity with a given name, and initial amount.
		The name is informal: It's legal for two different kinds of resources 
		to have the same name.  Entity types are distinguishe internally using
		unique integers. 
	*/
	public Resource(String name)
		{
		this.name = name;
		this.type = getNextType();
		}
		
	/** Fills out nothing, you'll have to do that. */
	protected Resource()
		{
		}
	
	/** Clears the resource.  For countable/uncountable resources, this sets it to zero.
		For entities, this removes any stored elements. */
	public abstract void clear();
	
	/**
		Prints the resource out in a pleasing manner. 
	*/
	public abstract String toString();

	/** 
		Returns true if the two objects are both Resources with the same type and amount.
	*/
	public abstract boolean equals(Object other);

	/** 
		Returns true if the two objects are both Entitys with the same type.
	*/
	public boolean isSameType(Resource other)
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
	

	/** Makes an exact copy of this resource */
	public abstract Resource duplicate();

	/** 
		Exactly copies the resource TIMES times.  Returns an array of the new resources.
		Note that this changes the amount of the given entity available in the world.
	*/
	public Resource[] duplicate(int times)
		{
		Resource[] res = new Resource[times];			// this will throw an exception for us
		for(int i = 0; i < times; i++)
			{
			res[i] = duplicate();
			}
		return res;
		}
	
	/**
		Returns the assigned entity type.
	*/
	public int getType()
		{
		return type;
		}
		
	protected void setType(int type)
		{
		this.type = type;
		}
		
	/**
		Returns the assigned entity name.
	*/
	public String getName()
		{
		return name;
		}

	protected void setName(String name)
		{
		this.name = name;
		}
	}