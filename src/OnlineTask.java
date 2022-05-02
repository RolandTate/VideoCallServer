import com.example.videocall.User;
import com.example.videocall.UserList;
import org.omg.Messaging.SYNC_WITH_TRANSPORT;

import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class OnlineTask implements Runnable{
    private  Socket socket;
    //private  BufferedInputStream bufferedInputStream;
    //private  BufferedReader bufferedReader;
    //private  OutputStream clientOutputStream;


    ObjectInputStream objectInputStream;
    ObjectOutputStream objectOutputStream;

    String userID;
    String userPassword;
    boolean onlineTaskAlive;


    OnlineTask(Socket socket,ObjectInputStream objectInputStream,ObjectOutputStream objectOutputStream){
        this.socket = socket;
        this.objectInputStream = objectInputStream;
        this.objectOutputStream = objectOutputStream;
        onlineTaskAlive =true;

    }

    @Override
    public void run() {
        System.out.println("用户线程已启动");
        while (onlineTaskAlive) {
            try {
                    System.out.println("用户线程等待任务");
                    //String task = bufferedReader.readLine();
                    String task = (String)objectInputStream.readObject();
                    System.out.println("用户线程选择任务: " + task);

                    switch (task) {
                        case "Login":
                            Login();
                            break;

                        case "GetUsers":
                            GetUsers();
                            break;

                        case "StartCall":
                            StartCall();
                            break;

                        case "EndCall":
                            EndCall();
                            break;

                        case "EndConnection":
                            EndConnection();
                            EndThread("客户端退出登录");
                            break;

                    }
            } catch (Exception e) {
                e.printStackTrace();
                userOffline(userID);
                EndThread("连接出错！");
            }
        }
    }

    private void Login() throws Exception{
        boolean loginTaskAlive = true;
        while(loginTaskAlive) {
            if(DBServer.DBLock.tryLock(800,TimeUnit.MICROSECONDS)) {
                System.out.println("收到登录验证请求");

                //userID = bufferedReader.readLine();
                userID = (String) objectInputStream.readObject();
                System.out.println("读取客户端发送的数据成功！userID: " + userID);

                userPassword = (String) objectInputStream.readObject();
                //userPassword = bufferedReader.readLine();
                System.out.println("读取客户端发送的数据成功！userPassword: " + userPassword);


                if (DBServer.dbHelper.checkInTable(userID, userPassword)) {
                    //发送验证成功消息
                    //clientOutputStream.write("success\n".getBytes());
                    objectOutputStream.writeObject("success");

                    System.out.println("用户: " + userID + " 的密码验证正确！");
                    DBServer.DBLock.unlock();
                    loginTaskAlive = false;
                    System.out.println("登录验证结束");
                } else {
                    //clientOutputStream.write("fail\n".getBytes());
                    objectOutputStream.writeObject("fail");

                    DBServer.DBLock.unlock();
                    loginTaskAlive = false;
                    System.out.println("密码验证错误！");
                }
            }
        }
    }

    private void GetUsers() throws Exception{
        boolean getUsersTaskAlive = true;
        while (getUsersTaskAlive) {
            if(DBServer.DBLock.tryLock(800,TimeUnit.MICROSECONDS)) {
                if(DBServer.ListLock.tryLock(800,TimeUnit.MICROSECONDS)) {
                    System.out.println("收到用户: " + userID + " 请求成员信息");

                    addToOnlineSocketList(userID, socket);

                    List<User> users = new ArrayList();
                    ResultSet usersInfo = DBServer.dbHelper.getAllUserInformation();
                    while (usersInfo.next()) {
                        User user = new User();
                        user.setID(usersInfo.getString("id"));
                        user.setName(usersInfo.getString("name"));
                        user.setState(usersInfo.getString("state"));
                        System.out.println("id: " + user.getID() + " name: " + user.getName() + " state: " + user.getState());
                        users.add(user);
                    }
                    UserList userMsg = new UserList();
                    userMsg.setUsers(users);

                    //发送成员信息
                    objectOutputStream.writeObject(userMsg);

                    DBServer.ListLock.unlock();
                    DBServer.DBLock.unlock();
                    getUsersTaskAlive = false;
                    System.out.println("发送成员信息给用户: " + userID + " 成功!");
                }else{
                    DBServer.DBLock.unlock();
                }
            }
        }
    }

    private void StartCall() throws Exception{
        boolean startCallTaskAlive = true;
        while (startCallTaskAlive) {
            if(DBServer.DBLock.tryLock(800,TimeUnit.MICROSECONDS)) {
                if (DBServer.ListLock.tryLock(800, TimeUnit.MICROSECONDS)) {
                    System.out.println("收到用户视频通话请求");
                    String targetID = (String) objectInputStream.readObject();
                    System.out.println("用户: " + userID + "邀请用户: " + targetID + "进行通话");
                    Socket targetSocket = null;
                    boolean targetSend = false;

                    for (Map.Entry<String, Socket> entry : DBServer.clientList.entrySet()) {
                        if (entry.getKey().equals(targetID)) {
                            targetSocket = entry.getValue();
                            DBServer.clientList.remove(targetID);
                            targetSend = true;
                            System.out.println("目标用户：" + entry.getKey() + " 在线，可以发送通话邀请");
                        }
                    }
                    if (targetSend) {
                        //OutputStream targetOut = targetSocket.getOutputStream();
                        //targetOut.write("StartVideo\n".getBytes());
                        //targetOut.write("first\n".getBytes());
                        objectOutputStream.writeObject("StartSuccess");
                        objectOutputStream.writeObject("first");

                        setUserBusy(targetID);
                        setUserBusy(userID);
                    } else {
                        System.out.println("目标用户：" + targetID + " 离线，不可以发送通话邀请");
                    }

                    DBServer.ListLock.unlock();
                    DBServer.DBLock.unlock();
                    startCallTaskAlive = false;
                }else
                    DBServer.DBLock.unlock();
            }
        }
    }

    private void EndCall () throws Exception{

        boolean endCallTaskAlive = true;
        while (endCallTaskAlive) {
            if(DBServer.DBLock.tryLock(800,TimeUnit.MICROSECONDS)) {
                if (DBServer.ListLock.tryLock(800, TimeUnit.MICROSECONDS)) {
                    System.out.println("收到用户: "+ userID +" 中断通话通知");

                    addToOnlineSocketList(userID,socket);

                    System.out.println("用户：" + userID + "重新在线成功");

                    DBServer.ListLock.unlock();
                    DBServer.DBLock.unlock();
                    endCallTaskAlive = false;
                }else
                    DBServer.DBLock.unlock();
            }
        }

    }

    private void addToOnlineSocketList(String id,Socket socket){
        for(Map.Entry<String,Socket> entry : DBServer.clientList.entrySet()){
            if(entry.getKey().equals(id))
                DBServer.clientList.remove(id);
        }
        DBServer.clientList.put(id,socket);

        DBServer.dbHelper.updateTable(id,"online");
    }

    private void userOffline(String id){
        for(Map.Entry<String,Socket> entry : DBServer.clientList.entrySet()){
            if(entry.getKey().equals(id))
                DBServer.clientList.remove(id);
        }

        DBServer.dbHelper.updateTable(id,"offline");
    }


    private void setUserBusy(String id){
        for(Map.Entry<String,Socket> entry : DBServer.clientList.entrySet()){
            if(entry.getKey().equals(id))
                DBServer.clientList.remove(id);
        }

        DBServer.dbHelper.updateTable(id,"busy");
    }

    private void EndConnection() throws Exception{
        boolean endConnectionTaskAlive = true;
        while (endConnectionTaskAlive) {
            if(DBServer.DBLock.tryLock(800,TimeUnit.MICROSECONDS)) {
                if(DBServer.ListLock.tryLock(800,TimeUnit.MICROSECONDS)) {
                    System.out.println("收到用户: " + userID + " 退出登录通知");

                    userOffline(userID);

                    DBServer.DBLock.unlock();
                    DBServer.ListLock.unlock();
                    endConnectionTaskAlive = false;
                    System.out.println("更新用户: " + userID + " 离线状态成功");
                }else{
                    DBServer.DBLock.unlock();
                }
            }
        }
    }


    private void EndThread(String msg) {
        try {
            //clientOutputStream.write("fail".getBytes());
            System.out.println(msg);
            if(objectOutputStream != null)
                objectOutputStream.close();
            if(objectInputStream != null)
                objectInputStream.close();
            if(socket != null)
                socket.close();
            onlineTaskAlive = false;
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
