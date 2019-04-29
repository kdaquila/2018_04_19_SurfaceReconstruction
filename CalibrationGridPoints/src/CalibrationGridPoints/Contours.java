package CalibrationGridPoints;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Contours {
    
    BufferedImage bwImg;
    WritableRaster bwRaster;
    BufferedImage tagImg;
    WritableRaster tagRaster;
    int nRows;
    int nCols; 
    int high;
    int low;
    short noTag;
    short markTag;
    short initialTag;
    List<List<Point>> outerContours;
    List<List<List<Point>>> innerContours;
    
    public Contours(BufferedImage bwImg) {
        this.high = 0;
        this.low = 255;
        this.noTag = 0;
        this.markTag = 1;
        this.initialTag = 2;
        this.bwImg = bwImg;
        ImageUtil.fillBoundary(bwImg, low);
        this.bwRaster = bwImg.getRaster();
        this.nRows = bwImg.getHeight();
        this.nCols = bwImg.getWidth();
        this.tagImg = new BufferedImage(nCols, nRows, BufferedImage.TYPE_BYTE_GRAY);
        ImageUtil.fillImage(tagImg, noTag);
        this.tagRaster = tagImg.getRaster();
        this.outerContours = new ArrayList<>();
        
        // add empty outerContours
        for (int i = 0; i < initialTag; i++)
        {
            outerContours.add(new ArrayList<>());
        }
        this.innerContours = new ArrayList<>();
        
        // add empty contour indice lists
        for (int i = 0; i < initialTag; i++)
        {
            innerContours.add(new ArrayList<>());
        }
    }    
    
    public void findAllContours()
    {
        // container to receive new pixels
        int[] pixel = new int[1];
        
        // the tag to assign to new outerContours
        int newTag = initialTag;
        
        // begin the search
        for (int row = 1; row < nRows-1; row++)
        {
            for (int col = 1; col < nCols-1; col++)
            {                               
                // get some nearby black-white pixels                
                bwRaster.getPixel(col, row, pixel);
                int bw = pixel[0];
                bwRaster.getPixel(col, row - 1, pixel);
                int bwUp = pixel[0];
                bwRaster.getPixel(col, row + 1, pixel);
                int bwDown = pixel[0];
                
                // get some nearby tag pixels
                tagRaster.getPixel(col, row, pixel);
                int tag = pixel[0];
                tagRaster.getPixel(col, row - 1, pixel);
                int tagUp = pixel[0];
                tagRaster.getPixel(col, row + 1, pixel);
                int tagDown = pixel[0];
                tagRaster.getPixel(col - 1, row, pixel);
                int tagLeft = pixel[0];                
                
                // Found a New Contour's Outer Edge
                if (bw == high &&
                    bwUp == low &&
                    tag == noTag )
                {        
                    // simultaneously findAllContours the contour's edge and mark the tagImage
                    int startAngle = 315; //degrees
                    List<Point> newContour = findSingleContour(col, row, startAngle, newTag);
                                        
                    // store the new contour
                    outerContours.add(newContour);

                    // add storage for future inner contour edges
                    innerContours.add(new ArrayList<>());
                    
                    // advance
                    newTag += 1;
                }
                
                // Found a Exisiting Contour's Inner Edge
                else if (bw == high &&
                         bwDown == low &&
                         tagDown != markTag)
                {
                    // determine the tag
                    int parentTag;
                    // use the existing tag
                    if (tag > markTag) 
                    {
                        parentTag = tag;
                    }
                    // use the tag to the left
                    else if (tagLeft > markTag)
                    {
                        parentTag = tagLeft;
                    }
                    // use the tag above
                    else if (tagUp > markTag)
                    {
                        parentTag = tagUp;
                    }
                    // can't determine the correct tag, skip?
                    else 
                    {
                        continue;
                    }
                    
                    // simultaneously findAllContours the contour's edge and mark the tagImage
                    int startAngle = 135; //degrees
                    List<Point> newContour = findSingleContour(col, row, startAngle, parentTag);
                    
                    // store the new contour's points
                    innerContours.get(parentTag).add(newContour);
                } 
                
                // Found an interior Point
                else if (bw == high)
                {
                    // determine the tag
                    int parentTag;
                    // use the left tag
                    if (tag == noTag) 
                    {
                        parentTag = tagLeft;
                    }
                    // use the existing tag, which must have been assigned during a previous contour trace
                    else {
                        parentTag = tag;
                    }
                    
                    tagRaster.setPixel(col, row, new int[]{parentTag});
                }   
                
            } // end col loop
        } // end row loop      
    }
    
    private List<Point> findSingleContour(int startX, int startY, int startAngle, int tag)
    {
        List<Point> outputEdge = new ArrayList<>();
         
        // First Point
        Point firstPoint = new Point(startX, startY);
        tagRaster.setPixel(startX, startY, new double[]{tag});
        outputEdge.add(new Point(startX, startY));
        
        // Second Point
        Point secondPoint = findNextContourPoint(firstPoint, startAngle, high);
        // If we can't findAllContours second index because the contour is a single pixel        
        if (secondPoint == null) {
            return outputEdge; 
        }        
        
        // Current Index
        Point currPoint = secondPoint;
        
        // Next Index
        NeighborPixels neighborPixels = new NeighborPixels(secondPoint);
        int searchAngle = (neighborPixels.getAngleTo(firstPoint) + 90);
        Point nextPoint = findNextContourPoint(secondPoint, searchAngle, high);
        
        while(!currPoint.equals(firstPoint)  || !nextPoint.equals(secondPoint))
        {
            // Store the point
            outputEdge.add(currPoint);
            
            // Tag the point
            tagRaster.setPixel(currPoint.x, currPoint.y, new int[]{tag});
            
            // Update
            neighborPixels = new NeighborPixels(nextPoint);
            searchAngle = (neighborPixels.getAngleTo(currPoint) + 90);
            currPoint = nextPoint;            
            nextPoint = findNextContourPoint(currPoint, searchAngle, high);
        }

        return outputEdge;
    }
    
    private Point findNextContourPoint(Point currentPoint, int startAngle, int value)
    {
        NeighborPixels neighborPixels = new NeighborPixels(currentPoint);
        List<Point> neighborList = neighborPixels.getSearchPts(startAngle); 
            
        for (Point neighbor: neighborList)
        {                        
            // if this search point is outside the image boundaries, skip it
            if (neighbor.x < 0 || neighbor.x > (nCols-1) || 
                neighbor.y < 0 || neighbor.y > (nRows-1)) {
                continue;
            }
            
            // get the current black and white pixel
            int[] bwPixel = new int[1];
            bwRaster.getPixel(neighbor.x, neighbor.y, bwPixel);
            int bw = bwPixel[0];
            
            // we found the next neighbor
            if (bw == value)
            {
                return neighbor;
            } 
            // we found an empty pixel, so only mark it
            else
            {
                tagRaster.setPixel(neighbor.x, neighbor.y, new int[]{markTag});
            }
        }
        
        // could not findAllContours the next neighbor
        return null; 
    }
}

