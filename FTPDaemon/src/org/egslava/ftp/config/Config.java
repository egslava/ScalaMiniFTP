package org.egslava.ftp.config;

import java.io.File;
import java.io.FileNotFoundException;

import org.ho.yaml.Yaml;

public class Config {
	public String hostName = "localhost";
	public int portNumber = 21;
	public boolean debugOutput = false;
	public boolean anonymousOnly = false;
	public User users[] = null;
	
	static public Config load(String fileName) throws FileNotFoundException{
		return Yaml.loadType(new File(fileName), Config.class);
	}
}
