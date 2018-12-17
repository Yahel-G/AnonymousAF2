package bgu.spl.mics.application.services;

import bgu.spl.mics.Future;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.BookStoreRunner;
import bgu.spl.mics.application.messages.AcquireVehicleEvent;
import bgu.spl.mics.application.messages.DeliveryEvent;
import bgu.spl.mics.application.messages.FreeVehicleEvent;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.passiveObjects.DeliveryVehicle;
import bgu.spl.mics.application.passiveObjects.Inventory;
import bgu.spl.mics.application.passiveObjects.MoneyRegister;
import bgu.spl.mics.application.passiveObjects.ResourcesHolder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * Logistic service in charge of delivering books that have been purchased to customers.
 * Handles {@link DeliveryEvent}.
 * This class may not hold references for objects which it is not responsible for:
 * {@link ResourcesHolder}, {@link MoneyRegister}, {@link Inventory}.
 * 
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
public class LogisticsService extends MicroService {
	private Semaphore locker = new Semaphore(1, true);

	public LogisticsService(String name) {
		super(name);

	}

	@Override
	protected void initialize() {
		System.out.println(getName() + " has initialized"); // todo remove

		subscribeBroadcast(TickBroadcast.class, clock -> {
			System.out.println(" --- Tick #" +Integer.toString(clock.giveMeSomeTime()) +"# received in service " +getName() + " ---"); // todo remove

			if (clock.getTimeOfDeath() == clock.giveMeSomeTime()) {
				BookStoreRunner.latch2.countDown();
				terminate();
			}
		});

		subscribeEvent(DeliveryEvent.class, delivery ->{
			// this function uses future within a future. the event acquireVehicle is needed for the deliveryEvent
			// the logistic service is doing.  and the acquireVehicle Event has it own future.
			// the line of calling is - delivery creates future - waiting for a vehicle to complete the delivery from the
			// resource service. the resource service need another future - waiting to get a vehicle from the resources holder.
			boolean completed = false;
			Future<Future<DeliveryVehicle>> FutureDeliveryVehicleFuture = sendEvent(new AcquireVehicleEvent());
			if(FutureDeliveryVehicleFuture != null){
				Future<DeliveryVehicle> dev = FutureDeliveryVehicleFuture.get(); // resource service is done, and we are trying to get the future he is holding.
				if (dev != null){
					DeliveryVehicle taxi = dev.get(); // if there is a vehicle at the ready we got into the 2nd future to get that vehicle.
					if (taxi != null){
						taxi.deliver(delivery.getAddress(), delivery.getDistance()); // going on delivery with ease.
						complete(delivery, true);
						completed = true;
						System.out.println(getName() + " sent a Free Vehicle Event"); // todo remove
						sendEvent(new FreeVehicleEvent(taxi));
					}else { // there is no vehicle in the 2nd future.
						complete(delivery, false);
						completed = true;
					}
				 }else { // there is no 2nd future - he is null
					complete(delivery, false);
					completed = true;
				}
			}else{ //1st future isn't resolve - he is null.
				complete(delivery, false);
				completed = true;
			}
if (!completed){
	complete(delivery, false);

}
		});
		BookStoreRunner.latch.countDown();

	}

}
