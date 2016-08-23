package model;

import model.database.DbConnector;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by Duane on 03/07/2016.
 */
public class UserMod {

    DbConnector db = DbConnector.getInstance();

    public UserMod() {
        super();
    }

    public ArrayList allUsers(){

        ArrayList<String> arrayList = new ArrayList<String>();

        try {
            // 3. Execute SQL query
            db.sql =  this.db.mySql.executeQuery("SELECT * FROM profile");

            // 4. Process the result set

            while (this.db.sql.next()) {
                arrayList.add(this.db.sql.getString("username"));
            }

            return arrayList;

        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        return arrayList;
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
            //this.db.sql.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        return this.db.sql;

    }
}


