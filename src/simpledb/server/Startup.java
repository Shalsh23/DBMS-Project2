package simpledb.server;

import simpledb.remote.*;
import java.util.Scanner;
import java.rmi.registry.*;

public class Startup {
	//Code Added by Ashwin
	public static int reference_counter_initialized=0; 
	public static int gclock_iterator=0;
	public static Scanner sc= new Scanner(System.in);
   public static void main(String args[]) throws Exception {
	   
	      //Code added by Ashwin
	      //Ask for the two variables
	      System.out.println("Enter the value for the reference counter to be considered for the replacement policy");
	      int input=sc.nextInt();
	      reference_counter_initialized=input;
	      gclock_iterator=input;
      // configure and initialize the database
      SimpleDB.init(args[0]);
      
      // create a registry specific for the server on the default port
      Registry reg = LocateRegistry.createRegistry(1099);
      
      // and post the server entry in it
      RemoteDriver d = new RemoteDriverImpl();
      reg.rebind("simpledb", d);
      
      System.out.println("database server ready");
   }
}
