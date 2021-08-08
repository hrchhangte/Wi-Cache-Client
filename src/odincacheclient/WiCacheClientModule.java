/*
 * This is the main class that will be instantiated
 * by the client applications.
 * 
 * The main function that will be called is  DownloadFile
 * input parameters are: <server address> <server port for control> <client port for data>
 * <file name> <protocol>
 * 
 * Depending upon the protocol, it instantiates the 
 * appropriate object for downloading the file
 */

package odincacheclient;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.ParseException;

public class WiCacheClientModule {
	
	private String transProto;
	private OdinClientTcpMobility tcp;
	private OdinClientSctpOne sctpOne;
	private OdinClientSctpMany sctpMany;
	
	public void DownloadFile(String ctrIp, int ctrPort, int dataPort, String reqFileName, String proto){
		// TODO Auto-generated constructor stub

		transProto = proto;
		
		/*
		 * This uses TCP for file download
		 */
		if(transProto.equals("tcp")){
			//OdinClientTcp tcp = new OdinClientTcp(ctrIp, ctrPort, dataPort, reqFileName);
			tcp = new OdinClientTcpMobility(ctrIp, ctrPort, dataPort, reqFileName);
			
			try {
				tcp.strtClnt();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
		
		/*
		 * This uses SCTP for file download
		 * Supports one-to-one socket
		 * Currently does not support mobility
		 */
		else if(transProto.equals("sctpOne")){
			sctpOne = new OdinClientSctpOne(ctrIp, ctrPort, dataPort, reqFileName);
			try {
				sctpOne.strtClnt();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 
		
		/*
		 * This uses SCTP for file download
		 * Supports one-to-many socket
		 * Currently does not support mobility
		 */
		else if(transProto.equals("sctpMany")){
			sctpMany = new OdinClientSctpMany(ctrIp, ctrPort, dataPort, reqFileName);
			try {
				sctpMany.strtClnt();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 	
	}

};
