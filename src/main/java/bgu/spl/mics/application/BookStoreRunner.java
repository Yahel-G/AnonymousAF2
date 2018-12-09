package bgu.spl.mics.application;

import bgu.spl.mics.application.passiveObjects.*;
import bgu.spl.mics.application.services.*;
import com.google.gson.*;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/** This is the Main class of the application. You should parse the input file,
 * create the different instances of the objects, and run the system.
 * In the end, you should output serialized objects.
 */
public class BookStoreRunner {
        public static Vector<Thread> Threads = null;
        public static TimeService timeService = null; //Changed
        public static Vector<APIService> APIServices = null; // Changed
        public static Vector<Customer> customersArray = null; //Changed
        public static Vector<InventoryService> inventoryServices = null; //Changed
        public static Vector<SellingService> sellingServices = null;//Changed
        public static Vector<LogisticsService> logisticsServices = null;//Changed
        public static Vector<ResourceService> resourceServices = null;//Changed
        public static Inventory inventory = null;//Changed
        public static ResourcesHolder resourcesHolder = null;//Changed
        public static MoneyRegister moneyRegister = null;//changed


    public static void main(String[] args) {
        Threads = new Vector<>();
        APIServices = new Vector<>();
        customersArray = new Vector<>();
        inventoryServices = new Vector<>();
        sellingServices = new Vector<>();
        logisticsServices = new Vector<>();
        resourceServices = new Vector<>();

        GsonParser();
        Vector<Runnable> runnables = new Vector<>();
        for (int i = 0; i< APIServices.size(); i++){
            runnables.add(APIServices.elementAt(i));
        }
        for (int i = 0; i< inventoryServices.size(); i++){
            runnables.add(inventoryServices.elementAt(i));
        }
        for (int i = 0; i< sellingServices.size(); i++){
            runnables.add(sellingServices.elementAt(i));
        }
        for (int i = 0; i< logisticsServices.size(); i++){
            runnables.add(logisticsServices.elementAt(i));
        }
        for (int i = 0; i< resourceServices.size(); i++){
            runnables.add(resourceServices.elementAt(i));
        }
        runnables.add(timeService);

        for(Runnable r: runnables){
            Threads.add(new Thread(r));
        }
        for (Thread t: Threads){
            t.run();
        }
    }

    private static void GsonParser(){
        JsonParser Parser = new JsonParser();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream("input.json");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Reader reader = new InputStreamReader(inputStream);
        JsonElement rootElement = Parser.parse(reader);
        JsonObject rootObject = rootElement.getAsJsonObject();
        JsonArray initialInventory = rootObject.getAsJsonArray("initialInventory");
        BookInventoryInfo[] booksInventory = new BookInventoryInfo[initialInventory.size()];
        int index = 0;
        for (JsonElement bookInfo: initialInventory){
            booksInventory[index] = new BookInventoryInfo(bookInfo.getAsJsonObject().getAsJsonPrimitive("bookTitle").getAsString(),
                    bookInfo.getAsJsonObject().getAsJsonPrimitive("amount").getAsInt(),
                    bookInfo.getAsJsonObject().getAsJsonPrimitive("price").getAsInt());
            index++;
        }
        index = 0;
        JsonArray initialResources = rootObject.getAsJsonArray("initialResources");
        DeliveryVehicle[] vehicles = null;
        for (JsonElement item: initialResources){ // in case the input consists of several arrays of vehicles
            JsonArray vehiclesInput = item.getAsJsonObject().getAsJsonArray("vehicles");
            vehicles = new DeliveryVehicle[vehiclesInput.size()];
            for(JsonElement aVhicle: vehiclesInput){
                vehicles[index] = new DeliveryVehicle(aVhicle.getAsJsonObject().getAsJsonPrimitive("license").getAsInt(),
                        aVhicle.getAsJsonObject().getAsJsonPrimitive("speed").getAsInt());
                index++;
            }
        }
        JsonObject servicesInput = rootObject.getAsJsonObject("services");
        JsonObject timeInput = servicesInput.getAsJsonObject("time");
        int timeSpeed = timeInput.get("speed").getAsInt();
        int timeDuration = timeInput.get("duration").getAsInt();
        int numOfSelling = servicesInput.get("selling").getAsInt();
        int numOfInventory = servicesInput.get("inventoryService").getAsInt();
        int numOfLogistics = servicesInput.get("logistics").getAsInt();
        int numOfResources = servicesInput.get("resourcesService").getAsInt();

        JsonArray customersInput = servicesInput.getAsJsonArray("customers");
        HashMap<Customer, List<OrderPair>> customers = new HashMap<>();
        for(JsonElement customer: customersInput){
            Customer tempCust = new Customer(customer.getAsJsonObject().getAsJsonPrimitive("id").getAsInt(),
                    customer.getAsJsonObject().getAsJsonPrimitive("name").getAsString(),
                    customer.getAsJsonObject().getAsJsonPrimitive("address").getAsString(),
                    customer.getAsJsonObject().getAsJsonPrimitive("distance").getAsInt(),
                    customer.getAsJsonObject().getAsJsonObject("creditCard").getAsJsonObject().get("number").getAsInt(),
                    customer.getAsJsonObject().getAsJsonObject("creditCard").getAsJsonObject().get("amount").getAsInt());
            List<OrderPair> tempSchedule = new Vector<>();
            JsonArray schedules = customer.getAsJsonObject().getAsJsonArray("orderSchedule");
            for(JsonElement schedule: schedules){
                tempSchedule.add(new OrderPair(schedule.getAsJsonObject().get("tick").getAsJsonPrimitive().getAsInt(),
                        schedule.getAsJsonObject().get("bookTitle").getAsString()));
            }
            customers.put(tempCust, tempSchedule);
        }

        inventory.getInstance().load(booksInventory);
        resourcesHolder.getInstance().load(vehicles);
        timeService = new TimeService(timeSpeed,timeDuration); //BigBen?
        int i;
        String name;
        for(i = 0 ; i < numOfSelling ; i++){
            name = "Sell "+ Integer.toString(i);
            sellingServices.add(new SellingService(name));
        }
        for(i = 0 ; i < numOfInventory ; i++){
            name = "Inv "+ Integer.toString(i);
            inventoryServices.add(new InventoryService(name));
        }
        for(i = 0 ; i < numOfLogistics ; i++){
            name = "Log "+ Integer.toString(i);
            logisticsServices.add(new LogisticsService(name));
        }
        for(i = 0 ; i < numOfResources ; i++){
            name = "Res "+ Integer.toString(i);
            resourceServices.add(new ResourceService(name));
        }
        for(HashMap.Entry<Customer, List<OrderPair>> it: customers.entrySet()){
            customersArray.add(it.getKey());
            APIServices.add(new APIService(it.getKey().getName(), it.getKey(), it.getValue()));
        }
        moneyRegister = moneyRegister.getInstance();

    } // end GsonParser
}