class NeighborPixels {
    
    Integer[] neighborAngles;
    List<Point> neighborPoints;
    
    public NeighborPixels(Point pt)
    {        
        int x = pt.x;
        int y = pt.y;
        neighborAngles = new Integer[]{0,45,90,135,180,225,270,315};    
        neighborPoints = new ArrayList<>(8);
        neighborPoints.add(new Point((x + 1), (y + 0)));
        neighborPoints.add(new Point((x + 1), (y + 1)));
        neighborPoints.add(new Point((x + 0), (y + 1)));
        neighborPoints.add(new Point((x - 1), (y + 1)));
        neighborPoints.add(new Point((x - 1), (y + 0)));
        neighborPoints.add(new Point((x - 1), (y - 1)));
        neighborPoints.add(new Point((x + 0), (y - 1)));
        neighborPoints.add(new Point((x + 1), (y - 1)));
    }
       
    public int getAngleTo(Point pt)
    {
        int key = neighborPoints.indexOf(pt);
        return neighborAngles[key];
    }
    
    public List<Point> getSearchPts(int startAngle)
    {
        // make sure angle is from 0 to 360 degrees
        startAngle = startAngle%360;
        
        // copy the neighboring points
        List<Point> output = new ArrayList<>(neighborPoints);
        
        // rotate the list of neighboring points to start with the one at the start angle
        int offset = Arrays.binarySearch(neighborAngles, startAngle);        
        Collections.rotate(output, -1*offset);
        
        return output;
    }
    
    
    
    
    
    

}
