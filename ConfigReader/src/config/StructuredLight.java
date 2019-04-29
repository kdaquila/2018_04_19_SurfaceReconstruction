package config;

import java.util.ArrayList;
import java.util.HashMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class StructuredLight {
    
    Element e;
    private final ArrayList<Object> phaseFrames;

    public StructuredLight(Node node) {
        e = (Element) node;
        phaseFrames = loadPhaseFrames();
    }
    
    public int getNFrameRepeats() {
        String text = this.e.getElementsByTagName("nFrameRepeats").item(0).getTextContent();
        return Integer.parseInt(text);
    }
    
    public int getNSyncPulses() {
        String text = this.e.getElementsByTagName("nSyncPulses").item(0).getTextContent();
        return Integer.parseInt(text);
    }
    
    public ArrayList<Integer> getCalibLevels() {
        String text = this.e.getElementsByTagName("calibLevels").item(0).getTextContent();
        return Util.parseIntegerArray(text);
    }
    
    public int getAmplitude() {
        String text = this.e.getElementsByTagName("amplitude").item(0).getTextContent();
        return Integer.parseInt(text);
    }
    
    public int getOffset() {
        String text = this.e.getElementsByTagName("offset").item(0).getTextContent();
        return Integer.parseInt(text);
    }
    
    public Integer getNCols() {
        String text = this.e.getElementsByTagName("nCols").item(0).getTextContent();
        return Integer.parseInt(text);
    }
    
    public int getNRows() {
        String text = this.e.getElementsByTagName("nRows").item(0).getTextContent();
        return Integer.parseInt(text);
    }   
        
    final public ArrayList<Object> loadPhaseFrames() {
        ArrayList<Object> newPhaseFrameList = new ArrayList<>();
        Node phaseFramesNode = this.e.getElementsByTagName("phaseFrames").item(0);
        Node currentNode = phaseFramesNode.getFirstChild();
        while(!(currentNode==null)){
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                String frameName = ((Element)currentNode).getTagName();
                HashMap<String,String> newPhaseFrame = new HashMap<>();
                if (frameName.equals("stripeFrame")) {
                    newPhaseFrameList.add(new StripeFrame(currentNode));
                }
                else if (frameName.equals("waveFrame")) {
                    newPhaseFrameList.add(new WaveFrame(currentNode));
                }               
            }
            currentNode = currentNode.getNextSibling();
        }    
        return newPhaseFrameList;
    }

    public ArrayList<Object> getPhaseFrames() {
        return phaseFrames;
    }
       
    
    

}
