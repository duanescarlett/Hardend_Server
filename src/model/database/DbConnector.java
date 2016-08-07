package model.database;

import java.sql.*;

/**
 * Created by Duane on 02/07/2016.
 */
public class DbConnector {

    //  Database credentials
    static final String USER = "root";
    static final String PASS = "scarstar";

    // Database URL
    static final String URL = "jdbc:mysql://localhost:3306/fire_brigade";
    public static Connection con;
    public static Statement mySql;
    public static ResultSet sql = null;

    private DbConnector()  {
        try {
            // 1. Get db connection
            this.con = DriverManager.getConnection(URL, USER, PASS);

            // 2. Create a SQL statement
            this.mySql = con.createStatement();
        }
        catch(SQLException e){
            e.printStackTrace();
        }

    }

    private static class SingletonHolder {
        private static final DbConnector INSTANCE = new DbConnector();
    }

    public static DbConnector getInstance(){
        return SingletonHolder.INSTANCE;
    }

}
