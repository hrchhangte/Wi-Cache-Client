/*
 * This class implements the functionality
 * to download the requested file using TCP
 * This supports mobile client, i.e., client
 * moving across APs during file download
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.spi.LocaleServiceProvider;

public class OdinClientTcpMobility{
	
	private static final String outFile = "tcp_output.csv";
	private ServerSocket dwnldSockMain;
	private Socket mesgSock;
	
	/*
	private String ctrIp = "192.168.1.2";
	private int ctrPort = 9000;
	private int dataPort = 9001;
    */
	private String ctrIp;
	private int ctrPort;
	private int dataPort;
	private String reqFileName;
	
	private String startTime;
    private String firstReadTime;
    private String finishTime;    
    private AtomicReference<String> dummyStartTime = new AtomicReference<String>(null);
    
    private AtomicBoolean switchAngt = new AtomicBoolean(false);
    private long totalTimeElapsed = 0;
    
    private AtomicLong lastByteNo = new AtomicLong(0);
    BufferedWriter foscsv;
    
    public OdinClientTcpMobility(String ctrIp, int ctrPort, int dataPort, String reqFileName) {
		// TODO Auto-generated constructor stub
    	
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
	     	     
		String fileName;
		String request;		
		
		System.out.print(getCurrTimeStmp(0)+" - Request File : ");
	    //fileName = usrRdr.readLine();
		fileName = reqFileName;
	    request = "FILE_REQ"+" "+fileName+" ";		     
	    ctrMesgWrtr.println(request); //println to java
	    ctrMesgWrtr.flush();		     
	    
	    startTime = getCurrTimeStmp(0);
	    dummyStartTime.set(startTime);
	    System.out.println(startTime+" - Request : "+request+" sent");		
	    	   	   
	    int agntCount = 0;
	    Socket dataSock;
	    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
	    foscsv = new BufferedWriter(new FileWriter(outFile, true));
	    while(true){	    		    
	    	
	    	dataSock = null;
	    	dataSock = dwnldSockMain.accept();
		    System.out.println("\n\n"+getCurrTimeStmp(0)+" - Connection received from : "+dataSock.getInetAddress());
		    		    
			Date t1 = sdf.parse(dummyStartTime.get());
			Date t2 = sdf.parse(getCurrTimeStmp(0));
			long diff = t2.getTime() - t1.getTime();
			System.out.println("\nResponse time : "+diff+"\n");
			foscsv.write("Response time "+diff+"\n");
		    
		    agntCount++;
		    dwnldFile(dataSock, fileName, agntCount);
		    
	    }		    
	     
	}
    
	public void dwnldFile(Socket dataSock, String fileNameReq, int agntCount) throws IOException, ParseException{
			
		Runnable thread = new Runnable(){
			
			public void run(){
				
				try{					
					
					System.out.println(getCurrTimeStmp(agntCount)+" - New thread created successfully");				    			    
				    
				    String mesg = null;
				    boolean append = false;
				    
				    //another agent due to mobility
				    if(agntCount>1){
				    	
				    	switchAngt.set(true);
				    	append = true;				    	
				    	
				    	while(switchAngt.get()==true)
				    		continue;
				    	
				    	PrintWriter agntMesgWrtr = new PrintWriter(dataSock.getOutputStream());
				    	mesg = "SEG_UPDATE"+" "+fileNameReq+" "+lastByteNo.get()+" ";
				    	agntMesgWrtr.write(mesg); //write to c++
				    	agntMesgWrtr.flush();
				    	
					}
				     
				    BufferedReader mesgRdr = new BufferedReader(new InputStreamReader(dataSock.getInputStream()));	
				    //mesg format : fileName fileSize bytesSent
				    mesg = mesgRdr.readLine();
				    String[] tokens = mesg.split("\\s+");		    
				    				    
				    String fileName = tokens[0];		    
				    Long fileSize = Long.parseLong(tokens[1]);		
				    Long bytesSent = Long.parseLong(tokens[2]);
				    
				    System.out.println("\n");
				    System.out.println(getCurrTimeStmp(agntCount)+" - File name : "+fileName);
				    System.out.println(getCurrTimeStmp(agntCount)+" - File size : "+fileSize);
				    System.out.println(getCurrTimeStmp(agntCount)+" - File bytes sent : "+bytesSent);									
						
					FileOutputStream fos = new FileOutputStream(fileName, append);		  				    
					
				    String localFirstReadTime = null;
				    String localFinishTime = null;
					System.out.println(getCurrTimeStmp(agntCount)+" - Starting File Download : "+fileName+" from "+dataSock.getInetAddress().getHostAddress());
				   				    
				    DataInputStream dataStrm = new DataInputStream(dataSock.getInputStream());
				    //check if data is available, wait until then
				    while(dataStrm.available()<=0){
				    	continue;				    	
				    }
				    
				    localFirstReadTime = getCurrTimeStmp(agntCount);
		    		System.out.println(localFirstReadTime+" - File Download started/available (Local)");
		    		
	    			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
	    			Date t1 = sdf.parse(dummyStartTime.get());
	    			Date t2 = sdf.parse(localFirstReadTime);
	    			long diff = t2.getTime() - t1.getTime();
	    			System.out.println("\nSetup time : "+diff+"\n");
	    			foscsv.write("Setup time "+diff+"\n");
		    		
		    		//from the first agent
		    		if(bytesSent==0){
		    			firstReadTime = localFirstReadTime;
		    			System.out.println(firstReadTime+" - File Download started/available (Overall)");
		    		}
		    		
		    		byte[] buffer = new byte[4096];
				    int read = 0;
				    long totalRead = bytesSent;
				    long localTotalRead = 0;
				    long remaining = fileSize - bytesSent;			
				    long trRead = 0;
				    
				    //String lastTime = localFirstReadTime;
				    String lastTime = null;
		    		if(bytesSent==0){
		    			//lastTime = startTime;
		    			lastTime = firstReadTime;
		    		}
		    		else{
		    			lastTime = dummyStartTime.get();
		    		}
		    		//set socket timeout
		    		//dataSock.setSoTimeout(30000);
		    		
		    		trRead = 0;
		    		while(remaining>0 && (switchAngt.get()==false)){		    		
		    		
				    //while((read = dataStrm.read(buffer,0,(int) Math.min(remaining, buffer.length)))>0
				    		//&& (switchAngt.get()==false)){
				    
				    
	    			//while((read = dataStrm.read(buffer,0, buffer.length))>0				    	
	    				//		&& (switchAngt.get()==false)){    				   			   
				    	
				    	if(dataStrm.available()>0) {			    		
				    		
				    		//read = dataStrm.read(buffer,0,(int) Math.min(remaining, buffer.length));
				    		read = dataStrm.read(buffer,0,buffer.length);
					    	
					    	localTotalRead+=read;				    	
					    	totalRead +=read;
					    	lastByteNo.set(totalRead);
					    	
					    	trRead+=read;
					    	
					    	t1 = sdf.parse(lastTime);
					    	String endTime = getCurrTimeStmp(0);
		    			    t2 = sdf.parse(endTime);
		    			    diff = t2.getTime() - t1.getTime();
		    			    
		    			    if(diff>1000){
					    	
						    	Date tin1 = sdf.parse(lastTime);					    	
						    	
			    			    Date tin2 = sdf.parse(endTime);
			    			    //update lastTime here, lasTimeTemp becomes new lastTime
						    	lastTime = endTime;
						    	
			    			    long differencein = tin2.getTime() - tin1.getTime();
			    			    //System.out.println("\n"+getCurrTimeStmp(agntCount)+" - Time taken to Download file : "+ difference+" ms");	    
			    			  		    			  
			    			    double temp1 = (double) trRead;
			    			    double temp2 = (double) differencein;
			    			    double speed = (temp1*8/1000/1000)/(temp2/1000);
			    			    //System.out.printf(getCurrTimeStmp(agntCount)+" - Throughput (Download Speed) : "+"%.2f"+" Mbps\n",speed);
			    			    DecimalFormat df = new DecimalFormat("#.##");
			    			    Number n = speed;
			    			    foscsv.write(df.format(n)+"\n");
			    			    
			    			    trRead = 0;
		    			    }
		    			    
					    	System.out.print("\r"+getCurrTimeStmp(agntCount)+" - Bytes Received Status : "+totalRead);
					    	remaining -= read;		
					    	
					    	//write to file
				    		fos.write(buffer,0,read);
					        //fos.flush(); 
				    		
				    		//last one will be the final one, keep updating inside the loop
					    	localFinishTime = getCurrTimeStmp(agntCount);	
					    	dummyStartTime.set(localFinishTime); //for sim mobility only
					
					        if(totalRead>=fileSize){        	 
					        	break;    
				    		}
				    	}//if(dataStrm.availabe())
				    	        
				   }	    				    
				    	
		    		//close the file every time a fragment is obtained from an agent
			    	fos.close();			    	
				    
				    //do this for every break			    	
			    	if(bytesSent==0){
			    		System.out.println("\n\n"+getCurrTimeStmp(agntCount)+" - Local Stats. :");
					    calStats(localTotalRead, localFirstReadTime, localFinishTime, startTime, bytesSent, 
					    		totalRead, 1, agntCount);			    		
			    	}			    	
			    	else if(bytesSent>0){
					    System.out.println("\n\n"+getCurrTimeStmp(agntCount)+" - Local Stats. :");
					    calStats(localTotalRead, localFirstReadTime, localFinishTime, dummyStartTime.get(), bytesSent, 
					    		totalRead, 1, agntCount);	
			    	}				    
			    	
			    	//finished getting the data, for total stats
			    	if(remaining<=0){
			    					    		
			    		//close the stats output file only when file is completely obtained
				    	foscsv.write("Finished"+"\n");	
				    	foscsv.close();
			    		
			    		finishTime = localFinishTime;
				    	System.out.println("\n"+getCurrTimeStmp(agntCount)+" - Overall Stats :");
				    	long bytesSentZero = 0;
				    	calStats(totalRead, firstReadTime, finishTime, startTime, bytesSentZero, totalRead, 0, agntCount);
				    	
				    	System.out.println("\n"+getCurrTimeStmp(agntCount)+" - Total Time Elapsed : "+totalTimeElapsed);			    	
			    		
			    	}				    
				
			    	//set the flag back to false
			    	if((switchAngt.get()==true)){					   	
				   		switchAngt.set(false);
			    	}				   
				   
				}catch(Exception e){
					e.printStackTrace();
				}
				
			}
		};
		new Thread(thread).start();
	}


	public void calStats(long total, String first, String finish, String start, long bytesSent, long totalRead,
			int flag, int agntCount) throws ParseException{
		
		//System.out.println(total);
		//System.out.println(first);
		//System.out.println(finish);
		//System.out.println(start);
			  
	    SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
	    Date time1 = format.parse(start);
	    Date time2 = format.parse(first);
	    long difference = time2.getTime() - time1.getTime();
	    System.out.println(getCurrTimeStmp(agntCount)+" - Time elapsed before File Download started : "+ difference+" ms");
	    
	    //add only for local stats, global one will be redundant
	    if(flag==1)
	    	totalTimeElapsed+=difference;
	    
	    System.out.println(getCurrTimeStmp(agntCount)+" - First byte no. : "+bytesSent+" (+1)");
		System.out.println(getCurrTimeStmp(agntCount)+" - Last byte no. : "+totalRead);	    
	    System.out.printf(getCurrTimeStmp(agntCount)+" - Total Download : "+total);
	    
	    format = new SimpleDateFormat("HH:mm:ss.SSS");
	    time1 = format.parse(first);
	    time2 = format.parse(finish);
	    difference = time2.getTime() - time1.getTime();
	    System.out.println("\n"+getCurrTimeStmp(agntCount)+" - Time taken to Download file : "+ difference+" ms");	    
	    
	    double temp1 = (double) total;
	    double temp2 = (double) difference;
	    double speed = (temp1*8/1000/1000)/(temp2/1000);
	    System.out.printf(getCurrTimeStmp(agntCount)+" - Throughput (Download Speed) : "+"%.2f"+" Mbps\n",speed);		
		
	}
	
	//returns current time
    public static String getCurrTimeStmp(int agntCount) {
    	
        String time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        //time = time+"["+agntCount+"]";
        return time;        
    }	
};