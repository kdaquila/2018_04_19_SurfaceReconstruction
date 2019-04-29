package config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Util {
    
    public static Element loadXML(String path) {
        File inputFile = new File(path);
        SAXReader reader = new SAXReader();
        Element output;
        try {
            org.dom4j.Document doc = reader.read( inputFile );
            output = doc.getRootElement();
        }
        catch (DocumentException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Could not open the XML configuration document");
        }
        return output;
    }
    
    public static Document openXML(String path) throws ParserConfigurationException, SAXException, IOException {
        // get the parser
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        
        // parse the file
        File xmlFile = new File(path);
        Document doc = dBuilder.parse(xmlFile);
        
        // normalize the document format
        doc.getDocumentElement().normalize();
        
        return doc;
    }
    
    public static ArrayList<Integer> parseIntegerArray(String in){
        String[] inParts = in.split(",");
        ArrayList<Integer> out = new ArrayList<>();
        for (String part: inParts) {
            out.add(Integer.parseInt(part));
        }
        return out;
    }
    
    public static ArrayList<Double> parseDoubleArray(String in){
        String[] inParts = in.split(",");
        ArrayList<Double> out = new ArrayList<>();
        for (String part: inParts) {
            out.add(Double.parseDouble(part));
        }
        return out;
    }

}
