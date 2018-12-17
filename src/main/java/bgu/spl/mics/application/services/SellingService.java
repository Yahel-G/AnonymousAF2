package bgu.spl.mics.application.services;

import bgu.spl.mics.Future;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.BookStoreRunner;
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
		subscribeBroadcast(TickBroadcast.class, time->{
			if (time.getTimeOfDeath() == time.giveMeSomeTime()) {
				BookStoreRunner.latch2.countDown();
				terminate();
			}
			theTimeNow = time.giveMeSomeTime();
		});


		subscribeEvent(BookOrderEvent.class, seller ->{
			bookTitle = seller.getBookTitle();
			customer = seller.getCustomer();
			int processedTick = theTimeNow;
			try {
				locker.acquire();

			Future<Integer> bookPrice = sendEvent(new CheckAvailabilityEvent(bookTitle));
			if (bookPrice != null){
				int price = bookPrice.get();
				if (price != -1){
					if(customer.getAvailableCreditAmount() >= price){	/// sync this
						Future<OrderResult> orderResultFuture = sendEvent(new TakeBookEvent(bookTitle));
						if (orderResultFuture.get() == OrderResult.SUCCESSFULLY_TAKEN){
							moneyRegister.chargeCreditCard(customer, price);
							OrderReceipt receipt = new OrderReceipt(customer.getId(), getName(), bookTitle, price, theTimeNow, seller.getOrderedTick(), processedTick);
							moneyRegister.file(receipt);
							sendEvent(new DeliveryEvent(customer.getDistance(), customer.getAddress()));
							complete(seller, receipt);
						}
						else{
							complete(seller, null);
						}
					}
					else{
						complete(seller, null);
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
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		});
		BookStoreRunner.latch.countDown();

	}

}
