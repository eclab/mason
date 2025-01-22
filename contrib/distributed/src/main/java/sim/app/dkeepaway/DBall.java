package sim.app.dkeepaway;

import java.awt.Color;
import java.io.Serializable;
import java.rmi.RemoteException;

import sim.engine.Distinguished;
import sim.engine.SimState;
import sim.util.Bag;
import sim.util.Double2D;

public class DBall extends DEntity implements Distinguished{
        
    private static final long serialVersionUID = 1;

	static
		{
		turnOnDistinguished();
		}
    

    // used to determine if the ball is stuck
    public Double2D stillPos;                           // last position
    public double dt;                                   // delta time--how many steps it has been still

    public DBall( final double x, final double y)
        {
        super(x,y,1,Color.white);
        
        cap = 2.0;
        
        bump = new Double2D(0,0);
        stillPos = new Double2D(0,0);
        dt = 0;
        }
 
        

    public Double2D getForces( final DKeepaway keepaway)
        {
        //sumVector.setTo(0,0);
        sumVector = new Double2D(0,0);
        Bag objs = new Bag(keepaway.fieldEnvironment.getNeighborsWithinDistance(new Double2D(loc.x, loc.y), 100));

        double dist = 0;

        for(int x=0; x<objs.numObjs; x++)
            {
            if(objs.objs[x] != this)
                {              
                dist = ((DEntity)objs.objs[x]).loc.distance(loc);
                    
                if((((DEntity)objs.objs[x]).radius + radius) > dist)  // collision!
                    {
                    if(objs.objs[x] instanceof DBall)
                        {
                        // ball
                        // actually this is not possible with current settings
                        }
                    else // if(objs.objs[x] instanceof Ball)
                        {
                        // bot
                        // and this is handled by the bots themselves
                        }
                    }
                }
            }
        
        // add bump vector
        sumVector = sumVector.add(bump);
        //bump.x = 0;
        //bump.y = 0;
        bump = new Double2D(0,0);
        return sumVector;
        }
    
    Double2D friction = new Double2D();
    Double2D stuckPos = new Double2D();
        
    public void step( final SimState state )
        {
        DKeepaway keepaway = (DKeepaway)state;
        

        
        // get force
        final Double2D force = getForces(keepaway);
        

        
        // acceleration = f/m
        accel = force.multiply(1/mass); // resets accel
                
        // hacked friction
        friction = velocity.multiply(-0.025);  // resets friction
        
        // v = v + a
        velocity = velocity.add(accel);
        velocity = velocity.add(friction);
        capVelocity();
        

        
        // L = L + v
        newLoc = loc.add(velocity);  // resets newLoc
        

        
        // is new location valid?
        Double2D oldloc = new Double2D(loc.getX(), loc.getY());
        


        if(isValidMove(keepaway, newLoc))
            {
            loc = newLoc;
            }
        

        
        // check if ball hasn't moved much
        if(loc.distanceSq(stuckPos) < (0.1*0.1))
            dt++;
        else
            {
            dt = 0;
            //stuckPos.setTo(loc);
            stuckPos = loc;
            }
                
        // might be stuck...  move to random location!
        if(dt > 1000)
            {
            dt = 0;
            //stuckPos.setTo(loc);
            stuckPos = loc;
            //loc.x = keepaway.random.nextDouble()*keepaway.xMax;
            //loc.y = keepaway.random.nextDouble()*keepaway.yMax;
            loc = new Double2D(keepaway.random.nextDouble()*keepaway.xMax, keepaway.random.nextDouble()*keepaway.yMax);
            }
        

                    
        Double2D temploc = new Double2D(this.loc.getX(), this.loc.getY());
        keepaway.fieldEnvironment.moveAgent(temploc, this);
        
        }



    @Override
    public Serializable remoteMessage(int tag, Serializable arguments) throws RemoteException {
        // TODO Auto-generated method stub
                
        //return the actual object
        if (tag == 0) {
            return loc; //return loc instead
            }
                
        if (tag == 1) {
            return radius; //return radius instead
            }
                
        if (tag == 2) {
            this.velocity = this.velocity.add((Double2D)arguments);
            return null;
            }
                
        return null;
        }



    @Override
    public String distinguishedName() {
        // TODO Auto-generated method stub
        return "DBall";
        }

    }
