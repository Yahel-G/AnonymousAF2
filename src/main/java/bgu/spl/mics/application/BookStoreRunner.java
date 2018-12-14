package bgu.spl.mics.application;

import bgu.spl.mics.application.passiveObjects.*;
import bgu.spl.mics.application.services.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

/** This is the Main class of the application. You should parse the input file,
 * create the different instances of the objects, and run the system.
 * In the end, you should output serialized objects.
 */
public class BookStoreRunner implements Serializable {
    private static Vector<Thread> Threads = new Vector<>();
    private static TimeService timeService = null; //Changed
    private static Vector<APIService> APIServices = null; // Changed
    private static Vector<Customer> customersArray = null; //Changed
    private static Vector<InventoryService> inventoryServices = null; //Changed
    private static Vector<SellingService> sellingServices = null;//Changed
    private static Vector<LogisticsService> logisticsServices = null;//Changed
    private static Vector<ResourceService> resourceServices = null;//Changed
    public static Inventory inventory = null;//Changed
    private static ResourcesHolder resourcesHolder = null;//Changed
    private static MoneyRegister moneyRegister = null;//changed

    private static String customerOutput;
    private static String booksOutput;
    private static String receiptOutput;
    private static String registerOutput;
    private static HashMap<Integer, Customer> customersForPrinting;
    public static CountDownLatch latch;
    public static CountDownLatch latch2;


    public static void main(String[] args) {
        inventory = Inventory.getInstance();
        resourcesHolder = ResourcesHolder.getInstance();
        moneyRegister = MoneyRegister.getInstance();

        Vector<Runnable> runnables = new Vector<>();
        APIServices = new Vector<>();
        customersArray = new Vector<>();
        inventoryServices = new Vector<>();
        inventoryServices = new Vector<>();
        sellingServices = new Vector<>();
        logisticsServices = new Vector<>();
        resourceServices = new Vector<>();
        customersForPrinting = new HashMap<>();

        GsonParser();


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
        moneyRegister.printReceipts(receiptOutput);
        moneyRegister.printToFile(registerOutput);
        printCustomers(customerOutput);

        //TODO: Delete!!!!!!
        ObjectInputStream input = null;
        try {

            FileInputStream stream = new FileInputStream("a");
            input = new ObjectInputStream(stream);
            Object CustomerPrint =  input.readObject();

            FileInputStream stream2 = new FileInputStream("b");
            input = new ObjectInputStream(stream2);
            Object BooksPrint =  input.readObject();

            FileInputStream stream3 = new FileInputStream("c");
            input = new ObjectInputStream(stream3);
            Object ReceiptPrint =  input.readObject();

            FileInputStream stream4 = new FileInputStream("d");
            input = new ObjectInputStream(stream4);
            Object MoneyRegPrint = input.readObject();
            int helloooooooooooooooooooooo = 17;
        } catch (FileNotFoundException ex) {
        }
        catch (IOException ex2){}
        catch (ClassNotFoundException ex3){}
        finally {
            if (input != null){
                try {
                    input.close();
                }catch (IOException e){}
            }
        }
        //TODO: End Delete

    }




    private static void GsonParser() {
        JsonParser Parser = new JsonParser();
        InputStream inputStream = inputReaderFun();

        Reader reader = new InputStreamReader(inputStream);
        JsonElement rootElement = Parser.parse(reader);
        JsonObject rootObject = rootElement.getAsJsonObject();

        initialInventoryFun(rootObject);
        initialResourcesFun (rootObject);

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


    } // end GsonParser

    private static void initialize(int numOfSelling, int numOfInventory, int numOfLogistics, int numOfResources, HashMap<Customer, List<OrderPair>> customers, int NumOfServicesExceptTime, int timeSpeed, int timeDuration){
        latch = new CountDownLatch(NumOfServicesExceptTime);
        latch2 = new CountDownLatch(NumOfServicesExceptTime+1);
        System.out.println(Integer.toString(NumOfServicesExceptTime+1) + " services should initialize"); // todo remove


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
    private static InputStream inputReaderFun(){
        InputStream inputStream = null;
        System.out.println("Please enter the json input and output files: input, output(Customer, Books, Receipts, MoneyRegister...");
        Scanner scanner = new Scanner(System.in);
        String inputFile = scanner.next();
        customerOutput = scanner.next();
        booksOutput = scanner.next();
        receiptOutput = scanner.next();
        registerOutput = scanner.next();

        try {
            inputStream = new FileInputStream(inputFile); // input.json
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return inputStream;
    }
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
