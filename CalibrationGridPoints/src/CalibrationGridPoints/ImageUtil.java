package CalibrationGridPoints;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;


public class ImageUtil {
    
    public static BufferedImage load(String folder, String name)
    {
        BufferedImage output;
        try {
            output = ImageIO.read(new File(folder + "\\" + name));
        }
        catch (IOException exp) {
            for (StackTraceElement elem: exp.getStackTrace()) {
                System.out.println(elem);
            }
            throw new RuntimeException("Can't read the images");
        }
        return output;
    }
        
    public static void save(BufferedImage inputImage, String folder, String name)
    {        
        try {
            // create the folder if necessary
            File folderDir = new File(folder);
            if (!folderDir.exists()) {
                FileUtils.forceMkdir(folderDir);
            }
            // write the image
            File outputFile = new File (folderDir + "\\" + name);
            ImageIO.write(inputImage, "png", outputFile);
        }
        catch (IOException exp) {
            for (StackTraceElement elem: exp.getStackTrace()) {
                System.out.println(elem);
            }
            throw new RuntimeException("Could not save the image");
        } 
    }
    
    public static BufferedImage transpose(BufferedImage inputImage)
    {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        int type = inputImage.getType();
        BufferedImage outputImage = new BufferedImage(height, width, type);
        AffineTransform transform = new AffineTransform();
        transform.concatenate(AffineTransform.getQuadrantRotateInstance(1));
        transform.concatenate(AffineTransform.getScaleInstance(1.0, -1.0));
        Graphics2D g = outputImage.createGraphics();
        g.transform(transform);
        g.drawImage(inputImage, 0, 0, null);
        g.dispose();
        return outputImage;
    }
    
    public static BufferedImage meanFilter(BufferedImage inputGrayImage, int radius)
    {
        BufferedImage outputImage;        
        outputImage = ImageUtil.meanFilterRows(inputGrayImage, radius);
        outputImage = ImageUtil.meanFilterRows(outputImage, radius);
        outputImage = ImageUtil.meanFilterRows(outputImage, radius);
        outputImage = ImageUtil.transpose(outputImage);
        outputImage = ImageUtil.meanFilterRows(outputImage, radius);
        outputImage = ImageUtil.meanFilterRows(outputImage, radius);
        outputImage = ImageUtil.meanFilterRows(outputImage, radius);
        outputImage = ImageUtil.transpose(outputImage);
        
        return outputImage;
    }
    
    private static BufferedImage meanFilterRows(BufferedImage inputGrayImage, int radius)
    {
        // Get the gray data
        byte[] grayData = ((DataBufferByte)inputGrayImage.getData().getDataBuffer()).getData();
        
        // Initialize the output image
        int width = inputGrayImage.getWidth();
        int height = inputGrayImage.getHeight();
        int type = inputGrayImage.getType();
        BufferedImage outputImage = new BufferedImage(width, height, type);
        
        // Get the output image data
        byte[] outputData = ((DataBufferByte)outputImage.getRaster().getDataBuffer()).getData();
        
        // Define kernal value
        float kernalWt = 1.0f/(2*radius + 1);
              
        // Filter along each row
        int i = 0;
        for (int row = 0; row < height; row++)
        {
            float rowConvSum = 0;
            for (int col = 0; col < width; col++)
            {
                // Define position in linear array
                i = row*width + col;
                
                // Sum initial terms for rowConvSum and for localConvSum
                if (col == 0)
                {
                    float localConvSum = 0;
                    float localKernalWt = 1.0f/(radius + 1);
                    for (int offSet = 0; offSet <= radius; offSet++ )
                    {
                        int gray = grayData[i + offSet] & 0xFF;
                        rowConvSum += kernalWt*gray;  
                        localConvSum += localKernalWt*gray;
                    }
                    outputData[i] = (byte) localConvSum;                     
                }
                // Only add to rowConvSum, but sum terms for localConvSum
                else if (col <= radius)
                {
                    // update rowConvSum
                    int gray = grayData[i + radius] & 0xFF;
                    rowConvSum += kernalWt*gray;
                    
                    // update localConvSum
                    float localConvSum = 0;
                    float localKernalWt = 1.0f/(col + radius + 1);
                    for (int offset = -col; offset <= radius; offset++ )
                    {
                        gray = grayData[i + offset] & 0xFF;
                        localConvSum += localKernalWt*gray;
                    }
                    outputData[i] = (byte) localConvSum; 
                }
                // Add and subtract from convSum
                else if (col > radius && col < (width - radius))
                {
                    int newGray = grayData[i + radius] & 0xFF;
                    rowConvSum += kernalWt*newGray;
                    int oldGray = grayData[i - radius - 1] & 0xFF;
                    rowConvSum -= kernalWt*oldGray;
                    outputData[i] = (byte) rowConvSum; 
                }
                // Sum terms for localConvSum
                else if (col >= width - radius)
                {
                    float localConvSum = 0;
                    float localKernalWt = 1.0f/(width - col + radius);
                    for (int offset = -radius; offset < width - col - 1; offset++ )
                    {
                        int gray = grayData[i + offset] & 0xFF;
                        localConvSum += localKernalWt*gray;
                    }
                    outputData[i] = (byte) localConvSum; 
                }                
            }
        }      
        return outputImage;
    }
    
    public static BufferedImage adaptiveThreshold(BufferedImage grayImage, int windowSize, int offset)
    {        
        // Declare the output image        
        int nCols = grayImage.getWidth();
        int nRows = grayImage.getHeight();
        int type = BufferedImage.TYPE_BYTE_GRAY;
        BufferedImage outputImage = new BufferedImage(nCols, nRows, type);
        
        // Blur
        BufferedImage blurImage = ImageUtil.meanFilter(grayImage, windowSize);
        
        // threshold
        WritableRaster blurRaster = blurImage.getRaster();
        WritableRaster grayRaster = grayImage.getRaster();        
        BufferedImage bwImage = new BufferedImage(nCols, nRows, type);
        WritableRaster bwRaster = bwImage.getRaster();        
        for (int row =0; row < nRows; row++) {
            for (int col = 0; col < nCols; col++) {
                
                // get the gray image value
                double[] grayValueArr = new double[4];
                grayRaster.getPixel(col, row, grayValueArr);
                double grayValue = grayValueArr[0];
                
                // get the blur image value
                double[] blurValueArr = new double[4];
                blurRaster.getPixel(col, row, blurValueArr);
                double blurValue = blurValueArr[0];
                
                // evaluate black white image value
                int bwValue;
                if (grayValue > (blurValue + offset)) {
                     bwValue = 255;
                }
                else {
                    bwValue = 0;
                }                
                
                // set the black white image value
                bwRaster.setPixel(col, row, new int[]{bwValue});
            }
        }
        
        return bwImage;
    }
    
    public static void fillBoundary(BufferedImage img, int value) 
    {
        WritableRaster raster = img.getRaster();
        int height = img.getHeight();
        int width = img.getWidth();
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if ((row == 0) || (col == 0) || (row == (height-1)) || (col == (width-1)) ) 
                {
                    raster.setPixel(col, row, new int[]{value});
                }
            }    
        }
    }
    
    public static void fillImage(BufferedImage img, int value) 
    {
        WritableRaster raster = img.getRaster();
        int height = img.getHeight();
        int width = img.getWidth();
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                raster.setPixel(col, row, new int[]{value});
            }    
        }
    }
    
}

