package videoframes;

public class Main {
    
    public static void main(String[] args) {
        String configPath = "F:\\kdaquila_SoftwareDev\\2018_04_19_SurfaceReconstruction_Datasets\\Dataset9\\User\\Configuration.xml";
        FrameSelection frameExtraction = new FrameSelection(configPath);
        FramePlot framePlot = new FramePlot(configPath);       
    }

}
