package intensitycalibration;

import config.Util;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.analysis.function.Power;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.dom4j.Element;
import org.dom4j.Node;

public class IntensityCalibration {

    public IntensityCalibration(String configPath) {      
        
        // load configuration data
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
            calibrateIntensity(e, baseDirPath);
        } 
    }
    
    public final void calibrateIntensity(Element root, String baseDirPath)
    {
        // load image stacks
        System.out.println("Loading images: Please Wait ...");
        List<BufferedImage> calibFrames = loadStack(baseDirPath + "\\" +
                                                        root.selectSingleNode("/config/folders/calibFrames").getText());
        List<BufferedImage> phaseFrames = loadStack(baseDirPath + "\\" +
                                                        root.selectSingleNode("/config/folders/phaseFrames").getText());
        
        // polynomial fitting
        System.out.println("Performing polynomial fitting: Please Wait ...");
        List<Integer> calibLevels = Util.parseIntegerArray(root.selectSingleNode("/config/structuredLight/calibLevels").getText());
        List<List<RealVector>> P = polyFitStack(calibFrames, calibLevels);
        
        // apply fitting to stacks
        System.out.println("Applying fitting results to images: Please Wait ...");
        List<BufferedImage> calibFramesFitted = polyValStack(P, calibFrames);
        List<BufferedImage> phaseFramesFitted = polyValStack(P, phaseFrames);
        
        // save image stacks
        System.out.println("Saving images: Please Wait ...");
        String calibFitFramesPath = baseDirPath + "\\" +
                                    root.selectSingleNode("/config/folders/calibFitFrames").getText();
        saveStack(calibFramesFitted, calibFitFramesPath);
        String phaseFitFramesPath = baseDirPath + "\\" +
                                    root.selectSingleNode("/config/folders/phaseFitFrames").getText();
        saveStack(phaseFramesFitted, phaseFitFramesPath);
    }
    
    public static List<BufferedImage> loadStack(String dirPath) {
        try {
            // get the image names
            File dir = new File(dirPath);
            String[] names = dir.list();

            // load the images
            List<BufferedImage> frames = new ArrayList<>();
            for (String name: names) {
                frames.add(ImageIO.read(new File(dirPath + "\\" + name)));
            } 
            return frames;
        }
        catch (IOException exp) {
            for (StackTraceElement el: exp.getStackTrace()) {
                System.out.println(el);
            }
            throw new RuntimeException("Could not load the image stack.");
        }
    }
    
    public static void saveStack(List<BufferedImage> stack, String saveDirPath) {
        try {
            // create directory if needed
            File saveDir = new File(saveDirPath);
            if (! saveDir.exists()){
                FileUtils.forceMkdir(saveDir);
            }

            // save the images
            for (Integer i = 0; i < stack.size(); i++) {
                File newFile = new File(saveDirPath + "\\" + String.format("%03d.png", i));
                ImageIO.write(stack.get(i), "png", newFile);
            }
        }
        catch (IOException exp) {
            for (StackTraceElement el: exp.getStackTrace()) {
                System.out.println(el);
            }
            throw new RuntimeException("Could not save the image stack.");
        }
    }
    
    
    public static List<List<RealVector>> polyFitStack(List<BufferedImage> stack, List<Integer> calibLevels) {
        
        // compute variables
        int nRows = stack.get(0).getHeight();
        int nCols = stack.get(0).getWidth();   
        
        // perform curve fitting on the stack
        int polyFitDeg = 3;
        int nCalibLevels = calibLevels.size();
        RealMatrix A = MatrixUtils.createRealMatrix(nCalibLevels, polyFitDeg + 1);
        double[] calibLevelDouble = new double[nCalibLevels];
        for (int i = 0; i < nCalibLevels; i++){
            calibLevelDouble[i] = (double)calibLevels.get(i);
        }
        RealVector B = MatrixUtils.createRealVector(calibLevelDouble);       
        RealVector Z = MatrixUtils.createRealVector(new double[nCalibLevels]);
        List<List<RealVector>> P = new ArrayList<>();      
        for (int row = 0; row < nRows; row++) {
            List<RealVector> Prow = new ArrayList<>();
            for (int col = 0; col < nCols; col++) {
                // extract a Z profile
                for (int slice = 0; slice < nCalibLevels; slice++){                    
                    double[] newEntryArray = new double[1];
                    stack.get(slice).getRaster().getPixel(col, row, newEntryArray);
                    Z.setEntry(slice, newEntryArray[0]);                   
                }
                // build the "A" matrix
                int Acol = 0;
                for (double order = polyFitDeg; Acol < polyFitDeg + 1; order--, Acol++){
                    A.setColumnVector(Acol, Z.map(new Power(order)));
                }
                // solve "A*x = B"
                RealVector p = MatrixUtils.createRealVector(new double[polyFitDeg+1]);
                try {
                   DecompositionSolver solver = new QRDecomposition(A).getSolver();
                   p = solver.solve(B); 
                }
                catch (SingularMatrixException e) {                   
                }
                
                Prow.add(p);                
            }
            P.add(Prow);
        }
        return P;
    }
    
    public static List<BufferedImage> polyValStack(List<List<RealVector>> P, List<BufferedImage> stack) {    
        // compute variables
        int nRows = stack.get(0).getHeight();
        int nCols = stack.get(0).getWidth();
        int nSlices = stack.size();
        int polyFitDeg = P.get(0).get(0).getDimension() - 1;

        // apply fitting the images
        List<BufferedImage> stackFitted = new ArrayList<>(stack);      
        RealVector Z = MatrixUtils.createRealVector(new double[nSlices]);
        RealMatrix A = MatrixUtils.createRealMatrix(nSlices, polyFitDeg + 1);
        for (int row = 0; row < nRows; row++) {
            for (int col = 0; col < nCols; col++) {
                
                // extract a Z profile
                for (int slice = 0; slice < nSlices; slice++){                    
                    double[] newEntryArray = new double[1];
                    stackFitted.get(slice).getRaster().getPixel(col, row, newEntryArray); 
                    Z.setEntry(slice, newEntryArray[0]);
                }
                
                // build the "A" matrix
                int Acol = 0;
                for (double order = polyFitDeg; Acol < polyFitDeg + 1; order--, Acol++){
                    A.setColumnVector(Acol, Z.map(new Power(order)));
                }
                
                // compute fitted Z profile
                RealVector Zfitted = A.operate(P.get(row).get(col));
                
                // store fitted Z profile
                for (int slice = 0; slice < nSlices; slice++){               
                    double[] newEntryArray = {Zfitted.getEntry(slice)};
                    int newEntry = (int)Math.round(newEntryArray[0]);
                    if (newEntry > 255) {
                        newEntry = 255;
                    }
                    else if (newEntry < 0){
                        newEntry = 0;
                    }
                    stackFitted.get(slice).getRaster().setPixel(col, row, new int[]{newEntry});
                }                
            }
        }
        
        return stackFitted;
    }
        
    
}

