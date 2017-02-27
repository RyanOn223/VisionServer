package team223;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import org.opencv.core.Mat;


/**
 * Class for 
 * @author Brian Duemmer
 *
 */
public abstract class VisionCommunicationServer 
{
	public final int port = 5807;
	
	private ServerSocket server;
	
	private ArrayList<ClientHandler> clients;

	
///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

	/**
	 * Manages client connections to the server
	 * @author Duemmer
	 *
	 */
	class ClientHandler extends Thread
	{
		private InputStream fromClient;
		private OutputStream toClient;
		
		
		

		public ClientHandler(Socket soc) 
		{
			try 
			{
				this.fromClient = soc.getInputStream();
				this.toClient = soc.getOutputStream();
			} 
			catch (IOException e) {
				System.err.println("Error getting streams from socket");
				e.printStackTrace();
			}
		}


		
		@Override
		public void run() 
		{
			boolean shouldStop = false;

			// run infinitely, and break out if necessary
			while(!shouldStop)
			{
				// read a request byte and take appropriate action
				try 
				{
					int request = this.fromClient.read();

					// send out data according to the request recieved
					switch(request)
					{
						case -1 :
							System.out.println("Request is -1, meaning the client disconnected");
							shouldStop = true;
							break;
							
						// ping
						case 0 :
							System.out.println("Ping request recieved");
							this.toClient.write(0);
							break;
							
						// raw frame
						case 1 :
							System.out.println("Raw frame request recieved");
							Mat rawImg = getRawImage();
							
							this.toClient.write(getImgSizeData(rawImg));
							this.toClient.write(getImgBytes(rawImg));
							break;
							
						// masked frame
						case 2 :
							System.out.println("Masked frame request recieved");
							Mat maskImg = getMaskedImage();
							
							this.toClient.write(getImgSizeData(maskImg));
							this.toClient.write(getImgBytes(maskImg));
							break;
							
						// data elements
						case 3 :
							System.out.println("Data request recieved");
							
							// get the data
							byte[] data = formDataOut();
							
							// send the length out, as a 4 byte int
							int len = data.length;
							
							ByteBuffer buf = ByteBuffer.allocate(4);
							buf.asIntBuffer().put(len);
							
							// write the length data
							this.toClient.write(buf.array());
							
							// write out the bulk data
							this.toClient.write(data);
							break;
							
					}
				} 

				// if an exception occurs, report the error and break out of the loop
				catch (Exception e) 
				{
					System.err.println("Exception while reading from client");
					e.printStackTrace();
					shouldStop = true;
				}
			}
		}
	}


	
	
///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
	
	
	public VisionCommunicationServer() 
	{
		// populate me with dynamic data loading
	}
	
	
	
	
	
	/**
	 * Starts the Vision communication server. This method should not exit, except 
	 * on an unexpected error condition
	 */
	public void runServer()
	{
		System.out.println("Starting vision Communication server...");
		
		this.clients = new ArrayList<ClientHandler>();
		
		//init the server socket
		try {
			server = new ServerSocket(port);
			System.out.println("Created server socket");
			
			// set the timeout for accept() to 3 seconds
			server.setSoTimeout(3000);
		} catch (IOException e) {
			System.err.println("Error opening server socket");
			e.printStackTrace();
		}
		
		
		// Temporary socket and handler objects used when accepting connections
		Socket tmpSock;
		ClientHandler tmpHandler;
		
		// accept incoming connections
		while(true)
		{
			try 
			{
				tmpSock = server.accept();
				
				// if we reach this, then a connection has been established, so create a new clientHdler, and add it to the client list
				System.out.println("Connection with a client established");
				tmpHandler = new ClientHandler(tmpSock);
				tmpHandler.setDaemon(true);
				tmpHandler.start();
				
				clients.add(tmpHandler);
			} 
			
			catch (SocketTimeoutException e) {
				// Do nothing, we only have timeout enabled to be able to do housekeeping on the clients in the main thread
			} 
			
			catch (IOException e) {
				System.err.println("IOException while waiting for socket");
				e.printStackTrace();
			}
			
			// remove any dead threads
			for(int i=clients.size()-1; i>=0; i--)
			{
				if(!clients.get(i).isAlive())
				{
					System.out.println("Dead client removed from client list");
					clients.remove(i);
				}
			}
		}
	}
	
	
	
	/**
	 * Converts an image into bytes, ready for transmission
	 * @param img the {@link Mat} that will be converted
	 * @return
	 */
	public byte[] getImgBytes(Mat img)
	{
		// create a buffer for the data
		byte[] data = new byte[(int) (img.total() * img.channels())];
		
		// fill the buffer
		img.get(0, 0, data);
		
		return data;
	}
	
	
	
	/**
	 * Gets the width and height of the image, and formats it to an 8 byte
	 * array
	 */
	public byte[] getImgSizeData(Mat img)
	{
		// construct an 8 byte long buffer
		ByteBuffer buff = ByteBuffer.allocate(8);
		
		int imWidth = img.rows();
		int imHeight = img.cols();
		
		IntBuffer ibuff = buff.asIntBuffer();
		
		// add the width and height to the buffer
		ibuff.put(imHeight);
		ibuff.put(imWidth);
		
		return buff.array();
	}
	
	
	
	
	/**
	 * Creates a byte array of a data packet, ready for transmission.
	 * Should be overridden by the caller
	 */
	public abstract byte[] formDataOut();
	
	
	/**
	 * Obtains a single raw frame from the camera
	 */
	public abstract Mat getRawImage();
	
	
	
	/**
	 * Obtains a single masked frame from the camera, complete with 
	 * any overlays
	 */
	public abstract Mat getMaskedImage();
	
	
}

























