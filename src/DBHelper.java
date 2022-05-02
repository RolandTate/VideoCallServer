import org.sqlite.SQLiteJDBCLoader;

import java.sql.*;

public class DBHelper {
    private Connection connection;
    private Statement statement;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    DBHelper(){
        try{
            Class.forName("org.sqlite.JDBC");
            //connection = DriverManager.getConnection("jdbc:sqlite:D:\\UndergraduateGraduationProject\\Server\\DBServer\\user.sqlite");
            connection = DriverManager.getConnection("jdbc:sqlite:user.sqlite");
            statement = connection.createStatement();
            ResultSet rs = connection.getMetaData().getTables(null,null,"user",null);
            if(rs.next()){
                System.out.println("table exist");
                //table exist
            }else {
                //table not exist
                statement.executeUpdate("create table user(id text primary key ,password text,name text,state text);");
                System.out.println("table not exist, create successfully");
            }

            System.out.println("数据库连接成功");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public synchronized boolean checkInTable(String ID,String password){
        if (!ID.equals("") && !password.equals("")) {
            try {
                resultSet = statement.executeQuery("select * from user;");
                while (resultSet.next()) {
                    if (resultSet.getString("id").equals(ID) &&
                            resultSet.getString("password").equals(password))
                        return true;
                }
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }else {
            return false;
        }
    }

    public synchronized boolean checkInTable(String ID){
        if (!ID.equals("")) {
            try {
                resultSet = statement.executeQuery("select * from user;");
                while (resultSet.next()) {
                    if (resultSet.getString("id").equals(ID))
                        return true;
                }
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }else {
            return false;
        }
    }

    public synchronized ResultSet getAllUserInformation() {
        try{
            resultSet = statement.executeQuery("select * from user;");
            return resultSet;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public synchronized boolean insertTable(String ID, String password, String name, String state){
        if(!checkInTable(ID,password)) {
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "INSERT INTO user VALUES(?,?,?,?)");
                preparedStatement.setString(1, ID);
                preparedStatement.setString(2, password);
                preparedStatement.setString(3, name);
                preparedStatement.setString(4, state);
                preparedStatement.executeUpdate();
                System.out.println("创建用户成功！");
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public synchronized boolean updateTable(String ID,  String state){
        if(checkInTable(ID)) {
            System.out.println("更新数据库时，查询到有此用户");
            try {
                //statement.execute("update user set state = ? where id = ?", new String[]{ID, state});
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "update user set state = ? where id = ?");
                preparedStatement.setString(1, state);
                preparedStatement.setString(2, ID);
                preparedStatement.executeUpdate();
                System.out.println("用户："+ ID + " " + state);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("更新数据库时，查询没有此用户");
        return false;
    }
}
