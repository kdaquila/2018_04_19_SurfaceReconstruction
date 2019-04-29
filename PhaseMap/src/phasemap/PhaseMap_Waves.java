package phasemap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import config.Util;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.util.FastMath;
import org.dom4j.Element;
import org.dom4j.Node;

public class PhaseMap_Waves {

    public PhaseMap_Waves(String configPath) {
        
        // Load the configuration
        System.out.println("Loading the configuration data: Please Wait ...");
        Element e = Util.loadXML(configPath);
        
        List<Node> calibFolders = e.selectNodes("/config/calibrationFolders/folder");
        List<Node> sceneFolders = e.selectNodes("/config/sceneFolders/folder");
        List<Node> allFolders = new ArrayList<>();
        allFolders.addAll(calibFolders);
        allFolders.addAll(sceneFolders);
        
        // work on each folder using its own base path
        for (Node folder: allFolders)
        {            
            String baseDirPath = e.selectSingleNode("/config/baseFolder").getText() + "\\" +
                                 folder.selectSingleNode("name").getText();
            System.out.println("\nNow working on images from: " + baseDirPath);
            computePhaseMap(e, baseDirPath);
        } 
    }
    
    public final void computePhaseMap(Element root, String baseDirPath)
    {
        
        // Get the image names
        System.out.println("Getting the image names: Please Wait ...");
        String phaseFitFramesDirPath = baseDirPath + "\\" +
                                       root.selectSingleNode("/config/folders/phaseFitFrames").getText();
        File phaseFitFramesDir = new File(phaseFitFramesDirPath);
        String[] imageNames = phaseFitFramesDir.list();

        // Get the phase image sequence configuration
        List<Node> phaseFrameConfigList = root.selectNodes("/config/structuredLight/frame");        
        
        // Validate
        if (phaseFrameConfigList.size() != imageNames.length) {
            throw new IllegalArgumentException("The number of images doesn't match the number of configuration entries");
        }
        
        // accumulate the phase from each image
        System.out.println("Computing the phase maps: Please Wait ...");
        BufferedImage firstImage = loadImage(phaseFitFramesDirPath + "\\" + imageNames[0]);  
        int nCols = firstImage.getWidth();
        int nRows = firstImage.getHeight();
        int nWaves = 4; // don't change this, its fixed by the phase formula
        int nPhaseMaps = 2; // need to figure out how to generalize to multiples
        double[][][] hPhaseMapStack = new double[nRows][nCols][nPhaseMaps];
        double[][][] vPhaseMapStack = new double[nRows][nCols][nPhaseMaps];
        int hPhaseMapIndex = 0;
        int vPhaseMapIndex = 0;
        double[] hPhaseMapWavelengths = new double[nPhaseMaps];
        double[] vPhaseMapWavelengths = new double[nPhaseMaps];
        double[][][] waveBuffer = new double[nRows][nCols][nWaves];
        int waveIndex = 0;
        List<Integer> calibLevels = Util.parseIntegerArray(root.selectSingleNode("/config/structuredLight/calibLevels").getText());
        double minAmplitudeX = Double.parseDouble(root.selectSingleNode("/config/phaseMap/minAmplitudeX").getText());
        double minAmplitudeY = Double.parseDouble(root.selectSingleNode("/config/phaseMap/minAmplitudeY").getText());
        double nullPhase = Double.parseDouble(root.selectSingleNode("/config/phaseMap/nullPhase").getText());
        for (int i = 0; i < phaseFrameConfigList.size(); i++) {
            
            System.out.println("Now analyzing image: " + i);
            
            // load the image
            String name = imageNames[i];
            BufferedImage image = loadImage(phaseFitFramesDirPath + "\\" + name);
            WritableRaster raster = image.getRaster();

            // get the image config
            Node phaseFrameConfig = phaseFrameConfigList.get(i);           

            // Update the wave buffers
            for (int row = 0; row < nRows; row++) {
                for (int col = 0; col < nCols; col++) {

                    // read the pixel   
                    int[] pixel = new int[1];
                    raster.getPixel(col, row, pixel);
                    int gray = pixel[0];

                    // Store Wave Frame Pixels                          
                    waveBuffer[row][col][waveIndex] = (double) gray; 
                }
            }
            waveIndex += 1;

            // Check if its the last wave frame in the set
            boolean isLastofSet = Boolean.parseBoolean(phaseFrameConfig.selectSingleNode("isLastofSet").getText());
            if (isLastofSet == true) {                

                // Update the phase map
                String orientation = phaseFrameConfig.selectSingleNode("orientation").getText();
                double waveLength = Double.parseDouble(phaseFrameConfig.selectSingleNode("waveLength").getText());                
                if (orientation.equals("vertical")) 
                {
                    hPhaseMapWavelengths[hPhaseMapIndex] = waveLength;
                    for (int row = 0; row < nRows; row++)
                    {
                        for (int col = 0; col < nCols; col++)
                        {                        
                            double[] I = waveBuffer[row][col];
                            double b = I[3] - I[1];
                            double a = I[2] - I[0];
                            double newPhase = Math.PI + FastMath.atan2(b,a);
                            double newAmplitude = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
                            
                            if (newAmplitude >= minAmplitudeX)
                            {
                                hPhaseMapStack[row][col][hPhaseMapIndex] = newPhase;                                 
                            }
                            else
                            {
                                hPhaseMapStack[row][col][hPhaseMapIndex] = nullPhase;
                            }
                                                        
                        }
                    }
                    hPhaseMapIndex += 1;
                }
                else if (orientation.equals("horizontal")) {  
                    vPhaseMapWavelengths[vPhaseMapIndex] = waveLength;
                    for (int row = 0; row < nRows; row++)
                    {
                        for (int col = 0; col < nCols; col++)
                        {                        
                            double[] I = waveBuffer[row][col];
                            double b = I[3] - I[1];
                            double a = I[2] - I[0];
                            double newPhase = Math.PI + FastMath.atan2(b,a);
                            double newAmplitude = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
                            
                            if (newAmplitude >= minAmplitudeY)
                            {
                                vPhaseMapStack[row][col][vPhaseMapIndex] = newPhase;
                            }
                            else
                            {
                                vPhaseMapStack[row][col][vPhaseMapIndex] = nullPhase;
                            }
                                                        
                        }
                    }
                    vPhaseMapIndex += 1;
                }
                waveIndex = 0;               
            }                        
        }
        
        // Compute the unwrapped phase maps        
        double[][] hPhaseMap = new double[nRows][nCols];
        double[][] vPhaseMap = new double[nRows][nCols];
        for (int row = 0; row < nRows; row++)
        {
            for (int col = 0; col < nCols; col++)
            {
                // unwrap horizontal phase map
                double hPhaseLow = hPhaseMapStack[row][col][0];
                double hPhaseHigh = hPhaseMapStack[row][col][1];
                if (hPhaseLow == nullPhase || hPhaseHigh == nullPhase)
                {
                    hPhaseMap[row][col] = nullPhase;
                }
                else
                {
                    double hWaveLengthLow = hPhaseMapWavelengths[0];                
                    double hWaveLengthHigh = hPhaseMapWavelengths[1];
                    double hPhaseOffset = Math.round(((hWaveLengthLow/hWaveLengthHigh)*hPhaseLow-hPhaseHigh)/(2*Math.PI));
                    hPhaseMap[row][col] = hPhaseHigh + 2*Math.PI*hPhaseOffset;
                }
                
                // unwrap vertical phase map
                double vPhaseLow = vPhaseMapStack[row][col][0];
                double vPhaseHigh = vPhaseMapStack[row][col][1];
                if (vPhaseLow == nullPhase || vPhaseHigh == nullPhase)
                {
                    vPhaseMap[row][col] = nullPhase;
                }
                else
                {
                    double vWaveLengthLow = vPhaseMapWavelengths[0];                
                    double vWaveLengthHigh = vPhaseMapWavelengths[1];
                    double vPhaseOffset = Math.round(((vWaveLengthLow/vWaveLengthHigh)*vPhaseLow-vPhaseHigh)/(2*Math.PI));
                    vPhaseMap[row][col] = vPhaseHigh + 2*Math.PI*vPhaseOffset;
                }
            }
        }  

        // Get the folder path
        String phaseMapDirPath = baseDirPath + "\\" +
                                 root.selectSingleNode("/config/folders/phaseMap").getText();
        
        // Create folder if necessary
        createFolder(phaseMapDirPath); 

        // Export the phase data        
        System.out.println("Saving the phase maps data: Please Wait ...");
        
        String horizPhaseMapDataFileName = root.selectSingleNode("/config/files/horizPhaseMapData").getText();
        writeDoubleArray(hPhaseMap, phaseMapDirPath, horizPhaseMapDataFileName);

        String vertPhaseMapDataFileName = root.selectSingleNode("/config/files/vertPhaseMapData").getText();
        writeDoubleArray(vPhaseMap, phaseMapDirPath, vertPhaseMapDataFileName);

        // Draw the phase images
        System.out.println("Saving the phase maps image: Please Wait ...");
        
        String horizPhaseMapImageFileName = root.selectSingleNode("/config/files/horizPhaseMapImage").getText();
        BufferedImage horizPhaseMapImage = drawDoubleArray(hPhaseMap);
        saveImage(horizPhaseMapImage,phaseMapDirPath + "\\" + horizPhaseMapImageFileName);

        String vertPhaseMapImageFileName = root.selectSingleNode("/config/files/vertPhaseMapImage").getText();
        BufferedImage vertPhaseMapImage = drawDoubleArray(vPhaseMap);
        saveImage(vertPhaseMapImage, phaseMapDirPath + "\\" + vertPhaseMapImageFileName);
                     
    }          
                    
