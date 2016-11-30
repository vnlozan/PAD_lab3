package model;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static org.apache.commons.lang3.SerializationUtils.deserialize;
import static org.apache.commons.lang3.SerializationUtils.serialize;

/**
 * Created by Vladok on 28.10.2016.
 */
public class ServerNode {
    private List<Employee> employeesList;
    private List<Integer> connectedPortsList;
    private ServerSocket tcpClientServerSocket;
    private ServerSocket tcpNodeServerSocket;
    private int tcpClientPort;
    private int tcpNodePort;
    public ServerNode(int tcpClientPort,
                      int tcpNodePort,
                      List<Integer> connectedPortsList,
                      List<Employee> employeesList
    ) throws IOException {
        this.tcpClientPort=tcpClientPort;
        this.tcpNodePort=tcpNodePort;
        tcpClientServerSocket = new ServerSocket(tcpClientPort);
        tcpNodeServerSocket=new ServerSocket(tcpNodePort);
        this.connectedPortsList=connectedPortsList;
        this.employeesList=employeesList;
    }
    /*  TCP UNICAST message = "get special data"
     *  uses Socket
     *  returns node employeeList
     */
    public void tcpSpecialEmployeeListRequest(){
        Thread thread=new Thread(new Runnable() {
            @Override
            public void run() {
                while(true)
                {
                    InputStream istream = null;
                    StringBuffer externalResult = new StringBuffer();
                    try {
                        Socket connectionSocket = tcpNodeServerSocket.accept();
                        istream = connectionSocket.getInputStream();
                        BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));
                        String partlyTransData;
                        while(!(partlyTransData=receiveRead.readLine()).isEmpty())
                            externalResult.append(partlyTransData.trim());
                        String message=externalResult.toString();
                        if(message.equals("get special data"))
                        {
                            printMessage("Server with external port = " + tcpClientPort
                                    + " has got a message : "+ message);
                            OutputStream ostream = connectionSocket.getOutputStream();
                            Employee[] s = new Employee[employeesList.size()];
                            serialize((Employee[]) employeesList.toArray(s), ostream);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }
    /*  TCP UNICAST message = "get all data"
     *  uses Socket
     *  returns all connected nodes' employeeLists
     */
    public void tcpAllEmployeeListRequest(){
        List<Employee>theWholeList=new ArrayList<>();
        theWholeList.addAll(employeesList);
        Thread thread=new Thread(new Runnable() {
            @Override
            public void run() {
                while(true)
                {
                    InputStream istream = null;
                    StringBuffer externalResult = new StringBuffer();
                    try {
                        Socket connectionSocket = tcpClientServerSocket.accept();
                        istream = connectionSocket.getInputStream();
                        BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));
                        String partlyTransData;
                        while(!(partlyTransData=receiveRead.readLine()).isEmpty())
                            externalResult.append(partlyTransData.trim());
                        String message= externalResult.toString();
                        if(message.equals("get all data"))
                        {
                            printMessage("Server with external port = " + tcpClientPort + " has got a message : "+ message);
                            ExecutorService executorService= Executors.newCachedThreadPool();
                            printMessage("Port used to connect to special nodes = "+ tcpClientPort);
                            Semaphore semaphore=new Semaphore(connectedPortsList.size()+1);
                            for (int port:connectedPortsList) {
                                printMessage("Current port is "+port);
                                Callable<Employee[]> callableThread=new Callable<Employee[]>() {
                                    @Override
                                    public Employee[] call() {
                                        Socket socketNode = null;
                                        Employee[] employees=null;
                                        try {
                                            socketNode = new Socket("localhost", port);
                                            //send
                                            OutputStream ostream = socketNode.getOutputStream();
                                            PrintWriter pwrite = new PrintWriter(ostream, true);
                                            //receive
                                            InputStream istream = socketNode.getInputStream();
                                            //message
                                            String sendMessage = "get special data\n";
                                            //send
                                            pwrite.println(sendMessage);       // sending to server
                                            pwrite.flush();                    // flush the data
                                            employees = (Employee[]) deserialize(istream);

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        printMessage("Returned data from Server with internal port = "+port+" to Server with external port = " + tcpClientPort);
                                        return employees;
                                    }
                                };
                                Future<Employee[]> future = executorService.submit(callableThread);
                                while(!future.isDone());
                                try{
                                    Employee[] employees=future.get();
                                    theWholeList.addAll(new ArrayList<Employee>(Arrays.asList(employees)));
                                    semaphore.release();
                                } catch (InterruptedException ie) {
                                    ie.printStackTrace(System.err);
                                } catch (ExecutionException ee) {
                                    ee.printStackTrace(System.err);
                                }
                            }
                            executorService.shutdown();
                            semaphore.acquire();
                            ObjectMapper mapper = new ObjectMapper();
                            String jsonInStringPretty=mapper.writeValueAsString(theWholeList);
                            printMessage("JSON data ready :\n "+jsonInStringPretty);
                            OutputStream ostream = connectionSocket.getOutputStream();
                            PrintWriter pw=new PrintWriter(ostream,true);
                            pw.write(jsonInStringPretty+"\n\n");
                            pw.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }
    private void printMessage(String message){
        System.out.println("--------------NODE-Server----------");
        System.out.println(message);
        System.out.println("-----------------------------------");
    }
}
