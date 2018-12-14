package bgu.spl.mics.application.passiveObjects;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

/**
 * Passive data-object representing a customer of the store.
 * You must not alter any of the given public methods of this class.
 * <p>
 * You may add fields and methods to this class as you see fit (including public methods).
 */
public class Customer implements Serializable {

	private int id;
	private String name;
	private String address;
	private int distance;
	private List Receipts;
	private int creditCard;
	private  int availableAmountInCreditCard;

	public Customer(int id, String name, String address, int distance, int creditCard, int availableAmountInCreditCard){
		this.id = id;
		this.name = name;
		this.address = address;
		this.distance = distance;
		Receipts = new Vector<OrderReceipt>();
		this.creditCard = creditCard;
		this.availableAmountInCreditCard = availableAmountInCreditCard;

	}
	/**
     * Retrieves the name of the customer.
     */
	public String getName() {
		return name;
	}

	/**
     * Retrieves the ID of the customer  . 
     */
	public int getId() {
		return id;
	}
	
	/**
     * Retrieves the address of the customer.  
     */
	public String getAddress() {
		return address;
	}
	
	/**
     * Retrieves the distance of the customer from the store.  
     */
	public int getDistance() {
		return distance;
	}

	
	/**
     * Retrieves a list of receipts for the purchases this customer has made.
     * <p>
     * @return A list of receipts.
     */
	public List<OrderReceipt> getCustomerReceiptList() {
		return Receipts;
	}
	
	/**
     * Retrieves the amount of money left on this customers credit card.
     * <p>
     * @return Amount of money left.   
     */
	public int getAvailableCreditAmount() {
		return availableAmountInCreditCard;
	}
	
	/**
     * Retrieves this customers credit card serial number.    
     */
	public int getCreditNumber() {
		return creditCard;
	}


	public void chargeCredit (int howMuch){
		// if check is only a safe check - the credit amount should be checked by the seller microService.
		if (availableAmountInCreditCard >= howMuch){
			availableAmountInCreditCard = availableAmountInCreditCard - howMuch;
		}
	}
}