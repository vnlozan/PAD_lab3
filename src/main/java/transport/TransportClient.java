package transport;

import model.Employee;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
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
        // create a SchemaFactory capable of understanding WXS schemas
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        // load a WXS schema, represented by a Schema instance
        Source schemaFile = new StreamSource(new File("src/main/resources/schema.xsd"));
        Schema schema = null;

        try {
            schema = factory.newSchema(schemaFile);
        } catch (SAXException e) {
            e.printStackTrace();
        }

        // create a Validator instance, which can be used to validate an instance document
        Validator validator = schema.newValidator();
        String rr;

        try {
            rr=fromStream(istream);
            System.out.println(rr);
            XMLDecoder xmlDecoder = new XMLDecoder(new ByteArrayInputStream(rr.getBytes()));
            result = xmlDecoder.readObject();

        } finally {
            if (istream != null) {
                istream.close();
            }
        }
        // validate the DOM tree
        try {
            validator.validate(new DOMSource(stringToDocument(rr)));
        } catch (SAXException e) {
            System.out.println("Instance document is invalid!");
            // instance document is invalid!
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
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
    public static Document stringToDocument(final String xmlSource)
            throws SAXException, ParserConfigurationException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        return builder.parse(new InputSource(new StringReader(xmlSource)));
    }
}
