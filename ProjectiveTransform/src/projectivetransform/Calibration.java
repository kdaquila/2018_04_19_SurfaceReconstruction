package projectivetransform;

import CalibrationGridPoints.CSV;
import config.Util;
import java.util.ArrayList;
import java.util.List;
import org.dom4j.Element;

public class Calibration {
    
    Element config;
    
    public Calibration(String configPath) 
    {
        // load the configuration data
        config = Util.loadXML(configPath);
        
        // load the points data
        String folderPath = config.selectSingleNode("/config/baseFolder").getText() + "\\" +
                            config.selectSingleNode("/config/masterCalFolder").getText();
        String fileName = config.selectSingleNode("/config/files/calibGridData").getText();
        List<List<Double>> calibGridData = CSV.loadDoubleMatrix(folderPath + "\\" + fileName);
        
        // Extract the columns
        List<List<Double>> cameraPoints = new ArrayList<>();
        List<List<Double>> projectorPoints = new ArrayList<>();
        List<List<Double>> worldPoints = new ArrayList<>();
        for (List<Double> row: calibGridData)
        {
            cameraPoints.add(row.subList(0, 2));
            projectorPoints.add(row.subList(2, 4));
            worldPoints.add(row.subList(4, 7));
        }        
        
        // compute the projective transformation
        System.out.println("\nComputing the camera calibration: Please Wait ...");
        ProjectiveTransform cameraTransform = new ProjectiveTransform(worldPoints, cameraPoints);
        String cameraMatrixName = config.selectSingleNode("/config/files/cameraMatrix").getText();
        CSV.saveDoubleMatrix(cameraTransform.M.getData(), folderPath, cameraMatrixName);
        double cameraError = cameraTransform.computeReprojectionError();
        System.out.println("The camera reprojection error is: " + cameraError);
        
        // compute the projective transformation
        System.out.println("\nComputing the projector calibration: Please Wait ...");
        ProjectiveTransform projectorTransform = new ProjectiveTransform(worldPoints, projectorPoints);
        String projectorMatrixName = config.selectSingleNode("/config/files/projectorMatrix").getText();
        CSV.saveDoubleMatrix(projectorTransform.M.getData(), folderPath, projectorMatrixName);
        double projectorError = projectorTransform.computeReprojectionError();
        System.out.println("The projector reprojection error is: " + projectorError);
        
        
    }

}
