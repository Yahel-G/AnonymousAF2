package bgu.spl.mics.application.passiveObjects;



import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Passive object representing the store finance management. 
 * It should hold a list of receipts issued by the store.
 * <p>
 * This class must be implemented safely as a thread-safe singleton.
 * You must not alter any of the given public methods of this class.
 * <p>
 * You can add ONLY private fields and methods to this class as you see fit.
 */
public class MoneyRegister implements Serializable {

	private static class singletonHolder{
		private static MoneyRegister daRegister = new MoneyRegister();
	}

	private ConcurrentHashMap<Integer , OrderReceipt> Receipts ;
	private int totalEarnings;
	private int orderId;

	private MoneyRegister(){
		Receipts = new ConcurrentHashMap<>();
		totalEarnings = 0;
		orderId = 0;
	}

	/**
	 * a function that get the receipts in the money register as a vector when ask.
	 * @return a vector with the receipts.
	 */
	public List<OrderReceipt> getOrderReceipts() {
		List<OrderReceipt> ret = new Vector<>();
		for(Integer it : Receipts.keySet()){
			ret.add(Receipts.get(it));
		}
		return ret;
	}


	/**
     * Retrieves the single instance of this class.
     */
	public static MoneyRegister getInstance() {

		return singletonHolder.daRegister;
	}
	
	/**
     * Saves an order receipt in the money register.
     * <p>   
     * @param r		The receipt to save in the money register.
     */
	public synchronized void  file (OrderReceipt r) {
		r.setOrderId(orderId);
		totalEarnings += r.getPrice();
		Receipts.put(orderId , r);
		orderId++;
	}
	
	/**
     * Retrieves the current total earnings of the store.  
     */
	public int getTotalEarnings() {
		return totalEarnings;
	}
	
	/**
     * Charges the credit card of the customer a certain amount of money.
     * <p>
     * @param amount 	amount to charge
     */
	public void chargeCreditCard(Customer c, int amount) {
		c.chargeCredit(amount);
	}
	

	/**
	 * Prints to a file named @filename an object MoneyRegister by printing all of its fields.
	 * This method is called by the main method in order to generate the output..
	 */
	public void printReceipts(String filename) { // print ONLY the receipts that are in the register. for output uses.
		List<OrderReceipt> recList = new ArrayList<OrderReceipt>(Receipts.values());
		try {
			FileOutputStream file = new FileOutputStream(filename);
			ObjectOutputStream oos = new ObjectOutputStream(file);
			oos.writeObject(recList);
			oos.close();
			file.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
