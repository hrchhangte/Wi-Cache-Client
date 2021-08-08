/*
 * This is a simple application that uses the 
 * WiCacheClientModule to download a file
 * from a given server that caches files/segments 
 * on Wi-Cache APs.
 * 
 * command line arguments are: <server address> <server port no. for control> 
 * <client port no. for data> <file name to download> <protocol to use for file download>
 */

import java.net.ServerSocket;
import java.net.Socket;

import odincacheclient.*;

public class MobileClient {
    
	public static void main(String args[]) throws Exception{
		
		/*
		 * parse the input parameters
		 * command line input format: <server address> <server port for control> <client port for data> <file name> <protocol>
		 */
		final String ctrIp = args[0];
		final int ctrPort = Integer.parseInt(args[1]);
		final int dataPort = Integer.parseInt(args[2]);
		String reqFileName = args[3];
		String protocol =  args[4];
		
		//instantiate the WiCacheClientModule and 
		//call DownloadFile
		WiCacheClientModule wicache = new WiCacheClientModule();
		wicache.DownloadFile(ctrIp, ctrPort, dataPort, reqFileName, protocol); 
	}	
}