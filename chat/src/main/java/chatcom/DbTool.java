package chat;
import java.sql.*;

public class DbTool{
	private static Connection con = null;
	private static Statement sta = null;
	public static boolean connect_to_db(){
		if(con != null){
			return false;
		}
		try{
			Class.forName("com.mysql.cj.jdbc.Driver");
		}catch(ClassNotFoundException e){
			e.printStackTrace();
			return false;
		}
		String url = "jdbc:mysql://127.0.0.1:3306/user";
		String user = "root";
		String password = "12345";
		try{
			con = DriverManager.getConnection(url,user,password);
			sta = con.createStatement();
		}catch(SQLException e){
			e.printStackTrace();
			System.exit(1);
		}
		
		if(con != null){
			return true;
		}
		else return false;
	}
	public static boolean insert(String id , String ip , int port){
		if(sta != null){
			String sql = "insert into user_info values("+"\""+id+"\""+","+"\""+ip+"\""+","+port+");";
			try{
				sta.executeUpdate(sql);
			}catch(SQLException e){
				e.printStackTrace();
				return false;
			}
			return true;
		}
		return false;
	}
	public static ResultSet queryData(){
		if(sta != null){
			String sql = "select * from user_info";
			ResultSet res;
			try{
				res = sta.executeQuery(sql);
			}catch(SQLException e){
				e.printStackTrace();
				return null;
			}
			return res;
		}
		return null;
	}
	
}
