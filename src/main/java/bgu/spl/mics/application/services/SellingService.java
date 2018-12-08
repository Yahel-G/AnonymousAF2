package bgu.spl.mics.application.services;

import bgu.spl.mics.Future;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.passiveObjects.Customer;
import bgu.spl.mics.application.passiveObjects.MoneyRegister;
import bgu.spl.mics.application.passiveObjects.OrderReceipt;
import bgu.spl.mics.application.passiveObjects.OrderResult;

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

	public SellingService(String name) {
		super(name);
		moneyRegister.getInstance();
	}

	@Override
	protected void initialize() {
		subscribeBroadcast(TickBroadcast.class, time->{
			theTimeNow = time.giveMeSomeTime();
		});

		subscribeEvent(BookOrderEvent.class, seller ->{
			bookTitle = seller.getBookTitle();
			customer = seller.getCustomer();
			int processedTick = theTimeNow;
			Future<Integer> bookPrice = sendEvent(new CheckAvailabilityEvent(bookTitle));
			if (bookPrice.get() != -1){
				if(customer.getAvailableCreditAmount() >= bookPrice.get()){	/// sync this
					Future<OrderResult> orderResultFuture = sendEvent(new TakeBookEvent(bookTitle));
					if (orderResultFuture.get() == OrderResult.SUCCESSFULLY_TAKEN){
						moneyRegister.chargeCreditCard(customer, bookPrice.get());
						sendEvent(new DeliveryEvent(customer.getDistance(), customer.getAddress()));
						complete(seller, new OrderReceipt(customer.getId(), getName(), bookTitle, bookPrice.get(), theTimeNow, seller.getOrderedTick(), processedTick));
					}
					else{
						complete(seller, null); // is this necessary? if it is, need to add more of these here maybe.
					}
				}

			}
		});

	}

}
