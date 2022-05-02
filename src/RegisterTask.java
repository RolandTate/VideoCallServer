import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import javax.sql.rowset.serial.SerialBlob;
import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.concurrent.TimeUnit;

public class RegisterTask implements Runnable {
    private  Socket socket;
    private  BufferedInputStream bufferedInputStream;
    private  BufferedReader bufferedReader;
    private  OutputStream clientOutputStream;
    private boolean registerThreadAlive;


    RegisterTask(Socket socket){
        this.socket = socket;
        registerThreadAlive = true;
        try{
            bufferedInputStream = new BufferedInputStream(socket.getInputStream());
            //bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(bufferedInputStream));
            clientOutputStream = socket.getOutputStream();
        }catch (Exception e){
            registerThreadAlive = false;
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("注册线程启动");
        while(registerThreadAlive) {
            try {
                System.out.println("注册线程等待任务");
                String task = bufferedReader.readLine();
                System.out.println("注册线程开始任务: " + task);

                switch (task) {
                    case "register":
                        Register();
                        break;

                }
            } catch (Exception e) {
                EndThread("用户注册连接出错");
                e.printStackTrace();

            }
        }
    }

    void Register() throws Exception{
        boolean registerTaskAlive = true;
        while (registerTaskAlive) {
            if(DBServer.DBLock.tryLock(800,TimeUnit.MICROSECONDS)) {
                if (DBServer.ListLock.tryLock(800, TimeUnit.MICROSECONDS)) {
                    System.out.println("收到用户注册申请");

                    String userID = bufferedReader.readLine();
                    System.out.println("注册模块读取客户端发送的数据成功！userID: " + userID);

                    String userPassword = bufferedReader.readLine();
                    System.out.println("注册模块读取客户端发送的数据成功！userPassword: " + userPassword);

                    String userName = bufferedReader.readLine();
                    System.out.println("注册模块读取客户端发送的数据成功！userName: " + userName);

                    if (!userID.equals("") && !userPassword.equals("")) {
                        if (DBInsert(userID, userPassword, userName)) {
                            System.out.println("用户：" + userID + "注册成功");
                            clientOutputStream.write("success\n".getBytes());
                            EndThread("注册成功，结束线程");
                        } else {
                            System.out.println("已有用户！注册失败！");
                            clientOutputStream.write("fail\n".getBytes());
                        }
                    }else{
                        clientOutputStream.write("empty\n".getBytes());
                    }

                    DBServer.DBLock.unlock();
                    DBServer.ListLock.unlock();
                    registerTaskAlive = false;
                }else
                    DBServer.DBLock.unlock();
            }
        }

    }

    boolean DBInsert(String ID,String pass,String name){
        if(DBServer.dbHelper.checkInTable(ID,pass)){
            return false;
        }else{
            DBServer.dbHelper.insertTable(ID,pass,name,"offline");
            return true;
        }
    }

    void EndThread(String msg){
        System.out.println(msg);
        try {
            registerThreadAlive = false;
            //释放资源
            if(bufferedInputStream != null)
                bufferedInputStream.close();
            if (bufferedReader != null)
                bufferedReader.close();
            if (clientOutputStream != null)
                clientOutputStream.close();
            if (socket != null)
                socket.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
