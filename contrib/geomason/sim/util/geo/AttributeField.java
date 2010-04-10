package sim.util.geo; 

public class AttributeField { 

    public String name; 
    public char type; 
    public int size; 
    public Object value; 
        
    public AttributeField(String n, char t, int s)
    {
        name = n; 
        type =t; 
        size =s; 
    }
        
    public String toString()
    {
        return "Name: " + name + " Type: " + type + " Size: " + size + " Value: " + value; 
    }
        

    public Object clone()
    {
        AttributeField a = new AttributeField(name, type, size); 
        a.value = value; 
        return a; 
    }

}
