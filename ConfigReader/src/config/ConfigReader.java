package config;

import java.util.List;
import org.dom4j.Element;
import org.dom4j.Node;

public class ConfigReader {
    
    public static void main(String[] args)  {        
        //String path1 = "F:\\kdaquila_SoftwareDev\\2018_04_19_SurfaceReconstruction_Datasets\\Dataset4\\Configuration.xml";
        //testConfigXML(path);
        String path = "F:\\kdaquila_SoftwareDev\\2018_04_19_SurfaceReconstruction_Datasets\\Dataset4\\Configuration_new.xml";
        testDOM4J(path);
    }
    
    public static void testConfigXML(String path) {
        ConfigXML configData = new ConfigXML(path);
        System.out.println(configData.toString());
    }
    
//    public static void testConfig(String path) {
//        Config_old configData = new Config_old(path);
//        for (int i = 0; i < 12; i++){
//           Object obj = configData.getStructuredLight().getPhaseFrames().get(i);
//            if (obj instanceof StripeFrame) {
//                System.out.println(((StripeFrame)obj).getOrientation());
//            }
//            else if (obj instanceof WaveFrame) {
//                System.out.println(((WaveFrame)obj).getOrientation());
//            } 
//        }     
//    }
    
    public static void testConfig(String path) {
        Config myConfig = new Config(path);
        String selector;
        
        System.out.println(myConfig.readStringAt("/config/structuredLight/frame/type", 3));

        System.out.println(myConfig.readStringAt("/config/structuredLight/frame/orientation", 3));

        System.out.println(myConfig.readDoubleAt("/config/structuredLight/frame/phaseOffset", 3));

        System.out.println(myConfig.readBooleanAt("/config/structuredLight/frame/isLastofSet", 3));
    }
    
    public static void testDOM4J(String path){
        Element root = Util.loadXML(path);
        System.out.println(root.selectNodes("/config/structuredLight/frame/type").get(3).getText());
        System.out.println(root.selectNodes("/config/structuredLight/frame/orientation").get(3).getText());
        System.out.println(Double.parseDouble(root.selectNodes("/config/structuredLight/frame/phaseOffset").get(3).getText()));
        System.out.println(Boolean.parseBoolean(root.selectNodes("/config/structuredLight/frame/isLastofSet").get(3).getText()));

        List<Node> frameNodes = root.selectNodes("/config/structuredLight/frame");
        for (Node frame: frameNodes) {
            if (frame.selectSingleNode("type").getText().equals("stripe")){
                System.out.println(frame.selectSingleNode("type").getText());
                System.out.println(frame.selectSingleNode("orientation").getText());
                System.out.println(Util.parseIntegerArray(frame.selectSingleNode("levelIndices").getText()));
            }
        }
    }}
