package CalibrationGridPoints;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Quad {
    
    public static List<Point2D.Double> findMaxAreaQuad(List<Point2D.Double> hull)
    {
        /**
         * This function requires that the convex hull points were sorted in clockwise direction
         */
        List<Point2D.Double> quad = new ArrayList<>();
        int nPts = hull.size();

        if (nPts < 4)
        {
            return quad;
        }

        int a = 0;
        int b = 1;
        int c = 3;
        int d = 4;
        int best_a = 0;
        int best_b = 1;
        int best_c = 3;
        int best_d = 4;

        while(true) // loop A
        {
            while(true) // loop B
            {
                while(true) // loop C
                {
                    while(findQuadArea(hull.get(a), hull.get(b), hull.get(c), hull.get((d+1)%nPts)) >= 
                          findQuadArea(hull.get(a), hull.get(b), hull.get(c), hull.get(d)))
                    {
                        d = (d+1)%nPts; // advance d
                    }

                    if(findQuadArea(hull.get(a), hull.get(b), hull.get((c+1)%nPts), hull.get(d)) >= 
                       findQuadArea(hull.get(a), hull.get(b), hull.get(c), hull.get(d)))
                    {
                        c  = (c+1)%nPts; // advance c
                    }
                    else
                    {
                        break; // loop C
                    }
                }

                if(findQuadArea(hull.get(a), hull.get((b+1)%nPts), hull.get(c), hull.get(d)) >= 
                   findQuadArea(hull.get(a), hull.get(b), hull.get(c), hull.get(d)))
                {
                    b  = (b+1)%nPts; //advance b
                }
                else
                {
                    break; // loop B
                }
            }

            if (findQuadArea(hull.get(a), hull.get(b), hull.get(c), hull.get(d)) > 
                findQuadArea(hull.get(best_a), hull.get(best_b), hull.get(best_c), hull.get(best_d)))
            {
                best_a = a;
                best_b = b;
                best_c = c;
                best_d = d;
            }

            a = (a+1)%nPts; // advance a

            if (a == b)
            {
                b = (b+1)%nPts; // avoid collision
            }
            if (b == c)
            {
                c = (c+1)%nPts; // avoid collision\
            }
            if (c == d)
            {
                d = (d+1)%nPts; // avoid collision
            }
            if (a == 0)
            {
                break; // loop A
            }
        }

        // store the points
        quad.add(new Point2D.Double(hull.get(best_a).x, hull.get(best_a).y));
        quad.add(new Point2D.Double(hull.get(best_b).x, hull.get(best_b).y));
        quad.add(new Point2D.Double(hull.get(best_c).x, hull.get(best_c).y));
        quad.add(new Point2D.Double(hull.get(best_d).x, hull.get(best_d).y));
        
        // sort the points
        quad = sortCorners(quad);        
        
        return quad;        
    }
    
    public static double findQuadArea(Point2D.Double pt1, 
                                        Point2D.Double pt2, 
                                        Point2D.Double pt3, 
                                        Point2D.Double pt4)
    {
        /** This function uses the "shoelace formula" to compute the area
            of a quadrilateral given the Cartesian corner points
        */
        double x1 = pt1.x;
        double x2 = pt2.x;
        double x3 = pt3.x;
        double x4 = pt4.x;
        double y1 = pt1.y;
        double y2 = pt2.y;
        double y3 = pt3.y;
        double y4 = pt4.y;
        return 0.5*(x1*y2 + x2*y3 + x3*y4 + x4*y1 - x2*y1 - x3*y2 - x4*y3 - x1*y4);
    }
    
    public static List<Point2D.Double> sortCorners(List<Point2D.Double> corners)
    {
        boolean isDone = false;
        int n = corners.size();
        for (int i = 0; i < n; i++)
        {
            Point2D.Double pt = corners.get(i);
            double pt0y = corners.get(0).y;
            double pt3y = corners.get((0+3)%n).y;
            double lenSide0 = pointDistance(corners.get(0), corners.get((0+1)%n));
            double lenSide1 = pointDistance(corners.get((0+1)%n), corners.get((0+2)%n));
            if ((pt0y < pt3y) && (lenSide0 > lenSide1))
            {
                isDone = true;
                break;
            }
            else
            {
                Collections.rotate(corners, -1);
            }
        }
        if (isDone == false)
        {
            throw new RuntimeException("can't identify the quad corners");
        }
        return corners;
    }
    
    public static double pointDistance(Point2D.Double pt1, Point2D.Double pt2)
    {
        return Math.sqrt(Math.pow(pt2.x-pt1.x, 2) + Math.pow(pt2.y-pt1.y, 2));
    }

    
}
