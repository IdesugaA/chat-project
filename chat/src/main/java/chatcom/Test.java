package chat;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.io.IOException;
import java.io.*;
import java.sql.*;
import redis.clients.jedis.*;
public class Test{
	public static void main(String[] args) throws Exception{
//		Class.forName("com.mysql.cj.jdbc.Driver");
//		String url = "jdbc:mysql://127.0.0.1:3306/user";
//		String user = "root";
//		String password = "12345";
//		Connection con = DriverManager.getConnection(url,user,password);
		Jedis jedis = new Jedis("localhost",6379);
		System.out.println(jedis.get("mystr"));
	}
}
