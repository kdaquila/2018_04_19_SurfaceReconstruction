package CalibrationGridPoints;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;

public class ContourUtils {
    
    public static Point2D.Double findContourCenter(List<Point> contour)
    {
        double sumX = 0;
        double n = 0;
        double sumY = 0;
        
        for (Point p: contour)
        {
            sumX += p.x;            
            sumY += p.y; 
            n += 1;
        }        
        
        return new Point2D.Double(sumX/n, sumY/n);
    }
    
    public static List<Point2D.Double> findConvexHull(List<Point2D.Double> contour)
    {        
        List<Point2D.Double> hull = new ArrayList<>();
        
        int n = contour.size();
        
        // find the leftmost point
        int L = 0;
        for (int i = 1; i < n; i++)
        {
            if (contour.get(i).x < contour.get(L).x)
            {
                L = i;
            }
        }
        
        // add the first point
        int p = L;
        //hull.add(new Point2D.Double(contour.get(p).x, contour.get(p).y));
        
        // look for the other points
        boolean isDone = false;
        while (isDone == false)
        {
            // update q
            int q = (p + 1)%n;
            for (int i = 0; i < n; i++)
            {
                // compute determinant
                double px = contour.get(p).x;
                double py = contour.get(p).y;
                double qx = contour.get(q).x;
                double qy = contour.get(q).y;
                double ix = contour.get(i).x;
                double iy = contour.get(i).y;
                double determinant = ((qx-px)*(iy-py)-(qy-py)*(ix-px));
                
                if (determinant < 0) // must be < 0 keep get clockwise sorting needed for maxAreaQuad later
                {
                    q = i;
                }
            }
            
            // save the point
            hull.add(new Point2D.Double(contour.get(p).x, contour.get(p).y));
            
            // update p
            p = q;
            if (p == L) {
                isDone = true;
            }
        }
        
        return hull;
    }
    
    
    public static List<Point> findMaxAreaQuad(List<Point> contour)
    {
        List<Point> quad = new ArrayList<>(4);
        // find quad corners
        
        
        // sort them in clockwise order starting from top-left
        
        return quad;
    }
    
    public static void saveContour(List<Point2D.Double> contour, String folder, String name)
    {
        File f = new File(folder);
        try {
            if (! f.exists()){
            FileUtils.forceMkdir(f);
            }
            File file = new File(folder + "\\" + name);
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter buffFileWriter = new BufferedWriter(fileWriter);
        
            for (Point2D.Double pt: contour)
            {
                buffFileWriter.write(pt.x + "," + pt.y);
                buffFileWriter.newLine();
            }
            
            buffFileWriter.close();
        }
        catch (IOException exp) {            
            throw new RuntimeException("Could not save the frame plot data");
        }
    }   
    
    public static void saveContourInt(List<Point> contour, String folder, String name)
    {
        File f = new File(folder);
        try {
            if (! f.exists()){
            FileUtils.forceMkdir(f);
            }
            File file = new File(folder + "\\" + name);
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter buffFileWriter = new BufferedWriter(fileWriter);
        
            for (Point pt: contour)
            {
                buffFileWriter.write(pt.x + "," + pt.y);
                buffFileWriter.newLine();
            }
            
            buffFileWriter.close();
        }
        catch (IOException exp) {            
            throw new RuntimeException("Could not save the frame plot data");
        }
    } 

}
