package bgu.spl.mics.application.services;

import bgu.spl.mics.Future;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.BookOrderEvent;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.passiveObjects.Customer;
import bgu.spl.mics.application.passiveObjects.OrderPair;
import bgu.spl.mics.application.passiveObjects.OrderReceipt;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * APIService is in charge of the connection between a client and the store.
 * It informs the store about desired purchases using {@link BookOrderEvent}.
 * This class may not hold references for objects which it is not responsible for:
 * {@link ResourcesHolder}, {@link MoneyRegister}, {@link Inventory}.
 * 
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
/*
 * I want to send a book order event with relevant book when the time service tells me it's the right time
 * according to the tick that came with the book.
 *
 */


public class APIService extends MicroService{

	private Customer daCustomer;
	private HashMap<Integer, Vector<String>> scheduler;
	private int theTime;
	private Vector<Future<OrderReceipt>> futReceipts;
	private Vector<OrderReceipt> actualReceipts;

	public APIService(String name ,Customer customer, List<OrderPair> orderSchedule) {
		super(name);
		daCustomer = customer;
		theTime = -1;
		futReceipts = new Vector<>();
		actualReceipts = new Vector<>();
		scheduler = new HashMap<>();
		for (OrderPair OP: orderSchedule){
			if(scheduler.containsKey(OP.getTick())){	// if the scheduler already has a book in this time tick
				scheduler.get(OP.getTick()).add(OP.getBookTitle());	// add this book to the list of books to be ordered in this tick
			}
			else{
				Vector<String> temp = new Vector<>();
				temp.add(OP.getBookTitle());
				scheduler.put(OP.getTick(),temp);
			}
		}

		initialize();
	}

	@Override
	protected void initialize() {
		subscribeBroadcast(TickBroadcast.class, clock ->{
			if(clock.getTimeOfDeath() == clock.giveMeSomeTime()){
				terminate();
				System.out.println(getName() + " was terminated.");
			}
			theTime = clock.giveMeSomeTime();
			if (scheduler.containsKey(theTime)){
				for (String bookTitle: scheduler.get(theTime)){
					BookOrderEvent bookEvent = new BookOrderEvent(daCustomer, bookTitle, theTime);
					Future<OrderReceipt> receiptFuture= sendEvent(bookEvent);
					futReceipts.add(receiptFuture);// TODO  COMPLETE FUTURE??
				}
				scheduler.remove(theTime);	// cleanup
			}
			for (Future<OrderReceipt> futReceipt: futReceipts){
				if(futReceipt != null){ // is this the right condition?
					actualReceipts.add(futReceipt.get());
					futReceipts.remove(futReceipt);		// cleanup
				}
			}

		}); // end callback
	}

}
