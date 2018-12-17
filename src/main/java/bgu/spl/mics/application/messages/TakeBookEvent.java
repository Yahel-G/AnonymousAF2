package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;


public class TakeBookEvent implements Event{
    private String bookTitle;

    public TakeBookEvent(String bookTitle){
        this.bookTitle = bookTitle;
    }

    public String getBookTitle() {
        return bookTitle;
    }
}
