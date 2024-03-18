package chatcom;
import redis.clients.jedis.Jedis;

public class JdTool {
    static Jedis con;
    public static boolean ini() {
        con = new Jedis("localhost",6379);
        if(con == null) {
            return false;
        }
        else {
            return true;
        }
    }

    public static void insert(String id , String ip , int port){
        con.hset("id_ip",id,ip);
        con.hset("ip_port",ip,String.valueOf(port));
    }

    public static String queryIp(String id){
        String ip = con.hget("id_ip",id);
        return ip;
    }

    public static int queryPort(String ip){
        int port = Integer.parseInt(con.hget("ip_port",ip));
        return port;
    }

}
