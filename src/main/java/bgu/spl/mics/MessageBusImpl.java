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
	private ConcurrentHashMap<MicroService, Semaphore> microLocks;


	// make constructor
	private MessageBusImpl(){
		Events = new ConcurrentHashMap<>();
		Broadcasts = new ConcurrentHashMap<>();
		microServices = new ConcurrentHashMap<>();
		FuturesMap = new ConcurrentHashMap<>();
		locks = new ConcurrentHashMap<>();
		microLocks = new ConcurrentHashMap<>();

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
		Semaphore semaphore = locks.get(type);
		try {
			semaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if(Events.containsKey(type)){
			Events.get(type).add(m);
		}
		else{// if another thread has created a new queue for this event type, we will overwrite it here
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
		for (MicroService m: sendTo){
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
//		Semaphore locker = locks.get(e.getClass());
		Future<T> fut = null;
//		try {
//			locker.acquire();
			System.out.println("Send Event "+e.toString()+" Initiated"); //todo remove
			ConcurrentLinkedQueue<MicroService> microQueue = Events.get(e.getClass());
		if (microQueue != null && !microQueue.isEmpty()) {
			MicroService service = microQueue.poll();
			if (service != null) {
				Semaphore microLock = microLocks.get(service);
				try{
					microLock.acquire();
					LinkedBlockingQueue<Message> messageQueue = microServices.get(service);
					if (messageQueue != null) {
						microQueue.offer(service);
						fut = new Future<T>();
						FuturesMap.put(e, fut);
						messageQueue.offer(e);
					}
					microLock.release();
				}catch (InterruptedException ex) {
					ex.printStackTrace();
				}
		}

						}

//					}

//				}
//				System.out.println("Send Event "+e.toString()+" added to FuturesMap" + '\n' + FuturesMap.get(e).toString()); //todo remove


			System.out.println("Send Event "+e.toString()+" Completed"); //todo remove
//			locker.release();
//		} catch (InterruptedException ex) {
//			ex.printStackTrace();
//		}

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
		if (!microLocks.containsKey(m)){
			microLocks.put(m, new Semaphore(1,true));
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
		Semaphore microLock = microLocks.get(m);
		try {
			microLock.acquire();
			if (microServices.get(m) != null ) {
				for (Class<? extends Message> tmp : Events.keySet()) {
					Semaphore lock = locks.get(tmp);
					try {
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
					while (iter.hasNext()){
						Class tmp = iter.next();
						Broadcasts.get(tmp).remove(m);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				broadcastSemaphore.release();

				// someone might send me an event while i'm trying to unregister myself (im a microservice).

				LinkedBlockingQueue youCompleteMe = microServices.get(m);
				if (youCompleteMe != null) {
					for (Object eventToComplete : youCompleteMe) {
						Semaphore locky = locks.get(eventToComplete.getClass());
						try {
							locky.acquire();
							complete((Event) eventToComplete, null);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						locky.release();
					}
					microServices.remove(m);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		microLock.release();
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
//		while (microServices.get(m) == null);
//		return microServices.get(m).take();
///		Lock lock1 = new ReentrantLock(); // a more sophisticated and flexiable way to synchronize.
//		lock1.lock();


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
