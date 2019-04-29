package config;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class ConfigXML {
    Document doc;
    
    public ConfigXML(String path) {
        // open the document
        try {
            this.doc = Util.openXML(path); 
        }
        catch (ParserConfigurationException | SAXException | IOException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Could not open the XML configuration file");
        }
    }  
    
    // Video
    public Integer getNCols() {
        String text = this.doc.getElementsByTagName("nCols").item(0).getTextContent();
        return Integer.parseInt(text);
    }
    
    public int getNRows() {
        String text = this.doc.getElementsByTagName("nRows").item(0).getTextContent();
        return Integer.parseInt(text);
    }
    
    public int getNFrameRepeats() {
        String text = this.doc.getElementsByTagName("nFrameRepeats").item(0).getTextContent();
        return Integer.parseInt(text);
    }
    
    public int getNSyncPulses() {
        String text = this.doc.getElementsByTagName("nSyncPulses").item(0).getTextContent();
        return Integer.parseInt(text);
    }
    
    public ArrayList<Integer> getCalibLevels() {
        String text = this.doc.getElementsByTagName("calibLevels").item(0).getTextContent();
        return Util.parseIntegerArray(text);
    }   
    
    public ArrayList<HashMap<String,String>> getPhaseFrameSequence() {
        
        Node frameSequence = this.doc.getElementsByTagName("phaseFrameSequence").item(0);
        Node frameSeqNode = frameSequence.getFirstChild();
        ArrayList<HashMap<String,String>> output = new ArrayList<>();
        while(!(frameSeqNode==null)){
            if (frameSeqNode.getNodeType() == Node.ELEMENT_NODE) {
                String frameName = ((Element)frameSeqNode).getTagName();
                HashMap<String,String> outputHash = new HashMap<>();
                if (frameName.equals("stripeFrame")) {
                    outputHash.put("type", "stripeFrame");
                    outputHash.put("levelSet", ((Element)frameSeqNode).getElementsByTagName("levelSet").item(0).getTextContent());
                    outputHash.put("nCopies", ((Element)frameSeqNode).getElementsByTagName("nCopies").item(0).getTextContent());
                    outputHash.put("nItemCopies", ((Element)frameSeqNode).getElementsByTagName("nItemCopies").item(0).getTextContent());
                    outputHash.put("orientation", ((Element)frameSeqNode).getElementsByTagName("orientation").item(0).getTextContent());
                    outputHash.put("phasePerStripe", ((Element)frameSeqNode).getElementsByTagName("phasePerStripe").item(0).getTextContent());                
                }
                else if (frameName.equals("waveFrame")) {
                    outputHash.put("type", "waveFrame");
                    outputHash.put("phaseOffset", ((Element)frameSeqNode).getElementsByTagName("phaseOffset").item(0).getTextContent());
                    outputHash.put("waveFront", ((Element)frameSeqNode).getElementsByTagName("waveFront").item(0).getTextContent());
                    outputHash.put("waveNumber", ((Element)frameSeqNode).getElementsByTagName("waveNumber").item(0).getTextContent());
                    outputHash.put("amplitude", ((Element)frameSeqNode).getElementsByTagName("amplitude").item(0).getTextContent());
                    outputHash.put("offset", ((Element)frameSeqNode).getElementsByTagName("offset").item(0).getTextContent());
                }               
                output.add(outputHash);
            }
            frameSeqNode = frameSeqNode.getNextSibling();
        }        
        return output;
    }
    
    // Directories
    public String getFramesDir() {
        String text = this.doc.getElementsByTagName("framesDir").item(0).getTextContent();
        return text;
    }
    
    public String getCaptureFramesDir() {
        String text = this.doc.getElementsByTagName("captureFramesDir").item(0).getTextContent();
        return text;
    }
    
    public String getCalibFramesDir() {
        String text = this.doc.getElementsByTagName("calibFramesDir").item(0).getTextContent();
        return text;
    }
    
    public String getPhaseFramesDir() {
        String text = this.doc.getElementsByTagName("phaseFramesDir").item(0).getTextContent();
        return text;
    }
    
    public String getCalibFitFramesDir() {
        String text = this.doc.getElementsByTagName("calibFitFramesDir").item(0).getTextContent();
        return text;
    }
    
    public String getPhaseFitFramesDir() {
        String text = this.doc.getElementsByTagName("phaseFitFramesDir").item(0).getTextContent();
        return text;
    }
    
    public String getPlotDir() {
        String text = this.doc.getElementsByTagName("plotDir").item(0).getTextContent();
        return text;
    }
    
    public String getPhaseMapDir() {
        String text = this.doc.getElementsByTagName("phaseMapDir").item(0).getTextContent();
        return text;
    }
    
    public String getSyncFrameDir() {
        String text = this.doc.getElementsByTagName("syncFrameDir").item(0).getTextContent();
        return text;
    }
    
    // Files
    public String getFramePlotDataFile() {
        String text = this.doc.getElementsByTagName("framePlotDataFile").item(0).getTextContent();
        return text;
    }
    
    public String getFramePlotFile() {
        String text = this.doc.getElementsByTagName("framePlotFile").item(0).getTextContent();
        return text;
    }   
    
    public String getHorizPhaseMapDataFile() {
        String text = this.doc.getElementsByTagName("horizPhaseMapDataFile").item(0).getTextContent();
        return text;
    }
    
    public String getVertPhaseMapDataFile() {
        String text = this.doc.getElementsByTagName("vertPhaseMapDataFile").item(0).getTextContent();
        return text;
    }
    
    public String getHorizPhaseMapFile() {
        String text = this.doc.getElementsByTagName("horizPhaseMapFile").item(0).getTextContent();
        return text;
    }
    
    public String getVertPhaseMapFile() {
        String text = this.doc.getElementsByTagName("vertPhaseMapFile").item(0).getTextContent();
        return text;
    }
    
    // Analysis
    public int getJumpThreshold() {
        String text = this.doc.getElementsByTagName("jumpThreshold").item(0).getTextContent();
        return Integer.parseInt(text);
    }
    
    public int getframePlotWidth() {
        String text = this.doc.getElementsByTagName("framePlotWidth").item(0).getTextContent();
        return Integer.parseInt(text);
    }
    
    public int getframePlotHeight() {
        String text = this.doc.getElementsByTagName("framePlotHeight").item(0).getTextContent();
        return Integer.parseInt(text);
    }
    
    // add new configuration methods here ...
 
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String newLine = System.getProperty("line.separator");

        //determine methods declared in this class only
        Method[] fields = this.getClass().getDeclaredMethods();

        //print methods paired with their return values
        for ( Method method : fields  ) {
            
            if (method.getName().equals("toString")) {
                continue;             
            }
            
            try {
                result.append( method.getName() );
                result.append(": ");
                if (method.getName().equals("getFrameSequence")){
                    ArrayList<HashMap<String,String>> newList = (ArrayList<HashMap<String,String>>) method.invoke(this);
                    for (HashMap item: newList) {
                        result.append(item.toString());
                    }
                }
                else {                    
                    result.append( method.invoke(this) );
                }
            }             
            catch (IllegalAccessException | InvocationTargetException ex ) {
                System.out.println(ex.getCause());
            }
            
            result.append(newLine);
            
            
            
        }
        
        return result.toString();
    }
}
