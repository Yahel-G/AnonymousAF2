package bgu.spl.mics;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;


/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus interface.
 * Write your implementation here!
 * Only private fields and methods can be added to this class.
 */
public class MessageBusImpl implements MessageBus {

	private static class singletonHolder{
		private static MessageBusImpl disBus = new MessageBusImpl();
	}


	// add a queue or something to hold all the event subscribers
	private ConcurrentHashMap<Class<? extends Message>,ConcurrentLinkedQueue<MicroService>> Events;
	private ConcurrentHashMap<Class<? extends Broadcast>,ConcurrentLinkedQueue<MicroService>> Broadcasts;
	private ConcurrentHashMap<MicroService, LinkedBlockingQueue<Message>> microServices;
	private ConcurrentHashMap<Message, Future> FuturesMap;
	private ConcurrentHashMap<Class<? extends Message>, Semaphore> locks;
	private Semaphore broadcastSemaphore = new Semaphore(1, true); // todo maybe don't hard code it - do it like events



	// make constructor
	private MessageBusImpl(){
		Events = new ConcurrentHashMap<>();
		Broadcasts = new ConcurrentHashMap<>();
		microServices = new ConcurrentHashMap<>();
		FuturesMap = new ConcurrentHashMap<>();
		locks = new ConcurrentHashMap<>();
/*
		locks.put(AcquireVehicleEvent.class, new Semaphore(1, true));
		locks.put(BookOrderEvent.class, new Semaphore(1,true));
		locks.put(CheckAvailabilityEvent.class, new Semaphore(1,true));
		locks.put(DeliveryEvent.class, new Semaphore(1,true));
		locks.put(FreeVehicleEvent.class, new Semaphore(1,true));
		locks.put(TakeBookEvent.class, new Semaphore(1,true));
*/

	}


	public static MessageBusImpl getInstance(){
		return singletonHolder.disBus;
	}

	/**
	 * Subscribes {@code m} to receive {@link Event}s of type {@code type}.
	 * <p>
	 * @param <T>  The type of the result expected by the completed event.
	 * @param type The type to subscribe to,
	 * @param m    The subscribing micro-service.
	 */
	@Override
	public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
		addLock(type);
		Semaphore semaphore = locks.get(type); // semaphore implement as synchronization - with 1 permit.
		try {
			semaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if(Events.containsKey(type)){
			Events.get(type).add(m);
		}
		else{
			ConcurrentLinkedQueue<MicroService> newQ = new ConcurrentLinkedQueue<>();
			newQ.add(m);
			Events.put(type, newQ);
		}
		semaphore.release();

	}

	/**
	 * Subscribes {@code m} to receive {@link Broadcast}s of type {@code type}.
	 * <p>
	 * @param type 	The type to subscribe to.
	 * @param m    	The subscribing micro-service.
	 */
	@Override
	public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
		try {
			broadcastSemaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if(Broadcasts.containsKey(type)){
			Broadcasts.get(type).add(m);
		}
		else{
			ConcurrentLinkedQueue<MicroService> newQ = new ConcurrentLinkedQueue<>();
			newQ.add(m);
			Broadcasts.put(type, newQ);
		}
		broadcastSemaphore.release();

	}

	/**
	 * Notifies the MessageBus that the event {@code e} is completed and its
	 * result was {@code result}.
	 * When this method is called, the message-bus will resolve the {@link Future}
	 * object associated with {@link Event} {@code e}.
	 * <p>
	 * @param <T>    The type of the result expected by the completed event.
	 * @param e      The completed event.
	 * @param result The resolved result of the completed event.
	 */
	@Override
	public  <T> void complete(Event<T> e, T result) {
		Future fut = FuturesMap.get(e);
		fut.resolve(result);
	}

