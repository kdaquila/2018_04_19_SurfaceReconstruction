package config;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class WaveFrame {
    
    Element e;

    public WaveFrame(Node node) {
        e = (Element) node;
    }
    
    public String getOrientation(){
        String text = e.getElementsByTagName("orientation").item(0).getTextContent();
        return text;
    }
    
    public Integer getPhaseOffset(){
        String text = e.getElementsByTagName("phaseOffset").item(0).getTextContent();
        return Integer.parseInt(text);
    }
    
    public Double getWaveNumber(){
        String text = e.getElementsByTagName("waveNumber").item(0).getTextContent();
        return Double.parseDouble(text);
    }

}
