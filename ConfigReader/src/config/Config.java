package config;

import java.io.File;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

public class Config {

    Element root;
    
    public Config(String path) {
        File inputFile = new File(path);
        SAXReader reader = new SAXReader();
        try {
            Document doc = reader.read( inputFile );
            this.root = doc.getRootElement();
        }
        catch (DocumentException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Could not open the XML configuration document");
        }
    }        
    
    public Integer readInteger(String selector) {
        Node node = this.root.selectSingleNode(selector);
        if (node == null) {
            throw new RuntimeException("Could not find the requested XML node");
        }
        return Integer.parseInt(node.getText());
    }
    
    public List<Integer> readIntegerArray(String selector) {
        Node node = this.root.selectSingleNode(selector);
        if (node == null) {
            throw new RuntimeException("Could not find the requested XML node");
        }
        return Util.parseIntegerArray(node.getText());
    }
    
    public List<Integer> readIntegerArrayAt(String selector, int index) {
        List<Node> nodeList = this.root.selectNodes(selector);
        if (nodeList == null) {
            throw new RuntimeException("Could not find the requested XML node");
        }
        return Util.parseIntegerArray(nodeList.get(index).getText());
    }
    
    public Double readDouble(String selector) {
        Node node = this.root.selectSingleNode(selector);
        if (node == null) {
            throw new RuntimeException("Could not find the requested XML node");
        }
        return Double.parseDouble(node.getText());
    }
    
    public Double readDoubleAt(String selector, int index) {
        List<Node> nodeList = this.root.selectNodes(selector);
        if (nodeList == null) {
            throw new RuntimeException("Could not find the requested XML node");
        }
        return Double.parseDouble(nodeList.get(index).getText());
    }
    
    public List<Double> readDoubleArray(String selector) {
        Node node = this.root.selectSingleNode(selector);
        if (node == null) {
            throw new RuntimeException("Could not find the requested XML node");
        }
        return Util.parseDoubleArray(node.getText());
    }
    
    public List<Double> readDoubleArrayAt(String selector, int index) {
        List<Node> nodeList = this.root.selectNodes(selector);
        if (nodeList == null) {
            throw new RuntimeException("Could not find the requested XML node");
        }
        return Util.parseDoubleArray(nodeList.get(index).getText());
    }
    
    public String readString(String selector) {
        Node node = this.root.selectSingleNode(selector);
        if (node == null) {
            throw new RuntimeException("Could not find the requested XML node");
        }
        return node.getText();
    }
    
    public String readStringAt(String selector, int index) {
        List<Node> nodeList = this.root.selectNodes(selector);
        if (nodeList == null) {
            throw new RuntimeException("Could not find the requested XML node");
        }
        return nodeList.get(index).getText();
    }
    
    public boolean readBoolean(String selector) {
        Node node = this.root.selectSingleNode(selector);
        if (node == null) {
            throw new RuntimeException("Could not find the requested XML node");
        }
        return Boolean.parseBoolean(node.getText());
    }
    
    public boolean readBooleanAt(String selector, int index) {
        List<Node> nodeList = this.root.selectNodes(selector);
        if (nodeList == null) {
            throw new RuntimeException("Could not find the requested XML node");
        }
        return Boolean.parseBoolean(nodeList.get(index).getText());
    }
}
