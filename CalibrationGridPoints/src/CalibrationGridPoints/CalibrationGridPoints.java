package CalibrationGridPoints;

import config.Util;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.dom4j.Element;
import org.dom4j.Node;

public class CalibrationGridPoints {
       
    public CalibrationGridPoints(String configPath) {
        
        // Load the configuration
        Element e = Util.loadXML(configPath);        
        
        List<Node> calibFolders = e.selectNodes("/config/calibrationFolders/folder");
        List<Node> sceneFolders = e.selectNodes("/config/sceneFolders/folder");
        
        // work on each folder using its own base path
        for (Node folder: calibFolders)
        {            
            String baseDirPath = e.selectSingleNode("/config/baseFolder").getText() + "\\" +
                                 folder.selectSingleNode("name").getText();
            System.out.println("\nNow working on images from: " + baseDirPath);
            double worldZ = Double.parseDouble(folder.selectSingleNode("worldZ").getText());
            findCalibPoints(e, baseDirPath, worldZ);
        } 
        
        for (Node folder: sceneFolders)
        { 
            String baseDirPath = e.selectSingleNode("/config/baseFolder").getText() + "\\" +
                                 folder.selectSingleNode("name").getText();
            System.out.println("\nNow working on images from: " + baseDirPath);
            findScenePoints(e, baseDirPath);
        }
        
    }
    
    public final void findScenePoints(Element config, String baseDirPath)
    {
        // Load the gray image
        System.out.println("Loading the gray image: Please wait ...");
        String grayImgDirPath = baseDirPath + "\\" +
                                config.selectSingleNode("/config/folders/syncFrames").getText();
        String grayImgName = (new File(grayImgDirPath)).list()[0];
        BufferedImage grayImage = ImageUtil.load(grayImgDirPath, grayImgName);
        
        // enumerate all the camera points
        System.out.println("Computing camera points: Please wait ...");
        int nCols = grayImage.getWidth();
        int nRows = grayImage.getHeight();
        List<Point2D.Double> cameraPts = new ArrayList<>(nCols*nRows);
        for (double row = 0; row < nRows; row++)
        {
            for (double col = 0; col < nCols; col++)
            {
                cameraPts.add(new Point2D.Double(col, row));
            }
        }
        
        // Compute the projector points 
        System.out.println("Computing projector points: Please wait ...");
        String hPhaseMapPath = baseDirPath + "\\" +
                               config.selectSingleNode("/config/folders/phaseMap").getText() + "\\" +
                               config.selectSingleNode("/config/files/horizPhaseMapData").getText();
        List<List<Double>> hPhaseMap = CSV.loadDoubleMatrix(hPhaseMapPath);        
        String vPhaseMapPath = baseDirPath + "\\" +
                               config.selectSingleNode("/config/folders/phaseMap").getText() + "\\" +
                               config.selectSingleNode("/config/files/vertPhaseMapData").getText();
        List<List<Double>> vPhaseMap = CSV.loadDoubleMatrix(vPhaseMapPath);          
        Double waveNumberX = 2*Math.PI/Double.parseDouble(config.selectSingleNode("/config/structuredLight/minHorizWaveLength").getText());
        Double waveNumberY = 2*Math.PI/Double.parseDouble(config.selectSingleNode("/config/structuredLight/minVertWaveLength").getText());
        List<Point2D.Double> projectorPoints = PhaseMapUtils.computePointsAt(cameraPts, hPhaseMap, vPhaseMap, waveNumberX, waveNumberY);
        
        // Export the points to local folder
        System.out.println("Saving all points: Please wait ...");
        List<List<Double>> mergedList = mergeLists2(cameraPts, projectorPoints);
        String matchesDir = config.selectSingleNode("/config/folders/matches").getText();
        String matchPointsName = config.selectSingleNode("/config/files/matchPoints").getText();        
        CSV.saveDoubleMatrix(mergedList, baseDirPath + "\\" + matchesDir, matchPointsName, false);        
    }
    
