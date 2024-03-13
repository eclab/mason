package sim.app.example.mm1;

import sim.des.CountableResource;
import sim.des.Delay;
import sim.des.Entity;
import sim.des.Lock;
import sim.des.Pool;
import sim.des.Sink;
import sim.des.Source;
import sim.des.Unlock;
import sim.des.portrayal.DES2D;
import sim.engine.SimState;
import sim.util.distribution.Exponential;

/**
 * @author giuseppe
 * 
 *         An M/M/1 queuing system with the following characteristics: -
 *         interarrival time = exponential - service time = exponential - number
 *         of parallel server = 1 - capacity = infinite - calling population =
 *         infinite
 *
 *         The system works as follows: entities enter the system, try to
 *         acquire the single service available, hold it for a while, release
 *         it, and exit.
 * 
 *         Examples of M/M/1 systems are: - A line of passengers waiting passing
 *         security check before boarding a plane; - A single ATM outside a
 *         grocery store; - A dentist office with a single receptionist to check
 *         in patients.
 * 
 * 
 */

public class MM1Queue extends SimState {

    private static final long serialVersionUID = -1746164159521942024L;

    // Space where blocks will be drawn
    public DES2D field = new DES2D(100, 100);

    public MM1Queue(long seed) {
        super(seed);
        }

    public void start() {
        // any Class extending SimState must begin with this
        super.start();

        // a generic Entity that will go through the system
        Generic entity = new Generic("foo");

        // create the starting point of the system where
        // Generic entities are created
        Source source = new Source(this, entity);

        // Source will generate Entity at exponential rate
        Exponential exp = new Exponential(15, random);
        source.setRateDistribution(exp);
        // one Entity is produced each time
        source.setProduction(1);
        // set the Source to autoschedule
        // we need to add it to the schedule only once
        source.setAutoSchedules(true);
        schedule.scheduleOnce(source);

        // start building the server
        // a Pool containing a single resource representing
        // the single server available
        Pool pool = new Pool(new CountableResource("server", 1), 1);
        pool.setName("Pool");
        // Lock that acquire the resource from the pool
        // if is available it lets the Entity pass
        // one resource is acquired each time
        Lock lock = new Lock(this, entity, pool, 1);

        // create a Delay to simulate the time needed for the
        // Entity to "use" the server before releasing it
//              Delay delay = new Delay(this, entity);
                
/*      // a quick test
        Delay delay = new Delay(this, entity)
        {
        public boolean accept(sim.des.Provider provider, sim.des.Resource amount, double atLeast, double atMost)
        {
        double v = amount.getAmount();
        boolean ret = super.accept(provider, amount, atLeast, atMost);
        System.err.println("" + schedule.time() + " " + atLeast + " " + atMost + " " + v + " " + ret);
        return ret;
        }
                                        
        };
*/
        // delay time is exponential
        delay.setDelayDistribution(exp);
        // set capacity to infinite, the resource in the pool
        // will restrict the capacity
        delay.setCapacity(Double.POSITIVE_INFINITY);

        // When the Delay has offered a resource to the Unlock, it now has space for
        // another resource to come in.  But the Lock and Source don't know that.  So here
        // we set up the Delay to tell the Source to offer something to the Lock as soon
        // as the Delay has space.  It happens AFTER the Delay has offered to the Unlock, so
        // the Lock can now lock again. Another option is to have the Unlock set up the Lock
        // as a PARTNER so the Unlock tells the Lock to ask someone (notionaly the Source)
        // to provide it resources as soon as the Unlock has freed up the pool.
        delay.setSlackProvider(source);
        delay.setSlackReceiver(lock);

        // Unlock to release the resource
        Unlock unlock = new Unlock(this, entity, pool, 1);

        // Sink block to let the Entity exit the system
        Sink sink = new Sink(this, entity);

        // hook the blocks together
        // the Source produces entities that are sent
        // to the Lock to acquire the resource
        source.addReceiver(lock);
        // the lock tries to grab the resource from the Pool
        // if it succeeds the Entity can use the resource
        // usage of the resource is simulated by a Delay
        // the Lock pass the Entity to the Delay
        lock.addReceiver(delay);
        // after some time the Entity can release the resource
        // passing through the Unlock block
        delay.addReceiver(unlock);
        // finally the Entity can exit the system using the Sink
        unlock.addReceiver(sink);

        // draw and connect the block graphically
        field = new DES2D(100, 100);
        // add all the blocks
        field.add(source, 10, 50);
        field.add(lock, 30, 50);
//              field.add(pool, 30, 70);
        field.add(delay, 50, 50);
        field.add(unlock, 70, 50);
        field.add(sink, 90, 50);
        // connect the blocks
        field.connectAll();
        }

    public static void main(String[] args) {
        doLoop(MM1Queue.class, args);
        System.exit(0);
        }

    // Generic entity that will enter the queue
    class Generic extends Entity {

        private static final long serialVersionUID = -3093909727059005265L;

        public Generic(String name) {
            super(name);
            }

        }
    }
