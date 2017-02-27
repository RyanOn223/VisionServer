package team223;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.usfirst.frc.team223.AdvancedX.AdvancedXManager;
import org.usfirst.frc.team223.AdvancedX.RoboLogManagerBase;
import org.usfirst.frc.team223.AdvancedX.vision.VisionData;

/**
 * Serves as an entry point and starting class for the vision server. Very little is directly 
 * done here, as mostly everything is in the seperate modules
 * @author Brian Duemmer
 *
 */
public class DuemmerVisionMain 
{ 
	private static VisionAquisition aquisition;
	private static VisionProcessing processing;
	private static VisionCommunicationServer communication;
	
	private static AdvancedXManager manager;
	
	
	public static void main(String[] args) 
	{
		// import the opencv DLL
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
//		// init the AdvancedX components
//		RoboLogManagerBase logBase = new RoboLogManagerBase("/home/pi/223/logging", 5802, Level.TRACE);
//		manager = new AdvancedXManager("/home/pi/223/VisionConfig.xml") {
//			
//			@Override
//			public boolean load() {
//				// TODO Auto-generated method stub
//				return false;
//			}
//			
//			@Override
//			public boolean free() {
//				// TODO Auto-generated method stub
//				return false;
//			}
//		};
		
		// init everything
		aquisition = new VisionAquisition();
		processing = new VisionProcessing();
		
		// optionally test
		testFxn();
		
		communication = new VisionCommunicationServer() {
			
			@Override
			public Mat getRawImage() {
				return aquisition.grabFrame();
			}
			
			@Override
			public Mat getMaskedImage() 
			{
				Mat mask = processing.obtainMaskedImageRGB(aquisition.grabFrame());
				mask = processing.purifyImage(mask);
				
				Mat ret = new Mat();
				Imgproc.cvtColor(mask, ret, Imgproc.COLOR_GRAY2RGB);
				return ret;
			}
			
			@Override
			public byte[] formDataOut() 
			{
				Mat mask = processing.obtainMaskedImageRGB(aquisition.grabFrame());
				mask = processing.purifyImage(mask);
				
				Point goalCenter = processing.getGoalCenter(mask);
				VisionData data = processing.calcDataPacket(goalCenter, mask);
				
				return data.buildDataPacket();
			}
		};
		
		
		communication.runServer();
		
	}
	
	
	/**
	 * Provides a fairly thorough test of the vision targeting algorithm's 
	 * functionality. For testing only, of course
	 */
	public static void testFxn()
	{
		// grab a frame, and start the timer
		Mat rawFrame = aquisition.grabFrame();
		long start = System.currentTimeMillis();
		
		// do the bulk processing
		Mat rawMaskedFrame = processing.obtainMaskedImageRGB(rawFrame);
		Mat purifiedMaskedFrame = processing.purifyImage(rawMaskedFrame);
		Point center = processing.getGoalCenter(purifiedMaskedFrame);
		
		// overlay the detected center onto the original
		Point pt1 = new Point(center.x-3, center.y-3);
		Point pt2 = new Point(center.x+3, center.y+3);
		Imgproc.rectangle(rawFrame, pt1, pt2, new Scalar(0, 255, 0));
		
		// print the center coordinates, an the time it too to process everything
		System.out.println("CENTER: (" +center.x+ ", " +center.y+ ")");
		System.out.println("processing time: " + (System.currentTimeMillis() - start));
		
		// calc the data packet
		System.out.println(processing.calcDataPacket(center, rawMaskedFrame));
		
		
		// write the images to files
		Imgcodecs.imwrite("C:/Users/develoer/Desktop/camFrame.jpeg", rawFrame);
		Imgcodecs.imwrite("C:/Users/develoer/Desktop/rawMaskFrame.jpeg", rawMaskedFrame);
		Imgcodecs.imwrite("C:/Users/develoer/Desktop/purifiedMaskedFrame.jpeg", purifiedMaskedFrame);
		
		System.exit(0);
	}

}
