package bgu.spl.mics.application.services;

import bgu.spl.mics.Future;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.AcquireVehicleEvent;
import bgu.spl.mics.application.messages.DeliveryEvent;
import bgu.spl.mics.application.messages.FreeVehicleEvent;
import bgu.spl.mics.application.passiveObjects.DeliveryVehicle;

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

	public LogisticsService(String name) {
		super(name);
	}

	@Override
	protected void initialize() {
		subscribeEvent(DeliveryEvent.class, delivery ->{
			Future<Future<DeliveryVehicle>> FutureDeliveryVehicleFuture = sendEvent(new AcquireVehicleEvent(delivery.getDistance(), delivery.getAddress()));
			FutureDeliveryVehicleFuture.get().get().deliver(delivery.getAddress(), delivery.getDistance());
			complete(delivery, true);
			sendEvent(new FreeVehicleEvent(FutureDeliveryVehicleFuture.get().get()));
		});

	}

}
