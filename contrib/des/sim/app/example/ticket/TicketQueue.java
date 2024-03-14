package sim.app.example.ticket;

import sim.des.Delay;
import sim.des.Entity;
import sim.des.Queue;
import sim.des.SimpleDelay;
import sim.des.Sink;
import sim.des.Source;
import sim.des.portrayal.DES2D;
import sim.engine.Schedule;
import sim.engine.SimState;

/**
 * @author giuseppe
 * 
 *         A queue in front of a ticket booth
 * 
 *         Problem statement: You work for a concert promoter, and your manager
 *         wants to hold an hour‐long flash sale to sell the remaining seats for
 *         an upcoming concert. Now, you need to figure out the number of sales
 *         personnel you’ll need. There’s no space limitation in front of the
 *         ticket booth, so the queue length isn’t a concern. Previous sales
 *         tell you an average of 2 customers will join the queue each second.
 *         However, the low customer satisfaction that long queues often cause
 *         leads your manager to decide there should be no more than 100 people
 *         in the queue at one time.
 * 
 *         entrance (Source) -> line (Queue) -> serving (SimpleDelay) -> done (Sink)
 */

public class TicketQueue extends SimState {

    private static final long serialVersionUID = -1571451586552112368L;

    public DES2D field = new DES2D(100, 100);

    public TicketQueue(long seed) {
        super(seed);
        }

    public void start() {
        super.start();
                
        // basic entity representing a customer
        Person person = new Person("Customer");

        // entrance where people enter
        Source entrance = new Source(this, person);

        // set the arrival rate at 2 customer per step
        entrance.setRate(1.0);
        entrance.setProduction(2.0);
        entrance.setAutoSchedules(true);
        schedule.scheduleOnce(entrance);

        // queue in front of the ticket booth
        Queue line = new Queue(this, person);

        // set the queue capacity to infinite
        line.setCapacity(Double.POSITIVE_INFINITY);
        schedule.scheduleRepeating(line);
                
                
        // delay simulating the time needed for buying a ticket
        SimpleDelay serving = new Delay(this, person);

        // set the delay time 
        serving.setDelayTime(3.0);
                
        // set the delay's capacity to 1; only one customer at a time can be served
        serving.setCapacity(1.0);
        serving.setAutoSchedules(true);
        schedule.scheduleOnce(Schedule.EPOCH, serving);

        // sink simulating the purchase of the ticket
        Sink done = new Sink(this, person);

        // hook things together
        entrance.addReceiver(line);
        line.addReceiver(serving);
        serving.addReceiver(done);
                
                
        // set up network for display purpose
        field = new DES2D(100, 100);
        field.add(entrance, 10, 50);
        field.add(line, 30, 50);
        field.add(serving, 50, 50);
        field.add(done, 70, 50);

        field.connectAll();


        }

    public static void main(String[] args) {
        doLoop(TicketQueue.class, args);

        System.exit(0);
        }

    public class Person extends Entity {

        private static final long serialVersionUID = 174834329441644719L;

        public Person(String name) {
            super(name);
            }
        }
    }
