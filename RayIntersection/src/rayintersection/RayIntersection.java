package rayintersection;

import CalibrationGridPoints.CSV;
import config.Util;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.dom4j.Element;
import org.dom4j.Node;

public class RayIntersection {
    
    public RayIntersection(String configPath)
    {
        // Load the configuration
        Element e = Util.loadXML(configPath);        
        
        List<Node> sceneFolders = e.selectNodes("/config/sceneFolders/folder");
        
        // work on each folder using its own base path
        for (Node folder: sceneFolders)
        {            
            String baseDirPath = e.selectSingleNode("/config/baseFolder").getText() + "\\" +
                                 folder.selectSingleNode("name").getText();
            System.out.println("\nNow working on images from: " + baseDirPath);
            intersectRays(e, baseDirPath);
        } 
        
    }
    
    final public void intersectRays(Element root, String sceneFolder)
    {
        // Load the camera and projector matrices
        System.out.println("Loading camera and projector matrices: Please Wait ...");
        String baseFolder = root.selectSingleNode("/config/baseFolder").getText();
        String calibrationMatrixFolder = root.selectSingleNode("/config/masterCalFolder").getText();
        String cameraMatrixName = root.selectSingleNode("/config/files/cameraMatrix").getText();
        String cameraMatrixFullPath = baseFolder + "\\" + calibrationMatrixFolder + "\\" + cameraMatrixName;
        String projectorMatrixName = root.selectSingleNode("/config/files/projectorMatrix").getText();
        String projectorMatrixFullPath = baseFolder + "\\" + calibrationMatrixFolder + "\\" + projectorMatrixName;        
        double[][] cameraMatrix = CSV.loadDoubleMatrixAsArray(cameraMatrixFullPath);
        RealMatrix camera_M = MatrixUtils.createRealMatrix(cameraMatrix);                
        double[][] projectorMatrix = CSV.loadDoubleMatrixAsArray(projectorMatrixFullPath);
        RealMatrix projector_M = MatrixUtils.createRealMatrix(projectorMatrix);
        
        // compute the ray director vectors "A" for camera
        System.out.println("Compute Ray Direction Vectors: Please Wait ...");
        RealMatrix camera_M_13 = camera_M.getSubMatrix(0, 2, 0, 2);
        RealVector camera_M_4 = camera_M.getColumnVector(3);
        RealMatrix camera_M_13_inv = MatrixUtils.inverse(camera_M_13);
        RealVector camera_A = camera_M_13_inv.operate(camera_M_4);
        
        // compute the ray director vectors "A" for projector
        RealMatrix projector_M_13 = projector_M.getSubMatrix(0, 2, 0, 2);
        RealVector projector_M_4 = projector_M.getColumnVector(3);
        RealMatrix projector_M_13_inv = MatrixUtils.inverse(projector_M_13);
        RealVector projector_A = projector_M_13_inv.operate(projector_M_4);
        
        // compute a 3d point for each image point
        System.out.println("Computing 3D Points: Please Wait ...");
        String matchesFolder = root.selectSingleNode("/config/folders/matches").getText();
        String matchesName = root.selectSingleNode("/config/files/matchPoints").getText();
        List<List<Double>> calibGridData = CSV.loadDoubleMatrix(sceneFolder + "\\" + matchesFolder + "\\" + matchesName);
        List<List<Double>> pointCloud = new ArrayList<>();
        for (List<Double> pt: calibGridData)
        {
            // compute the ray origin "B" for camera
            Double camX = pt.get(0);
            Double camY = pt.get(1);
            Double camZ = 1.0;
            RealVector camPt = MatrixUtils.createRealVector(new double[]{camX, camY, camZ});
            RealVector camera_B = camera_M_13_inv.operate(camPt);
            
            // compute the ray origin "B" for projector
            Double projX = pt.get(2);
            Double projY = pt.get(3);
            Double projZ = 1.0;
            RealVector projPt = MatrixUtils.createRealVector(new double[]{projX, projY, projZ});
            RealVector projector_B = projector_M_13_inv.operate(projPt);
            
            // solve for "equation of line" parameter "t"
            RealMatrix system_A = MatrixUtils.createRealMatrix(3, 2);
            system_A.setColumnVector(0, projector_A);
            system_A.setColumnVector(1, camera_A.mapMultiply(-1));
            RealVector system_B = projector_B.subtract(camera_B);
            DecompositionSolver solver = new SingularValueDecomposition(system_A).getSolver();
            RealVector t = solver.solve(system_B);
            
            // compute the new 3d point
            RealVector point3D = camera_B.subtract(camera_A.mapMultiply(t.getEntry(0)));
            
            // store the new 3d point
            List<Double> new3dPoint = new ArrayList<>(3);
            new3dPoint.add(point3D.getEntry(0));
            new3dPoint.add(point3D.getEntry(1));
            new3dPoint.add(point3D.getEntry(2));
            pointCloud.add(new3dPoint);            
        }

        // save the 3d point cloud
        System.out.println("Saving the 3d point cloud: Please Wait ...");
        String pointCloudDir = root.selectSingleNode("/config/folders/pointCloud").getText();
        String pointCloudName = root.selectSingleNode("/config/files/pointCloud").getText();
        String pointCloudFullDirPath = sceneFolder + "\\" + pointCloudDir;
        PLY.saveDoubleArray(pointCloud, pointCloudFullDirPath, pointCloudName, false);        
    }

}
