
package sim.app.robots;

import java.awt.*;

import sim.physics2D.constraint.*;

import sim.engine.*;
import sim.physics2D.physicalObject.*;
import sim.physics2D.forceGenerator.ForceGenerator;
import sim.physics2D.util.*;
import sim.util.Bag;
import sim.util.Double2D;

public class Bot extends sim.robot2D.Robot implements Steppable, ForceGenerator
    {
    Can currentCan;
    private PinJoint pj;
    private Double2D canHome;
    private double canHomeRadius = 20;
        
    private ConstraintEngine objCE;
        
    private Double2D botHome;
        
    private double normalForce;
        
    private int botState;
        
    private final int HAVECAN = 1;
    private final int APPROACHINGCAN = 2;
    private final int RELEASINGCAN = 3;
    private final int RETURNINGHOME = 4;
    private final int SEARCHING = 5;
        
    public Effector e1;
    public Effector e2;
        
    public Bot(Double2D pos, Double2D vel)
        {
        // vary the mass with the size
        this.setPose(pos, new Angle(0));
        this.setVelocity(vel);
        this.setShape(new sim.physics2D.shape.Circle(10, Color.gray), 300);
        this.setCoefficientOfFriction(.2);
        this.setCoefficientOfStaticFriction(0);
        this.setCoefficientOfRestitution(1);
                
        this.normalForce = this.getMass();
                
        currentCan = null;
                
        canHome = new Double2D(50, 50);
        botHome = pos;
                
        botState = SEARCHING;
                
        objCE = ConstraintEngine.getInstance();
        }
 
    public void step(SimState state)
        {
        Double2D position = this.getPosition();
        Robots simRobots = (Robots)state;
        simRobots.fieldEnvironment.setObjectLocation(this, new sim.util.Double2D(position.x, position.y));
        
        // Find a can
        if (botState == SEARCHING)
            {
            Bag objs = simRobots.fieldEnvironment.allObjects;
            objs.shuffle(state.random);
            for (int i = 0; i < objs.numObjs; i++)
                {
                if (objs.objs[i] instanceof Can)
                    {
                    currentCan = (Can)objs.objs[i];
                    if (currentCan.getPosition().y > 50 && currentCan.visible)
                        { 
                        botState = APPROACHINGCAN;      
                        break;
                        }
                    else
                        currentCan = null; // can is already home or has been picked
                    // up by another bot
                    }
                }
                        
            if (currentCan == null)
                botState = RETURNINGHOME;
            }
        }

    public void addForce()
        {
                
        switch (botState)
            {
            case HAVECAN:
                if (this.getPosition().y <= 40)
                    {
                    if (this.getVelocity().length() > 0.01 || this.getAngularVelocity() > 0.01)
                        this.stop();
                    else
                        {
                        objCE.unRegisterForceConstraint(pj);                            
                        botState = RELEASINGCAN;
                        objCE.removeNoCollisions(this, currentCan);
                        objCE.removeNoCollisions(e1, currentCan);
                        objCE.removeNoCollisions(e2, currentCan);
                        currentCan.visible = true;
                        }
                    }
                else
                    this.goTo(new Double2D(this.getPosition().x, 40));
                break;
            case RELEASINGCAN:
                // back out of can home
                if (this.getPosition().subtract(currentCan.getPosition()).length() <= 30)
                    backup();
                else
                    botState = SEARCHING;
                break;
            case APPROACHINGCAN:
                if (currentCan.visible)
                    this.goTo(currentCan.getPosition());
                else
                    botState = SEARCHING;
                break;
            case RETURNINGHOME:
                if (this.getPosition().subtract(botHome).length() <= 30)
                    {
                    if (this.getOrientation().radians != 0)
                        this.faceTowards(new Angle(0));
                    else
                        stop();
                    }
                else
                    this.goTo(botHome);
                break;  
            }

        }
        
    public int handleCollision(PhysicalObject2D other, Double2D colPoint)
        {
        Double2D globalPointPos = this.getPosition().add(colPoint);
        Double2D localPointPos = this.localFromGlobal(globalPointPos);
        Angle colAngle = this.getAngle(localPointPos);
                
        // Make sure the object is a can and that it is (roughly) between
        // the effectors
        if (other instanceof Can && botState == APPROACHINGCAN
            && (colAngle.radians < Math.PI / 8 || colAngle.radians > (Math.PI * 2 - Math.PI / 8)))
            {
            // Create a fixed joint directly at the center of the can
            pj = new PinJoint(other.getPosition(), this, other);
            objCE.registerForceConstraint(pj);
                        
            botState = HAVECAN;
                        
            objCE.setNoCollisions(this, other);
            objCE.setNoCollisions(e1, other);
            objCE.setNoCollisions(e2, other);
                        
            currentCan.visible = false;
            return 2; // sticky collision
            }
        else
            return 1; // regular collision
        }
    }
