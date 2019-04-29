package config;

import java.util.ArrayList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class StripeFrame {    
    
    Element e;

    public StripeFrame(Node node) {
        e = (Element)node;
    }
    
    public String getOrientation(){
        String text = e.getElementsByTagName("orientation").item(0).getTextContent();
        return text;
    }
    
    public ArrayList<Integer> getLevelSetIndices(){
        String text = e.getElementsByTagName("levelSetIndices").item(0).getTextContent();
        return Util.parseIntegerArray(text);
    }
    
    public Integer getNCopies(){
        String text = e.getElementsByTagName("nCopies").item(0).getTextContent();
        return Integer.parseInt(text);
    }
    
    public Integer getNItemCopies(){
        String text = e.getElementsByTagName("nItemCopies").item(0).getTextContent();
        return Integer.parseInt(text);
    }
    
    public Double getPhasePerStripe(){
        String text = e.getElementsByTagName("phasePerStripe").item(0).getTextContent();
        return Double.parseDouble(text);
    }                

}
