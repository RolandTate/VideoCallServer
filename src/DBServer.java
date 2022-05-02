import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DBServer {
    private final static int port = 8923;
    public static ConcurrentHashMap<String,Socket> clientList =new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, ClientConnection> clientConnections = new ConcurrentHashMap<>();
    //public static HashMap<String, Socket> clientList = new HashMap<>();
    public static String clientUid;
    private static BufferedReader bufferedReader;
    private static InputStream inputStream;

    //private static ObjectInputStream objectInputStream;
    //private static ObjectOutputStream objectOutputStream;
    //接听电话的时候会有io流冲突，尝试添加一个threadList来进行io流复用

    public static DBHelper dbHelper;

    static Lock ListLock  =   new ReentrantLock(   )   ;
    static Lock DBLock  =   new ReentrantLock(   )   ;
    static Lock IOStreamLock = new ReentrantLock();

    public static void main(String args[]){
        dbHelper = new DBHelper();
        try{
            ServerSocket server = new ServerSocket(port);
            ExecutorService executorService = Executors.newCachedThreadPool();
            while(true){
                Socket clientSocket = server.accept();

                ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                String msg = (String) objectInputStream.readObject();




                System.out.println("msg: "+msg);
                if(msg != null) {
                    if (msg.equals("registerRequest")) {
                        System.out.println("接收到客户端注册请求");
                        RegisterTask registerTask = new RegisterTask(clientSocket);
                        executorService.submit(registerTask);
                    }
                    if(msg.equals("loginRequest")){
                        System.out.println("接收到客户端登录请求");
                        OnlineTask onlineTask = new OnlineTask(clientSocket,objectInputStream,objectOutputStream);
                        executorService.submit(onlineTask);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
