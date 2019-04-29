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

public class PhaseMap_StripesAndWaves {

    public PhaseMap_StripesAndWaves(String configPath) {
        
        // Load the configuration
        System.out.println("Loading the configuration data: Please Wait ...");
        Element e = Util.loadXML(configPath);
        
        // Get the image names
        System.out.println("Getting the image names: Please Wait ...");
        String phaseFitFramesDirPath = e.selectSingleNode("/config/folders/base").getText() + "\\" +
                                       e.selectSingleNode("/config/folders/phaseFitFrames").getText();
        File phaseFitFramesDir = new File(phaseFitFramesDirPath);
        String[] imageNames = phaseFitFramesDir.list();

        // Get the phase image sequence configuration
        List<Node> phaseFrameConfigList = e.selectNodes("/config/structuredLight/frame");        
        
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
        double[][] hPhaseMap = new double[nRows][nCols];
        double[][] vPhaseMap = new double[nRows][nCols];
        double[][][] waveBuffer = new double[nRows][nCols][nWaves];
        int waveIndex = 0;
        List<Integer> calibLevels = Util.parseIntegerArray(e.selectSingleNode("/config/structuredLight/calibLevels").getText());
        for (int i = 0; i < phaseFrameConfigList.size(); i++) {
            
            System.out.println("Now analyzing image: " + i);
            
            // load the image
            String name = imageNames[i];
            BufferedImage image = loadImage(phaseFitFramesDirPath + "\\" + name);
            WritableRaster raster = image.getRaster();

            // get the image config
            Node phaseFrameConfig = phaseFrameConfigList.get(i);           

            // found new stripe frame
            if (phaseFrameConfig.selectSingleNode("type").getText().equals("stripe")) {

                // compute the new phase per stripe
                List<Integer> levelIndices = Util.parseIntegerArray(phaseFrameConfig.selectSingleNode("levelIndices").getText());
                List<Integer> levelSet = new ArrayList<>();
                for (Integer index: levelIndices) {
                    levelSet.add(calibLevels.get(index));
                }
                Double phasePerStripe = Double.parseDouble(phaseFrameConfig.selectSingleNode("phasePerStripe").getText());
                String orientation = phaseFrameConfig.selectSingleNode("orientation").getText();
                
                for (int row = 0; row < nRows; row++) {
                    for (int col = 0; col < nCols; col++) {

                        // read the pixel   
                        int[] pixel = new int[1];
                        raster.getPixel(col, row, pixel);
                        int gray = pixel[0];  
                        
                        // compute the new phase
                        Double newPhase = phasePerStripe*closestIndex(gray, levelSet);

                        // store the new phase
                        if (orientation.equals("vertical")) {                            
                            hPhaseMap[row][col] += newPhase;
                        }
                        else if (orientation.equals("horizontal")) {                            
                            vPhaseMap[row][col] += newPhase;
                        }
                    }
                }                                                
            }

            // found new wave frame
            else if (phaseFrameConfig.selectSingleNode("type").getText().equals("wave")) {  

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
                    for (int row = 0; row < nRows; row++) {
                        for (int col = 0; col < nCols; col++) {                        
                            double[] I = waveBuffer[row][col];
                            double newPhase = Math.PI + FastMath.atan2((I[3] - I[1]),(I[2] - I[0]));  
                            if (orientation.equals("vertical")) {
                                hPhaseMap[row][col] += newPhase;
                            }
                            else if (orientation.equals("horizontal")) {                            
                                vPhaseMap[row][col] += newPhase;
                            }
                        }                        
                    }
                    waveIndex = 0;
                }
            }
        }            

        // Get the folder path
        String phaseMapDirPath = e.selectSingleNode("/config/folders/base").getText() + "\\" +
                                 e.selectSingleNode("/config/folders/phaseMap").getText();
        
        // Create folder if necessary
        createFolder(phaseMapDirPath); 

        // Export the phase data        
        System.out.println("Saving the phase maps data: Please Wait ...");
        
        String horizPhaseMapDataFileName = e.selectSingleNode("/config/files/horizPhaseMapData").getText();
        writeDoubleArray(hPhaseMap, phaseMapDirPath, horizPhaseMapDataFileName);

        String vertPhaseMapDataFileName = e.selectSingleNode("/config/files/vertPhaseMapData").getText();
        writeDoubleArray(vPhaseMap, phaseMapDirPath, vertPhaseMapDataFileName);

        // Draw the phase images
        System.out.println("Saving the phase maps image: Please Wait ...");
        
        String horizPhaseMapImageFileName = e.selectSingleNode("/config/files/horizPhaseMapImage").getText();
        BufferedImage horizPhaseMapImage = drawDoubleArray(hPhaseMap);
        saveImage(horizPhaseMapImage,phaseMapDirPath + "\\" + horizPhaseMapImageFileName);

        String vertPhaseMapImageFileName = e.selectSingleNode("/config/files/vertPhaseMapImage").getText();
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

