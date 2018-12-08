package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;

/**
 * Created by Yahel on 05/12/2018.
 */
public class CheckAvailabilityEvent implements Event {
    private String bookTitle;

    public CheckAvailabilityEvent(String bookTitle){
        this.bookTitle = bookTitle;
    }

    public String getBookTitle() {
        return bookTitle;
    }
}
