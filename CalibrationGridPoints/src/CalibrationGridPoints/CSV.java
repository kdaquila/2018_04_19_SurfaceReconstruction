package CalibrationGridPoints;

import config.Util;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;

public class CSV {
    
    public static double[][] loadDoubleMatrixAsArray(String path)
    {
        List<List<Double>> outputList = new ArrayList<>();
        double[][] outputArray;
        try {
            File newFile = new File(path);
            FileReader reader = new FileReader(newFile);
            BufferedReader buffReader = new BufferedReader(reader);
            String newLine = buffReader.readLine();
            while (newLine != null)
            {
                outputList.add(Util.parseDoubleArray(newLine));
                newLine = buffReader.readLine();
            }
            
            outputArray = new double[outputList.size()][outputList.get(0).size()];
            for (int row = 0; row < outputList.size(); row++)
            {
                for (int col = 0; col < outputList.get(0).size(); col++ )
                {
                    outputArray[row][col] = outputList.get(row).get(col);
                }
            }
            
        }
        catch (FileNotFoundException exp)
        {
            for (StackTraceElement elem: exp.getStackTrace())
            {
                System.out.println(elem);
            }
            throw new RuntimeException("Could not open the file");
        }
        catch (IOException exp)
        {
            for (StackTraceElement elem: exp.getStackTrace())
            {
                System.out.println(elem);
            }
            throw new RuntimeException("Could not read a line from the file");
        }
        return outputArray;
    }
    
    public static List<List<Double>> loadDoubleMatrix(String path)
    {
        List<List<Double>> output = new ArrayList<>();
        try {
            File newFile = new File(path);
            FileReader reader = new FileReader(newFile);
            BufferedReader buffReader = new BufferedReader(reader);
            String newLine = buffReader.readLine();
            while (newLine != null)
            {
                output.add(Util.parseDoubleArray(newLine));
                newLine = buffReader.readLine();
            }
        }
        catch (FileNotFoundException exp)
        {
            for (StackTraceElement elem: exp.getStackTrace())
            {
                System.out.println(elem);
            }
            throw new RuntimeException("Could not open the file");
        }
        catch (IOException exp)
        {
            for (StackTraceElement elem: exp.getStackTrace())
            {
                System.out.println(elem);
            }
            throw new RuntimeException("Could not read a line from the file");
        }
        return output;
    }
    
    public static void saveDoubleMatrix(List<List<Double>> matrix, String folder, String filename, boolean append)
    {
        List<List<Double>> output = new ArrayList<>();
        try {
            // create the folder if needed
            File dir = new File(folder);
            if (!dir.exists())
            {
                FileUtils.forceMkdir(dir);
            }
            
            // open the write
            File newFile = new File(folder + "\\" + filename);
            FileWriter writer = new FileWriter(newFile, append);
            BufferedWriter buffWriter = new BufferedWriter(writer);
            
            for (List<Double> row: matrix)
            {
                for (Double item: row)
                {
                    buffWriter.write(String.format("%.3f", item));
                    buffWriter.write(",");
                }                
                buffWriter.write("\n");
            }
            
            // close the writer
            buffWriter.close();
        }
        catch (IOException exp)
        {
            for (StackTraceElement elem: exp.getStackTrace())
            {
                System.out.println(elem);
            }
            throw new RuntimeException("Could not save to the file");
        }
    }
    
    public static void saveDoubleMatrix(double[][] matrix, String folder, String filename)
    {
        List<List<Double>> output = new ArrayList<>();
        try {
            // create the folder if needed
            File dir = new File(folder);
            if (!dir.exists())
            {
                FileUtils.forceMkdir(dir);
            }
            
            // open the write
            File newFile = new File(folder + "\\" + filename);
            FileWriter writer = new FileWriter(newFile);
            BufferedWriter buffWriter = new BufferedWriter(writer);
            
            for (double[] row: matrix)
            {
                for (double item: row)
                {
                    buffWriter.write(String.format("%.3f", item));
                    buffWriter.write(",");
                }                
                buffWriter.write("\n");
            }
            
            // close the writer
            buffWriter.close();
        }
        catch (IOException exp)
        {
            for (StackTraceElement elem: exp.getStackTrace())
            {
                System.out.println(elem);
            }
            throw new RuntimeException("Could not save to the file");
        }
    }
    

}
