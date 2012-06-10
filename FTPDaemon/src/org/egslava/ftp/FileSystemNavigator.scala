package org.egslava.ftp
import java.io.File
import java.net.Socket

class FileSystemNavigator (owner: ControlConnection) {
    var currentDirectory: String = """/""";
    val prefixFile: String = """-rw-r--r--	1	ftp	ftp	528	Nov	01	2007	""";
    val prefixDir: String = """drwxr-xr-x 	40	ftp	ftp	4096	May	23	11:00	""";
    	    
	def pwd(): String = {
	    return "257 " + currentDirectory +"\r\n";
	}

	def cwd(path: String): String = {
	    var mutablePath = "";
	    if(path(path.length() - 1) != '/'){
	    	mutablePath = path + '/';	    
	    }else{
	        mutablePath = path;
	    }
	    
	    val newPath = makeFullPath(mutablePath);
	    
	    if(new File(homePath + newPath).exists() ){
	        currentDirectory = newPath;
	    	return "250 Directory succesfully changed.\r\n";    
	    }else{
	        return "550 Faled to change directory.\r\n";
	    }
	}
	
	def cdup():String = {
	    currentDirectory = getUpDirectory(currentDirectory);
	    "250 Directory successfully changed\r\n";
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
	
	def retr(fileName: String): String = {
	    
	    val fullFileName = homePath + makeFullPath(fileName);
	    val file = new File(fullFileName);
	    
	    if( !file.exists() || file.isDirectory())
	        return "550 Failed to open file\r\n";
	    
	    if(file.length > Int.MaxValue)
	        return "550 File is too large\r\n";
	    //125 Using existing data connection.
	    return "150 Opening BINARY mode data connection for " + fileName + " (" + file.length() + " bytes).\r\n";
	}
	
	val GoUp = """^\Q../\E(.*)$""".r;
	val GoThis = """^\Q./\E(.*)$""".r;
	val AbsolutePath = """^\/.*$""".r;
	
	def makeFullPath(filePath: String): String = {
	    	
	    var currentFilePath: String = filePath;
	    var currentAbsolute: String = currentDirectory;
	    
	    def applyPath(path: String): String = path match{
	        case AbsolutePath() => return path;
	        case GoThis(path) => return applyPath(path);
	        case GoUp(path) => {
	            currentAbsolute = getUpDirectory(currentAbsolute);
	            return applyPath(path);
	        }
	        case cleanRelativePath => return currentAbsolute + cleanRelativePath;
	    }
	    
	    return applyPath(currentFilePath);	    
	};
	
	def homePath: String = owner.currentUser match{
	    case null => return Main.config.anonHome;
	    case _ => return owner.currentUser.home;
	};
	        
    protected val UpPath = """(.*\/)[^\/]+\/""".r;
    protected def getUpDirectory(dir: String):String = dir match{
        case UpPath(path) => path;
        case _ => "/"
    }
}