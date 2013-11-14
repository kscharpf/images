import javax.swing.*;
import java.awt.*;
import javax.imageio.*;
import java.awt.image.*;
import java.io.*;

public class EdgeDetecter {
   private class MyPic extends JPanel {
     private final Image img;
     public MyPic(final Image img) {
       this.img = img;
     }
     public void paintComponent(Graphics g) {
       g.drawImage(this.img, 0, 0, null);
     }
   }

   // Coefficients to smooth out the image where we smooth over a 3x3 matrix.  
   // The center pixel is waited twice as heavily as those vertically and horizontally adjacent
   // the vertically / horizontally adjacent pixels are waited twice as heavily as the corners.
   private static final double[][] COEFFICIENTS = {{0.0625, 0.125, 0.0625}, 
                                                   {0.125, 0.25, 0.125}, 
                                                   {0.0625, 0.125, 0.0625}};
   
   //private static final double[][] GAUSSIAN = {{ 

   private void setGray(final BufferedImage img, final int x, final int y, final int gray)
   {
     setRGB(img,x,y,gray, gray, gray);
   }
   
   private void setRGB(final BufferedImage img, final int x, final int y, final int red, final int green, final int blue)
   {
     img.setRGB(x,y,(red<<16) + (green<<8) + blue);
   }
   
