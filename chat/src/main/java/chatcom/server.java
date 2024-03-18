package chat;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;
import java.util.HashMap;
import java.util.ArrayList;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.io.IOException;
import java.io.File;
import java.sql.*;

/**
1.客户端与服务器进行握手，客户端一开始发送给服务器的信息包括本方IP、PORT
2.服务器拿到客户端信息后，查看是否已经存入到clientIpPortMap，如果否，则存入，
同时上传到数据库，并为用户分配IP
3.服务器将其存储的客户名单发送给客户端，以使得客户端知道自己能给谁发送消息
**/
public class server{
	class sendingTask implements Callable<Integer>{
		public pair<String[],String> msgpair;
		public HashMap<String,Integer> clientIpPortMap;
		public HashMap<String,String> clientIdIPMap;
		public DatagramSocket serverSocketSender;
		public sendingTask(pair<String[],String> mp,HashMap<String,Integer> cim,HashMap<String,String> cii) throws SocketException{
			msgpair = mp;
			clientIpPortMap = cim;
			clientIdIPMap = cii;
			serverSocketSender = new DatagramSocket();
		}
		@Override
		public Integer call()throws IOException,UnknownHostException{
			try{
			String senderid = msgpair.first[0];
			String receiverid = msgpair.first[1];
			
			String receiverIp_s = clientIdIPMap.get(receiverid);
			int receiverPort = clientIpPortMap.get(receiverIp_s);
			InetAddress receiverIp = InetAddress.getByName(receiverIp_s);
			
			String msgstr = msgpair.second + senderid;
			byte[] sendBuffer = msgstr.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendBuffer,sendBuffer.length,receiverIp,receiverPort);
			serverSocketSender.send(sendPacket);
			}catch(Exception e){
				System.out.println(e.getMessage());
			}
			return 1;
		}
	}
	private ThreadPoolExecutor threadPoolReceiving;
	private ThreadPoolExecutor threadPoolSending;
	private ThreadPoolExecutor threadPoolExtra;
	private ArrayList<pair<String[],String>> msgCtner;
	private int msgCtnerLength;
	private HashMap<String,Integer> clientIpPortMap;
	private HashMap<String,String> clientIdIPMap;
	private final DatagramSocket serverSocketReceiver;
	private DatagramPacket receiverPacket;
	private byte[] receiverBuffer;
	private ReentrantLock lock_msgque;
	private ReentrantLock lock_quelength; 
	private int client_number;
	private StringBuffer members;
	public void PacketDeal(final byte[] data,final int length){
		threadPoolReceiving.execute(new Runnable(){
			@Override
			public void run(){
				System.out.println("DEALING");
				String data_tostr = new String(data,0,length);
				String infostr = data_tostr.substring(0,6);
				boolean is_con = infostr.equals("HELLO#");
				if(is_con == false){  //判断是握手包还是信息包
					System.out.println("msg come!");
					String msg = data_tostr.substring(6,data_tostr.length()); // ???something wrong
					System.out.println("msg:"+msg);
					String senderid = infostr.substring(0,3);
					String receiverid = infostr.substring(3,6);
					lock_msgque.lock();
					try{
						msgCtner.add(new pair<String[],String>(new String[]{senderid,receiverid},msg));
					}finally{
						lock_msgque.unlock();
					}
					lock_quelength.lock();
					try{
						msgCtnerLength = msgCtner.size();
					}finally{
						lock_quelength.unlock();
					}
				}
				else{
					System.out.println("get con!");
					System.out.println("data_str:"+data_tostr);
					String[] client_info = data_tostr.split("#");
					String ip_client = client_info[1];
					System.out.println("ip from client:"+ip_client);
					int port_client = Integer.parseInt(client_info[2]);
					boolean is_contains;
					if(clientIpPortMap.containsKey(ip_client)){
						int port_db = clientIpPortMap.get(ip_client);
						is_contains = clientIdIPMap.containsValue(ip_client) && (port_db == port_client);
					}
					else{
						is_contains = clientIdIPMap.containsValue(ip_client);
					}
					short flag = 0;
					try{
						DatagramSocket info_sender = new DatagramSocket();
						if(is_contains == false){
							flag = 1;
							client_number++;
							String op_s;
							if(client_number >= 10){
								op_s = new String("0"+client_number);
							}
							else{
								op_s = new String("00"+client_number);
							}
							members.append(op_s+"#");
							System.out.println("id:"+op_s);
							clientIpPortMap.put(ip_client,port_client);
							clientIdIPMap.put(op_s,ip_client);
							boolean insert_b = DbTool.insert(op_s,ip_client,port_client);
							if(insert_b) System.out.println("Insert success");
							else System.out.println("Insert fail");
						}
						
						/**
							当flag为1时，代表服务器数据库中不存在对应IP和端口的数据，说明该IP和端口
							尚未进行注册，于是就会发送新的ID给客户端。客户端接收到包后检查flag位，
							如果发现flag位为1，那说明需要更新自己的ID（或者由于是第一次登录，所以需要
							首次得到ID）
							
							同时要注意的是，程序尚未连接数据库，也暂时还没有利用本地文件进行数据存储，
							所以当服务器程序关闭，再次启动时，所有IP和端口记录都会被清除
						*/
						byte[] op = (flag + "#" + members.toString()).getBytes();
						DatagramPacket dp = new DatagramPacket(op,op.length,InetAddress.getByName(
						ip_client),port_client);
						info_sender.send(dp);
						System.out.println("server send over");
					}
					catch(Exception e){
						e.printStackTrace();
						System.exit(1);
					}
				}

			}	
		});
	}
	
	public void sending()throws UnknownHostException,IOException{
		while(true){
			lock_quelength.lock();
			try{
			if(msgCtnerLength != 0){
				System.out.println("SENDING!");
				lock_msgque.lock();
				try{
					pair<String[],String> pr = msgCtner.get(msgCtnerLength-1);
					msgCtner.remove(msgCtnerLength-1);
					sendingTask st = new sendingTask(pr,clientIpPortMap,clientIdIPMap);
					threadPoolSending.submit(st);
					msgCtnerLength = msgCtner.size();
				}catch(Exception e){
					System.out.println(e.getMessage());
				}
				finally{
					lock_msgque.unlock();
				}
			}
			}finally{
				lock_quelength.unlock();
			}
		}
	}

	public void receiving()throws IOException{
		while(true){
			System.out.println("I AM LISTENING");
			serverSocketReceiver.receive(receiverPacket);
			System.out.println("RECEIVE");
			PacketDeal(receiverPacket.getData(),receiverPacket.getLength());
		}
	}

	public void start()throws UnknownHostException , IOException{
		threadPoolExtra.submit(new Callable<Integer>(){
			@Override
			public Integer call() throws IOException{
				receiving();
				return 1;
			}	
		});
		threadPoolExtra.submit(new Callable<Integer>(){
			@Override
			public Integer call() throws UnknownHostException , IOException{
				sending();
				return 1;
			}
		});
	}

	public server()throws SocketException{
		threadPoolReceiving = new ThreadPoolExecutor(
			4,
			Integer.MAX_VALUE,
			60,
			TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>(5)
				);
		threadPoolSending = new ThreadPoolExecutor(
			4,
			Integer.MAX_VALUE,
			60,
			TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>(5)
				);
		threadPoolExtra = new ThreadPoolExecutor(
			2,
			Integer.MAX_VALUE,
			60,
			TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>(2)
				);
		msgCtner = new ArrayList<>();
		members = new StringBuffer();
		clientIpPortMap = new HashMap<>();
		clientIdIPMap = new HashMap<>();
		boolean connect_success = DbTool.connect_to_db();
		ResultSet query = DbTool.queryData();
		if(query == null || connect_success != true){
			System.out.println("Initiation fail");
		}
		
		try{
			query.next(); //跳过第一个数据
			while(query.next()){ //获取数据库中用户的ID、IP、PORT数据
				client_number++;
				String id = query.getString("id");
				String ip = query.getString("ip");
				int port = query.getInt("port");
				System.out.println("IP:"+ip+" port:"+port);
				members.append(id+"#");
				clientIdIPMap.put(id,ip);
				clientIpPortMap.put(ip,port);
			}
		}catch(SQLException e){
			e.printStackTrace();
			System.exit(1);
		}
		
		client_number = 0;
		serverSocketReceiver = new DatagramSocket(23335);
		receiverBuffer = new byte[5000];
		receiverPacket = new DatagramPacket(receiverBuffer,receiverBuffer.length);
		lock_msgque = new ReentrantLock();
		lock_quelength = new ReentrantLock();
	}

	public static void main(String[] args)throws UnknownHostException,SocketException,IOException{
		server sr = new server();
		sr.start();
	}
	

}