    public static int closestIndex(int value, List<Integer> in) {
        int min = Integer.MAX_VALUE;
        int closest = value;
        for (Integer v : in) {
            int diff = Math.abs(v - value);
            if (diff < min) {
                min = diff;
                closest = v;
            }
        }        
        int closestIndex = in.indexOf(closest);
        return closestIndex;
    }   

    public static void writeDoubleArray(double[][] array, String folder, String fileName) {
        // create the folder if necessary
        createFolder(folder);
        
        String fullPath = folder + "\\" + fileName;   
        try ( BufferedWriter writer = new BufferedWriter(new FileWriter(fullPath))) {                    
            for (double[] rowData: array) {
                for (double rowItem: rowData) {                    
                    writer.write(String.valueOf(rowItem));
                    writer.write(",");
                }
                writer.newLine();
            }
        }
        catch (IOException exp) {
            for (StackTraceElement elem: exp.getStackTrace()) {
                System.out.println(elem);
            }
            throw new RuntimeException("Can't write the array");
        }   
    }
    
    public static double[][] normalizeDoubleArray(double[][] array){
         // find the max
        double max = computeDoubleArrayMax(array);

        // divide all elements by the max
        array = divideDoubleArray(array, max);

        // find the min
        double min = computeDoubleArrayMin(array);

        // subtract all elements by the min
        array = subtractDoubleArray(array, min);

        // multiple all elements by 255
        array = multiplyDoubleArray(array, 255.0);
        
        return array;
    }

