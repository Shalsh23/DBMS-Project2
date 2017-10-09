package studentClient.simpledb;

import java.sql.*;
import java.util.Scanner;

import simpledb.remote.SimpleDriver;

public class Test_1 {
	static Scanner sc = new Scanner(System.in);
	
    public static void main(String[] args) {
		Connection conn = null;
		try {
			Driver d = new SimpleDriver();
			conn = d.connect("jdbc:simpledb://localhost", null);
			Statement stmt = conn.createStatement();

			String s = "create table TEST1(test int)";
			stmt.executeUpdate(s);
			System.out.println("Table TEST1 created.");
			int i=0;
			for(i=0;i<1;i++)
			{
				System.out.println(i);
				s = "insert into TEST1 (test) values("+i+")";
				sc.nextInt();
				stmt.executeUpdate(s);
			}
			
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (conn != null)
					conn.close();
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}