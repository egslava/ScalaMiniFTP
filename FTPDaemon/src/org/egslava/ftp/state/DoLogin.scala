package org.egslava.ftp.state
import org.egslava.ftp.config.Config
import org.egslava.ftp.Main
import org.egslava.ftp.ControlConnection

class DoLogin(owner: ControlConnection) extends FtpState(owner) {
	val AnonymousLogin = """(?i)ftp|anonymous""".r;
	
	var login: String = null;
	var password: String = null;
	val loginFirst: String = "503 Login with USER first.\r\n";
    
    override def user(login: String): String = {
        
        if (Main.config.anonymousOnly) {
            
            login match{
                case AnonymousLogin() => {
                	"331 Please specify the password.\r\n"
                }
                case _ => "530 This server is anonymous only.\r\n"
            }
            
        }else{
            Main.config.users.find( current => {
	        	current.login.equals(login)   
	        }) match{
	            case Some(user) => "331 User " + user + "OK. Password required" 
	            case None => "530 User " + login + "is not exist";
	        };
        }
    }

    override def pass(password: String): String = loginFirst;
    override def noop(): String = loginFirst;
    
}