package videoframes;

import config.Util;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.dom4j.Element;
import org.dom4j.Node;

public class FrameSelection {

    public FrameSelection(String configPath) {
        // load configuration data
        System.out.println("Loading configuration data: Please Wait ...");
        Element e = Util.loadXML(configPath);
        
        // get each folder to work one
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
            selectFrames(e, baseDirPath);
        }        

    }
    
    public final void selectFrames(Element root, String baseDirPath)
    {
        // find the sync frames
        System.out.println("Identifying the sync frames: Please Wait ...");
        boolean pulseOn = false;
        Integer pulseStartIndex = 0;
        Integer pulseStopIndex = 0;
        Integer nPulsesFound = 0;  
        Integer nSyncPulses = Integer.parseInt(root.selectSingleNode("/config/structuredLight/nSyncPulses").getText());
        ArrayList<Integer> pulseIndices = new ArrayList<>(nSyncPulses);
        int syncFrameThresh = 100;
        String captureFramesDirPath = baseDirPath + "\\" +
                                      root.selectSingleNode("/config/folders/captureFrames").getText();
        File captureFramesDir = new File(captureFramesDirPath);
        String[] captureFrameNames = captureFramesDir.list();
        
        double frameAvgPrev = 0;
        Integer frameIndex = 0;  
        Integer nRows = Integer.parseInt(root.selectSingleNode("/config/structuredLight/nRows").getText());
        Integer nCols = Integer.parseInt(root.selectSingleNode("/config/structuredLight/nCols").getText());
        while (nPulsesFound < nSyncPulses) {
                        
            // lower the sync frame threshold if there are no more images to check
            if (frameIndex >= captureFrameNames.length) {
                // stop if the sync frame threshold does below 30
                if (syncFrameThresh < 30)
                {
                    throw new IllegalArgumentException("Can't find the sync pulses");
                }
                else
                {
                    syncFrameThresh -= 10;
                    frameIndex = 0;
                    System.out.println("Sync frame threshold lowered to " + syncFrameThresh);
                }
            }
            
            // read the next frame
            String name = captureFrameNames[frameIndex];
            File newImageFile = new File(captureFramesDirPath + "\\" + name);
            BufferedImage grayFrame;
            try {
                grayFrame = ImageIO.read(newImageFile);
            }
            catch (IOException exp) {
                throw new RuntimeException("Could not open the image");
            }
                       
            // compute the average frame brightness
            double frameAvg = 0;
            Raster raster = grayFrame.getData();
            for (int row = 0; row < nRows; row++) {
                for (int col = 0; col < nCols; col++) {
                    double[] newValueArray = new double[1];
                    raster.getPixel(col, row, newValueArray);
                    frameAvg += newValueArray[0];     
                }
            }
            frameAvg = frameAvg / (nRows*nCols);           
            
            // a pulse has begun
            if (!pulseOn && (frameAvg - frameAvgPrev) > syncFrameThresh) {
                pulseStartIndex = frameIndex;
                pulseOn = true;
            }
            
            // a pulse has ended
            else if (pulseOn && (frameAvgPrev - frameAvg) > syncFrameThresh) {
                pulseStopIndex = frameIndex - 1;
                Integer pulseMiddleIndex = Math.round((pulseStartIndex + pulseStopIndex)/2.0f);
                pulseIndices.add(pulseMiddleIndex);
                pulseOn = false;
                nPulsesFound += 1;
            }
                
            frameIndex += 1;
            frameAvgPrev = frameAvg;
        }
        
        // save the sync frames
        System.out.println("Saving sync frames: Please Wait ...");
        String syncFramesDirPath =  baseDirPath + "\\" +
                                    root.selectSingleNode("/config/folders/syncFrames").getText();
        File syncFramesDir = new File(syncFramesDirPath);
        try {
            if (! syncFramesDir.exists()){
                FileUtils.forceMkdir(syncFramesDir);
            }
            for (Integer index: pulseIndices){
                String name = captureFrameNames[index];            
                BufferedImage frame = ImageIO.read(new File(captureFramesDirPath + "\\" + name));
                String saveName = String.format("\\%03d.png", index);
                File saveFile = new File(syncFramesDirPath + saveName);            
                ImageIO.write(frame, "png", saveFile);            
            }
        }
        catch (IOException exp) {            
            throw new RuntimeException("Could not save the sync frames");
        }
        
        // compute the frame stride
        System.out.println("Computing the frame stride: Please Wait ...");
        Float pulseDistanceSum = 0.0f;
        for (int j = 0; j < pulseIndices.size()-1; j++) {
            pulseDistanceSum += (pulseIndices.get(j+1) - pulseIndices.get(j));
        }
        Float pulseDistanceAvg = pulseDistanceSum/(pulseIndices.size()-1);
        Integer frameStride = Math.round(pulseDistanceAvg/2);

        // compute the calibration frame indices
        System.out.println("Computing the calibration frame indices: Please Wait ...");
        String xxx = root.selectSingleNode("/config/structuredLight/calibLevels").getText();
        Integer nCalibImages = Util.parseIntegerArray(root.selectSingleNode("/config/structuredLight/calibLevels").getText()).size();
        int calibFrameIndexStart = pulseIndices.get(pulseIndices.size()-1) + frameStride;
        int calibFrameIndexStop = calibFrameIndexStart + nCalibImages * frameStride;
        ArrayList<Integer> calibFrameIndices = new ArrayList<>();
        for (int index = calibFrameIndexStart; index < calibFrameIndexStop; index += frameStride) {
            calibFrameIndices.add(index);
        }
        
        // save the calibration frames
        System.out.println("Saving calibration frames: Please Wait ...");
        String calibFramesDirPath = baseDirPath + "\\" +
                                    root.selectSingleNode("/config/folders/calibFrames").getText();
        File calibFramesDir = new File(calibFramesDirPath);    
        try {
            if (! calibFramesDir.exists()){
                FileUtils.forceMkdir(calibFramesDir);
            }
            for (Integer index: calibFrameIndices){
                String name = captureFrameNames[index];            
                BufferedImage frame = ImageIO.read(new File(captureFramesDirPath + "\\" + name));
                String saveName = String.format("%03d.png", index);
                File saveFile = new File(calibFramesDirPath + "\\" + saveName);            
                ImageIO.write(frame, "png", saveFile);            
            }
        }
        catch (IOException exp) {            
            throw new RuntimeException("Could not save the calib frames");
        }

        // compute the phase frame indices     
        System.out.println("Computing the phase frame indices: Please Wait ...");
        Integer nPhaseImages = root.selectNodes("/config/structuredLight/frame").size();
        int phaseFrameIndexStart = calibFrameIndexStop;
        int phaseFrameIndexStop = phaseFrameIndexStart + nPhaseImages * frameStride;
        ArrayList<Integer> phaseFrameIndices = new ArrayList<>();
        for (int index = phaseFrameIndexStart; index < phaseFrameIndexStop; index += frameStride) {
            phaseFrameIndices.add(index);
        }      
        
        // save the phase frames
        System.out.println("Saving phase frames: Please Wait ...");
        String phaseFramesDirPath =  baseDirPath + "\\" +
                                     root.selectSingleNode("/config/folders/phaseFrames").getText();
        File phaseFramesDir = new File(phaseFramesDirPath);        
        try {
            if (! phaseFramesDir.exists()){
                FileUtils.forceMkdir(phaseFramesDir);
            }
            for (Integer index: phaseFrameIndices){
                String name = captureFrameNames[index];            
                BufferedImage frame = ImageIO.read(new File(captureFramesDirPath + "\\" + name));
                String saveName = String.format("%03d.png", index);
                File saveFile = new File(phaseFramesDirPath + "\\" + saveName);            
                ImageIO.write(frame, "png", saveFile);            
            }
        }
        catch (IOException exp) {            
            throw new RuntimeException("Could not save the phase frames");
        }
        
        // compute the average brighness of all frames
        System.out.println("Computing average brightness of all frames: Please Wait ...");
        ArrayList<Double> frameAvgList = new ArrayList<>();
        ArrayList<Integer> frameIndexList = new ArrayList<>();
        try {
            for (Integer frameAvgIndex = 0;frameAvgIndex < captureFrameNames.length; frameAvgIndex++){
                String name = captureFrameNames[frameAvgIndex];
                BufferedImage frame = ImageIO.read(new File(captureFramesDirPath + "\\" + name));
                double frameAvg = 0;
                Raster raster = frame.getData();
                for (int row = 0; row < nRows; row++) {
                    for (int col = 0; col < nCols; col++) {
                        double[] newValueArray = new double[1];
                        raster.getPixel(col, row, newValueArray);
                        frameAvg += newValueArray[0];     
                    }
                }
                frameAvg = frameAvg / (nRows*nCols);
                frameAvgList.add(frameAvg);
                frameIndexList.add(frameAvgIndex);
            }
        }
        catch (IOException exp) {            
            throw new RuntimeException("Could not open the images");
        }
        
        // define plot data
        System.out.println("Defining plot data: Please Wait ...");
        
        // define plot set 1 data: mean frame brightness        
        int n1 = frameIndexList.size();
        ArrayList<Integer> x1 = new ArrayList<>(n1);
        ArrayList<Double> y1 = new ArrayList<>(n1);
        for (int x = pulseIndices.get(0) - 5; x < phaseFrameIndices.get(phaseFrameIndices.size() - 1) + 5; x++) {
          x1.add(x);
          y1.add(frameAvgList.get(x));
        }
        // define plot set 2 data: pulse frames
        int n2 = pulseIndices.size();
        ArrayList<Integer> x2 = new ArrayList<>(n2);
        ArrayList<Double> y2 = new ArrayList<>(n2);
        for (int i = 0; i < n2; i++) {
          x2.add(pulseIndices.get(i));
          y2.add(frameAvgList.get(pulseIndices.get(i)));
        }
        // define plot set 3 data: calibration frames
        int n3 = calibFrameIndices.size();
        ArrayList<Integer> x3 = new ArrayList<>(n3);
        ArrayList<Double> y3 = new ArrayList<>(n3);
        for (int i = 0; i < n3; i++) {
          x3.add(calibFrameIndices.get(i));
          y3.add(frameAvgList.get(calibFrameIndices.get(i)));
        }
        // define plot set 4 data: phase frames
        int n4 = phaseFrameIndices.size();
        ArrayList<Integer> x4 = new ArrayList<>(n3);
        ArrayList<Double> y4 = new ArrayList<>(n3);
        for (int i = 0; i < n4; i++) {
          x4.add(phaseFrameIndices.get(i));
          y4.add(frameAvgList.get(phaseFrameIndices.get(i)));
        }
        
        // save plot data
        System.out.println("Saving plot data: Please Wait ...");
        String plotDirPath = baseDirPath + "\\" +
                             root.selectSingleNode("/config/folders/framePlot").getText();
        File plotDir = new File(plotDirPath);
        try {
            if (! plotDir.exists()){
            FileUtils.forceMkdir(plotDir);
            }
            String framePlotDataFileName = root.selectSingleNode("/config/files/framePlotData").getText();
            File plotFile = new File(plotDirPath + "\\" + framePlotDataFileName);
            FileWriter fileWriter = new FileWriter(plotFile);
            BufferedWriter buffFileWriter = new BufferedWriter(fileWriter);
            String newString;
        
            buffFileWriter.write("phase frames");
            buffFileWriter.newLine();
            newString = x4.toString();
            buffFileWriter.write(newString.substring(1, newString.length()-1));
            buffFileWriter.newLine();
            newString = y4.toString();
            buffFileWriter.write(newString.substring(1, newString.length()-1));
            buffFileWriter.newLine();
            
            buffFileWriter.write("calibration frames");
            buffFileWriter.newLine();
            newString = x3.toString();
            buffFileWriter.write(newString.substring(1, newString.length()-1));
            buffFileWriter.newLine();
            newString = y3.toString();
            buffFileWriter.write(newString.substring(1, newString.length()-1));
            buffFileWriter.newLine();
            
            buffFileWriter.write("pulse frames");
            buffFileWriter.newLine();
            newString = x2.toString();
            buffFileWriter.write(newString.substring(1, newString.length()-1));
            buffFileWriter.newLine();
            newString = y2.toString();
            buffFileWriter.write(newString.substring(1, newString.length()-1));
            buffFileWriter.newLine();
            
            buffFileWriter.write("average frame brightness");
            buffFileWriter.newLine();
            newString = x1.toString();
            buffFileWriter.write(newString.substring(1, newString.length()-1));
            buffFileWriter.newLine();
            newString = y1.toString();
            buffFileWriter.write(newString.substring(1, newString.length()-1));
            buffFileWriter.newLine();
            
            buffFileWriter.close();
        }
        catch (IOException exp) {            
            throw new RuntimeException("Could not save the frame plot data");
        }
        
        
    }

}
