package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CheckAvailabilityEvent;
import bgu.spl.mics.application.messages.TakeBookEvent;
import bgu.spl.mics.application.passiveObjects.Inventory;
import bgu.spl.mics.application.passiveObjects.OrderReceipt;
import bgu.spl.mics.application.passiveObjects.OrderResult;

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

	public InventoryService() {
		super("InventoryService");
		inventory.getInstance();
	}

	@Override
	protected void initialize() {
		subscribeEvent(CheckAvailabilityEvent.class, check ->{
				complete(check, inventory.checkAvailabiltyAndGetPrice(check.getBookTitle()));
		});

		subscribeEvent(TakeBookEvent.class, take ->{
			if (inventory.checkAvailabiltyAndGetPrice(take.getBookTitle()) > -1){
				inventory.take(take.getBookTitle());
				complete(take, OrderResult.SUCCESSFULLY_TAKEN);
			}
			else{
				complete(take, OrderResult.NOT_IN_STOCK);
			}
		});

	}
}
