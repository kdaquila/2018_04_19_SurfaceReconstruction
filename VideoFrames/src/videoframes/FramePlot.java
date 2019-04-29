package videoframes;

import config.Util;
import java.awt.BasicStroke;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.dom4j.Element;
import org.dom4j.Node;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;

public class FramePlot {
    
    public FramePlot(String configPath) {
        // load configuration data
        Element e = Util.loadXML(configPath);
        
        // get each folder to work one
        List<Node> calibFolders = e.selectNodes("/config/calibrationFolders/folder");
        List<Node> sceneFolders = e.selectNodes("/config/sceneFolders/folder");
        List<Node> allFolders = new ArrayList<>();
        allFolders.addAll(calibFolders);
        allFolders.addAll(sceneFolders);
        
        // work on each folder using its own base path
        for (Node folder: allFolders)
        {
            String baseDirPath = e.selectSingleNode("/config/baseFolder").getText() + "\\" +
                                 folder.selectSingleNode("name").getText();
            System.out.println("\nNow working on images from: " + baseDirPath);
            plotFrames(e, baseDirPath);
        }  
    }
    
    public final void plotFrames(Element root, String baseDirPath)
    {
        
        String plotDirPath = baseDirPath + "\\" +
                             root.selectSingleNode("/config/folders/framePlot").getText();
        String framePlotDataName = root.selectSingleNode("/config/files/framePlotData").getText();
        String framePlotImageName = root.selectSingleNode("/config/files/framePlotImage").getText();
        
        // read the plot dataset from file
        File framePlotDataFile = new File(plotDirPath + "\\" + framePlotDataName);        
        XYSeriesCollection dataset = new XYSeriesCollection();
        try {
            FileReader fileReader = new FileReader(framePlotDataFile);
            BufferedReader buffReader = new BufferedReader(fileReader);        
            int nPlots = 4;
            for (int plotIndex = 0; plotIndex < nPlots; plotIndex++){
                String plotName = buffReader.readLine();
                String xStringLine = buffReader.readLine();
                String[] xStrings = xStringLine.split(", ");
                String yStringLine = buffReader.readLine();
                String[] yStrings = yStringLine.split(", ");
                XYSeries xySeries = new XYSeries(plotName);
                for (int i = 0; i < xStrings.length; i++) {
                    xySeries.add(Double.parseDouble(xStrings[i]),
                                 Double.parseDouble(yStrings[i]));
                }
                dataset.addSeries(xySeries);            
            }
        }
        catch (FileNotFoundException exc) {
            throw new RuntimeException("Can't open the plot data file");
        }
        catch (IOException exc) {
            throw new RuntimeException("Can't read lines form the plot data file");
        }  
        catch (NullPointerException exc) {
            throw new RuntimeException("Can't parse lines form the plot data file. Maybe the file is empty?");
        } 
        
        // generate the plot
        int height = Integer.parseInt(root.selectSingleNode("/config/frameSelection/framePlotHeight").getText());
        int width = Integer.parseInt(root.selectSingleNode("/config/frameSelection/framePlotWidth").getText());
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                FramePlotWindow framePlotWindow = new FramePlotWindow(dataset, width, height);
                framePlotWindow.setup();
                framePlotWindow.pack();
                framePlotWindow.setVisible(true);
                
                // save the plot     
                File savePlotFile = new File(plotDirPath + "\\" + framePlotImageName);
                try {
                    ChartUtilities.saveChartAsPNG(savePlotFile, framePlotWindow.chart, width, height); 
                }
                catch (IOException e) {
                    throw new RuntimeException("Can't save the frame plot.");
                }
                
                // discard the plot
                framePlotWindow.dispose();
            }
        });
    }
}

class FramePlotWindow extends ApplicationFrame {
    
    JFreeChart chart;
    ChartPanel chartPanel;
    int height;
    int width;
    
    public FramePlotWindow(XYDataset dataset, int width, int height) {
        super("Frame Plot");
        this.chart = ChartFactory.createXYLineChart("Frame Mean Brightness" ,
                                                    "Frame Number" ,
                                                    "Frame Average Brightness" ,
                                                    dataset ,
                                                    PlotOrientation.VERTICAL ,
                                                    true , true , false);
        this.chartPanel = new ChartPanel(this.chart);       
        this.width = width;
        this.height = height;
    }
    
    public void setup(){
        this.chartPanel.setPreferredSize(new java.awt.Dimension(this.width, this.height));        
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint( 0 , Color.GREEN );
        renderer.setSeriesPaint( 1 , Color.RED );      
        renderer.setSeriesPaint( 2 , Color.CYAN );
        renderer.setSeriesPaint( 3 , Color.BLACK );
        renderer.setSeriesStroke( 0 , new BasicStroke( 0.0f ) );
        renderer.setSeriesStroke( 1 , new BasicStroke( 0.0f )  );      
        renderer.setSeriesStroke( 2 , new BasicStroke( 0.0f ) );
        renderer.setSeriesStroke( 3 , new BasicStroke( 1.0f ) );
        this.chart.getXYPlot().setRenderer(renderer); 
        setContentPane(this.chartPanel);
    }
    
    public void save(String path) { 
        
   }
}
