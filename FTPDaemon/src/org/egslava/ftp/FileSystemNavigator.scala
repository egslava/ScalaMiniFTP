package org.egslava.ftp
import java.io.File

class FileSystemNavigator (owner: ControlConnection) {
    var currentDirectory: String = """/""";
    val prefixFile: String = """-rw-r--r--	1	ftp	ftp	528	Nov	01	2007	""";
    val prefixDir: String = """drwxr-xr-x 	40	ftp	ftp	4096	May	23	11:00	""";
    
    
	val GoUp = """^\Q../\E(.*)$""".r;
	val GoThis = """^\Q./\E(.*)$""".r;
	val GoAbsolute = """^\/.*$""".r;
	    
	def pwd(): String = {
	    return "257 " + currentDirectory +"\r\n";
	}

	def cwd(path: String): String = {
	    //todo: fix this. Add relative and absolute path.
	    //todo: make recursive ../
	    
	    path match {
	        case GoUp(path) => return cwd(path);
	        case GoThis(path) => return cwd(path);
	        case GoAbsolute() => {
	            currentDirectory = path;
	            return "250 Directory succesfully changed.\r\n";
	        } 
	        case "" => {
	            return "250 Directory succesfully changed.\r\n";
	        }
	        case _ => ;	//continue
	    }
	    
	    //if(path)
	    
	    if(path(path.length() - 1) == '/'){
	    	currentDirectory += path;	    
	    }else{
	        currentDirectory += path + '/';
	    }
	    return "250 Directory succesfully changed.\r\n";
	    //todo test file existance
	    
	}
	
	def list(): String = {
	    var result: StringBuilder = new StringBuilder("");
	    
		var path: File = new File(homePath + currentDirectory);
		
		println(currentDirectory);
		println(homePath + currentDirectory);
		if(path.exists())
	    for( file <- path.listFiles() ){
	        if (file.isDirectory()){
	            result.append(prefixDir);
	        }else {
	            result.append(prefixFile);
	        }
	        
	        result.append(file.getName());
	        result.append("\r\n");
	    }
	    return result.toString();
	}
	
	def getFullPath(filePath: String): String = filePath(0) match{
	    case _ => "";
	};
	
	def homePath: String = owner.currentUser match{
	    case null => return Main.config.anonHome;
	    case _ => return owner.currentUser.home;
	}
	    
    def cdup():String = {
	    val UpPath = """(.*\/)[^\/]+\/""".r;
	    currentDirectory match{
	        case UpPath(path) => currentDirectory = path;
	        case _ => currentDirectory = "/";
	    }
	    
	    "250 Directory successfully changed\r\n";
	}
}