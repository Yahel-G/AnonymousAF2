package bgu.spl.mics.application.services;

import bgu.spl.mics.Future;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.AcquireVehicleEvent;
import bgu.spl.mics.application.messages.FreeVehicleEvent;
import bgu.spl.mics.application.passiveObjects.DeliveryVehicle;
import bgu.spl.mics.application.passiveObjects.ResourcesHolder;

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
	public ResourceService() {
		super("SomeResourceService");
		holder.getInstance();
	}

	@Override
	protected void initialize() {
		subscribeEvent(AcquireVehicleEvent.class, getTaxi->{
			Future<DeliveryVehicle> taxi = holder.acquireVehicle();
			if (taxi.get() != null){
				complete(getTaxi, taxi);
			}
		});
		subscribeEvent(FreeVehicleEvent.class, free ->{
			holder.releaseVehicle(free.getVehicle());
		});
	}

}
