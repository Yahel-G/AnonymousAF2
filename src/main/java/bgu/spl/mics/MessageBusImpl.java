package bgu.spl.mics;

import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
	private ConcurrentHashMap <Message, Future> FuturesMap;


	// make constructor
	private MessageBusImpl(){
		Events = new ConcurrentHashMap<>();
		Broadcasts = new ConcurrentHashMap<>();
		microServices = new ConcurrentHashMap<>();
		FuturesMap = new ConcurrentHashMap<>();

	}


	public static MessageBusImpl getInstance(){
		return singletonHolder.disBus;
	}

	@Override
	public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
		if(Events.containsKey(type)){
			Events.get(type).add(m);
		}
		else{
			ConcurrentLinkedQueue<MicroService> newQ = new ConcurrentLinkedQueue<>();
			newQ.add(m);
			Events.put(type, newQ);
		}

	}

	@Override
	public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
		if(Broadcasts.containsKey(type)){
			Broadcasts.get(type).add(m);
		}
		else{
			ConcurrentLinkedQueue<MicroService> newQ = new ConcurrentLinkedQueue<>();
			newQ.add(m);
			Broadcasts.put(type, newQ);
		}
	}

	@Override
	public <T> void complete(Event<T> e, T result) {
		FuturesMap.get(e).resolve(result);

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
	// round robin
	public <T> Future<T> sendEvent(Event<T> e) {
		if(Events.containsKey(e.getClass())){
			Future<T> fut = new Future<T>();
			try{
				microServices.get(Events.get(e.getClass())).put(e);
			}catch (InterruptedException ex){
				ex.printStackTrace();
			}
		// TODO implement round robin insertion

		// return some future
		}

		return null;
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
			microServices.remove(m);
			Iterator<Class <? extends Message>> it = Events.keySet().iterator();
			while (it.hasNext()){
				Class tmp = it.next();
				Events.get(tmp).remove(m);
			}
			Iterator<Class <? extends Broadcast>> iter = Broadcasts.keySet().iterator();
			while (iter.hasNext()){
				Class tmp = iter.next();
				Broadcasts.get(tmp).remove(m);
			}
		}
	}

	@Override
	public Message awaitMessage(MicroService m) throws InterruptedException {
		if (!microServices.containsKey(m)){
			throw new InterruptedException("Micro Service does not exist.");
		}
//		while (microServices.get(m) == null);
//		return microServices.get(m).take();
		Lock lock1 = new ReentrantLock(); // a more sophisticated and flexiable way to synchronize.
		lock1.lock();
		Message tmp = microServices.get(m).take();
		lock1.unlock();
		return tmp;
	}

	

}