//        // load the calibration image stack
//        String calibFramesDirPath = config.getCalibFramesDir();
//        File calibFramesDir = new File(calibFramesDirPath);
//        String[] calibFrameNames = calibFramesDir.list();
//        ArrayList<BufferedImage> calibFrames = new ArrayList<>();
//        for (String name: calibFrameNames) {
//            calibFrames.add(ImageIO.read(new File(calibFramesDirPath + "\\" + name)));
//        }        
//        
//        // compute variables for frame detection
//        int nRows = calibFrames.get(0).getHeight();
//        int nCols = calibFrames.get(0).getWidth();   
//        
//        // perform curve fitting on the calibration image stack
//        System.out.println("Polynomial Fitting: Please Wait ...");
//        int polyFitDeg = 3;
//        ArrayList<Integer> calibLevels = config.getCalibLevels();
//        int nCalibImages = calibLevels.size();
//        RealMatrix calibImageStackA = MatrixUtils.createRealMatrix(nCalibImages, polyFitDeg + 1);
//        double[] calibLevelDouble = new double[nCalibImages];
//        for (int i = 0; i < nCalibImages; i++){
//            calibLevelDouble[i] = (double)calibLevels.get(i);
//        }
//        RealVector B = MatrixUtils.createRealVector(calibLevelDouble);       
//        RealVector calibImageStackZ = MatrixUtils.createRealVector(new double[nCalibImages]);
//        ArrayList<ArrayList<RealVector>> P = new ArrayList<>();      
//        for (int row = 0; row < nRows; row++) {
//            ArrayList<RealVector> Prow = new ArrayList<>();
//            for (int col = 0; col < nCols; col++) {
//                // extract a Z profile
//                for (int slice = 0; slice < nCalibImages; slice++){                    
//                    double[] newEntryArray = new double[1];
//                    calibFrames.get(slice).getRaster().getPixel(col, row, newEntryArray);
//                    calibImageStackZ.setEntry(slice, newEntryArray[0]);                   
//                }
//                // build the "A" matrix
//                int Acol = 0;
//                for (double order = polyFitDeg; Acol < polyFitDeg + 1; order--, Acol++){
//                    calibImageStackA.setColumnVector(Acol, calibImageStackZ.map(new Power(order)));
//                }
//                // solve "A*x = B"
//                RealVector p = MatrixUtils.createRealVector(new double[polyFitDeg+1]);
//                try {
//                   DecompositionSolver solver = new QRDecomposition(calibImageStackA).getSolver();
//                   p = solver.solve(B); 
//                }
//                catch (SingularMatrixException e) {                   
//                }
//                
//                Prow.add(p);                
//            }
//            P.add(Prow);
//        } 
//        
//        // apply fitting to calibration images
//        System.out.println("Fitting Calibration Image Stack: Please Wait ...");
//        ArrayList<BufferedImage> calibFramesFitted = new ArrayList<>(calibFrames);        
//        for (int row = 0; row < nRows; row++) {
//            for (int col = 0; col < nCols; col++) {
//                
//                // extract a Z profile
//                for (int slice = 0; slice < nCalibImages; slice++){                    
//                    double[] newEntryArray = new double[1];
//                    calibFramesFitted.get(slice).getRaster().getPixel(col, row, newEntryArray); 
//                    calibImageStackZ.setEntry(slice, newEntryArray[0]);
//                }
//                
//                // build the "A" matrix
//                int Acol = 0;
//                for (double order = polyFitDeg; Acol < polyFitDeg + 1; order--, Acol++){
//                    calibImageStackA.setColumnVector(Acol, calibImageStackZ.map(new Power(order)));
//                }
//                
//                // compute fitted Z profile
//                RealVector Zfitted = calibImageStackA.operate(P.get(row).get(col));
//                
//                // store fitted Z profile
//                for (int slice = 0; slice < nCalibImages; slice++){               
//                    double[] newEntryArray = {Zfitted.getEntry(slice)};
//                    int newEntry = (int)Math.round(newEntryArray[0]);
//                    if (newEntry > 255) {
//                        newEntry = 255;
//                    }
//                    else if (newEntry < 0){
//                        newEntry = 0;
//                    }
//                    calibFramesFitted.get(slice).getRaster().setPixel(col, row, new int[]{newEntry});
//                }                
//            }
//        }
//        // save fitted calibration images
//        String calibFitFramesDirPath = config.getCalibFitFramesDir();
//        File calibFitFramesDir = new File(calibFitFramesDirPath);
//        if (! calibFitFramesDir.exists()){
//            FileUtils.forceMkdir(calibFitFramesDir);
//        }
//        for (Integer i = 0; i < calibFramesFitted.size(); i++) {
//            File newFile = new File(calibFitFramesDirPath + "\\" + String.format("%03d.png", i));
//            ImageIO.write(calibFramesFitted.get(i), "png", newFile);
//        }
//        
//        // load the phase image stack
//        String phaseFramesDirPath = config.getPhaseFramesDir();
//        File phaseFrameDir = new File(phaseFramesDirPath);
//        String[] phaseFrameNames = phaseFrameDir.list();
//        ArrayList<BufferedImage> phaseFrames = new ArrayList<>();
//        for (String name: phaseFrameNames) {
//            phaseFrames.add(ImageIO.read(new File(phaseFramesDirPath + "\\" + name)));
//        }
//        
//        // apply fitting to phase images
//        System.out.println("Fitting Phase Image Stack: Please Wait ...");
//        ArrayList<HashMap<String,String>> phaseImgSeqConfig = config.getPhaseFrameSequence();
//        int nPhaseImages = phaseImgSeqConfig.size();
//        ArrayList<BufferedImage> phaseFramesFitted = new ArrayList<>(phaseFrames);
//        RealVector phaseImageStackZ = MatrixUtils.createRealVector(new double[nPhaseImages]);
//        RealMatrix phaseImageStackA = MatrixUtils.createRealMatrix(nPhaseImages, polyFitDeg + 1);
//        for (int row = 0; row < nRows; row++) {
//            for (int col = 0; col < nCols; col++) {
//                
//                // extract a Z profile
//                for (int slice = 0; slice < nPhaseImages; slice++){                    
//                    double[] newEntryArray = new double[1];
//                    phaseFramesFitted.get(slice).getRaster().getPixel(col, row, newEntryArray);  
//                    phaseImageStackZ.setEntry(slice, newEntryArray[0]);
//                }
//                
//                // build the "A" matrix
//                int Acol = 0;
//                for (double order = polyFitDeg; Acol < polyFitDeg + 1; order--, Acol++){
//                    phaseImageStackA.setColumnVector(Acol, phaseImageStackZ.map(new Power(order)));
//                }
//                
//                // compute fitted Z profile
//                RealVector Zfitted = phaseImageStackA.operate(P.get(row).get(col));
//                
//                // store fitted Z profile
//                for (int slice = 0; slice < nPhaseImages; slice++){      
//                    double[] newEntryArray = {Zfitted.getEntry(slice)};
//                    int newEntry = (int)Math.round(newEntryArray[0]);
//                    if (newEntry > 255) {
//                        newEntry = 255;
//                    }
//                    else if (newEntry < 0){
//                        newEntry = 0;
//                    }
//                    phaseFramesFitted.get(slice).getRaster().setPixel(col, row, new int[]{newEntry});  
//                }                
//            }
//        }
//        
//        // save fitted phase images
//        String phaseFrameFitDirPath = config.getPhaseFitFramesDir();
//        File phaseFrameFitDir = new File(phaseFrameFitDirPath);
//        if (! phaseFrameFitDir.exists()){
//            FileUtils.forceMkdir(phaseFrameFitDir);
//        }
//        for (Integer i = 0; i < phaseFramesFitted.size(); i++) {
//            File newFile = new File(phaseFrameFitDirPath + "\\" + String.format("%03d.png", i));
//            ImageIO.write(phaseFramesFitted.get(i), "png", newFile);
//        }
