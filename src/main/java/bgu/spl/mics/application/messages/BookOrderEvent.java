package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.passiveObjects.Customer;

/**
 * Created by Yahel on 01/12/2018.
 */
public class BookOrderEvent implements Event {
    private Customer customer;
    private String bookTitle;
    private int orderedTick;
    public BookOrderEvent(Customer customer, String bookTitle,int orderedTick){
        this.customer = customer;
        this.bookTitle = bookTitle;
        this.orderedTick = orderedTick;
    }

    public int getOrderedTick() {
        return orderedTick;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getBookTitle() {
        return bookTitle;
    }
}
