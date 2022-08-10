package sim.app.dkeepaway;


import java.awt.Color;


import sim.engine.DSteppable;
import sim.util.Bag;
import sim.util.Double2D;

public abstract class DEntity extends DSteppable{

    private static final long serialVersionUID = 1;

    public Double2D loc, velocity, bump;
    public Double2D force = new Double2D();
    public Double2D accel = new Double2D();
    public Double2D newLoc = new Double2D();
    public Double2D sumVector = new Double2D(0,0);
    public Color c;

    
    public double speed, radius;
    
    public double cap;
    
    public double mass;
        
    // Accessors for inspector
    public double getX() { return loc.x;}
    public void setX( double newX ) { loc = new Double2D(newX, loc.y);} 
    
    public double getY() { return loc.y; }
    public void setY( double newY ) { loc = new Double2D(loc.x, newY); }
    
    public double getVelocityX() { return velocity.x; }
    public void setVelocityX( double newX ) { velocity = new Double2D(newX, velocity.y); }
    
    public double getVelocityY() { return velocity.y; }
    public void setVelocityY( double newY ) { velocity = new Double2D(velocity.x, newY); }
 
    public double getSpeed() { return speed; }
    public void setSpeed( double newSpeed ) { speed = newSpeed; }   
    
    public double getRadius() { return radius; }
    public void setRadius( double newRadius ) 
        {
        radius = newRadius;
        //scale = 2 * radius;  // we do NOT inherit from OvalPortrayal
        } 
    
    public double getMass() { return mass; }
    public void setMass( double newMass ) { mass = newMass; } 
    
    // Constructor
    public DEntity( double newX, double newY, double newRadius, Color c)
        {
        
        loc = new Double2D(newX, newY);
        velocity = new Double2D(0, 0);
        bump = new Double2D(0, 0);
        radius = newRadius;
        
        mass = 1.0;
        cap = 1.0;
        
        speed = 0.4;
        
        this.c = c;
        }
    
    public boolean isValidMove( final DKeepaway keepaway, final Double2D newLoc)
        {
    	


        Bag objs = new Bag(keepaway.fieldEnvironment.getNeighborsWithinDistance(new Double2D(loc.x, loc.y), 10));

        double dist = 0;
        



        // check objects
        for(int x=0; x<objs.numObjs; x++)
            {
            if(objs.objs[x] != this)
                {
                dist = ((DEntity)objs.objs[x]).loc.distance(newLoc);

                if((((DEntity)objs.objs[x]).radius + radius) > dist)  // collision!
                    return false;
                }
            }
        
 


        // check walls
        if(newLoc.x > keepaway.xMax)
            {
            if (velocity.x > 0) velocity = new Double2D(-velocity.x, velocity.y); //velocity.x = -velocity.x; 
  
            return false;
            }
               
        else if(newLoc.x < keepaway.xMin)
            {
            if (velocity.x < 0) velocity = new Double2D(-velocity.x, velocity.y); //velocity.x = -velocity.x;

            return false;
            }
        else if(newLoc.y > keepaway.yMax)
            {
            if (velocity.y > 0) velocity = new Double2D(velocity.x, -velocity.y); //velocity.y = -velocity.y;
            

            return false;
            }
        else if(newLoc.y < keepaway.yMin)
            {
            if (velocity.y < 0) velocity = new Double2D(velocity.x, -velocity.y); //velocity.y = -velocity.y;
            

            return false;
            }
        
        // no collisions: return, fool
        return true;
        }
    
    public void capVelocity()
        {
        if(velocity.length() > cap)
            velocity = velocity.resize(cap);
        }
    	
	
}
