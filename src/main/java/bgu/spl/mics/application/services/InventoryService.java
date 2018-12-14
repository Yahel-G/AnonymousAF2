package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CheckAvailabilityEvent;
import bgu.spl.mics.application.messages.TakeBookEvent;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.passiveObjects.Inventory;
import bgu.spl.mics.application.passiveObjects.OrderReceipt;
import bgu.spl.mics.application.passiveObjects.OrderResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * InventoryService is in charge of the book inventory and stock.
 * Holds a reference to the {@link Inventory} singleton of the store.
 * This class may not hold references for objects which it is not responsible for:
 * {@link ResourcesHolder}, {@link MoneyRegister}.
 * 
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */

public class InventoryService extends MicroService{
	private Inventory inventory = null;
	private Semaphore locker = new Semaphore(1);


	public InventoryService(String name) {
		super(name);
		inventory = Inventory.getInstance();

	}

	@Override
	protected void initialize() {
		System.out.println(getName() + " has initialized"); // todo remove

		subscribeBroadcast(TickBroadcast.class, clock -> {
			if (clock.getTimeOfDeath() == clock.giveMeSomeTime()) {
				terminate();
			}
		});

		subscribeEvent(CheckAvailabilityEvent.class, check ->{
			try {
				locker.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			int thePrice = inventory.checkAvailabiltyAndGetPrice(check.getBookTitle());
				complete(check, thePrice);
			locker.release();
		});

		subscribeEvent(TakeBookEvent.class, take ->{
			Semaphore locker = new Semaphore(1);
			try {
				locker.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (inventory.checkAvailabiltyAndGetPrice(take.getBookTitle()) > -1){
				inventory.take(take.getBookTitle());
				complete(take, OrderResult.SUCCESSFULLY_TAKEN);
			}
			else{
				complete(take, OrderResult.NOT_IN_STOCK);
			}
			locker.release();
		});


	}
}
