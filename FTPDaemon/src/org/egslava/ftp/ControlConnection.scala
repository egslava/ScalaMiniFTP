package org.egslava.ftp
import java.net.Socket
import java.net.ServerSocket
import java.io.OutputStreamWriter
import java.io.InputStreamReader
import java.io.BufferedWriter
import java.io.BufferedReader
import org.egslava.ftp.state.FtpState
import org.egslava.ftp.state.DoLogin

class ControlConnection (_socket: Socket) extends Thread {
    val socket = _socket;
	val transferSocket: AnyRef = null;
	
	var currentState: FtpState = new DoLogin(this);
	var currentUser: org.egslava.ftp.config.User = null;	// null for anonymous
	
	val User = """(?i)USER (\S+)""".r;
	val Pass = """(?i)PASS (\S+)""".r;
	val Noop = """(?i)NOOP""".r;
	
	override def run: Unit = {
	    val outStream = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
	    val inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	        
	    while(socket.isConnected()){
	        while(inStream.ready()) {
	            var message = inStream.readLine();
	            
	            message match{
	                case User(login) => outStream.write( currentState.user(login) )
		            case Pass(password) => outStream.write( currentState.pass(password) )
		            case Noop() => outStream.write( currentState.noop() )
		            case _ =>{
		                println(message);
		            }
	            }
	            
	            outStream.flush();
	        }
	        
	    	Thread.sleep(1);
	    }
	}
}