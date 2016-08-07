package model;

import model.database.DbConnector;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Duane on 03/07/2016.
 */
public class UserMod {

    DbConnector db = DbConnector.getInstance();

    public UserMod() {
        super();
    }

    public ResultSet allUsers(){

        try {
            // 3. Execute SQL query
            db.sql =  this.db.mySql.executeQuery("SELECT * FROM profile");

            // 4. Process the result set
            while(this.db.sql.next()){
                System.out.println("(UserMod.java): Database operation was successful");
                System.out.println("Users :> " + this.db.sql.getString("username"));
            }

            return this.db.sql;

        }
        catch (SQLException e) {
            e.printStackTrace();
            return this.db.sql = null;
        }

    }

    public ResultSet user(String id){
        try {
            this.db.sql =  this.db.mySql.executeQuery("SELECT * FROM profile WHERE id_key='id'");
            return this.db.sql;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return this.db.sql = null;
        }
    }

    public boolean insert(String statement){
        try {
            this.db.mySql.executeUpdate(statement);
            return true;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public ResultSet get(String queryString){

        this.db.sql = null;

        try {
            this.db.sql =  this.db.mySql.executeQuery(queryString);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        return this.db.sql;

    }
}


