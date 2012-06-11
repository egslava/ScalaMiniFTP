package org.egslava.ftp.state
import org.egslava.ftp.ControlConnection
import java.net.Socket
import java.net.ServerSocket
import org.egslava.ftp.Main
import org.egslava.ftp.config.PortRange
import java.net.BindException
import java.net.SocketTimeoutException
import java.io.File
import org.egslava.ftp.FileSystemNavigator
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.SocketException

class WaitForCommandsState(owner: ControlConnection) extends FtpState{
  
	var dataSocket: Socket = null;
	var pasvSocket: ServerSocket = null;
	
	val answer = 
"""
-rw-r--r--	1	ftp	ftp	528	Nov	01	2007	README
-rw-r--r--	1	ftp	ftp	560	Sep	28	2007	index.html
drwxr-xr-x 	40	ftp	ftp	4096	May	23	11:00	pub"""
	    
	val currentDir = new FileSystemNavigator(owner);
	var transferThread: Thread = null;
	
	def processMessage(message: String): String = {
	    tryAcceptPassive();
	    
	    Thread.sleep(1);
	    generalProcessing(message) match{
	        case null => ;
	        case answer => return answer;
	    }
	    
	    message match{
			case owner.Noop() => return "200 NOOP ok\r\n";
			case owner.User(_) => return "530 Can't change from guest user\r\n";
			case owner.Pass(_) => return "230 Already logged in\r\n";
			case owner.Pasv() => return pasv() + "\r\n";
			case owner.List() => return list() + "\r\n";
			case _ =>;
	    }
	    message match{
			case owner.Nlst() => return nlst();
			case owner.TypeCMD(mode) => return "200 Switching to Binary mode.\r\n";
			case owner.Pwd() => return currentDir.pwd();
			case owner.Site("HELP") => "450 Not realized yet\r\n";//return "200-\r\n200\r\n";
			case _ =>;
	    }
	    message match{
	        case owner.Cdup() => return currentDir.cdup();
	        case owner.Stor(fileName) => return stor(fileName);
	        case _ =>;
	    }
		message match{
			case owner.Cwd(path) => return currentDir.cwd(path);
			case owner.Retr(filePath) => return retr(filePath);
			case owner.Empty() => return ""
			case unrecognizedCommand => return "500 Unrecognized command " + unrecognizedCommand + "\r\n";
		}
	}
	
	def pasv(): String = {
	    if(pasvSocket != null){
	        pasvSocket.close();   
	        pasvSocket = null;
	    } 
	    
	    pasvSocket = bindInRanges();
	    if(pasvSocket == null){
	        println("There are not free port for connection");
	        return "425 There are not free port for connection"
	    }
	    pasvSocket.setSoTimeout(1);
	    
	    val IP = """/(\d+)\.(\d+)\.(\d+)\.(\d+)""".r;
	    val port1 = pasvSocket.getLocalPort() >> 8;
	    var port2 = pasvSocket.getLocalPort() - (port1 << 8);
	    
	    //println(pasvSocket.getLocalPort());

	    owner.socket.getInetAddress().toString() match {
	        case IP(ip1, ip2, ip3, ip4) => "227 Entered Passive Mode (" + ip1 + ","+ip2+","+ip3+","+ip4+","+port1+","+port2+")";
	        case _ => "451 Can not get local address of server socket" + owner.socket.getInetAddress() + "," + pasvSocket.getLocalPort();
	    }	    
	    
	}
	
	def list(): String = {
	    //Realize list, test passive mode
	    //TODO: hide hidden files
	    if(dataSocket == null)
	    	return "425 Use PORT or PASV first.";
	    
	    owner.socket.getOutputStream().write("150 Here comes the directory listing.\r\n".getBytes() );

	    dataSocket.getOutputStream().write( currentDir.list.getBytes() );
	    resetDataConnection();
	    return "226 Directory send ok.";
	}
	
	def nlst(): String = {
	    if(dataSocket == null)
	    	return "425 Use PORT or PASV first.\r\n";
	    return "";
	}
	
