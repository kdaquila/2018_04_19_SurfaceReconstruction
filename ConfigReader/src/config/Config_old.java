package config;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Config_old {
    Document doc;
    private final StructuredLight structuredLight;
    private final CalibCapture calibCapture;
    private final SceneCapture sceneCapture;
    
    public Config_old(String path) {
        try {
            doc = Util.openXML(path);
        }
        catch (SAXException | ParserConfigurationException | IOException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Could not load the XML configuration file");
        }
        NodeList nl = doc.getElementsByTagName("structuredLight");
        Node structuredLightNode = doc.getElementsByTagName("structuredLight").item(0);
        structuredLight = new StructuredLight(structuredLightNode);
        Node calibCaptureNode = doc.getElementsByTagName("calibCapture").item(0);
        calibCapture = new CalibCapture(calibCaptureNode);
        Node sceneCaptureNode = doc.getElementsByTagName("sceneCapture").item(0);
        sceneCapture = new SceneCapture(sceneCaptureNode);
    }

    public StructuredLight getStructuredLight() {
        return structuredLight;
    }

    public CalibCapture getCalibCapture() {
        return calibCapture;
    }

    public SceneCapture getSceneCapture() {
        return sceneCapture;
    }
    
    

}
