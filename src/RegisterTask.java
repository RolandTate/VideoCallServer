import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import javax.sql.rowset.serial.SerialBlob;
import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.concurrent.TimeUnit;

public class RegisterTask implements Runnable {
    private  Socket socket;
    ObjectInputStream objectInputStream;
    ObjectOutputStream objectOutputStream;
    //private  BufferedInputStream bufferedInputStream;
    //private  BufferedReader bufferedReader;
    //private  OutputStream clientOutputStream;
    private boolean registerThreadAlive;


    RegisterTask(Socket socket,ObjectInputStream objectInputStream,ObjectOutputStream objectOutputStream){
        this.socket = socket;
        this.objectInputStream = objectInputStream;
        this.objectOutputStream = objectOutputStream;
        registerThreadAlive = true;
    }

    @Override
    public void run() {
        System.out.println("注册线程启动");
        while(registerThreadAlive) {
            try {
                System.out.println("注册线程等待任务");
                String task = (String)objectInputStream.readObject();
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

                    String userID = (String)objectInputStream.readObject();
                    System.out.println("注册模块读取客户端发送的数据成功！userID: " + userID);

                    String userPassword = (String)objectInputStream.readObject();
                    System.out.println("注册模块读取客户端发送的数据成功！userPassword: " + userPassword);

                    String userName = (String)objectInputStream.readObject();
                    System.out.println("注册模块读取客户端发送的数据成功！userName: " + userName);

                    if (!userID.equals("") && !userPassword.equals("")) {
                        if (DBInsert(userID, userPassword, userName)) {
                            System.out.println("用户：" + userID + "注册成功");
                            objectOutputStream.writeObject("success");
                            EndThread("注册成功，结束线程");
                        } else {
                            System.out.println("已有用户！注册失败！");
                            objectOutputStream.writeObject("fail");
                        }
                    }else{
                        objectOutputStream.writeObject("empty");
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
            if(objectOutputStream != null)
                objectOutputStream.close();
            if (objectInputStream != null)
                objectInputStream.close();
            if (socket != null)
                socket.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