	def retr(fileName: String): String = {
	    val Success = "(150 .*\r\n)".r;
	    
	    if(dataSocket == null)
	        return "425 Use PORT or PASV first.\r\n";

	    currentDir.retr(fileName) match{
	        case Success(result) => {
	            owner.socket.getOutputStream().write(result.getBytes());
	            
	            val file = new File(currentDir.homePath + currentDir.makeFullPath(fileName));
	            val fileStream = new FileInputStream(file);
	            
	            val fileBuffer = new Array[Byte]( file.length().toInt);
	            fileStream.read(fileBuffer);
	            dataSocket.getOutputStream().write(fileBuffer);
	            resetDataConnection();
	            return "226 Transfer complete.\r\n";
	            //fileStream.
	        };
	        case badResult => {
	        	println("Bad result");
	            return badResult;
	        }
	    }
	    
	    ""
	}
	
	def stor(fileName: String): String = {
	    val path = currentDir.homePath + currentDir.makeFullPath(fileName);
	    val file = new File(path);
	    
	    if(!file.createNewFile())
	        return "532 Can not open file\r\n";//TODO: return error code
	    
	    owner.socket.getOutputStream().write("125 Using existing data connection.\r\n".getBytes());
	    owner.socket.getOutputStream().flush();
	    //dataSocket.shutdownOutput();
	    
	    val fileStream = new FileOutputStream(file);	    
	    val fileBuffer = new Array[Byte](1024 * 1024 * 4);	//4mb
	    
	    var isClosed = false;
	    try{
	        while( !isClosed){
	        	        
		        var available = dataSocket.getInputStream().available();
		        var readBytes = dataSocket.getInputStream().read(fileBuffer);
		        var EOS: Int = 0; 
		        if(readBytes <= 0)
		        	EOS = dataSocket.getInputStream().read();
		        if(readBytes > 0 ){
			        fileStream.write(fileBuffer, 0, readBytes);
			        fileStream.flush();
		        }else{
		            if(EOS == -1)
		                isClosed = true;
		            else
		                fileStream.write(EOS);
		            Thread.sleep(1);
		        }
		        
	        }
	    }catch{
	        case sockExcp: SocketException => {
	            dataSocket.close();
	            println("SOck error");
	        };
	    }
	    
	    fileStream.close();
	    resetDataConnection();
	    return "226 FILE: " + fileName + " transferred\r\n";
	}
	
	protected def bindInRanges(): ServerSocket = {
	    var result: ServerSocket = null;
	    
	    for(portRange <- Main.config.portRanges){
		    for (portNum <- portRange.from to portRange.to){
		        try{
		            result = new ServerSocket(portNum);
		        }catch{
		            case _:BindException => result = null;
		        };
		        
		        if(result != null)
		            return result;
            }
        }
	    
	  	null;
	}
	
	//accept datatransfer sockets in passive mode
	protected def tryAcceptPassive(): Unit = {
		if(dataSocket != null)
		    return;
		if(pasvSocket == null)
		    return;
		
		try{
		    dataSocket = pasvSocket.accept();
		    pasvSocket.close();
		    pasvSocket = null;
		}catch{
			case _: SocketTimeoutException => {
		        dataSocket = null;
		    }
		}
		
	}
	
	def resetDataConnection():Unit = {
	  if(dataSocket != null){
	    if (dataSocket.isConnected())
	        dataSocket.close();   
	  }
	    
	  dataSocket = null;
	}
	
	def transferFile(filePath: String): String = {
	    //TODO: realize
	    
	    //1. Check data transfer connection
	    //2. 
	    "Unrealized \r\n"
	}
	/*
	 * LIST log:
	 * -rw-r--r--	1 ftp	ftp 	528	Nov	01	2007	README
	 * -rw-r--r--	1 ftp	ftp		560	Sep 28	2007	index.html
	 * drwxr-xr-x  40 ftp	ftp	   4096 May 23 11:00	pub*/
	
}