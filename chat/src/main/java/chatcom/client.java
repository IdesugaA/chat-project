package chat;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;
import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
/**
1.初次登录时，得到一个随机空闲端口，并把端口和IP一起存储在本地，与服务器握手时把这个端口发送给服务器
2.再次登录时，检查存储的端口和IP，检查端口是否可用以及IP是否和现在的相同

*/

public class client{
	private DatagramSocket senderSocket;
	private DatagramSocket receiverSocket;	
	private DatagramPacket receiverPacket;
	private String ownID;
	private InetAddress serverip;
	private int serverport;
	private int ownport;
	private String ownip;
	private byte[] receiverBuffer;
	private ThreadPoolExecutor threadPoolClient;
	private File config;
	private String[] all_id;

	public void PacketDeal(byte[] data,int length){
		String data_tostr = new String(data,0,length);
		int length_str = data_tostr.length();
		String sender_id = data_tostr.substring(length_str-3,length_str);
		String msg = data_tostr.substring(0,length_str-3);
		System.out.println(sender_id+": "+msg);

	}

	public void connectToServer()throws IOException{
		/**
			启动客户端后第一件事是连接服务器，每次启动都需要连接。
			作用是获得最新的客户列表，并发送服务器，更新服务器中客户端对应的IP和端口
		*/
		System.out.println("now connecting...");
		byte[] info = ("HELLO"+"#"+ownip+"#"+ownport).getBytes();
		DatagramPacket dp = new DatagramPacket(info,info.length,serverip,serverport);
		byte[] buffer = new byte[1024];
		DatagramPacket dprc = new DatagramPacket(buffer,buffer.length);
		senderSocket.send(dp);
		receiverSocket.receive(dprc);
		all_id = new String(dprc.getData()).split("#");	//得到最新的客户列表
		try{
			RandomAccessFile raf = new RandomAccessFile(config,"rw");
			if(all_id[0].equals("1")){
				/**
					标志位为1，说明要么是首次登录，要么是端口或IP发生了变化，传来了新的ID，
					于是就要重写配置文件。同时all_id数组最后一个是本id
					如果说IP和端口都没有发生变化，那说明不是首次登录，all_id最后一个不是本ID
				*/
				String newid = all_id[all_id.length-2]; // 存放id的下标为1 , length-2
				System.out.println("newid:"+newid);
				ownID = newid;
				raf.readLine();
				raf.readLine();
				raf.writeBytes("id="+newid);
				
			}
			else{ //没传来新的
				
				raf.readLine();
				raf.readLine();
				ownID = raf.readLine().substring(3);
				
			}
			System.out.println("ownID:"+ownID);
			raf.close();
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("局域网内通信对象：");
		for(int i = 1 ; i < all_id.length - 1 ; i++){
			System.out.println(all_id[i]);
		}
	}

	public void initialize()throws SocketException,UnknownHostException,IOException{
		senderSocket = new DatagramSocket();
		receiverBuffer = new byte[5000];
		receiverPacket = new DatagramPacket(receiverBuffer,receiverBuffer.length);
		BufferedWriter bw;
		BufferedReader br;
		try{
			config = new File("config.txt"); //从配置文件里读取IP和端口
			if(config.exists()){
				br = new BufferedReader(
					new InputStreamReader(new FileInputStream(config))
				);
				ownip = br.readLine().substring(9);
				ownport = Integer.parseInt(br.readLine().substring(5));
				receiverSocket = new DatagramSocket(ownport);
				br.close();
			}
			else{
				receiverSocket = new DatagramSocket();
				ownport = receiverSocket.getLocalPort();   //自己的端口取决于接收Socket的端口
				ownip = InetAddress.getLocalHost().getHostAddress();
				config.createNewFile();
				bw = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(config))
				);
				bw.write("serverip="+ownip);
				bw.newLine();
				bw.write("port="+ownport);
				bw.newLine();
				bw.write("id="+"0");
				bw.close();
			}
		}catch(IOException e){
			e.printStackTrace();
			receiverSocket = new DatagramSocket();
			ownport = receiverSocket.getLocalPort();
			ownip = InetAddress.getLocalHost().getHostAddress();
		}
		
		serverip = InetAddress.getByName("192.168.1.5"); //服务器IP、端口都是固定的，提前设置好的
		serverport = 23335;
		connectToServer();
		
		threadPoolClient = new ThreadPoolExecutor(
				4,
				Integer.MAX_VALUE,
				60,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(5)
				);
		threadPoolClient.submit(new Callable<Integer>(){
			@Override
			public Integer call()throws IOException{
				receiving();
				return 1;
			}
			
		});
		
		// info includes port + id
	}

	public void sending(String msg,String receiver)throws IOException{
		byte[] sth = (ownID + receiver + msg).getBytes();
		DatagramPacket dp = new DatagramPacket(sth,sth.length,serverip,serverport);
		senderSocket.send(dp);
	}

	public void receiving() throws IOException{
		while(true){
			receiverSocket.receive(receiverPacket);
			PacketDeal(receiverPacket.getData(),receiverPacket.getLength());
		}
	}


	public static void main(String[] args)throws SocketException , UnknownHostException, IOException{
		client ct1 = new client();
		ct1.initialize();
		Scanner sc = new Scanner(System.in);
		System.out.println("输入接收者ID：");
		String receiver_id = sc.nextLine();
		System.out.println("输入消息：");
		String ip = sc.nextLine();
		while(!ip.equals("end")){
			ct1.sending(ip,receiver_id);
			System.out.println("输入接收者ID：");
			receiver_id = sc.nextLine();
			System.out.println("输入消息：");
			ip = sc.nextLine();
		}
	}
}
