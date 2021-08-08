/*
 * This class implements the functionality
 * to download the requested file using SCTP
 * (one-to-one) 
 * 
 */

package odincacheclient;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.sun.nio.sctp.*;

public class OdinClientSctpOne{
	
	private static final String outFile = "sctp_output.csv";
	private SctpServerChannel dataSrvrChannl;
	private SctpChannel sctpChannl;
	private SocketAddress clntInetSockAddr;
	private Socket mesgSock;
	/*
	private final String ctrIp = "192.168.1.2";
	private final int ctrPort = 9000;
	private final int dataPort = 9001;
    */
    private BufferedReader usrRdr;    
    private PrintWriter mesgWrtr; 
    
	private String ctrIp;
	private int ctrPort;
	private int dataPort;
	private String reqFileName;
	
	public OdinClientSctpOne(String ctrIp, int ctrPort, int dataPort, String reqFileName) {
		this.ctrIp = ctrIp;
    	this.ctrPort = ctrPort;
    	this.dataPort = dataPort;
    	this.reqFileName = reqFileName;  
	}
    
    
    public void strtClnt() throws UnknownHostException, IOException, ParseException{
    	
    	setupSock();
    	sndFileReq();
    	
    }
    
    public void setupSock() throws UnknownHostException, IOException{
		
		mesgSock = new Socket(ctrIp, ctrPort);
		System.out.println(getCurrTimeStmp()+" - Connection to controller "+ctrIp+":"+ctrPort+" established");
		
		dataSrvrChannl = SctpServerChannel.open();
		clntInetSockAddr = new InetSocketAddress(dataPort);
		dataSrvrChannl.bind(clntInetSockAddr);	
		System.out.println(getCurrTimeStmp()+" - Sctp Multi Channel established");
		
	}
	
    public void sndFileReq() throws IOException, ParseException{	
	     
		usrRdr = new BufferedReader(new InputStreamReader(System.in));	     		
		mesgWrtr = new PrintWriter(mesgSock.getOutputStream());	
	     	     
		String fileName;
		String request;
		String startTime;
		 
		String fileSizeStr;
		String temp;
		long fileSize;	     
		
		ByteBuffer mesgBuf;
		MessageInfo mesgInfo;
		Charset charset = Charset.forName("ISO-8859-1");
        CharsetDecoder decoder = charset.newDecoder();
	     
	    while(true){
	    	 
	    	System.out.print(getCurrTimeStmp()+" - Request File : ");
		    //fileName = usrRdr.readLine();
	    	fileName = reqFileName;
		    request = "FILE_REQ "+fileName;		     
		    mesgWrtr.println(request);
		    mesgWrtr.flush();		     
		    startTime = getCurrTimeStmp();
		    System.out.println(startTime+" - Request : "+request+" sent");	
		    
		    mesgBuf = ByteBuffer.allocateDirect(4096);			
		    sctpChannl = dataSrvrChannl.accept();
		    mesgInfo = sctpChannl.receive(mesgBuf, null, null);		    
		    mesgBuf.flip();	        		    
		    
		    System.out.println(getCurrTimeStmp()+" - Association iniated with : "+mesgInfo.address());
		    System.out.println(getCurrTimeStmp()+" - Association id : "+mesgInfo.association().associationID());
		    System.out.println(getCurrTimeStmp()+" - Message received at stream no. : "+mesgInfo.streamNumber());
		    System.out.println(getCurrTimeStmp()+" - Association max outstreams : "+mesgInfo.association().maxOutboundStreams());
		    System.out.println(getCurrTimeStmp()+" - Association max instreams : "+mesgInfo.association().maxInboundStreams());
		    
		    fileSizeStr = decoder.decode(mesgBuf).toString();
		    temp = fileSizeStr.replaceAll("\r", "").replaceAll("\n", "");
		    fileSize = Long.parseLong(temp);		
		    System.out.println(getCurrTimeStmp()+" - File size of "+fileName+" : "+fileSize);
		    
		    dwnldFile(fileName, fileSize, startTime, mesgInfo.association());		    
		     
	     }	
	     
	}
    
	public void dwnldFile(String fileName, long fileSize, String startTime, Association assoc) throws IOException, ParseException{
			
		FileOutputStream fos = new FileOutputStream(fileName);		     
	    System.out.println(getCurrTimeStmp()+" - File download location : "+fileName);
	    byte[] buffer;
	    int read;
	    long totalRead;   
		
		System.out.println(getCurrTimeStmp()+" - Starting File Download : "+fileName+" from "
		+sctpChannl.getRemoteAddresses());
	    int start;
	    String firstReadTime = null;	    
	    
	    ByteBuffer mesgBuf = ByteBuffer.allocateDirect(4096);
		MessageInfo mesgInfo;
		        
        read = 0;
        totalRead = 0;
        start = 0;
        //get file from the socket
	    while(totalRead<=fileSize){
	    	
	    	mesgBuf.clear();
	       	mesgInfo = sctpChannl.receive(mesgBuf, null, null);
	    	mesgBuf.flip();
	    	read = mesgInfo.bytes();
	    	
	    	if(start == 0 && read > 0){
	    		start = 1;
	    		firstReadTime = getCurrTimeStmp();
	    		System.out.println(firstReadTime+" - File Download started");
	    	}
	    	 
	    	totalRead +=read;
	        System.out.print("\r"+getCurrTimeStmp()+" - Bytes Received Status : "+totalRead);
	    		    	 
	        //write to file
	    	buffer = new byte[read];
	    	mesgBuf.get(buffer);
	    	fos.write(buffer,0,buffer.length);
	        //fos.flush();         
	
	    	//mesgBuf.clear();
	        if(totalRead>=fileSize)        	 
	        	break;            
	  
	    }        	    
	    
	    fos.close();
	    
	    calStats(startTime, firstReadTime, totalRead);
			
	}


	public void calStats(String startTime, String firstReadTime, long totalRead) throws ParseException{
		
		String finishTime = getCurrTimeStmp();
	    System.out.println("\n"+finishTime+" - File Download Finished! Total bytes received : "+totalRead);
	     
	    SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
	    Date time1 = format.parse(startTime);
	    Date time2 = format.parse(firstReadTime);
	    long difference = time2.getTime() - time1.getTime();
	    System.out.println(getCurrTimeStmp()+" - Time elapsed before File Download started : "+ difference+" ms");
	     
	    format = new SimpleDateFormat("HH:mm:ss.SSS");
	    time1 = format.parse(firstReadTime);
	    time2 = format.parse(finishTime);
	    difference = time2.getTime() - time1.getTime();
	    System.out.println(getCurrTimeStmp()+" - Time taken to Download file : "+ difference+" ms");
	     
	    double temp1 = (double) totalRead;
	    double temp2 = (double) difference;
	    double speed = (temp1*8/1000/1000)/(temp2/1000);
	    System.out.printf(getCurrTimeStmp()+" - File Download Speed : "+"%.2f"+" Mbps\n",speed);		
		
	}
	
	//returns current time
    public static String getCurrTimeStmp() {
    	
        String time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        return time;
        
    }

	
};