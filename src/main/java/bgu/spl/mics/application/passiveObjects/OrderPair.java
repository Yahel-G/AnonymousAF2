package bgu.spl.mics.application.passiveObjects;

/**
 * Created by Yahel on 02/12/2018.
 */
public class OrderPair {

    private String bookTitle;
    private Integer Tick;

    public OrderPair(Integer Tick, String bookTitle){
        this.bookTitle = bookTitle;
        this.Tick = Tick;
    }

    public String getBookTitle(){
        return  bookTitle;
    }

    public int getTick(){
        return Tick;
    }


}
