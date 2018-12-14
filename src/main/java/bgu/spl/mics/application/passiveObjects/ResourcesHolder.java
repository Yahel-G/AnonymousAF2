package bgu.spl.mics.application.passiveObjects;

import bgu.spl.mics.Future;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Passive object representing the resource manager.
 * You must not alter any of the given public methods of this class.
 * <p>
 * This class must be implemented safely as a thread-safe singleton.
 * You must not alter any of the given public methods of this class.
 * <p>
 * You can add ONLY private methods and fields to this class.
 */
public class ResourcesHolder {

	private static class singletonHolder{
		private static ResourcesHolder Holder = new ResourcesHolder();
	}

	private LinkedBlockingQueue<DeliveryVehicle> FleetAvailable;
	private LinkedBlockingQueue<DeliveryVehicle> FleetBusy;
	private LinkedBlockingQueue<Future<DeliveryVehicle>> waitingInLine;



	private ResourcesHolder(){

		FleetAvailable = new LinkedBlockingQueue<>();
		FleetBusy = new LinkedBlockingQueue<>();
		waitingInLine = new LinkedBlockingQueue<>();
	}

		/**
     * Retrieves the single instance of this class.
     */
	public static ResourcesHolder getInstance() {
		return singletonHolder.Holder;
	}
	
	/**
     * Tries to acquire a vehicle and gives a future object which will
     * resolve to a vehicle.
     * <p>
     * @return 	{@link Future<DeliveryVehicle>} object which will resolve to a 
     * 			{@link DeliveryVehicle} when completed.   
     */
	public synchronized Future<DeliveryVehicle> acquireVehicle() {
		Future<DeliveryVehicle> ret = new Future<>();
		DeliveryVehicle taxi = FleetAvailable.poll();
		if (taxi != null) {
			ret.resolve(taxi);
		}else {
			waitingInLine.add(ret);
		}
		return ret;
	}
	
	/**
     * Releases a specified vehicle, opening it again for the possibility of
     * acquisition.
     * <p>
     * @param vehicle	{@link DeliveryVehicle} to be released.
     */
	public synchronized void releaseVehicle(DeliveryVehicle vehicle) {
		if(!waitingInLine.isEmpty()){
			waitingInLine.poll().resolve(vehicle);
		}else{
			FleetAvailable.add(vehicle);
		}
	}
	
	/**
     * Receives a collection of vehicles and stores them.
     * <p>
     * @param vehicles	Array of {@link DeliveryVehicle} instances to store.
     */
	public void load(DeliveryVehicle[] vehicles) {
		for (int i = 0; i < vehicles.length; i++){
			FleetAvailable.add(vehicles[i]);
		}
	}

}
