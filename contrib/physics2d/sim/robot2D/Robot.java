package sim.robot2D;

import sim.physics2D.util.Angle;
//import sim.physics2D.util.Double2D;
import sim.util.matrix.*;
import sim.physics2D.physicalObject.*;
import sim.util.Double2D;

public class Robot extends MobileObject2D
    {
    private double P_angle;
    private double D_angle;
        
    private double P_pos;
    private double D_pos;
    public Robot()
        {
        P_angle = 10;
        D_angle = 500;
                
        P_pos = .2;
        D_pos = 10;
        }
        
    protected Double2D localFromGlobal(Double2D globalCoordinate)
        {
        // x0 = R'x1 - R'T
        DenseMatrix global = new sim.util.matrix.DenseMatrix(2, 1);
        global.vals[0][0] = globalCoordinate.x;
        global.vals[1][0] = globalCoordinate.y;
                
        DenseMatrix T = new sim.util.matrix.DenseMatrix(2, 1);
        T.vals[0][0] = this.getPosition().x;
        T.vals[1][0] = this.getPosition().y;
                
        double theta = this.getOrientation().radians;
        double[][] arR = {{ Math.cos(theta), -Math.sin(theta) },
                              { Math.sin(theta), Math.cos(theta) }};
                
        DenseMatrix R = new sim.util.matrix.DenseMatrix(arR);
        sim.util.matrix.DenseMatrix local = R.transpose().times(global).minus(R.transpose().times(T));
        return new Double2D(local.vals[0][0], local.vals[1][0]);
        }
        
    protected Double2D globalFromLocal(Double2D localCoordinate)
        {
        // x1 = Rx0 + T
        Double2D rotated = localCoordinate.rotate(this.getOrientation().radians);
        return rotated.add(this.getPosition());
        }
        
    /** Gives the angle of the vector (i.e. vector (1, 1) gives PI / 4)
     */
    protected Angle getAngle(Double2D vector)
        {
        // Get the angle  
        Angle theta;
        if (vector.x != 0)
            {
            theta = new Angle(Math.atan(vector.y / vector.x));
            if (vector.x < 0 && vector.y >= 0)
                theta = new Angle(Math.PI + theta.radians);
            else if (vector.x < 0 && vector.y < 0)
                theta = theta.add(Math.PI);
            else if (vector.x > 0 && vector.y < 0)
                theta = new Angle(Angle.twoPI + theta.radians);
            // otherwise (positive x,y quadrant) just theta
            }
        else
            theta = new Angle(vector.y > 0 ? Angle.halfPI : Angle.halfPI * 3);
                
        return theta;
        }
        
    protected void faceTowards(Angle globalAngle)
        {
        double angularVel = this.getAngularVelocity();
        double angularError = globalAngle.add(new Angle(-this.getOrientation().radians)).radians;
        if (angularError >= Math.PI)
            angularError = -(Angle.twoPI - angularError);
        double toAdd = P_angle * angularError - D_angle * angularVel;
        this.addTorque(toAdd);
        }
        
    protected void moveForward(double speed)
        {
        if (this.getVelocity().length() < speed - .5)
            this.addForce((new Double2D(1, 0)).rotate(this.getOrientation().radians));
        else if (this.getVelocity().length() > speed + .5)
            this.addForce((new Double2D(-1, 0)).rotate(this.getOrientation().radians));
                
        }
        
    protected void goTo(Double2D globalDestination)
        {
        // First, get the destination in local coordinates
        Double2D localDestination = localFromGlobal(globalDestination);

        Angle localAngle = getAngle(localDestination);
        double angularVel = this.getAngularVelocity();
        double angularError;
                
        // Turn towards the target
        if (localAngle.radians < Math.PI)
            angularError = localAngle.radians;
        else
            angularError = -(Angle.twoPI - localAngle.radians);
                
        double toAdd = P_angle * angularError - D_angle * angularVel;
        this.addTorque(toAdd);
                
        // approach the target
        if (Math.abs(angularError) < Math.PI / 15)
            {
            if (localDestination.length() < 20)
                this.addForce((new Double2D(4, 0)).rotate(this.getOrientation().radians));
            else
                {
                double scale = P_pos * localDestination.length() - D_pos * this.getVelocity().length();
                Double2D force = (new Double2D(1, 0)).rotate(this.getOrientation().radians).multiply(scale); 
                this.addForce(force);
                }
            }
        else
            {
            // otherwise, hit the breaks
            this.addForce(this.getVelocity().rotate(Math.PI).multiply(10));
            }
        }
        
    protected void stop()
        {
        double angularVel = this.getAngularVelocity();
        Double2D vel = this.getVelocity();
                
        this.addForce(vel.rotate(Math.PI).multiply(10));
        this.addTorque(-angularVel * 200);
        }
        
    protected void backup()
        {
        Double2D backward = new Double2D(4, 0).rotate(this.getOrientation().add(Math.PI).radians);
        this.addForce(backward);
        }
    }
