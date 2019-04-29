package projectivetransform;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.apache.commons.math3.linear.SingularValueDecomposition;

public class ProjectiveTransform {
       
    List<List<Double>> worldPts;
    List<List<Double>> imagePts;
    int n;
    RealMatrix A;
    RealVector B;
    RealVector X;
    RealMatrix M;

    public ProjectiveTransform(List<List<Double>> in, List<List<Double>> out)
    {
        this.worldPts = in;
        this.imagePts = out;
        this.n = in.size();
        createA();
        createB();
        computeX();
        buildM();
    }
    
    final public void createA()
    {
        A = MatrixUtils.createRealMatrix(2*n, 11);
        for (int i = 0; i < n; i++)
        {
            A.setRow(i,   new double[]{worldPts.get(i).get(0), worldPts.get(i).get(1), worldPts.get(i).get(2), 1.0, 0.0, 0.0, 0.0, 0.0, -1*worldPts.get(i).get(0)*imagePts.get(i).get(0), -1*worldPts.get(i).get(1)*imagePts.get(i).get(0), -1*worldPts.get(i).get(2)*imagePts.get(i).get(0)});
            A.setRow(i+n, new double[]{0.0, 0.0, 0.0, 0.0, worldPts.get(i).get(0), worldPts.get(i).get(1), worldPts.get(i).get(2), 1.0, -1*worldPts.get(i).get(0)*imagePts.get(i).get(1), -1*worldPts.get(i).get(1)*imagePts.get(i).get(1), -1*worldPts.get(i).get(2)*imagePts.get(i).get(1)});
        }
    }
    
    final public void createB()
    {
        double[] bData = new double[2*n];
        for (int i = 0; i < n; i++)
        {
            bData[i] = imagePts.get(i).get(0);
            bData[i+n] = imagePts.get(i).get(1);
        }
        B = MatrixUtils.createRealVector(bData); 
    }
    
    final public void computeX()
    {
        try {
            DecompositionSolver solver = new SingularValueDecomposition(A).getSolver();            
            X = solver.solve(B);
            System.out.println("the condition number is: " + new SingularValueDecomposition(A).getConditionNumber());
        }
        catch (SingularMatrixException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Could not compute the calibration matrix parameters");
        }
    }
    
    final public void buildM()
    {
        M = MatrixUtils.createRealMatrix(3, 4);
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 4; col++)
            {
                double entry;
                if (row == 2 && col == 3)
                {
                    entry = 1.0;
                }
                else
                {
                    int i = row*4 + col;
                    entry = X.getEntry(i);
                }
                M.setEntry(row, col, entry);
            }
        }
        
    }
    
    public List<List<Double>> projectOut(List<List<Double>> pts)
    {
        List<List<Double>> outPts = new ArrayList<>(pts.size());
        double m1 = X.getEntry(0);
        double m2 = X.getEntry(1);
        double m3 = X.getEntry(2);
        double m4 = X.getEntry(3);
        double m5 = X.getEntry(4);
        double m6 = X.getEntry(5);
        double m7 = X.getEntry(6);
        double m8 = X.getEntry(7);
        double m9 = X.getEntry(8);
        double m10 = X.getEntry(9);
        double m11 = X.getEntry(10);
        for (int i = 0; i < pts.size(); i++)
        {
            double x = pts.get(i).get(0);
            double y = pts.get(i).get(1);
            double z = pts.get(i).get(2);
            double outX = (m1*x + m2*y + m3*z + m4)/(m9*x + m10*y + m11*z + 1);
            double outY = (m5*x + m6*y + m7*z + m8)/(m9*x + m10*y + m11*z + 1);
            List<Double> newPoint = new ArrayList<>();
            newPoint.add(outX);
            newPoint.add(outY);
            outPts.add(newPoint);
        }        
        return outPts;
    }
    
    public double computeReprojectionError()
    {
        List<List<Double>> newOut = projectOut(worldPts);
        double errorSum = 0;
        for (int i = 0; i < n; i++)
        {
            errorSum += Math.sqrt(Math.pow(newOut.get(i).get(0) - imagePts.get(i).get(0), 2)+Math.pow(newOut.get(i).get(1) - imagePts.get(i).get(1), 2));
        }
        double avgError = errorSum/n;
        return avgError;
    }
    
}