	/**
	 * Adds the {@link Broadcast} {@code b} to the message queues of all the
	 * micro-services subscribed to {@code b.getClass()}.
	 * <p>
	 * @param b 	The message to added to the queues.
	 */
	@Override
	public void sendBroadcast(Broadcast b) {

		ConcurrentLinkedQueue<MicroService> sendTo = Broadcasts.get(b.getClass());
		for (MicroService m: sendTo){ //for each micro service subscribed
			try {
				microServices.get(m).put(b);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}


	}


	/**
	 * Adds the {@link Event} {@code e} to the message queue of one of the
	 * micro-services subscribed to {@code e.getClass()} in a round-robin
	 * fashion. This method should be non-blocking.
	 * <p>
	 * @param <T>    	The type of the result expected by the event and its corresponding future object.
	 * @param e     	The event to add to the queue.
	 * @return {@link Future<T>} object to be resolved once the processing is complete,
	 * 	       null in case no micro-service has subscribed to {@code e.getClass()}.
	 */
	@Override
	public <T> Future<T> sendEvent(Event<T> e) {
		addLock(e.getClass());
		Semaphore locker = locks.get(e.getClass());
		try {
			locker.acquire();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
		System.out.println("Send Event "+e.toString()+" Initiated"); //todo remove
		ConcurrentLinkedQueue<MicroService> queue;
		Future<T> fut = null;
		MicroService service;
		if(Events.get(e.getClass()) != null){
			// doing a round robin impl , takes the first services in the matching queue, add the event to that service,
			// and adding the services back, at the back of the queue
			fut = new Future<T>();
			FuturesMap.put(e, fut);
			queue = Events.get(e.getClass());
			service = queue.poll();
			queue.add(service);
			microServices.get(service).add(e);
			System.out.println("Send Event "+e.toString()+" added to FuturesMap" + '\n' + FuturesMap.get(e).toString()); //todo remove

		}

		System.out.println("Send Event "+e.toString()+" Completed"); //todo remove
		locker.release();
		return fut;
	}

	/**
	 * Allocates a message-queue for the {@link MicroService} {@code m}.
	 * <p>
	 * @param m the micro-service to create a queue for.
	 */
	@Override
	public void register(MicroService m) {
		if(!microServices.containsKey(m)){
			microServices.put(m, new LinkedBlockingQueue<>());
		}
	}

	/**
	 * Removes the message queue allocated to {@code m} via the call to
	 * {@link #register(bgu.spl.mics.MicroService)} and cleans all references
	 * related to {@code m} in this message-bus. If {@code m} was not
	 * registered, nothing should happen.
	 * <p>
	 * @param m the micro-service to unregister.
	 */
	public void unregister(MicroService m) {

		if (microServices.get(m) != null ) {
			for (Class<? extends Message> tmp : Events.keySet()) { // for each event under micro service m
				Semaphore lock = locks.get(tmp);
				try { // locking the event so no one can change it
					lock.acquire();
					Events.get(tmp).remove(m);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}finally {
					lock.release();
				}
			}
			Iterator<Class <? extends Broadcast>> iter = Broadcasts.keySet().iterator();
			try {
				broadcastSemaphore.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			while (iter.hasNext()){
				Class tmp = iter.next();
				Broadcasts.get(tmp).remove(m);
			}
			broadcastSemaphore.release();

			LinkedBlockingQueue youCompleteMe = microServices.get(m); // cleaning all the queue of the closing micro service by "complete" with null
			if (youCompleteMe != null) {
				for (Object eventToComplete : youCompleteMe) {
					Semaphore locky = locks.get(eventToComplete.getClass());
					try {
						locky.acquire();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					complete((Event) eventToComplete, null);
					locky.release();
				}
				microServices.remove(m);
			}
		}
	}
	/**
	 * Using this method, a <b>registered</b> micro-service can take message
	 * from its allocated queue.
	 * This method is blocking meaning that if no messages
	 * are available in the micro-service queue it
	 * should wait until a message becomes available.
	 * The method should throw the {@link IllegalStateException} in the case
	 * where {@code m} was never registered.
	 * <p>
	 * @param m The micro-service requesting to take a message from its message
	 *          queue.
	 * @return The next message in the {@code m}'s queue (blocking).
	 * @throws InterruptedException if interrupted while waiting for a message
	 *                              to became available.
	 */
	@Override
	public Message awaitMessage(MicroService m) throws InterruptedException {
		if (!microServices.containsKey(m)){
			throw new IllegalStateException("Micro Service does not exist.");
		}
		Message tmp = microServices.get(m).take();
		return tmp;

	}

	/**
	 * "inside call" function that uses to lock an event (@param e),
	 * @param e event that will be locked.
	 */
    private synchronized void addLock(Class<? extends Event>  e){
	    if(!locks.containsKey(e)){
            locks.put(e, new Semaphore(1,true));
        }

    }

}
