package sim.app.dkeepaway;

import java.awt.Color;

import java.awt.Graphics2D;
import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import sim.engine.Promised;
import sim.engine.SimState;
import sim.portrayal.DrawInfo2D;
import sim.util.Bag;
import sim.util.Double2D;

public class DBot extends DEntity{
	
	   private static final long serialVersionUID = 1;
	   Serializable promisedBallLoc;
	   Serializable promisedBallRadius;

	    public DBot( final double x, final double y, Color c)
	        {
	        super(x,y,2,c);
	        }


       //draw removed

	    public Double2D tempVector = new Double2D();
	        
	    public Double2D getForces( final DKeepaway keepaway)
	        {
	    	
	    	//keepaway.sendRemoteMessage("Dball", 1, velocity);

	    	
	        //sumVector.setTo(0,0);
	    	sumVector = new Double2D(0,0);
	        Bag objs = new Bag(keepaway.fieldEnvironment.getNeighborsWithinDistance(new Double2D(loc.x, loc.y), 100));
	        
	        //since ball might be on a different partition and not in halo, use registry if in range
	        //we want the location
	        try {
	        	Promised ballPromiseLoc = keepaway.sendRemoteMessage("DBall",0, null);
	        	Promised ballPromiseRadius = keepaway.sendRemoteMessage("DBall",1, null);
	        	
	        	if (ballPromiseLoc != null) {
	        	    promisedBallLoc = ballPromiseLoc.get(); //send a code to dball to get its location?, see p 414 in manual
	        	}

	        	if (ballPromiseRadius != null) {
	        		promisedBallRadius = ballPromiseRadius.get(); //send a code to dball to get its radius?, see p 414 in manual
	        	}
	        	
				
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
	        
	        Boolean ballIsLocal = false; //basically, flag to see if we need to use DRegistry or not

	        double dist = 0;

	        //http://www.martinb.com/physics/dynamics/collision/twod/index.htm
	        double mass1;
	        double mass2;

	        for(int x=0; x<objs.numObjs; x++)
	            {
	        	
	        	//if true we don't need to do registry stuff
	        	if (objs.objs[x] instanceof DBall) {
	        		ballIsLocal = true;
	        	}
	        	
	            if(objs.objs[x] != this)
	                {               
	                dist = ((DEntity)objs.objs[x]).loc.distance(loc);
	                    
	                if((((DEntity)objs.objs[x]).radius + radius)*1.25 > dist)  // collision!
	                    {
	                	
	                	
	                	
	                    // 10% chance of kicking the ball, if it's a ball
	                    // and kicking is not especially interesting.. its just accelerated impact
	                    if(objs.objs[x] instanceof DBall && keepaway.random.nextDouble() < .1)
	                        {
	                    	
	                    	
	                        //tempVector.subtract(((DEntity)objs.objs[x]).loc, loc);
	                        tempVector = ((DEntity)objs.objs[x]).loc.subtract(loc);
	                        tempVector.normalize().multiply(2.0);
	                        //((DEntity)objs.objs[x]).velocity = ((DEntity)objs.objs[x]).velocity.add(tempVector);
	                        Double2D velocity = ((DEntity)objs.objs[x]).velocity.add(tempVector);
	                        

	                        
	                        }
	                    else        // else just ram it...
	                        {               // shouldnt matter what type of object collision occurrs with
	                        //tempVector.x = 0;
	                        //tempVector.y = 0;
	                        tempVector = new Double2D(0,0);
	                        
	                        mass1 = mass - ((DEntity)objs.objs[x]).mass;
	                        mass1 /= (mass + ((DEntity)objs.objs[x]).mass);
	                        
	                        mass2 = 2 * ((DEntity)objs.objs[x]).mass;
	                        mass2 /= (mass + ((DEntity)objs.objs[x]).mass);
	                        
	                        // self = object a
	                        //tempVector.x = velocity.x * mass1 + ((DEntity)objs.objs[x]).velocity.x * mass2;
	                        //tempVector.y = velocity.y * mass1 + ((DEntity)objs.objs[x]).velocity.y * mass2;
	                        tempVector = new Double2D(velocity.x * mass1 + ((DEntity)objs.objs[x]).velocity.x * mass2,velocity.y * mass1 + ((DEntity)objs.objs[x]).velocity.y * mass2);
	                        
	                        // collided object = object 
	                        //((DEntity)objs.objs[x]).bump.x = velocity.x * mass2 - ((DEntity)objs.objs[x]).velocity.x * mass1;
	                        //((DEntity)objs.objs[x]).bump.y = velocity.y * mass2 - ((DEntity)objs.objs[x]).velocity.y * mass1;
	                        ((DEntity)objs.objs[x]).bump = new Double2D(velocity.x * mass2 - ((DEntity)objs.objs[x]).velocity.x * mass1, velocity.y * mass2 - ((DEntity)objs.objs[x]).velocity.y * mass1);
	                        //velocity.x = tempVector.x;
	                        //velocity.y = tempVector.y;
	                        velocity = new Double2D(tempVector.x, tempVector.y);
	                        
	                        }
	                    }
	                else if(objs.objs[x] instanceof DBall)
	                    {
	                    // if we didn't hit the ball, we want to go towards it
	                	tempVector = ((DEntity)objs.objs[x]).loc.subtract(loc);
	                    tempVector.resize(0.5);
	                    sumVector = sumVector.add(tempVector);
	                    
	                    
	                    
	                    }
	                }
	            }
	        
	        //handle ball if not local
	        if (ballIsLocal == false && promisedBallLoc != null) {
	        	
	        	Double2D promLoc = (Double2D)promisedBallLoc;
	            double promRadius = (double)promisedBallRadius;
	            
                dist = promLoc.distance(loc);
            
                if((promRadius + radius)*1.25 > dist)  // collision!
                    {
                	
                	
                	
                    // 10% chance of kicking the ball, if it's a ball
                    // and kicking is not especially interesting.. its just accelerated impact
                    if(keepaway.random.nextDouble() < .1)
                        {
                    	
                    	
                        //tempVector.subtract(((DEntity)objs.objs[x]).loc, loc);
                        tempVector = promLoc.subtract(loc);
                        tempVector.normalize().multiply(2.0);
                        //((DEntity)objs.objs[x]).velocity = ((DEntity)objs.objs[x]).velocity.add(tempVector);
                       // Double2D velocity = ((DEntity)objs.objs[x]).velocity.add(tempVector);
        	        	try {
							keepaway.sendRemoteMessage("DBall",2, tempVector);
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}


                        
                        }
                    }
              }
	        
	        // bump forces
	        sumVector = sumVector.add(bump);
	        //bump.x = 0;
	        //bump.y = 0;
	        bump = new Double2D(0,0);
	        return sumVector;
	        }
	 
	        
	    public void step( final SimState state )
	        {
	        DKeepaway keepaway = (DKeepaway)state;
	        
	        // get force
	        final Double2D force = getForces(keepaway);
	        
	        // acceleration = f/m
	        accel = force.multiply(1/mass); // resets accel
	        
	        // v = v + a
	        velocity = velocity.add(accel);
	        capVelocity();
	        
	        // L = L + v
	        newLoc = loc.add(velocity);  // resets newLoc
	        
	        // is new location valid?
	        //MutableDouble2D oldloc = loc;
	        if(isValidMove(keepaway, newLoc))
	            loc = newLoc;
	        	        
	        //System.out.println(this+" from "+loc+" to "+newLoc);
	        Double2D temploc = new Double2D(this.loc.getX(), this.loc.getY());
	        keepaway.fieldEnvironment.moveAgent(temploc, this);
	        
	        }

}
