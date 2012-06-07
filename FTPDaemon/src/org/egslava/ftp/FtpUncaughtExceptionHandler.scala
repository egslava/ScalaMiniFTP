package org.egslava.ftp
import java.lang.UncaughtExceptionHandler

object FtpUncaughtExceptionHandler extends UncaughtExceptionHandler{
	def uncaughtException(thread: Thread, throwable: Throwable) {
		println("Unhandled exception in thread" + thread.getName());
		println("-Classname of exception: " + throwable.getClass().getName());
		println("-Localized message: " + throwable.getLocalizedMessage());
		println("-Stack trace:");
		throwable.printStackTrace();
	}
}