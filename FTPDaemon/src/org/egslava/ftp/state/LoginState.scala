package org.egslava.ftp.state
import org.egslava.ftp.config.Config
import org.egslava.ftp.Main
import org.egslava.ftp.ControlConnection
import org.egslava.ftp.config.User

class LoginState(owner: ControlConnection) extends FtpState(owner) {
	val AnonymousLogin = """(?i)ftp|anonymous""".r;
	
	var login: String = null;
	var password: String = null;
	val loginFirst: String = "503 Login with USER first.";
    	
	override def processMessage(message: String): String = {
	    Thread.sleep(1);
	    
	    generalProcessing(message) match{
	        case null => ;
	        case answer => return answer;
	    }
	    
	    message match{
	        case owner.User(login) => user(login) + "\r\n";
	        case owner.Pass(password) => pass(password) + "\r\n";
	        case owner.Noop() => noop() + "\r\n";
	        case "" => "";//anything
	        case unrecognizedCommand => "500 Unrecognized command " + unrecognizedCommand + "\r\n";
	    }
	}
    
	def user(login: String): String = {
        if(owner.currentUser != null || this.login != null)
          return "503 Bad sequence of commands";
        
        if (Main.config.anonymousOnly) {
            
            login match{
                case AnonymousLogin() => {
                  return acceptLogin(null);
                }
                case _ => "530 This server is anonymous only."
            }
            
        }else{
            Main.config.users.find( current => {
	        	current.login.equals(login)   
	        }) match{
	            case Some(user) => {
	              return acceptLogin(user);
	            } 
	            case None => {
	              resetUser();
	              "530 User " + login + "is not exist";
	            }
	        };
        }
    }

    def pass(password: String): String = {
      if(login == null)
        return loginFirst;
      
      if(this.password != null)
        return "230 You are already logged in, no need for password";
      
      if(Main.config.anonymousOnly){
        return acceptPassword(password);
      }

      if(owner.currentUser.password.equals(password)){
        return acceptPassword(password);
      }
      
      //else
      resetUser();
      "530 The password is wrong"
    };
    
    def noop(): String = loginFirst;
    
    //unrecognized command - 331
    def acceptLogin(user: User): String = {
      owner.currentUser = user;
            
      this.login = user match{	//anonymous connection
      	case null => "anonymous";
      	case _ => user.login;
      }
      
      "331 User " + login + " is OK. Password required"  
    }
    
    def acceptPassword(password: String): String = {
      this.password = password;
      owner.currentState = new WaitForCommandsState(owner);
      "230 Logged in";
    }
    def resetUser(): Unit = {
      owner.currentUser = null;
      this.login = null;
      this.password = null;
    }
    
}