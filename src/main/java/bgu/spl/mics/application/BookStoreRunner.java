package bgu.spl.mics.application;

import bgu.spl.mics.application.passiveObjects.*;
import bgu.spl.mics.application.services.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/** This is the Main class of the application. You should parse the input file,
 * create the different instances of the objects, and run the system.
 * In the end, you should output serialized objects.
 */
public class BookStoreRunner implements Serializable {
    private static Vector<Thread> Threads = new Vector<>();
    private static TimeService timeService = null;
    private static Vector<APIService> APIServices = null;
    private static Vector<Customer> customersArray = null;
    private static Vector<InventoryService> inventoryServices = null;
    private static Vector<SellingService> sellingServices = null;
    private static Vector<LogisticsService> logisticsServices = null;
    private static Vector<ResourceService> resourceServices = null;
    public static Inventory inventory = null;
    private static ResourcesHolder resourcesHolder = null;
    private static MoneyRegister moneyRegister = null;

    private static String customerOutput;
    private static String booksOutput;
    private static String receiptOutput;
    private static String registerOutput;
    private static String inputFile;
    private static HashMap<Integer, Customer> customersForPrinting;

    public static CountDownLatch latch;
    public static CountDownLatch latch2;





    public static void main(String[] args) {
        inputFile = args[0];
        customerOutput = args[1];
        booksOutput = args[2];
        receiptOutput = args[3];
        registerOutput = args[4];

        inventory = Inventory.getInstance();
        resourcesHolder = ResourcesHolder.getInstance();
        moneyRegister = MoneyRegister.getInstance();

        Vector<Runnable> runnables = new Vector<>();
        APIServices = new Vector<>();
        customersArray = new Vector<>();
        inventoryServices = new Vector<>();
        sellingServices = new Vector<>();
        logisticsServices = new Vector<>();
        resourceServices = new Vector<>();
        customersForPrinting = new HashMap<>();

        GsonParser(); // working with the user - get the input, process it, and saving in the matching data structure. and preapering the output files



        for (int i = 0; i < APIServices.size(); i++) {
            runnables.add(APIServices.elementAt(i));
        }
        for (int i = 0; i < inventoryServices.size(); i++) {
            runnables.add(inventoryServices.elementAt(i));
        }
        for (int i = 0; i < sellingServices.size(); i++) {
            runnables.add(sellingServices.elementAt(i));
        }
        for (int i = 0; i < logisticsServices.size(); i++) {
            runnables.add(logisticsServices.elementAt(i));
        }
        for (int i = 0; i < resourceServices.size(); i++) {
            runnables.add(resourceServices.elementAt(i));
        }



        for (Runnable r : runnables) {
            Threads.add(new Thread(r));
        }
        for (Thread t : Threads) {
            t.start();
        }

        Thread TS = new Thread(timeService);
        TS.start();



        try {
            latch2.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        inventory.printToFile(booksOutput);
        printCustomers(customerOutput);
        moneyRegister.printReceipts(receiptOutput);

        // print the MoneyRegister object
        try {
            FileOutputStream file = new FileOutputStream(registerOutput);
            ObjectOutputStream oos = new ObjectOutputStream(file);
            oos.writeObject(moneyRegister);
            oos.close();
            file.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }


    /**
     * the parser reads the input file and break it down into data that he put in the data structure pre-made
     * for that data, and then (@Call initialize) to initialize all the services needed for that run.
     * a base function for building the framework, no params or returns.
     */
    private static void GsonParser() {
        JsonParser Parser = new JsonParser();
        InputStream inputStream = null; // breaks down the input to the matching structures
        try {
            inputStream = new FileInputStream(inputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        // "decodeing" the input down to an object that is easy to work with. using Json methods.
        Reader reader = new InputStreamReader(inputStream);
        JsonElement rootElement = Parser.parse(reader);
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }  finally{
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        JsonObject rootObject = rootElement.getAsJsonObject();

        initialInventoryFun(rootObject); // breaks done the inventory data and load the book for the store
        initialResourcesFun (rootObject);// breaks done the resources data and load the vehicles  for the store

        JsonObject servicesInput = rootObject.getAsJsonObject("services");
        JsonObject timeInput = servicesInput.getAsJsonObject("time");
        int timeSpeed = timeInput.get("speed").getAsInt();
        int timeDuration = timeInput.get("duration").getAsInt();
        int numOfSelling = servicesInput.get("selling").getAsInt();
        int numOfInventory = servicesInput.get("inventoryService").getAsInt();
        int numOfLogistics = servicesInput.get("logistics").getAsInt();
        int numOfResources = servicesInput.get("resourcesService").getAsInt();

        JsonArray customersInput = servicesInput.getAsJsonArray("customers");
        HashMap<Customer, List<OrderPair>> customers = customersBuilderFun(customersInput);

        int NumOfServicesExceptTime = numOfSelling + numOfInventory + numOfLogistics + numOfResources + customersInput.size();

        initialize(numOfSelling, numOfInventory, numOfLogistics, numOfResources, customers, NumOfServicesExceptTime, timeSpeed, timeDuration);
        // break down the services data and  keep them in there matching vectors.




    } // end GsonParser

    /**
     * gets from  the parser all the services needed for this run -which and how many, create them and put them in
     * matching vectors for later use and run.
     * @param numOfSelling how many selling services are needed
     * @param numOfInventory how many inventory services are needed
     * @param numOfLogistics how many logistics services are needed
     * @param numOfResources how many resources services are needed
     * @param customers all the customers in the store- for initializing API services
     * @param NumOfServicesExceptTime the sum amount of all the services , in use for the latch
     * @param timeSpeed data from the user about how long each tick is.
     * @param timeDuration data from the user about how many ticks the run will be.
     */
    private static void initialize(int numOfSelling, int numOfInventory, int numOfLogistics, int numOfResources, HashMap<Customer, List<OrderPair>> customers, int NumOfServicesExceptTime, int timeSpeed, int timeDuration){
        latch = new CountDownLatch(NumOfServicesExceptTime);
        latch2 = new CountDownLatch(NumOfServicesExceptTime+1);


        timeService = new TimeService(timeSpeed, timeDuration); //BigBen?

        int i;
        String name;
        for (i = 0; i < numOfSelling; i++) {
            name = "Selling Service " + Integer.toString(i);
            sellingServices.add(new SellingService(name));
        }
        for (i = 0; i < numOfInventory; i++) {
            name = "Inventory Service " + Integer.toString(i);
            inventoryServices.add(new InventoryService(name));
        }
        for (i = 0; i < numOfLogistics; i++) {
            name = "Logistics Service  " + Integer.toString(i);
            logisticsServices.add(new LogisticsService(name));
        }
        for (i = 0; i < numOfResources; i++) {
            name = "Resource Service " + Integer.toString(i);
            resourceServices.add(new ResourceService(name));
        }
        for (HashMap.Entry<Customer, List<OrderPair>> it : customers.entrySet()) {
            customersArray.add(it.getKey());
            APIServices.add(new APIService(it.getKey().getName(), it.getKey(), it.getValue()));
        }
    }

    /**
     * print to file name @filename the customer at the store for output use.
     * @param filename name to the output fill for customers.
     */
    public static void printCustomers(String filename) {
        try {
            FileOutputStream file = new FileOutputStream(filename);
            ObjectOutputStream oos = new ObjectOutputStream(file);
            oos.writeObject(customersForPrinting);
            oos.close();
            file.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }


    /**
     * inside break down function. the function is part of the parser, and as such it break down the input file to data
     * and stores it in the data structures made for it ahead.
     * this function is in charge for doing the parser job for the inventory data.
     * @param rootObject - input file data made into an object in the parser.
     */
    private static void initialInventoryFun(JsonObject rootObject){
        JsonArray initialInventory = rootObject.getAsJsonArray("initialInventory");
        BookInventoryInfo[] booksInventory = new BookInventoryInfo[initialInventory.size()];
        int index = 0;
        for (JsonElement bookInfo : initialInventory) {
            booksInventory[index] = new BookInventoryInfo(bookInfo.getAsJsonObject().getAsJsonPrimitive("bookTitle").getAsString(),
                    bookInfo.getAsJsonObject().getAsJsonPrimitive("amount").getAsInt(),
                    bookInfo.getAsJsonObject().getAsJsonPrimitive("price").getAsInt());
            index++;
        }
        inventory.load(booksInventory);

    }
    /**
     * inside break down function. the function is part of the parser, and as such it break down the input file to data
     * and stores it in the data structures made for it ahead.
     * this function is in charge for doing the parser job for the resources data.
     * @param rootObject - input file data made into an object in the parser.
     */
    private static void initialResourcesFun(JsonObject rootObject){
        JsonArray initialResources = rootObject.getAsJsonArray("initialResources");
        DeliveryVehicle[] vehicles = null;
        int index = 0;
        for (JsonElement item : initialResources) { // in case the input consists of several arrays of vehicles
            JsonArray vehiclesInput = item.getAsJsonObject().getAsJsonArray("vehicles");
            vehicles = new DeliveryVehicle[vehiclesInput.size()];
            for (JsonElement aVhicle : vehiclesInput) {
                vehicles[index] = new DeliveryVehicle(aVhicle.getAsJsonObject().getAsJsonPrimitive("license").getAsInt(),
                        aVhicle.getAsJsonObject().getAsJsonPrimitive("speed").getAsInt());
                index++;
            }
        }
        resourcesHolder.load(vehicles);
    }

    /**
     * inside break down function. the function is part of the parser, and as such it break down the input file to data
     * and stores it in the data structures made for it ahead.
     * this function is in charge for doing the parser job for the customers data - personal information and schedules .
     * @param customersInput the data from the input file made into an array at the parser
     * @return Hash map with each customer and his or her schedule.
     */
    private static HashMap customersBuilderFun(JsonArray customersInput){
        HashMap<Customer, List<OrderPair>> customers = new HashMap<>();

        for (JsonElement customer : customersInput) {
            Customer tempCust = new Customer(customer.getAsJsonObject().getAsJsonPrimitive("id").getAsInt(),
                    customer.getAsJsonObject().getAsJsonPrimitive("name").getAsString(),
                    customer.getAsJsonObject().getAsJsonPrimitive("address").getAsString(),
                    customer.getAsJsonObject().getAsJsonPrimitive("distance").getAsInt(),
                    customer.getAsJsonObject().getAsJsonObject("creditCard").getAsJsonObject().get("number").getAsInt(),
                    customer.getAsJsonObject().getAsJsonObject("creditCard").getAsJsonObject().get("amount").getAsInt());
            customersForPrinting.put(tempCust.getId(), tempCust);
            List<OrderPair> tempSchedule = new Vector<>();
            JsonArray schedules = customer.getAsJsonObject().getAsJsonArray("orderSchedule");
            for (JsonElement schedule : schedules) {
                tempSchedule.add(new OrderPair(schedule.getAsJsonObject().get("tick").getAsInt(),
                        schedule.getAsJsonObject().get("bookTitle").getAsString()));
            }
            customers.put(tempCust, tempSchedule);
        }
        return customers;
    }

}
