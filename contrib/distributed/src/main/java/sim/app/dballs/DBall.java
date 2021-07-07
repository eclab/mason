package sim.app.dballs;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;

import sim.engine.DSteppable;
import sim.engine.Distinguished;
import sim.engine.Promised;
import sim.engine.SimState;
import sim.field.continuous.DContinuous2D;
import sim.field.network.*;
import sim.util.Bag;
import sim.util.Double2D;

public class DBall extends DSteppable implements Distinguished {

	private static final long serialVersionUID = 1L;
	
	public String id;
	
	// force on the Ball
	public double forcex;
	public double forcey;

	// Ball mass
	public double mass;

	// Current Ball velocity
	public double velocityx;
	public double velocityy;

	// did the Ball collide?
	public boolean collision;

	// for drawing: always sqrt of mass
	public double diameter;

	public Double2D loc;

	public DBall(String id, final Double2D loc, double vx, double vy, double m) throws RemoteException {
		super();
		this.id = id;
		this.loc = loc;
		velocityx = vx;
		velocityy = vy;
		mass = m;
		diameter = Math.sqrt(m);
	}

	public void addForce(Double2D otherBallLoc, Double2D myLoc, DBand band) {
		// compute difference
		final double dx = otherBallLoc.x - myLoc.x;
		final double dy = otherBallLoc.y - myLoc.y;
		final double len = Math.sqrt(dx * dx + dy * dy);
		final double l = band.laxDistance;

		final double k = band.strength / 512.0; // cut-down
		final double forcemagnitude = (len - l) * k;

		// add rubber band force
		if (len - l > 0) {
			forcex += (dx * forcemagnitude) / len;
			forcey += (dy * forcemagnitude) / len;
		}
	}

	public void computeForce(SimState state) {
		DBalls allBalls = (DBalls) state;
		Network bands = allBalls.bands;
		DContinuous2D<DBall> balls = allBalls.balls;

		Double2D me = balls.getObjectLocationLocal(this);

		forcex = 0;
		forcey = 0;
		Bag in = bands.getEdgesIn(this);
		Bag out = bands.getEdgesOut(this);

		if (in != null)
			for (int x = 0; x < in.numObjs; x++) {
				Edge e = (Edge) (in.objs[x]);
				DBand b = (DBand) (e.info);
				DBall other = (DBall) (e.from()); // from him to me
				Double2D him = null;
				if(balls.containsLocal(other)) {
					him = balls.getObjectLocationLocal(other);
				} else {
					try {
						him = allBalls.getDRegistry().getObjectT(other.id);
					} catch (Exception e1) {
						System.out.println("Error in getting remote object with id " + other.id);
						e1.printStackTrace();
					}
						
				}
				System.out.println("Other ball " + him);
				addForce(him, me, b);
			}

		if (out != null)
			for (int x = 0; x < out.numObjs; x++) {
				Edge e = (Edge) (out.objs[x]);
				DBand b = (DBand) (e.info);
				DBall other = (DBall) (e.to()); // from me to him
				Double2D him = null;
				if(balls.containsLocal(other)) {
					him = balls.getObjectLocationLocal(other);
				} else {
					try {
						Promised remoteNodeHim = allBalls.contactRemoteObj(other.id, 0, null);
						if(remoteNodeHim.isReady()) { 
							him = (Double2D) remoteNodeHim.get();
						}
					} catch (Exception e1) {
						System.out.println("Error in getting remote object with id " + other.id);
						e1.printStackTrace();
					}
						
				}
				System.out.println("Other ball " + him);
				addForce(him, me, b);
			}
	}
	
	public void computeCollision(DBalls allBalls) {
		Double2D me = allBalls.balls.getObjectLocationLocal(this); 
		ArrayList<DBall> b = allBalls.balls.getNeighborsExactlyWithinDistance(me, DBalls.collisionDistance);
		collision = b.size() > 1;
	}

	@Override
	public void step(SimState state) {
		DBalls allBalls = (DBalls) state;

		// acceleration = force / mass
		final double ax = forcex / mass;
		final double ay = forcey / mass;

		// velocity = velocity + acceleration
		velocityx += ax;
		velocityy += ay;

		// position = position + velocity
		Double2D pos = allBalls.balls.getObjectLocationLocal(this);
		Double2D newpos = new Double2D(pos.x + velocityx, pos.y + velocityy);
		allBalls.balls.moveAgent(newpos, this);
//		System.out.println("&&&&&&&&&&&&&&&&&&&&AGENT MOVED");
		// compute collisions
        computeCollision(allBalls);
//      System.out.println("&&&&&&&&&&&&&&&&&&COLLISION COMPUTED");
	}

	public double getVelocityX() {
		return velocityx;
	}

	public void setVelocityX(double val) {
		velocityx = val;
	}

	public double getVelocityY() {
		return velocityy;
	}

	public void setVelocityY(double val) {
		velocityy = val;
	}

	public double getMass() {
		return mass;
	}

	public void setMass(double val) {
		if (val > 0) {
			mass = val;
			diameter = Math.sqrt(val);
		}
	}

	public Serializable respondToRemote(Integer tag, Serializable argument) throws RemoteException {
		return this.loc;
	}
	

}
