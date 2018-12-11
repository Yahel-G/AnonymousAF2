package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.passiveObjects.OrderPair;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

/**
 * TimeService is the global system timer There is only one instance of this micro-service.
 * It keeps track of the amount of ticks passed since initialization and notifies
 * all other micro-services about the current time tick using {@link Tick Broadcast}.
 * This class may not hold references for objects which it is not responsible for:
 * {@link ResourcesHolder}, {@link MoneyRegister}, {@link Inventory}.
 * 
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
public class TimeService extends MicroService{

	private int speed;
	private int duration;
	private Timer BigBen;
	private int ticksPassed;

	public TimeService(int speed, int duration) {
		super("The_Big_Ben");
		this.speed = speed;
		this.duration = duration;
		ticksPassed = 0; // "The current time always starts from 1"
		ActionListener taskPerformer = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				if (ticksPassed < duration) {
					ticksPassed++;
					sendBroadcast(new TickBroadcast(ticksPassed * speed, duration, speed));
				}
			}
		};
			BigBen = new Timer(speed, taskPerformer);
	}

	@Override
	protected void initialize() {
		BigBen.start();
		subscribeBroadcast(TickBroadcast.class, clock -> {
			if (clock.getTimeOfDeath() == clock.giveMeSomeTime()) {
				terminate();
				System.out.println(getName() + " was terminated.");
			}
		});
	}
}
