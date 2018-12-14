package bgu.spl.mics.application.services;

import bgu.spl.mics.Future;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.passiveObjects.Customer;
import bgu.spl.mics.application.passiveObjects.MoneyRegister;
import bgu.spl.mics.application.passiveObjects.OrderReceipt;
import bgu.spl.mics.application.passiveObjects.OrderResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * Selling service in charge of taking orders from customers.
 * Holds a reference to the {@link MoneyRegister} singleton of the store.
 * Handles {@link BookOrderEvent}.
 * This class may not hold references for objects which it is not responsible for:
 * {@link ResourcesHolder}, {@link Inventory}.
 * 
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
public class SellingService extends MicroService{
	private String bookTitle;
	private Customer customer;
	private MoneyRegister moneyRegister;
	private int theTimeNow;
	private Semaphore locker = new Semaphore(1);

	public SellingService(String name) {
		super(name);
		moneyRegister = MoneyRegister.getInstance();
	}

	@Override
	protected void initialize() {
		System.out.println(getName() + " has initialized"); // todo remove
		subscribeBroadcast(TickBroadcast.class, time->{
			System.out.println(" --- Tick #" +Integer.toString(time.giveMeSomeTime()) +"# received in service " +getName() + " ---"); // todo remove


			if (time.getTimeOfDeath() == time.giveMeSomeTime()) {
				terminate();
			}
			theTimeNow = time.giveMeSomeTime();
		});


		subscribeEvent(BookOrderEvent.class, seller ->{
			System.out.println(getName() + " has received a book order: " + seller.getBookTitle() +" for customer: " + seller.getCustomer().getName()); // todo remove
			bookTitle = seller.getBookTitle();
			customer = seller.getCustomer();

			int processedTick = theTimeNow;
			try {
				locker.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println(getName() + " is sending a CheckAvailabilityEvent: " + bookTitle + "for customer " + customer.getName()); // todo remove
			Future<Integer> bookPrice = sendEvent(new CheckAvailabilityEvent(bookTitle));
			int price = bookPrice.get();
			System.out.println(getName() + " CheckAvailabilityEvent for " + bookTitle + "Result:" + Integer.toString(price)); // todo remove
			if (price != -1){
				if(customer.getAvailableCreditAmount() >= price){	/// sync this
					Future<OrderResult> orderResultFuture = sendEvent(new TakeBookEvent(bookTitle));
					System.out.println(getName() + " is sending a TakeBookEvent: " + bookTitle); // todo remove
					if (orderResultFuture.get() == OrderResult.SUCCESSFULLY_TAKEN){
						System.out.println(getName() + " Successfully taken " + bookTitle); // todo remove
						moneyRegister.chargeCreditCard(customer, price);
				//		locker.release();
						OrderReceipt receipt = new OrderReceipt(customer.getId(), getName(), bookTitle, price, theTimeNow, seller.getOrderedTick(), processedTick);
						System.out.println(getName() + " produced a receipt for  " + customer.getName() + receipt.toString()); // todo remove
						moneyRegister.file(receipt);
						sendEvent(new DeliveryEvent(customer.getDistance(), customer.getAddress()));
						System.out.println(getName() + " is sending a DeliveryEvent for customer " + customer.getName()); // todo remove
						complete(seller, receipt);
					}
					else{
						complete(seller, null);
			//			locker.release();
					}
				}
				else{
					complete(seller, null);
				}

			}
			else{
				complete(seller, null);
			}
			locker.release();

		});

	}

}
