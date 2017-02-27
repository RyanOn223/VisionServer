package team223;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.usfirst.frc.team223.AdvancedX.vision.VisionData;


/**
 * Handles all of the processing of the camera frames, which includes:
 * <ul>
 * 		<li> generating the masked frame </li>
 * 		<li> filtering noise from the image </li>
 * 		<li> Applying contours to the image </li>
 * 		<li> extracting important data from the image </li>
 * </ul>
 * @author Brian Duemmer
 *
 */
public class VisionProcessing 
{
	// Configuration constants
	private int minHue = 0;
	private int maxHue = 110;
	
	private int minSat = 0;
	private int maxSat = 255;
	
	private int minVal = 0;
	private int maxVal = 255;
	
	private int erodeConst = 3;
	private int dilateConst = 12;
	
	
	
	private double visionTargetHeight = 6.83;
	
	private double camVertPosOffset = 1.58;
	private double camVertAngleOffset = 0.524;
	private double camHorzPosOffset = 0.917;
	private double camHorzAngleOffset = 0;
	private double camDistOffset = 2.75;
	
	private double camFOVh = 1.05;
	private double camFOVv = 0.875;
	
	private double optimalDist = 2;
	private double optimalAngle = -0.1;
	
	private double distThresh = 0.25;
	private double angleThresh = 0.05;
	
	
	
	
	public VisionProcessing() 
	{
		System.out.println("Starting VisionProcessing unit...");
	}
	
	
	/**
	 * Creates a mask from <code>rawImg</code>, with all pixels 
	 * within a certain color threshold being white, and all others black
	 * @param rawRGB the image to mask. <b>NOTE:</b> this should be in 
	 * standard RGB format!
	 * @return a masked image, in grayscale u8 format
	 */
	public Mat obtainMaskedImageRGB(Mat rawRGB)
	{
		Mat maskGray = new Mat();
		Mat rawHSV = new Mat();
		
		// convert to HSV
		Imgproc.cvtColor(rawRGB, rawHSV, Imgproc.COLOR_RGB2HSV);
		
		// these represent the min and max color values for the threshold
		Scalar threshMin = new Scalar(minHue, minSat, minVal, 0);
		Scalar threshMax = new Scalar(maxHue, maxSat, maxVal, 255);
		
		// do the mask
		Core.inRange(rawHSV, threshMin, threshMax, maskGray);

		return maskGray;
	}
	
	
	
	
	/**
	 * "purifies" a mask - that is, it attempts to remove noise 
	 * and false positive pixels in the image, as well as amplify 
	 * our target, the retro-reflective tape
	 * @param mask the input mask image
	 * @return a purified version of the mask
	 */
	public Mat purifyImage(Mat rawMask)
	{
		Mat purifiedMask = new Mat();
		
		// morphological element helpers
		Mat dilateElm = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,  new Size(dilateConst, dilateConst));
		Mat erodeElm = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,  new Size(erodeConst, erodeConst));
		
		
		Imgproc.erode(rawMask, purifiedMask, erodeElm);
		
		// run twice for MOAR purity
		Imgproc.dilate(purifiedMask, purifiedMask, dilateElm);
		Imgproc.dilate(purifiedMask, purifiedMask, dilateElm);
		
		// return the MOAR pure mask
		return purifiedMask;
	}
	
	
	
	/**
	 * Finds the center coordinates of the goal, in pixels.
	 * It does this by drawing contours around the blobs in 
	 * purifiedMask, finding the area, and finally taking the largest and 
	 * finding its center
	 * @param purifiedMask the purified binary image
	 * @return the coordinates of the goal center
	 */
	public Point getGoalCenter(Mat purifiedMask)
	{
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();
		
		// duplicate the input mask so it doesn't get screwed up
		Mat dupMask = purifiedMask.clone();
		
		// find the contours
		Imgproc.findContours(dupMask, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
		
		// pick the largest out of all of them
		double largestArea = -2;
		MatOfPoint biggestContour = null;
		
		for(MatOfPoint i : contours)
		{
			double currArea = Imgproc.contourArea(i);
			
			if(currArea > largestArea)
			{
				largestArea = currArea;
				biggestContour = i;
			}
		}
		
		// return (-1, -1) if nothing is found
		if(biggestContour == null)
			return new Point(-1, -1);
		
		
		// find the center of the biggest contour
		Rect br = Imgproc.boundingRect(biggestContour);
		
		int xPos = br.x + (br.width / 2);
		int yPos = br.y + (br.height / 2);
		
		return new Point(xPos, yPos);
	}
	
	
	
	
	public VisionData calcDataPacket(Point goalCoords, Mat frame)
	{
		VisionData data = new VisionData();
		
		// return an empty dataset if a goal wasn't detected
		if(goalCoords.x == -1 || goalCoords.y == -1)
			return data;
		
		// if we get here, we see the goal, and can proceed
		data.seesGoal = true;
		
		double radPerVpx = camFOVv / frame.height();
		double radPerHpx = camFOVh / frame.width();
		
		double camHorzAngle = radPerHpx * (goalCoords.x - (frame.width()/2));
		double camVertAngle = radPerVpx * (goalCoords.y - (frame.height()/2));
		
		data.measuredDist = ((visionTargetHeight - camVertPosOffset) / Math.tan(camVertAngle + camVertAngleOffset)) - camDistOffset;
		data.measuredAngle = camHorzAngle - camHorzAngleOffset;
		
		data.boundX = goalCoords.x;
		data.boundY = goalCoords.y;
		
		data.distError = data.measuredDist - optimalDist;
		data.angleError = data.measuredAngle - optimalAngle;
		
		data.clearToshoot = Math.abs(data.distError) < optimalDist  &&  Math.abs(data.angleError) < optimalAngle;
		
		return data;
	}

	
}













