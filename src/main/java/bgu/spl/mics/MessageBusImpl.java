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

	@Override
	public  <T> void complete(Event<T> e, T result) {
		if (e==null){
			System.out.println("EVENT SENT TO COMPLETE FUNCTION IS NULL");
		}
		if(result!=null)
		System.out.println("Trying to COMPLETE " +e.toString() + "The result is: " +result.toString()); // todo remove
		if (FuturesMap.get(e) == null){
			System.out.println("Whyyyyyyyyyyyyyyyyyyyyyy"); // todo remove
		}
		if(FuturesMap.containsKey(e)){
			System.out.println("FuturesMap contains the event!"); // todo remove

		}
		Future fut = FuturesMap.get(e); // .get?
//		try{	// todo remove
			fut.resolve(result);
//		} catch (NullPointerException ex){
//			System.out.println("NULL POINTER EXCEPTION: \n" + "Trying to resolve Future: " + fut.toString() + "\n for event: " + e.toString() + "\n With result: " + result.toString());
//			throw new NullPointerException();
//		}

	}

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
		Future<T> fut = null; // new Future<T>()?
		MicroService service;
		if(Events.get(e.getClass()) != null){
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

	@Override
	public void register(MicroService m) {
		if(!microServices.containsKey(m)){
			microServices.put(m, new LinkedBlockingQueue<>());
		}
	}


	@Override
	public void unregister(MicroService m) {

		if (microServices.get(m) != null ) {
			for (Class<? extends Message> tmp : Events.keySet()) {
				Semaphore lock = locks.get(tmp);
				try {
					lock.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Events.get(tmp).remove(m);
				lock.release();
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

			// someone might send me an event while i'm trying to unregister myself (im a microservice).

			LinkedBlockingQueue youCompleteMe = microServices.get(m);
			if (youCompleteMe != null) {
				for (Object eventToComplete : youCompleteMe) {
					Semaphore locky = locks.get(eventToComplete);
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

    private synchronized void addLock(Class<? extends Event>  e){
	    if(!locks.containsKey(e)){
            locks.put(e, new Semaphore(1,true));
        }

    }

}
