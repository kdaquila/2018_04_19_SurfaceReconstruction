package CalibrationGridPoints;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularMatrixException;

public class Homography {
       
    List<Point2D.Double> in;
    List<Point2D.Double> out;
    int n;
    RealMatrix A;
    RealVector B;
    RealVector X;

    public Homography(List<Point2D.Double> in, List<Point2D.Double> out)
    {
        this.in = in;
        this.out = out;
        this.n = in.size();
        createA();
        createB();
        computeX();
    }
    
    final public void createA()
    {
        A = MatrixUtils.createRealMatrix(2*n, 8);
        for (int i = 0; i < n; i++)
        {
            A.setRow(i, new double[]{in.get(i).x, in.get(i).y, 1.0, 0.0, 0.0, 0.0, -1*in.get(i).x*out.get(i).x, -1*in.get(i).y*out.get(i).x});
            A.setRow(i+n, new double[]{0.0, 0.0, 0.0, in.get(i).x, in.get(i).y, 1.0,  -1*in.get(i).x*out.get(i).y, -1*in.get(i).y*out.get(i).y});
        }
    }
    
    final public void createB()
    {
        double[] bData = new double[2*n];
        for (int i = 0; i < n; i++)
        {
            bData[i] = out.get(i).x;
            bData[i+n] = out.get(i).y;
        }
        B = MatrixUtils.createRealVector(bData); 
    }
    
    final public void computeX()
    {
        try {
           DecompositionSolver solver = new QRDecomposition(A).getSolver();
           X = solver.solve(B); 
        }
        catch (SingularMatrixException e) {                   
        }
    }
    
    public List<Point2D.Double> projectOut(List<Point2D.Double> pts)
    {
        List<Point2D.Double> outPts = new ArrayList<>(pts.size());
        double m1 = X.getEntry(0);
        double m2 = X.getEntry(1);
        double m3 = X.getEntry(2);
        double m4 = X.getEntry(3);
        double m5 = X.getEntry(4);
        double m6 = X.getEntry(5);
        double m7 = X.getEntry(6);
        double m8 = X.getEntry(7);
        for (int i = 0; i < pts.size(); i++)
        {
            double x = pts.get(i).x;
            double y = pts.get(i).y;
            double outX = (m1*x + m2*y + m3)/(m7*x + m8*y + 1);
            double outY = (m4*x + m5*y + m6)/(m7*x + m8*y + 1);
            outPts.add(new Point2D.Double(outX, outY));
        }        
        return outPts;
    }
    
    public double computeReprojectionError()
    {
        List<Point2D.Double> newOut = projectOut(in);
        double errorSum = 0;
        for (int i = 0; i < n; i++)
        {
            errorSum += Math.sqrt(Math.pow(newOut.get(i).x - out.get(i).x, 2)+Math.pow(newOut.get(i).y - out.get(i).y,2));
        }
        double avgError = errorSum/n;
        return avgError;
    }
    
}
