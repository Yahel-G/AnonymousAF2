package bgu.spl.mics.application.services;

import bgu.spl.mics.Future;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.BookStoreRunner;
import bgu.spl.mics.application.messages.AcquireVehicleEvent;
import bgu.spl.mics.application.messages.FreeVehicleEvent;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.passiveObjects.DeliveryVehicle;
import bgu.spl.mics.application.passiveObjects.ResourcesHolder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * ResourceService is in charge of the store resources - the delivery vehicles.
 * Holds a reference to the {@link ResourceHolder} singleton of the store.
 * This class may not hold references for objects which it is not responsible for:
 * {@link MoneyRegister}, {@link Inventory}.
 * 
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
public class ResourceService extends MicroService{
	private ResourcesHolder holder;
	private Semaphore locker = new Semaphore(1, true);
	private Semaphore locker2 = new Semaphore(1, true);



	public ResourceService(String name) {
		super(name);
		holder = ResourcesHolder.getInstance();
	}

	@Override
	protected void initialize() {
		System.out.println(getName() + " has initialized"); // todo remove
		subscribeBroadcast(TickBroadcast.class, clock -> {


			System.out.println(" --- Tick #" +Integer.toString(clock.giveMeSomeTime()) +"# received in service " +getName() + " ---"); // todo remove
			if (clock.getTimeOfDeath() == clock.giveMeSomeTime()) {// save the futures and and resolve all futures with null if they're not resolved
				BookStoreRunner.latch2.countDown();
				terminate();
			}
		});
/*		try {
			locker.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}*/
		subscribeEvent(AcquireVehicleEvent.class, getTaxi->{
			System.out.println(getName() + " has received an AcquireVehicleEvent"); // todo remove
			Future<DeliveryVehicle> taxi = holder.acquireVehicle();
			//if (!taxi.isDone())
			//	.get() != null){ // deadlock because the service may wait for get() with no 1 to free a vehicle
				complete(getTaxi, taxi);
		//	}
		});
//		locker.release();

/*		try {
			locker2.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}*/
		subscribeEvent(FreeVehicleEvent.class, free ->{
			System.out.println(getName() + " has received a Free Vehicle Event"); // todo remove

			holder.releaseVehicle(free.getVehicle());
			complete(free, true);
		});
//		locker2.release();

		BookStoreRunner.latch.countDown();

	}

}
