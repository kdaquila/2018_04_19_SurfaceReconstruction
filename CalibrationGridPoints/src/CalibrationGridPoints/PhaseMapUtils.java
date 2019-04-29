package CalibrationGridPoints;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class PhaseMapUtils {
    
    public static  List<Point2D.Double> computePointsAt(List<Point2D.Double> ptsIn, 
                                                        List<List<Double>> hPhaseMap, 
                                                        List<List<Double>> vPhaseMap,
                                                        double waveNumberX, double waveNumberY)
    {
        List<Point2D.Double> projector = new ArrayList<>(ptsIn.size());
        for (int i = 0; i < ptsIn.size(); i++)
        {
            // linear interpolation of phase maps
            double centerX = ptsIn.get(i).x;
            double centerY = ptsIn.get(i).y;
            Point centerNE = new Point((int)Math.ceil(centerX), (int)Math.floor(centerY));
            Point centerNW = new Point((int)Math.floor(centerX), (int)Math.floor(centerY));
            Point centerSE = new Point((int)Math.ceil(centerX), (int)Math.ceil(centerY));
            Point centerSW = new Point((int)Math.floor(centerX), (int)Math.ceil(centerY));
//            double hPhaseInterp = hPhaseMap.get((int)Math.round(centerY)).get((int)Math.round(centerX));
//            double vPhaseInterp = vPhaseMap.get((int)Math.round(centerY)).get((int)Math.round(centerX));
            double xFactor = (centerX-Math.floor(centerX));
            double yFactor = (centerY-Math.floor(centerY));
            double hPhaseInterp = xFactor * (1-yFactor) * hPhaseMap.get(centerNE.y).get(centerNE.x) + 
                                  (1-xFactor) * (1-yFactor) * hPhaseMap.get(centerNW.y).get(centerNW.x) +
                                  xFactor * yFactor * hPhaseMap.get(centerSE.y).get(centerSE.x) +
                                  (1-xFactor) * yFactor * hPhaseMap.get(centerSW.y).get(centerSW.x);
            double vPhaseInterp = xFactor * (1-yFactor) * vPhaseMap.get(centerNE.y).get(centerNE.x) + 
                                  (1-xFactor) * (1-yFactor) * vPhaseMap.get(centerNW.y).get(centerNW.x) +
                                  xFactor * yFactor * vPhaseMap.get(centerSE.y).get(centerSE.x) +
                                  (1-xFactor) * yFactor * vPhaseMap.get(centerSW.y).get(centerSW.x);
            
            // convert phase to projector image space position
            double projectorX = hPhaseInterp/waveNumberX;
            double projectorY = vPhaseInterp/waveNumberY;
            projector.add(new Point2D.Double(projectorX, projectorY));
        }     
        return projector;
        
    }

}
