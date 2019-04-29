package rayintersection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;

public class PLY {
    
    public static void saveDoubleArray(List<List<Double>> data, String folder, String filename, boolean append)
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
            
            // write the header
            buffWriter.write("ply\n");
            buffWriter.write("format ascii 1.0\n");
            buffWriter.write("comment this is a point cloud\n");
            buffWriter.write("element vertex " + data.size() + "\n");
            buffWriter.write("property float x\n");
            buffWriter.write("property float y\n");
            buffWriter.write("property float z\n");
            buffWriter.write("end_header\n");
            
            for (List<Double> row: data)
            {
                for (Double item: row)
                {
                    buffWriter.write(String.format("%.3f", item));
                    buffWriter.write(" ");
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