   private BufferedImage toGrayScale(BufferedImage img)
   {
     BufferedImage bwImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);      for (int i=0; i<img.getWidth(); ++i)
     {
       for (int j=0; j<img.getHeight(); ++j)
       {
         int val = img.getRGB(i,j) & 0x00ffffff;
         int rval = (val & 0x00ff0000) >> 16;
         int gval = (val & 0x0000ff00) >> 8;
         int bval = (val & 0x000000ff);
          
         int avgv = (int)(rval * 0.21 + gval * 0.72 + bval * 0.07);
         setGray(bwImg, i,j,avgv);
       }
     }
     return bwImg;
   }

   private int[][] image2Array(BufferedImage img, final int mask)
   {
     int[][] out = new int[img.getWidth()][img.getHeight()];
     for (int i=0; i<img.getWidth(); ++i)
     {
       for (int j=0; j<img.getHeight(); ++j)
       {
         out[i][j] = img.getRGB(i,j) & mask;
       }
     }
     return out;
   }
   
   private int[][] image2GrayArray(BufferedImage img)
   {
     return image2Array(img, 0x000000ff);
   }
   
   private void extendEdges(int[][] inArray)
   {
     for (int i=1; i<inArray.length-1; ++i)
     {
       inArray[i][0] = inArray[i][1];
       inArray[i][inArray[0].length-1] = inArray[i][inArray[0].length-2];
     }
     for (int i=1; i<inArray[0].length-1; ++i)
     {
       inArray[0][i] = inArray[1][i];
       inArray[inArray.length-1][i] = inArray[inArray.length-2][i];
     }
     inArray[0][0] = inArray[1][1];
     inArray[0][inArray[0].length-1] = inArray[1][inArray[0].length-2];
     inArray[inArray.length-1][0] = inArray[inArray.length-2][1];
     inArray[inArray.length-1][inArray[0].length-11] = inArray[inArray.length-2][inArray[0].length-2];
   }
   
   private int[][] extend2DArray(int[][] inArray)
   {
     int[][] out = new int[inArray.length+2][inArray[0].length+2];
     for (int i=1; i<=inArray.length; ++i)
     {
       for (int j=1; j<=inArray[0].length; ++j)
       {
         out[i][j] = inArray[i-1][j-1];
       }
     }

     extendEdges(out); 
     return out;
   }
   
   private BufferedImage array2Image(int[][] inArray, int xStart, int yStart, int xStop, int yStop)
   {
     BufferedImage img = new BufferedImage(xStop - xStart+1, yStop - yStart+1, BufferedImage.TYPE_BYTE_GRAY);
     for (int i=xStart; i<=xStop; ++i)
     {
       for (int j=yStart; j<=yStop; ++j)
       {
         setGray(img, i - xStart, j - yStart, inArray[i][j]);
       }
     }
     return img;
   }
   
   private int[][] smoothArray(final int[][] bwArray)
   {
     int[][] smoothedImage = new int[bwArray.length][bwArray[0].length];     
     // smooth the image
     for (int i=1; i< bwArray.length-1; i++)
     {
       for (int j=1; j<bwArray[0].length-1; j++)
       {
         smoothedImage[i][j] = (int)(COEFFICIENTS[0][0] * bwArray[i-1][j-1] + COEFFICIENTS[0][1]*bwArray[i][j-1] +
                         COEFFICIENTS[0][2] * bwArray[i+1][j-1] + COEFFICIENTS[1][0]*bwArray[i-1][j] +
                         COEFFICIENTS[1][1] * bwArray[i][j] + COEFFICIENTS[1][2] * bwArray[i+1][j] +
                         COEFFICIENTS[2][0] * bwArray[i-1][j+1] + COEFFICIENTS[2][1] * bwArray[i][j+1] +
                         COEFFICIENTS[2][2] * bwArray[i+1][j+1]);
       }
     }

     extendEdges(smoothedImage);
     return smoothedImage;
   }
   
   private int[][] gradient(final int[][] bwArray)
   {
     return gradient(bwArray, -1);
   }
   
   private int[][] gradient(final int[][] bwArray, final int gradientThreshold)
   {
     System.out.println("bwArray: " + bwArray.length + " x " + bwArray[0].length);
     int[][] gradientArray = new int[bwArray.length][bwArray[0].length];
     
     for (int i=1; i< bwArray.length - 1; ++i)
     {
       for(int j=1; j< bwArray[0].length - 1; ++j)
       {
         // Sobel operator
         int gx = bwArray[i-1][j+1] + 2*bwArray[i-1][j] + bwArray[i-1][j-1] 
                               - bwArray[i+1][j+1] - 2*bwArray[i+1][j] - bwArray[i+1][j-1];
         int gy = bwArray[i-1][j-1] + 2*bwArray[i][j-1] + bwArray[i+1][j-1]
                  - bwArray[i-1][j+1] - 2*bwArray[i][j+1] - bwArray[i+1][j+1];
         gradientArray[i-1][j-1] = (int)(Math.sqrt(gx*gx + gy*gy));
         if (gradientThreshold > 0)
         {
           if (gradientArray[i-1][j-1] < gradientThreshold)
           {
             gradientArray[i-1][j-1] = 0;
           } 
           else
           {
             gradientArray[i-1][j-1] = 0xff;
           }
         }
         double gradientAngle = Math.atan2(gy, gx) * 180.0 / Math.PI;
       }
     }
     return gradientArray;
   }
    
   public void run() {
      
      String fname = (String)JOptionPane.showInputDialog("Please enter the image filename");
   
      if ((fname == null) || (fname.length() == 0)) {
         System.out.println("Illegal Image name.  Please try again.");
         return;
      }
      
      int gradientThreshold = Integer.parseInt((String)JOptionPane.showInputDialog("Please enter the threshold"));
   
      BufferedImage img = null;
      try {
         img = ImageIO.read(new File(fname));
      } 
      catch(IOException e) {
         e.printStackTrace();
         System.err.println("Unable to read image");
         System.exit(1);
      }
   
      BufferedImage bwImg = toGrayScale(img);
      int[][] bwArray = extend2DArray(image2GrayArray(bwImg));
      int[][] smoothedArray = smoothArray(smoothArray(smoothArray(bwArray)));
      BufferedImage smoothedImg = array2Image(smoothedArray,1,1,smoothedArray.length-2, smoothedArray[0].length-2);
      int[][] gradientArray = gradient(smoothedArray, gradientThreshold);
      BufferedImage gradientImg = array2Image(gradientArray,1,1,gradientArray.length-2, gradientArray[0].length-2);
   
      JFrame frame = new JFrame();
      frame.add(new MyPic(gradientImg));  
      frame.setSize(img.getWidth(), img.getHeight());
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true); 
   }
     
   public static void main(String[] args) {
      new EdgeDetecter().run();  
   }
}