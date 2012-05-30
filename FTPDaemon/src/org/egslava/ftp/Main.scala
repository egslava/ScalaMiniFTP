package org.egslava.ftp
import org.ho.yaml.Yaml
import java.io.File
import java.net.ServerSocket
import java.net.InetSocketAddress
import java.net.Socket
import org.egslava.ftp.config.Config

object Main {
  
	var serverSocket = new ServerSocket();
	var config = Config.load("config.yaml");
  
	var clients = Array[Socket]();
	
  
	def main(args: Array[String]): Unit = {
     
	    try{
	        if(config.debugOutput){
	            println("Configuration file has loaded");
	            println("\tHost: " + config.hostName);
	            println("\tPort: " + config.portNumber);
	            println("\tAnonymous users only mode: " + config.anonymousOnly);  
	        }
	    
	        serverSocket.bind(new InetSocketAddress(config.hostName, config.portNumber));
		
		    
		    while(true){
		        var incomingSocket = serverSocket.accept();
		        
		        if(config.debugOutput)
		            println("New connection");
		    	new ControlConnection(incomingSocket).start();
		    }
	    }catch{
	        case e: Exception => {
	            println("Can not bind socket to port!");
			    println("Exception with className '" + e.getClass().getName() +"'");
			    e.printStackTrace();
			    println(e.getLocalizedMessage());
			};
		};
    };
};