    public static BufferedImage drawDoubleArray(double[][] array) {
        int nRows = array.length;
        int nCols = array[0].length;

        array = normalizeDoubleArray(array);

        BufferedImage saveImage = new BufferedImage(nCols, nRows, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = saveImage.getRaster();
        for(int row = 0; row < nRows; row++) {
            for(int col = 0; col < nCols; col++) {
                double pixel = array[row][col];
                int[] pixelArray = new int[]{(int)pixel};
                raster.setPixel(col, row, pixelArray);
            }
        }        
        return saveImage;
    }

    public static double computeDoubleArrayMax(double[][] numbers) {
        double maxValue = Double.MIN_VALUE;
        int nRows = numbers.length;
        int nCols = numbers[0].length;
        int rowStart = 0;
        int rowStop = nRows;
        int colStart = 0;
        int colStop = nCols;
        for (int i = rowStart; i < rowStop; i++) {
            for (int j = colStart; j < colStop; j++) {
                if (numbers[i][j] > maxValue) {
                    maxValue = numbers[i][j];
                }
            }
        }
        return maxValue;
    }

    public static double computeDoubleArrayMin(double[][] numbers) {
        double minValue = Double.MAX_VALUE;
        int nRows = numbers.length;
        int nCols = numbers[0].length;
        int rowStart = 0;
        int rowStop = nRows;
        int colStart = 0;
        int colStop = nCols;
        for (int i = rowStart; i < rowStop; i++) {
            for (int j = colStart; j < colStop; j++) {
                if (numbers[i][j] < minValue) {
                    minValue = numbers[i][j];
                }
            }
        }
        return minValue;
    }

    public static double[][] divideDoubleArray(double[][] numbers, double factor) {
        int nRows = numbers.length;
        int nCols = numbers[0].length;
        
        for (int row = 0; row < nRows; row++) {
            for (int col = 0; col < nCols; col++) {
                numbers[row][col] = (numbers[row][col] / factor);
            }
        }
        return numbers;
    }



    public static double[][] subtractDoubleArray(double[][] numbers, double factor) {
        int nRows = numbers.length;
        int nCols = numbers[0].length;
        
        for (int row = 0; row < nRows; row++) {
            for (int col = 0; col < nCols; col++) {
                numbers[row][col] = (numbers[row][col] - factor);
            }
        }
        return numbers;
    }

    public static double[][] multiplyDoubleArray(double[][] numbers, double factor) {
        int nRows = numbers.length;
        int nCols = numbers[0].length;
        
        for (int row = 0; row < nRows; row++) {
            for (int col = 0; col < nCols; col++) {
                numbers[row][col] = (numbers[row][col] * factor);
            }
        }
        return numbers;
    }
    
    public static BufferedImage loadImage(String path){
        try {
            return ImageIO.read(new File(path));
        }
        catch (IOException exp) {
            for (StackTraceElement elem: exp.getStackTrace()) {
                System.out.println(elem);
            }
            throw new RuntimeException("Can't open the images");
        }
    }
    
    public static void saveImage(BufferedImage img, String path){
        try {
            ImageIO.write(img, "png", new File(path));
        }
        catch (IOException exp) {
            for (StackTraceElement elem: exp.getStackTrace()) {
                System.out.println(elem);
            }
            throw new RuntimeException("Can't open the images");
        }
    }
    
    public static void createFolder(String path) {
        try {
            File newFile = new File(path);        
            if (! newFile.exists()){
                FileUtils.forceMkdir(newFile);
            }
        }
        catch (IOException exp) {
            for (StackTraceElement elem: exp.getStackTrace()) {
                System.out.println(elem);
            }
            throw new RuntimeException("Can't open the images");
        } 
    }
}

