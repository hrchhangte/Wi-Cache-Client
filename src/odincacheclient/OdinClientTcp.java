/*
 * This class implements the functionality
 * to download the requested file using TCP
 * 
 * the output (stats) will be written to tcp_output.csv
 */

package odincacheclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class OdinClientTcp {

	private static final String outFile = "tcp_output.csv";
	private ServerSocket dwnldSockMain;
	private Socket mesgSock;
	/*
	private final String ctrIp = "192.168.1.145";
	private final int ctrPort = 9000;
	private final int dataPort = 9001;  
	*/
	
	private String ctrIp;
	private int ctrPort;
	private int dataPort;
	private String reqFileName;
	
	public OdinClientTcp(String ctrIp, int ctrPort, int dataPort, String reqFileName) {
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
		System.out.println(getCurrTimeStmp(0)+" - Connection to controller "+ctrIp+":"+ctrPort+" established");
		
		dwnldSockMain = new ServerSocket(dataPort);
		System.out.println(getCurrTimeStmp(0)+" - Data download socket established");	
	}
	
    public void sndFileReq() throws IOException, ParseException{	
	     
		BufferedReader usrRdr = new BufferedReader(new InputStreamReader(System.in));	     		
		PrintWriter ctrMesgWrtr = new PrintWriter(mesgSock.getOutputStream());	
		BufferedReader ctrMesgRdr = new BufferedReader(new InputStreamReader(mesgSock.getInputStream()));			
		
		System.out.print(getCurrTimeStmp(0)+" - Request File : ");
	    //String fileName = usrRdr.readLine();
		String fileName = reqFileName;
	    String request = "FILE_REQ"+" "+fileName+" ";		     
	    ctrMesgWrtr.println(request); //println to java
	    ctrMesgWrtr.flush();		     	
	    
	    String startTime = getCurrTimeStmp(0);
	    System.out.println(startTime+" - Request : "+request+" sent");	
	    	
	    //mesg format : <SEG_COUNT> <chunkCount>
	    String ctrMesg = ctrMesgRdr.readLine();
	    System.out.println(getCurrTimeStmp(0)+" - Message received: "+ctrMesg);
	    
	    //override startTime here
	    startTime = getCurrTimeStmp(0);
	    
	    String[] ctrTokens = ctrMesg.split("\\s+");	
	    int chunkCount = Integer.parseInt(ctrTokens[1]);	        
	    
	    String firstReadTime = null;
	    int i = 0;
	    
	    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
    	Date t1 = null;
    	Date t2 = null;
    	long diff = 0;
    	int firstChunkFlag = 1;
    	long localRead = 0;
    	BufferedWriter foscsv = new BufferedWriter(new FileWriter(outFile, true));
    	
	    while(i<chunkCount) {
	    	
	    	Socket dataSock = dwnldSockMain.accept();
	    	String chunkConnTime = getCurrTimeStmp(0);
	    	System.out.println(chunkConnTime+" - Connection received from : "+dataSock.getInetAddress());
	    	
	    	BufferedReader agentMesgRdr = new BufferedReader(new InputStreamReader(dataSock.getInputStream()));	
	    	String agentMesg = agentMesgRdr.readLine();    	
	    	System.out.println(getCurrTimeStmp(0)+" - Message received: "+agentMesg);
	    	
	    	String[] agentTokens = agentMesg.split("\\s+");
	    	String chunkName = agentTokens[0];
	    	long chunkSize = Long.parseLong(agentTokens[1]);
	    	//long skipBytes = Long.parseLong(agentTokens[2]);    	
	    	
	    	DataInputStream dataStrm = new DataInputStream(dataSock.getInputStream());
	    	FileOutputStream fos = new FileOutputStream(chunkName);
	    	
	    	long remaining = chunkSize;
	    	byte[] buffer = new byte[4096];
	    	int read = 0;
	    	long totalRead = 0;    	
	    	int flag = 0;    	
	    	
	    	while(remaining>0) {    		
	    		
	    		read =  dataStrm.read(buffer,0,buffer.length);  		    		
	    		fos.write(buffer,0,buffer.length);    		
	    		
	    		if(flag==0) {
	    			firstReadTime = getCurrTimeStmp(0);
	    			System.out.println("\n"+firstReadTime+" - Chunk data arrived");
	    			flag=1;
	    			
	    			if(firstChunkFlag==1) { //for entire file
	    				firstChunkFlag = 0;
	    				
	    				t1 = sdf.parse(firstReadTime); //start this for the entire file
	    			}
	    		}	   
	    		
	    		if(read<0)
	    			break;
	    		
	    		localRead =  localRead + read;   		
	    		
	    		totalRead = totalRead + read;	    		
	    		remaining = remaining - read;
	    		System.out.print("\r"+getCurrTimeStmp(0)+" - Bytes Received Status : "+totalRead);
	    			    		
	    		if(firstChunkFlag==0) {//if first byte of first chunk has arrived
	    			
	    			t2 = sdf.parse(getCurrTimeStmp(0));
	    			diff = t2.getTime() - t1.getTime();	    			
	    			
	    			if(diff>2000) {    				
	    				
	    			    long diffin = t2.getTime() - t1.getTime();   			      			  		    			  
	    			    t1 = t2;
	    			    
	    			    double temp1 = (double) localRead;
	    			    double temp2 = (double) diffin;
	    			    double speed = (temp1*8/1000/1000)/(temp2/1000);
	    			    //System.out.printf(getCurrTimeStmp(agntCount)+" - Throughput (Download Speed) : "+"%.2f"+" Mbps\n",speed);
	    			    DecimalFormat df = new DecimalFormat("#.##");
	    			    Number n = speed;
	    			    foscsv.write(df.format(n)+"\n");
	    			    
	    			    localRead = 0;
	    				
		    		}
	    			
	    		}	    		
	    			    		
		        if(totalRead>=chunkSize)        	 
		        	break;    
	    	}//while remaining	    	
	    	
	    	fos.close();
	    	
	    	String finishTime = getCurrTimeStmp(0);
	    	calStats(startTime, chunkConnTime, firstReadTime, finishTime, totalRead); //this will take up some time
	    	startTime = getCurrTimeStmp(0);
	    	
	    	i++;
	    	dataSock.close();
	    	
	    }//while chunkCount
	    
	    foscsv.close();
	     
	}
    
    public void calStats(String startTime, String chunkConnTime, String firstReadTime, String finishTime, long totalRead) throws ParseException{
		
		System.out.println("\n"+finishTime+" - Chunk Download Finished! Total bytes received : "+totalRead);
		
		SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
	    Date time1 = format.parse(startTime);
	    Date time2 = format.parse(chunkConnTime);
	    long difference = time2.getTime() - time1.getTime();
	    System.out.println(getCurrTimeStmp(0)+" - Time elapsed before  Connection (Chunk) started : "+ difference+" ms");
	     
	    format = new SimpleDateFormat("HH:mm:ss.SSS");
	    time1 = format.parse(startTime);
	    time2 = format.parse(firstReadTime);
	    difference = time2.getTime() - time1.getTime();
	    System.out.println(getCurrTimeStmp(0)+" - Time elapsed before Chunk Download started : "+ difference+" ms");
	     
	    format = new SimpleDateFormat("HH:mm:ss.SSS");
	    time1 = format.parse(firstReadTime);
	    time2 = format.parse(finishTime);
	    difference = time2.getTime() - time1.getTime();
	    System.out.println(getCurrTimeStmp(0)+" - Time taken to Download Chunk : "+ difference+" ms");
	     
	    double temp1 = (double) totalRead;
	    double temp2 = (double) difference;
	    double speed = (temp1*8/1000/1000)/(temp2/1000);
	    System.out.printf(getCurrTimeStmp(0)+" - Chunk Download Speed : "+"%.2f"+" Mbps\n",speed);		
		
	}
    
  //returns current time
    public static String getCurrTimeStmp(int agntCount) {
    	
        String time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        return time;        
    }	
    
};