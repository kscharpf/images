import javax.swing.*;
import java.awt.*;
import javax.imageio.*;
import java.awt.image.*;
import java.io.*;

/**
 * ImageConverter takes an input image file and generates
 * a Java source file that can be compiled and run to 
 * regenerate that same image.  In other words, ImageConverter
 * is an extremely inefficient encoding scheme for images.
 */
public class ImageConverter {

   // Coefficients to smooth out the image where we smooth over a 3x3 matrix.  
   // The center pixel is waited twice as heavily as those vertically and horizontally adjacent
   // the vertically / horizontally adjacent pixels are waited twice as heavily as the corners.
   private static final double[] COEFFICIENTS = {0.0625, 0.125, 0.0625, 0.125, 0.25, 0.125, 0.0625, 0.125, 0.0625};
   
   
   public void run() {
      
      String fname = (String)JOptionPane.showInputDialog("Please enter the image filename");
   
      if ((fname == null) || (fname.length() == 0)) {
         System.out.println("Illegal Image name.  Please try again.");
         return;
      }
      
      String outfname = (String)JOptionPane.showInputDialog("Please enter the output class name");
      if ((outfname == null) || (outfname.length() == 0)) {
         System.out.println("Illegal class name.  Please try again.");
         return;
      } 
      
      BufferedImage img = null;
      try {
         img = ImageIO.read(new File(fname));
      } 
      catch(IOException e) {
         e.printStackTrace();
         System.err.println("Unable to read image");
         System.exit(1);
      }
      
      int scale = img.getHeight() * img.getWidth() / 288000;
      int stretch = 4;
      
      try {
         FileWriter outf = new FileWriter(outfname + ".java");
         outf.write("import javax.swing.*;\n");
         outf.write("import java.awt.*;\n");
         outf.write("\n");
         outf.write("public class " + outfname + " {\n");
         outf.write("\tpublic static class TestPanel extends JPanel {\n");
         int methodNum = 0;
         int callsThisMethod = 0;
         outf.write("\t\tpublic void paintComponent" + methodNum + "(Graphics g) {\n");
         for (int i=scale/2; i<img.getWidth(); i+=scale)
         {
            for (int j=scale/2; j<img.getHeight(); j+=scale) 
            {
              double sum_rs = 0;
              double sum_gs = 0;
              double sum_bs = 0;
              int counts = 0;
              double normalize = 0.0;
              
              // apply a smoothing filter
              for (int k=0; k<3 && ((k*scale)+i - scale/2) < img.getWidth(); k++) {
                for (int l=0; l<3 && ((l*scale)+j - scale/2) < img.getHeight(); l++) {
                  int col = img.getRGB((k*scale)+i - scale/2, (l*scale)+j - scale/2);
                  double rv = (double)((col & 0x00ff0000) >> 16);
                  rv *= COEFFICIENTS[k*3 + l];
                  sum_rs += rv;
                  
                  double gv = (double)((col & 0x0000ff00) >> 8);
                  gv *= COEFFICIENTS[k*3 + l];
                  sum_gs += gv;
                  
                  double bv = (double)(col & 0x000000ff);
                  bv *= COEFFICIENTS[k*3 + l];
                  sum_bs += bv; 

                  normalize += COEFFICIENTS[k*3 + l];
                }
              }

              int v = (((int)(sum_rs/normalize)) << 16) + (((int)(sum_gs/normalize)) << 8) + (((int)(sum_bs / normalize)));
              outf.write("\t\t\tg.setColor(new Color(" + v + "));\n");
              outf.write("\t\t\tg.fillRect(" + (i-scale/2)/scale*stretch + ", " + (j-scale/2)/scale*stretch + ", " + stretch + ", " + stretch + ");\n");
               
              callsThisMethod++;
              if (callsThisMethod > 500) {
                 outf.write("\t\t}\n");
                 methodNum++;
                 callsThisMethod = 0;
                 outf.write("\t\tpublic void paintComponent" + methodNum + "(Graphics g) {\n");
              }   
            }
         }
         
         outf.write("\t\t}\n");
         outf.write("\t\tpublic void paintComponent(Graphics g) {\n");
         for (int i=0; i<= methodNum; ++i) {
            outf.write("\t\t\tpaintComponent" + i + "(g);\n");
         }
         outf.write("\t\t}\n");
         outf.write("\t}\n");
         outf.write("\tpublic static void main(String []args) {\n");
         outf.write("\t\tJFrame frame = new JFrame();\n");
         outf.write("\t\tframe.add(new TestPanel());\n");
         outf.write("\t\tframe.setSize(" + img.getWidth()/scale*stretch + ", " + img.getHeight()/scale*stretch+");\n");
         outf.write("\t\tframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);\n");
         outf.write("\t\tframe.setVisible(true);\n");
         outf.write("\t}\n");
         outf.write("}\n");
         outf.close();
         System.out.println("Processed " + img.getWidth() + " by " + img.getHeight() + " image");
      } 
      catch (IOException e) {
         e.printStackTrace();
         System.err.println("Failed to generate java file");
         System.exit(1);
      }
   }
     
   public static void main(String[] args) {
      new ImageConverter().run();  
   }
}
