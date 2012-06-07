package org.egslava.ftp
import java.net.Socket
import java.net.ServerSocket
import java.io.OutputStreamWriter
import java.io.InputStreamReader
import java.io.BufferedWriter
import java.io.BufferedReader
import state.FtpState
import state.LoginState

class ControlConnection (_socket: Socket) extends Thread {
    val socket = _socket;
	
	var currentState: FtpState = new LoginState(this);
	var currentUser: org.egslava.ftp.config.User = null;	// null for anonymous
	
	val Feat = """(?i)FEAT""".r;
	val User = """(?i)USER (\S+)""".r;
	val Pass = """(?i)PASS (\S+)""".r;
	val Noop = """(?i)NOOP""".r;
	val Port = """(?i)PORT""".r;
	val Pasv = """(?i)PASV""".r;
	val List = """(?i)LIST""".r;
	val Nlst = """(?i)NLST""".r;
	val Type = """(?i)TYPE (\S+)""".r;
	val Cwd = """(?i)CWD (\S+)""".r;
	
	override def run: Unit = {
	    val outStream = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
	    val inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	        
	    outStream.write("220 Welcome to ScalaMiniFTP Server v1.0\r\n");
	    outStream.flush();
	    
	    while(socket.isConnected()){
	        while(inStream.ready()) {
	            var message = inStream.readLine();
	            println(message);
	            outStream.write(currentState.processMessage(message));	            
	            outStream.flush();
	        }
	        
	    	Thread.sleep(1);
	    }
	}
}