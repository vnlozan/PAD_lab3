package transport;

import model.Employee;

import java.beans.XMLDecoder;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.apache.commons.lang3.SerializationUtils.deserialize;

/**
 * Created by Vladok on 19.10.2016.
 */
public class TransportClient {

    private static final String tcpUnicastAdress="localhost";
    private static int tcpUnicastPort=3002;                                 //default
    private static final String tcpGetAllNodesEmployeeLists="get data";

    private Socket serverSocketToConnect;
    private PrintWriter pwrite;
    private OutputStream ostream;
    private InputStream istream;
    private List<Employee>employeesList;

    public TransportClient(int tcpUnicastPort) throws IOException {
        this.tcpUnicastPort=tcpUnicastPort;
        serverSocketToConnect= new Socket(tcpUnicastAdress, tcpUnicastPort);
        ostream = serverSocketToConnect.getOutputStream();
        pwrite = new PrintWriter(ostream, true);
        istream = serverSocketToConnect.getInputStream();
        employeesList = new ArrayList<>();
    }
    public void receiveFilteredData(String minSalary,String SortField) throws IOException {
        pwrite.println(tcpGetAllNodesEmployeeLists+","+minSalary+","+SortField+"\n");
        pwrite.flush();
        Object result = null;
        try {
//            String rr=fromStream(istream);
//            System.out.println(rr);
            XMLDecoder xmlDecoder = new XMLDecoder(istream);
            result = xmlDecoder.readObject();

        } finally {
            if (istream != null) {
                istream.close();
            }
        }

        employeesList.addAll((List<Employee>)result);
        System.out.println("Data received:");
        for (Employee e:employeesList) {
            System.out.println(e.toString());
        }
    }
    public List<Employee> getData()
    {
        return employeesList;
    }
    public static String fromStream(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String newLine = System.getProperty("line.separator");
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
            out.append(newLine);
        }
        return out.toString();
    }
}
