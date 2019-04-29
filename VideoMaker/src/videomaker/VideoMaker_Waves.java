package videomaker;

import config.Util;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.dom4j.Element;
import org.dom4j.Node;

public class VideoMaker_Waves {

    public VideoMaker_Waves(String xmlPath) {
        // get the configuration data
        Element root = Util.loadXML(xmlPath);

        // define variables for image creation
        File newFile;
        int highLevel = 255;
        int lowLevel = 0;
        int frameIndex = 0;
        int nRows = Integer.parseInt(root.selectSingleNode("/config/structuredLight/nRows").getText());
        int nCols = Integer.parseInt(root.selectSingleNode("/config/structuredLight/nCols").getText());
        int nFrameRepeats = Integer.parseInt(root.selectSingleNode("/config/structuredLight/nFrameRepeats").getText());
        int nSyncPulses = Integer.parseInt(root.selectSingleNode("/config/structuredLight/nSyncPulses").getText());
        String baseDir = root.selectSingleNode("/config/baseFolder").getText();
        String framesDir = baseDir + "\\" + 
                           root.selectSingleNode("/config/projectionFolder").getText() + "\\" +
                           root.selectSingleNode("/config/folders/createdFrames").getText() ;
        
        // Create the save folder if necessary
        try {
            File saveDir = new File(framesDir);        
            if (! saveDir.exists()){
                FileUtils.forceMkdir(saveDir);
            }        
        
            // create synchronization images
            BufferedImage newImage = new BufferedImage(nCols, nRows, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster = newImage.getRaster();
            for (int pulse =0; pulse < nSyncPulses; pulse++) {            
                // update the raster data with dark frames            
                for (int row = 0; row < nRows; row++) {
                    for (int col = 0; col < nCols; col++) {
                        raster.setPixel(col, row, new int[]{lowLevel});                    
                    }
                }            
                // write the images
                for (int repeat = 0; repeat < nFrameRepeats; repeat++) {                
                    newFile = new File(framesDir + "\\" + String.format("%03d", frameIndex) + ".png");
                    ImageIO.write(newImage, "png", newFile);
                    frameIndex += 1;                            
                }

                // update the raster data with bright frames
                for (int row = 0; row < nRows; row++) {
                    for (int col = 0; col < nCols; col++) {
                        raster.setPixel(col, row, new int[]{highLevel});                    
                    }
                }            
                // write the images
                for (int repeat = 0; repeat < nFrameRepeats; repeat++) {                
                    newFile = new File(framesDir + "\\" + String.format("%03d", frameIndex) + ".png");
                    ImageIO.write(newImage, "png", newFile);
                    frameIndex += 1;                            
                }
            }

            // create intensity calibration images
            List<Integer> calibLevels = Util.parseIntegerArray(root.selectSingleNode("/config/structuredLight/calibLevels").getText());
            for (int level: calibLevels) {
                // update the raster data
                for (int row = 0; row < nRows; row++) {
                    for (int col = 0; col < nCols; col++) {
                        raster.setPixel(col, row, new int[]{level});                        
                    }
                }
                // write the images
                for (int repeat = 0; repeat < nFrameRepeats; repeat++) {                
                    newFile = new File(framesDir + "\\" + String.format("%03d", frameIndex) + ".png");
                    ImageIO.write(newImage, "png", newFile);
                    frameIndex += 1;
                }
            }
        
            // write the phase encoding images
            List<Node> frameNodes = root.selectNodes("/config/structuredLight/frame");
            Integer amplitude = Integer.parseInt(root.selectSingleNode("/config/structuredLight/waveAmplitude").getText());
            Integer offset = Integer.parseInt(root.selectSingleNode("/config/structuredLight/waveOffset").getText());
            for (Node frameNode: frameNodes) {
                
                // set parameters
                Double phaseOffsetFactor = Double.parseDouble(frameNode.selectSingleNode("phaseOffsetFactor").getText());
                String orientation = frameNode.selectSingleNode("orientation").getText();
                Double waveLength = Double.parseDouble(frameNode.selectSingleNode("waveLength").getText());
                int[] sinValues = new int[nCols];

                if (orientation.equals("vertical")) {
                    // compute the sine values
                    for (int x = 0; x < nCols; x++) {
                    sinValues[x] = (int)(amplitude*Math.cos( (2*Math.PI/waveLength) * (x+0.5) - (Math.PI*phaseOffsetFactor) ) + offset); // the -0.5 is critical
                    }

                    // update the raster data row-by-row with wave images
                    for (int row = 0; row < nRows; row++){
                        for (int col = 0; col < nCols; col++){
                            raster.setPixel(col, row, new int[]{sinValues[col]});
                        }
                    }
                }
                else if (orientation.equals("horizontal")) {
                    // compute the sine values
                    for (int y = 0; y < nRows; y++) {
                    sinValues[y] = (int)(amplitude*Math.cos( (2*Math.PI/waveLength) * (y+0.5) - (Math.PI*phaseOffsetFactor) ) + offset); // the -0.5 is critical
                    }

                    // update the raster data col-by-col with wave images
                    for (int col = 0; col < nCols; col++){
                        for (int row = 0; row < nRows; row++){
                            raster.setPixel(col, row, new int[]{sinValues[row]});
                        }
                    }
                } 

                // write the wave images
                for (int repeat = 0; repeat < nFrameRepeats; repeat++) {
                    newFile = new File(framesDir + "\\" + String.format("%03d", frameIndex) + ".png");
                    ImageIO.write(newImage, "png", newFile);
                    frameIndex += 1;
                }                             
                           
            }
        
            // update the raster data with dark images       
            for (int row = 0; row < nRows; row++) {
                for (int col = 0; col < nCols; col++) {
                    raster.setPixel(col, row, new int[]{lowLevel});                        
                }
            }
            // write the dark images
            for (int repeat = 0; repeat < nFrameRepeats; repeat++) {            
                newFile = new File(framesDir + "\\" + String.format("%03d", frameIndex) + ".png");
                ImageIO.write(newImage, "png", newFile);
                frameIndex += 1;
            }
        }
        catch (IOException exp) {
            for (StackTraceElement elem: exp.getStackTrace()) {
                System.out.println(elem);
            }
            throw new RuntimeException("Could not write the images");
        }        
    }
}
