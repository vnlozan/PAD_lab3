package model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.impl.FailingDeserializer;

import java.beans.XMLEncoder;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Vladok on 11.11.2016.
 */
public class WarehouseServer {
    private static int tcpNodePort=3002;                                 //default
    private static final String tcpGetAllNodesEmployeeLists="get all data";
    private int tcpWHPort;
    private List<Employee>employeeList;
    private ServerSocket warehouseServerSocket;

    public WarehouseServer(int tcpWHPort, int tcpMavenNodePort) throws IOException {
        this.tcpNodePort=tcpMavenNodePort;
        this.tcpWHPort=tcpWHPort;
        warehouseServerSocket= new ServerSocket(tcpWHPort);
        employeeList=new ArrayList<>();
    }
    public void listen(){
        printMessage("Warehouse starts listening");
        Thread thread=new Thread(new Runnable() {
            @Override
            public void run() {
                while(true)
                {
                    InputStream istream = null;
                    OutputStream outputStream = null;
                    StringBuffer externalResult = new StringBuffer();
                    try {
                        Socket connectionSocket = warehouseServerSocket.accept();
                        printMessage("New Connection to WareHouse ");
                        istream = connectionSocket.getInputStream();
                        outputStream=connectionSocket.getOutputStream();
                        BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));
                        String partlyTransData;
                        while(!(partlyTransData=receiveRead.readLine()).isEmpty())
                            externalResult.append(partlyTransData.trim());
                        String message= externalResult.toString();
                        printMessage("Warehouse received certain message = "+ message);
                        String[] tokens =message.split(",");
                        if(tokens[0].equals("get data"))
                        {
                            int Salary=Integer.parseInt(tokens[1]);
                            // connecting to maven
                            Socket sock = new Socket("localhost",tcpNodePort);
                            OutputStream ostreamN = sock.getOutputStream();
                            PrintWriter pwriteN = new PrintWriter(ostreamN, true);
                            InputStream istreamN = sock.getInputStream();

                            String sendMessage = "get all data\n";
                            pwriteN.println(sendMessage);
                            pwriteN.flush();

                            printMessage("Sent message = "+sendMessage+" to Node with port "+tcpNodePort);

                            BufferedReader receiveRead1 = new BufferedReader(new InputStreamReader(istreamN));
                            String partlyTransData1="";
                            externalResult=new StringBuffer();

                            //reading messages
                            while(!(partlyTransData1=receiveRead1.readLine()).isEmpty())
                                externalResult.append(partlyTransData1.trim());
                            String msg=externalResult.toString();
                            ObjectMapper mapper1 = new ObjectMapper();
                            employeeList = mapper1.readValue(msg, new TypeReference<List<Employee>>(){});
                            filter(Salary,tokens[2]);
                            String jsonInStringPretty=mapper1.writerWithDefaultPrettyPrinter().writeValueAsString(employeeList);
                            printMessage("Warehouse received JSON data and filtered it:"+jsonInStringPretty);
                            XMLEncoder xmlEncoder=new XMLEncoder(outputStream);
                            xmlEncoder.writeObject(employeeList);
                            xmlEncoder.flush();
                            xmlEncoder.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }
    private void filter(int Salary,String SortField){
        printMessage("Sorting Field = "+SortField+"\n Minimum Salary filter = "+ Salary);
        employeeList= employeeList.stream().filter(e -> e.getSalary() > Salary).collect(Collectors.toList());;
        switch (SortField)
        {
            case "FirstName":
                employeeList =  employeeList.stream().sorted(Comparator.comparing(Employee::getFirstName)).collect(Collectors.toList());;
                break;
            default: break;
        }
    }
    private void printMessage(String message){
        System.out.println("---------WareHouse-Server----------");
        System.out.println(message);
        System.out.println("-----------------------------------");
    }
}
