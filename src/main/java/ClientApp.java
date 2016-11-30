import discovery.DiscoveryClient;
import model.Employee;
import transport.TransportClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Vladok on 28.10.2016.
 */
public class ClientApp {
    private static int warehousePort=2556;
    public static void main(String[] args) throws IOException {
        System.out.println("--------------TCP-----------------");
        TransportClient transportClient=new TransportClient(warehousePort);
        transportClient.receiveFilteredData("999","FirstName");
    }
}
