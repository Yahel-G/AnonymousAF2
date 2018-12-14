package bgu.spl.mics.application.services;

import bgu.spl.mics.Future;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.AcquireVehicleEvent;
import bgu.spl.mics.application.messages.DeliveryEvent;
import bgu.spl.mics.application.messages.FreeVehicleEvent;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.passiveObjects.DeliveryVehicle;

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
				terminate();
			}
		});
/*		try {
			locker.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}*/
		subscribeEvent(DeliveryEvent.class, delivery ->{
			Future<Future<DeliveryVehicle>> FutureDeliveryVehicleFuture = sendEvent(new AcquireVehicleEvent());
			DeliveryVehicle taxi = FutureDeliveryVehicleFuture.get().get();
			if (taxi != null){
				taxi.deliver(delivery.getAddress(), delivery.getDistance());
				complete(delivery, true);
				System.out.println(getName() + " sent a Free Vehicle Event"); // todo remove

				sendEvent(new FreeVehicleEvent(taxi));

			}else {
				complete(delivery, false);
			}

		});
//		locker.release();

	}

}
