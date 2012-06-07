package org.egslava.ftp.state
import org.egslava.ftp.ControlConnection
import java.net.Socket
import java.net.ServerSocket
import org.egslava.ftp.Main
import org.egslava.ftp.config.PortRange
import java.net.BindException
import java.net.SocketTimeoutException

class WaitForCommandsState(owner: ControlConnection) extends FtpState(owner){
  
	var dataSocket: Socket = null;
	var pasvSocket: ServerSocket = null;
	
	val answer = 
"""
-rw-r--r--	1	ftp	ftp	528	Nov	01	2007	README\r\n
-rw-r--r--	1	ftp	ftp	560	Sep	28	2007	index.html\r\n
drwxr-xr-x 	40	ftp	ftp	4096	May	23	11:00	pub\r\n"""
	    
	var currentDirectory = "/";
	
	override def processMessage(message: String): String = {
	    tryAcceptPassive();
	    
	    Thread.sleep(1);
	    generalProcessing(message) match{
	        case null => ;
	        case answer => return answer;
	    }
	    
	    message match{
			case owner.Noop() => "200 NOOP ok\r\n";
			case owner.User(_) => "530 Can't change from guest user\r\n";
			case owner.Pass(_) => "230 Already logged in\r\n";
			case owner.Pasv() => pasv() + "\r\n";
			case owner.List() => list() + "\r\n";
			case owner.Nlst() => nlst() + "\r\n";
			case owner.Type(letter) => "200 Switching to binary mode\r\n";
			case "PWD" => "257 \"" + currentDirectory + "\"\r\n";
			case "SITE HELP" => "200-\r\n200\r\n";
			case owner.Cwd(path) => "250 Directory successfuly changed\r\n";
			case "" => ""
			case unrecognizedCommand => "500 Unrecognized command " + unrecognizedCommand + "\r\n";
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
	    
	    println(pasvSocket.getLocalPort());

	    owner.socket.getInetAddress().toString() match {
	        case IP(ip1, ip2, ip3, ip4) => "227 Entered Passive Mode (" + ip1 + ","+ip2+","+ip3+","+ip4+","+port1+","+port2+")";
	        case _ => "451 Can not get local address of server socket" + owner.socket.getInetAddress() + "," + pasvSocket.getLocalPort();
	    }	    
	    
	}
	
	def list(): String = {
	    //Реализовать команду list, проверить passive mode
	    
	    owner.socket.getOutputStream().write("150 Here comes the directory listing.\r\n".getBytes() );
	    dataSocket.getOutputStream().write(answer.getBytes() );
	    resetDataConnection();
	    "226 Directory send ok."
	}
	
	def nlst(): String = {
	    ""
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
	    dataSocket.close();
	    dataSocket = null;
	}
	/*
	 * LIST log:
	 * -rw-r--r--	1 ftp	ftp 	528	Nov	01	2007	README
	 * -rw-r--r--	1 ftp	ftp		560	Sep 28	2007	index.html
	 * drwxr-xr-x  40 ftp	ftp	   4096 May 23 11:00	pub*/
	
}