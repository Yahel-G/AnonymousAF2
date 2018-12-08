package bgu.spl.mics.application.passiveObjects;


import java.util.concurrent.*;

/**
 * Passive data-object representing the store inventory.
 * It holds a collection of {@link BookInventoryInfo} for all the
 * books in the store.
 * <p>
 * This class must be implemented safely as a thread-safe singleton.
 * You must not alter any of the given public methods of this class.
 * <p>
 * You can add ONLY private fields and methods to this class as you see fit.
 */
public class Inventory {

	/**
     * Retrieves the single instance of this class.
     */

	private static class singletonHolder{
		private static Inventory daInventory = new Inventory();
	}

	private ConcurrentHashMap<String, BookInventoryInfo> bookMap;

	private Inventory(){
		bookMap = new ConcurrentHashMap<>();
	}



	public static Inventory getInstance() {

		return singletonHolder.daInventory;
	}

	/**
     * Initializes the store inventory. This method adds all the items given to the store
     * inventory.
     * <p>
     * @param inventory 	Data structure containing all data necessary for initialization
     * 						of the inventory.
     */
	public void load (BookInventoryInfo[ ] inventory ) {
		for (int i = 0; i < inventory.length; i++){
			bookMap.put(inventory[i].getBookTitle(),inventory[i]);
		}
	}

	/**
     * Attempts to take one book from the store.
     * <p>
     * @param book 		Name of the book to take from the store
     * @return 	an {@link Enum} with options NOT_IN_STOCK and SUCCESSFULLY_TAKEN.
     * 			The first should not change the state of the inventory while the
     * 			second should reduce by one the number of books of the desired type.
     */
	public OrderResult take (String book) {
		if (!bookMap.containsKey(book) || bookMap.get(book).getAmountInInventory() == 0){
			return OrderResult.NOT_IN_STOCK;
		}
		else{
			bookMap.get(book).takeBook();
			return OrderResult.SUCCESSFULLY_TAKEN;
		}
	}



	/**
     * Checks if a certain book is available in the inventory.
     * <p>
     * @param book 		Name of the book.
     * @return the price of the book if it is available, -1 otherwise.
     */
	public int checkAvailabiltyAndGetPrice(String book) {
		if (!bookMap.containsKey(book) || bookMap.get(book).getAmountInInventory() == 0){
			return -1;
		}
		else{
			return bookMap.get(book).getPrice();
		}
	}

	/**
     *
     * <p>
     * Prints to a file name @filename a serialized object HashMap<String,Integer> which is a Map of all the books in the inventory. The keys of the Map (type {@link String})
     * should be the titles of the books while the values (type {@link Integer}) should be
     * their respective available amount in the inventory.
     * This method is called by the main method in order to generate the output.
     */
	public void printInventoryToFile(String filename){
		//TODO: Implement this
	}





	/*
* 	ONLY FOR TESTING --- DELETE WHEN SUBMIT
* */
/*
	public LinkedList<BookInventoryInfo> getListForTesting(){
		return bookList;
	}
*/


}
