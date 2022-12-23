package sim.app.example.streetfood;

import java.util.ArrayList;

import sim.des.CountableResource;
import sim.des.Entity;
import sim.des.If;
import sim.des.Lock;
import sim.des.Pool;
import sim.des.Queue;
import sim.des.Receiver;
import sim.des.Resource;
import sim.des.SimpleDelay;
import sim.des.Sink;
import sim.des.Source;
import sim.des.Unlock;
import sim.des.portrayal.DES2D;
import sim.engine.SimState;
import sim.util.distribution.Exponential;

/**
 * @author giuseppe Example from Kristiansen et al (2022) "Experimental
 *         Comparison of Open Source Discrete-Event Simulation Frameworks."
 * 
 *         The model emulates a street food like scenario.
 * 
 *         Execution flow: 
 *         -1 The customer (entity) arrives with an exponential distribution; 
 *         -2 Customers queue for a table (limited resource); 
 *         -3 Customers decide to order A=food (p=0.6) or B=drink (p=0.4); 
 *         -4A.1 Customers distribute over three food stands: Pizza (p=0.33), 
 *         Burger (p=0.33), Chinese (p=0.33); 
 *         -4A.2 Customers queue for food (limited resource); 
 *         -4B.1 Customers queue for drink (limited resource);
 *         -4B.2 Customer decide to order A=also food (p=0.5) or B=only drink (p=0.5); 
 *         -4B.3A Return to 4A.1;
 *         -4B.3B Go to 5; 
 *         -5 Customer decide to A=order again (p=0.3) or B=leave (p=0.7); 
 *         -6A Return to 3; 
 *         -6B Go to 7; 
 *         -7 Customers queue for checkout (limited resource) and leave;
 * 
 */

public class StreetFood extends SimState {
	private static final long serialVersionUID = 3555311977425471226L;

	// Space where blocks will be drawn
	public DES2D field = new DES2D(450, 450);

	public StreetFood(long seed) {
		super(seed);
	}

