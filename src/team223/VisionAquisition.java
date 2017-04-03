package team223;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;


/**
 * Class whose only job is to be able to aquire images
 * from the camera
 * @author Brian Duemmer
 *
 */
public class VisionAquisition 
{
	// robot's camera
	private VideoCapture camera = new VideoCapture();

	private boolean cameraOpen = false;
	
	private Mat currFrame = new Mat();
	
	
	// keeps continually grabbing frames from the camera
	private Runnable frameGrabber = new Runnable() 
	{
		public void run() 
		{
			if(!cameraOpen)
				openCamera();
			
			try
			{
				camera.read(currFrame);
				
			} catch (Exception e)
			{
				System.err.println("Error grabbing frame!");
				
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				e.printStackTrace();
				cameraOpen = false;
			}
		}
	};

	////////// Configuration variables - Set to safe defaults, and override later with AdvancedX

	// address of the camera
	private String cameraAddress;

	/**
	 * Generates a new instance of the videoAquisition class
	 */
	public VisionAquisition(String camAddress) 
	{
		System.out.println("Starting VisionAquisition...");
		this.cameraAddress = camAddress;
		
		// init the executor service
		ScheduledExecutorService sexs = Executors.newScheduledThreadPool(1);
		sexs.scheduleAtFixedRate(frameGrabber, 0, 67, TimeUnit.MILLISECONDS);
	}

	/**
	 * Attempts to open the camera, or does nothing if it is already open.
	 * @return true if the camera was opened before this, or it was just opened
	 */
	public boolean openCamera()
	{
		// if already opened, return true
		if(camera.isOpened())
		{
			cameraOpen = true;
			return true;
		}


		System.out.println("Attempting to open camera...");

		try
		{
			camera.open("http://" +cameraAddress+ "/mjpg/video.mjpg");
			//camera.open(cameraAddress);
		} catch(Exception e)
		{
			System.err.println("Error opening camera! DETAILS:");
			e.printStackTrace();
		}
		
		cameraOpen =  camera.isOpened();
		
		if(cameraOpen)
			System.out.println("Camera opened successfully");
		
		else
		{
			System.err.println("Failed to open camera");
			try { Thread.sleep(1000);  } catch (InterruptedException e) { e.printStackTrace();}
		}
		
		return cameraOpen;
	}


	/**
	 * Acquires a frame from the camera
	 * @return a frame from the camera, or an empty mat if the camera couldn't be opened
	 */
	public Mat grabFrame()
	{
		if(currFrame != null)
			return currFrame;
		
		else
		{
			System.out.println("WARNING! attempted to grab frame, but it was null!");
			return new Mat();
		}
	}

}