    public final void findCalibPoints(Element config, String baseDirPath, double Z) {
        // Load the gray image
        System.out.println("Loading the gray image: Please wait ...");
        String grayImgDirPath = baseDirPath + "\\" +
                                   config.selectSingleNode("/config/folders/syncFrames").getText();
        String grayImgName = (new File(grayImgDirPath)).list()[0];
        BufferedImage grayImage = ImageUtil.load(grayImgDirPath, grayImgName);
        
        // Adaptive threshold to black and white
        System.out.println("Adaptive thresholding to black and white: Please wait ...");
        int windowSize = 21;
        int offset = 5;
        BufferedImage bwImage = ImageUtil.adaptiveThreshold(grayImage, windowSize, offset);
        
        // Save the black and white image
        System.out.println("Saving the black and white image: Please wait ...");
        String calibGridDirPath = baseDirPath + "\\" +
                                  config.selectSingleNode("/config/folders/calibGrid").getText();            
        String calibGridBWName = config.selectSingleNode("/config/files/calibGridBW").getText();
        ImageUtil.save(bwImage, calibGridDirPath, calibGridBWName);
        
        // Find the outerContours
        System.out.println("Looking for contours: Please wait ...");
        Contours contours = new Contours(bwImage);
        contours.findAllContours();
        List<List<List<Point>>> allInnerContours = contours.innerContours;
        
        // Save the contour image
        System.out.println("Saving the contour image: Please wait ...");
        String contourImageName = config.selectSingleNode("/config/files/contourImage").getText();
        ImageUtil.save(contours.tagImg, calibGridDirPath, contourImageName);
        
        // Find the set of designed N inner contours
        int calibTargetNRows = Integer.parseInt(config.selectSingleNode("/config/calibrationTarget/nRows").getText());
        int calibTargetNCols = Integer.parseInt(config.selectSingleNode("/config/calibrationTarget/nCols").getText());
        List<List<Point>> innerContours = new ArrayList<>();
        boolean isSuccess = false;
        for (int i = 0; i < allInnerContours.size(); i++) {
            int nInner = allInnerContours.get(i).size();
            if (nInner == (calibTargetNRows * calibTargetNCols))
            {
                innerContours = allInnerContours.get(i);
                isSuccess = true;
                break;
            }
        }
        if (isSuccess == false)
        {
            throw new RuntimeException("Can't find the correct number of inner contours");
        }

        
        // Find the contour centers
        List<Point2D.Double> centersCamera = new ArrayList<>();
        for (List<Point> contour: innerContours)
        {
            centersCamera.add(ContourUtils.findContourCenter(contour));
        }        
        
        // Find the convex hull
        List<Point2D.Double> hull = ContourUtils.findConvexHull(centersCamera);
        ContourUtils.saveContour(hull, calibGridDirPath, "hull.csv");
        
        // Find the quadralateral
        List<Point2D.Double> quad = Quad.findMaxAreaQuad(hull);
        ContourUtils.saveContour(quad, calibGridDirPath, "quad.csv");
        
        // Define Quad corners in grid coordinates
        List<Point2D.Double> quadGrid = new ArrayList<>();
        quadGrid.add(new Point2D.Double(0.0,0.0));
        quadGrid.add(new Point2D.Double(calibTargetNCols-1,0.0));
        quadGrid.add(new Point2D.Double(calibTargetNCols-1,calibTargetNRows-1));
        quadGrid.add(new Point2D.Double(0.0,calibTargetNRows-1));

        // Find the grid points
        System.out.println("Finding grid points: Please Wait ...");
        Homography homography = new Homography(quad, quadGrid);
        List<Point2D.Double> centersGrid = homography.projectOut(centersCamera);
        System.out.println("the reprojection error is: " + String.format("%.3e", homography.computeReprojectionError()));
                
        // Compute the world points
        System.out.println("Computing world points: Please wait ...");
        double worldDX = Integer.parseInt(config.selectSingleNode("/config/calibrationTarget/worldDX").getText());
        double worldDY = Integer.parseInt(config.selectSingleNode("/config/calibrationTarget/worldDY").getText());
        double worldZ = Z;
        List<List<Double>> centersWorld = new ArrayList<>(centersGrid.size());
        for (int i = 0; i < centersGrid.size(); i++)
        {
            int xGrid = (int)Math.round(centersGrid.get(i).x);
            int yGrid = (int)Math.round(centersGrid.get(i).y);
            List<Double> newWorldPt = new ArrayList<>(3);
            newWorldPt.add(worldDX*xGrid);
            newWorldPt.add(worldDY*yGrid);
            newWorldPt.add(worldZ);
            centersWorld.add(newWorldPt);
        }
        
        // Compute the projector points 
        System.out.println("Computing projector points: Please wait ...");
        String hPhaseMapPath = baseDirPath + "\\" +
                               config.selectSingleNode("/config/folders/phaseMap").getText() + "\\" +
                               config.selectSingleNode("/config/files/horizPhaseMapData").getText();
        List<List<Double>> hPhaseMap = CSV.loadDoubleMatrix(hPhaseMapPath);        
        String vPhaseMapPath = baseDirPath + "\\" +
                               config.selectSingleNode("/config/folders/phaseMap").getText() + "\\" +
                               config.selectSingleNode("/config/files/vertPhaseMapData").getText();
        List<List<Double>> vPhaseMap = CSV.loadDoubleMatrix(vPhaseMapPath);          
        Double waveNumberX = 2*Math.PI/Double.parseDouble(config.selectSingleNode("/config/structuredLight/minHorizWaveLength").getText());
        Double waveNumberY = 2*Math.PI/Double.parseDouble(config.selectSingleNode("/config/structuredLight/minVertWaveLength").getText());
        List<Point2D.Double> centersProjector = PhaseMapUtils.computePointsAt(centersCamera, hPhaseMap, vPhaseMap, waveNumberX, waveNumberY);
        
        // Export the points to local folder
        System.out.println("Saving all points: Please wait ...");
        List<List<Double>> mergedList = mergeLists3(centersCamera, centersProjector, centersWorld);
        String calibGridDataName = config.selectSingleNode("/config/files/calibGridData").getText();
        CSV.saveDoubleMatrix(mergedList, calibGridDirPath, calibGridDataName, false);
        
        // Export the points to the master folder
        String masterCalFolderPath = config.selectSingleNode("/config/baseFolder").getText() + "\\" + 
                                     config.selectSingleNode("/config/masterCalFolder").getText();
        CSV.saveDoubleMatrix(mergedList, masterCalFolderPath, calibGridDataName, true);       
    }   
    
    public List<List<Double>> mergeLists2(List<Point2D.Double> first, 
                                           List<Point2D.Double> second)
    {
        List<List<Double>> output = new ArrayList<>();
        for (int i = 0; i < first.size(); i++ )
        {
            List<Double> newRow = new ArrayList<>();
            newRow.add(first.get(i).x);
            newRow.add(first.get(i).y);
            newRow.add(second.get(i).x);
            newRow.add(second.get(i).y);
            output.add(newRow);
        }        
        return output;
    }
    
    public List<List<Double>> mergeLists3(List<Point2D.Double> first, 
                                           List<Point2D.Double> second,
                                           List<List<Double>> third)
    {
        List<List<Double>> output = new ArrayList<>();
        for (int i = 0; i < first.size(); i++ )
        {
            List<Double> newRow = new ArrayList<>();
            newRow.add(first.get(i).x);
            newRow.add(first.get(i).y);
            newRow.add(second.get(i).x);
            newRow.add(second.get(i).y);
            newRow.add(third.get(i).get(0));
            newRow.add(third.get(i).get(1));
            newRow.add(third.get(i).get(2));
            output.add(newRow);
        }        
        return output;
    }

}