	public void start() {
		super.start();

		// Entity representing a customer
		Customer customerE = new Customer("GenericCustomer");

		// Customers arrival (1)
		// source generating customers
		Source entranceS = new Source(this, customerE);
		// customers arrive at exponential rate
		Exponential expTime = new Exponential(15, random);
		entranceS.setRateDistribution(expTime);
		// set the amount of customer that arrives each time
		Exponential expAmt = new Exponential(5, random);
		entranceS.setProductionDistribution(expAmt);
//		entrance.setProduction(1);
		entranceS.setAutoSchedules(true);
		schedule.scheduleOnce(entranceS);

		// the customer enters the queue to get a table (2)
		Queue tableQ = new Queue(this, customerE);
		// pool of resource representing tables
		Pool tableP = new Pool(new CountableResource("table", 100),100);
		// the customer try to get a table
		Lock getTable = new Lock(this, customerE, tableP, 1);
		// block for when customer leaves to release the table
		Unlock releaseTable = new Unlock(getTable);
		// the customer goes to the table and choose what to order
		// capacity of the Delay is irrelevant since tables are limited
		SimpleDelay orderChoice = new SimpleDelay(this, 5, customerE);
		// from the entrance the customer is send to the queue
		// waiting for a table
		entranceS.addReceiver(tableQ);
		// when a table is available
		tableQ.addReceiver(getTable);
		// the customer choose what to order
		getTable.addReceiver(orderChoice);
		
		// we need to model other parts of the system
		// to be able to build the If blocks
		
		// available food stands where customers wait to be served (4A.2)
		// pizza stand
		Queue pizzaQ = new Queue(this, customerE);
		Pool pizzaP = new Pool(new CountableResource("pizza-maker", 3), 3);
		Lock getPizza = new Lock(this, customerE, pizzaP, 1);
		SimpleDelay makePizza = new SimpleDelay(this, 5, customerE);
		Unlock releasePizza = new Unlock(getPizza);
		SimpleDelay eatingPizza = new SimpleDelay(this, 5, customerE);
		// connect (same behavior as before to acquire limited resource)
		pizzaQ.addReceiver(getPizza);
		getPizza.addReceiver(makePizza);
		makePizza.addReceiver(releasePizza);
		releasePizza.addReceiver(eatingPizza);

		// burger stand
		Queue burgerQ = new Queue(this, customerE);
		Pool burgerP = new Pool(new CountableResource("burger-maker", 3), 3);
		Lock getBurger = new Lock(this, customerE, burgerP, 1);
		SimpleDelay makeBurger = new SimpleDelay(this, 5, customerE);
		Unlock releaseBurger = new Unlock(getBurger);
		SimpleDelay eatingBurger = new SimpleDelay(this, 5, customerE);
		// connect (same behavior as before to acquire limited resource)
		burgerQ.addReceiver(getBurger);
		getBurger.addReceiver(makeBurger);
		makeBurger.addReceiver(releaseBurger);
		releaseBurger.addReceiver(eatingBurger);

		// chinese stand
		Queue chineseQ = new Queue(this, customerE);
		Pool chineseP = new Pool(new CountableResource("chinese-maker", 3), 3);
		Lock getChinese = new Lock(this, customerE, chineseP, 1);
		SimpleDelay makeChinese = new SimpleDelay(this, 5, customerE);
		Unlock releaseChinese = new Unlock(getChinese);
		SimpleDelay eatingChinese = new SimpleDelay(this, 5, customerE);
		// connect (same behavior as before to acquire limited resource)
		chineseQ.addReceiver(getChinese);
		getChinese.addReceiver(makeChinese);
		makeChinese.addReceiver(releaseChinese);
		releaseChinese.addReceiver(eatingChinese);

		// drink stand where customer wait to be served (4B.1)
		Queue drinkQ = new Queue(this, customerE);
		Pool drinkP = new Pool(new CountableResource("drink-maker", 3), 3);
		Lock getDrink = new Lock(this, customerE, drinkP, 1);
		SimpleDelay makeDrink = new SimpleDelay(this, 5, customerE);
		Unlock releaseDrink = new Unlock(getDrink);
		SimpleDelay drinking = new SimpleDelay(this, 5, customerE);
		// connect (same behavior as before to acquire limited resource)
		drinkQ.addReceiver(getDrink);
		getDrink.addReceiver(makeDrink);
		makeDrink.addReceiver(releaseDrink);
		releaseDrink.addReceiver(drinking);

		// the customer leaves the system releasing the table (7)
		Queue checkoutQ = new Queue(this, customerE);
		Pool checkoutP = new Pool(new CountableResource("cashier", 3), 3);
		Lock getCashier = new Lock(this, customerE, checkoutP, 1);
		SimpleDelay checkoutT = new SimpleDelay(this, 5, customerE);
		Unlock releaseCashier = new Unlock(getCashier);
		Sink exit = new Sink(this, customerE);
		// connect (same behavior as before to acquire limited resource)
		// before checkout the customer release the table
		releaseTable.addReceiver(checkoutQ);
		checkoutQ.addReceiver(getCashier);
		getCashier.addReceiver(checkoutT);
		checkoutT.addReceiver(releaseCashier);
		releaseCashier.addReceiver(exit);
		
		// customer choose the food stand (4A.1)
		If foodChoice = new If(this, customerE) {
			private static final long serialVersionUID = -838320681827301421L;

			public Receiver selectReceiver(ArrayList<Receiver> receivers, Resource resource) {
				if (receivers.size() == 0)
					throw new IllegalArgumentException("No receivers!");
				
				Receiver chosen = null;
				double rng = random.nextDouble(true, true);
				
				if (rng <= 0.33) { // go Pizza
					chosen = receivers.get(receivers.indexOf(pizzaQ));
				} else if (rng > 0.33 && rng <= 0.66) { // go Burger
					chosen = receivers.get(receivers.indexOf(burgerQ));
				} else { // go Chinese
					chosen = receivers.get(receivers.indexOf(chineseQ));
				}
				
				if (chosen == null)
					throw new IllegalArgumentException(
							"Something wrong: chose null out of " + receivers.size() + " receivers!");
				
				return chosen;
			}
		};
		// connect with the stand queues
		foodChoice.addReceiver(pizzaQ);
		foodChoice.addReceiver(burgerQ);
		foodChoice.addReceiver(chineseQ);

		// the customer chooses between if eat or drink (3)
		If eatOrDrink = new If(this, customerE) {
			private static final long serialVersionUID = -2417594310685445026L;

			public Receiver selectReceiver(ArrayList<Receiver> receivers, Resource resource) {
				if (receivers.size() == 0)
					throw new IllegalArgumentException("No receivers!");
				
				Receiver chosen = null;
				double rng = random.nextDouble(true, true);
				
				if (rng <= 0.6) { // go eat
					chosen = receivers.get(receivers.indexOf(foodChoice));
				} else { // go drink
					chosen = receivers.get(receivers.indexOf(drinkQ));
				}

				if (chosen == null)
					throw new IllegalArgumentException(
							"Something wrong: chose null out of " + receivers.size() + " receivers!");

				return chosen;
			}
		};
		// connect
		eatOrDrink.addReceiver(foodChoice);
		eatOrDrink.addReceiver(drinkQ);
		
		orderChoice.addReceiver(eatOrDrink);

		// customer choose to order again or leave (5)
		If leaveOrReorder = new If(this, customerE) {
			private static final long serialVersionUID = -2417594310685445026L;

			public Receiver selectReceiver(ArrayList<Receiver> receivers, Resource resource) {
				
				if (receivers.size() == 0)
					throw new IllegalArgumentException("No receivers!");
				
				Receiver chosen = null;
				double rng = random.nextDouble(true, true);

				if (rng <= 0.7) { // release the table and leave
					chosen = receivers.get(receivers.indexOf(releaseTable));
				} else { // choose again if eat or drink
					chosen = receivers.get(receivers.indexOf(eatOrDrink));
				}

				if (chosen == null)
					throw new IllegalArgumentException(
							"Something wrong: chose null out of " + receivers.size() + " receivers!");
							
				return chosen;
			}
		};
		// connect
		leaveOrReorder.addReceiver(releaseTable);
		leaveOrReorder.addReceiver(eatOrDrink);
		
		eatingPizza.addReceiver(leaveOrReorder);
		eatingBurger.addReceiver(leaveOrReorder);
		eatingChinese.addReceiver(leaveOrReorder);
		drinking.addReceiver(leaveOrReorder);
		
		// after drink the customer choose to order also food or not (4B.2)
		If alsoFood = new If(this, customerE) {
			private static final long serialVersionUID = 7741417186693871255L;

			public Receiver selectReceiver(ArrayList<Receiver> receivers, Resource resource) {
				
				if (receivers.size() == 0)
					throw new IllegalArgumentException("No receivers!");
				
				Receiver chosen = null;
				double rng = random.nextDouble(true, true);

				if (rng < 0.5) { // go order food
					chosen = receivers.get(receivers.indexOf(foodChoice));
				} else { // go decide if order again
					chosen = receivers.get(receivers.indexOf(leaveOrReorder));
				}

				if (chosen == null)
					throw new IllegalArgumentException(
							"Something wrong: chose null out of " + receivers.size() + " receivers!");
				return chosen;
			}
		};
		alsoFood.addReceiver(foodChoice);
		alsoFood.addReceiver(leaveOrReorder);

		// draw and connect the block graphically
		field = new DES2D(450, 450);
		field.add(entranceS, 15, 320);
		entranceS.setName("Entrance");
		field.add(tableQ, 15, 270);
		tableQ.setName("TableQueue");
		tableP.setName("TablePool");
		field.add(getTable, 15, 220);
		getTable.setName("GetTable");
		field.add(orderChoice, 15, 170);
		orderChoice.setName("Choosing");
		
		field.add(eatOrDrink, 55, 170);
		eatOrDrink.setName("EatOrDrink");
		
		field.add(foodChoice, 110, 170);
		foodChoice.setName("FoodChoice");
		
		field.add(pizzaQ, 140, 70);
		pizzaQ.setName("PizzaQueue");
		pizzaP.setName("PizzaPool");
		field.add(getPizza, 190, 70);
		getPizza.setName("GetPizza");
		field.add(makePizza, 240, 70);
		makePizza.setName("MakePizza");
		field.add(releasePizza, 265, 20);
		releasePizza.setName("ReleasePizza");
		field.add(eatingPizza, 290, 70);
		eatingPizza.setName("EatingPizza");
		
		field.add(burgerQ, 140, 170);
		burgerQ.setName("BurgerQueue");
		burgerP.setName("BurgerPool");
		field.add(getBurger, 190, 170);
		getBurger.setName("GetBurger");
		field.add(makeBurger, 240, 170);
		makeBurger.setName("MakeBurger");
		field.add(releaseBurger, 265, 120);
		releaseBurger.setName("ReleaseBurger");
		field.add(eatingBurger, 290, 170);
		eatingBurger.setName("EatingBurger");
		
		field.add(chineseQ, 140, 270);
		chineseQ.setName("ChineseQueue");
		chineseP.setName("ChinesePool");
		field.add(getChinese, 190, 270);
		getChinese.setName("GetChinese");
		field.add(makeChinese, 240, 270);
		makeChinese.setName("MakeChinese");
		field.add(releaseChinese, 265, 220);
		releaseChinese.setName("ReleaseChinese");
		field.add(eatingChinese, 290, 270);
		eatingChinese.setName("EatingChinese");
		
		field.add(drinkQ, 140, 370);
		drinkQ.setName("DrinkQueue");
		drinkP.setName("DrinkPool");
		field.add(getDrink, 190, 370);
		getDrink.setName("GetDrink");
		field.add(makeDrink, 240, 370);
		makeDrink.setName("MakeDrink");
		field.add(releaseDrink, 265, 320);
		releaseDrink.setName("ReleaseDrink");
		field.add(drinking, 290, 370);
		drinking.setName("Drinking");
		
		field.add(leaveOrReorder, 350, 70);
		leaveOrReorder.setName("LeaveOrReorder");
		field.add(releaseTable, 390, 120);
		releaseTable.setName("ReleaseTable");
		
		field.add(checkoutQ, 390, 170);
		checkoutQ.setName("CheckoutQueue");
		field.add(getCashier, 390, 220);
		getCashier.setName("GetCashier");
		checkoutP.setName("CashierPool");
		field.add(checkoutT, 390, 270);
		checkoutT.setName("CheckingOut");
		field.add(releaseCashier, 390, 320);
		releaseCashier.setName("ReleaseCashier");
		field.add(exit, 390, 370);
		exit.setName("Exit");
		
		field.connectAll();
	}

	public static void main(String[] args) {
		doLoop(StreetFood.class, args);
		System.exit(0);
	}

	class Customer extends Entity {
		private static final long serialVersionUID = -1161909723116387894L;

		public Customer(String name) {
			super(name);
		}
	}

}
