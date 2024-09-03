package com.ibm.sms_length_app;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONArray;
import org.json.JSONObject;

public class Database {
    public static void update_database(String dbHost, String dbPort, String dbName, String dbUser, String dbPasswd, JSONArray jsonArray){
        try(Connection conn = connectToDb(dbHost, dbPort, dbName, dbUser, dbPasswd)){
            dropTopicsTable(conn);
            createTable(conn);
            insertAllTopic(conn,jsonArray);
            
        } catch( SQLException e) {
            System.out.println(e);
        }

    }

    public static Connection connectToDb(String dbHost, String dbPort, String dbName, String dbUser, String dbPasswd) throws SQLException{
        Connection conn = null;
        
        conn = DriverManager.getConnection("jdbc:postgresql://"+dbHost+":"+dbPort+"/"+ dbName, dbUser, dbPasswd);
        System.out.println("Connection to "+dbName+" is "+conn.isValid(1));
        /** possible URL-Strings:
        * dbUrl = "jdbc:postgresql:postgres" Server running on localhost at port 5432
        * dbUrl = "jdbc:postgresql://" + dbHost + "/" + dbname; -> Default port 5432
        * dbUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbname;
        *
        */
	return conn;
}

    private static void createTable(Connection conn) throws SQLException {
        CallableStatement stmt = conn.prepareCall("CREATE TABLE IF NOT EXISTS public.topics (content_name character varying(100) NOT NULL PRIMARY KEY,\n" + //
                        "\t\t\t\t \tcontent character varying(3000),\n" + //
                        "\t\t\t\t \tcategory character varying(31),\n" + //
                        "\t\t\t\t \tpriority INTEGER,\n" + //
                        "\t\t\t\t \tis_boss character varying(5),\n" + //
                        "\t\t\t\t \ttarget_x DOUBLE PRECISION,\n" + //
                        "\t\t\t\t \ttarget_y DOUBLE PRECISION,\n" + //
                        "\t\t\t\t\tchildren JSONB);");

        System.out.println("created new table");
        stmt.execute();
        stmt.close();
    }

    public static void insertAllTopic(Connection conn, JSONArray jsonArray) throws SQLException{
        int starting_index = howManyTopics(conn);
        System.out.println("current size"+starting_index);
        for (int i = starting_index; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            insertTopic(conn,obj);
        }
    }

        private static void insertTopic(Connection conn, JSONObject topic) throws SQLException {
            CallableStatement stmt = conn.prepareCall("INSERT INTO public.topics (content_name,content,category,priority,is_boss,target_x,target_y,children) VALUES(?,?,?,?,?,?,?,?)");
            stmt.setString(1, topic.getString("name"));
            stmt.setString(2, topic.getString("content"));
            stmt.setString(3, topic.getString("category"));
            stmt.setInt(4, topic.getInt("priority"));
            if (topic.getBoolean("is_boss")){
                stmt.setString(5, "true");
            }else{
                stmt.setString(5, "false");
            }
            
            stmt.setInt(6, 0);
            stmt.setInt(7, 0);
            JSONArray childrenString = topic.getJSONArray("children");
            stmt.setObject(8, childrenString, java.sql.Types.OTHER);
            stmt.execute();       
            stmt.close();
        
        }

        public static int howManyTopics(Connection conn) throws SQLException {
            PreparedStatement prpStmt = conn.prepareStatement("select count(*) from topics;");
            ResultSet rs = prpStmt.executeQuery();
            int result = 0;
            while(rs.next()) {

                result = rs.getInt(1);
            }
            prpStmt.close();
            rs.close();
            return result;
        }
    

        public static void dropTopicsTable(Connection conn) throws SQLException {
            CallableStatement stmt = conn.prepareCall("DROP TABLE IF EXISTS topics;");
            stmt.execute();
            System.out.println("Table successfully dropped!");       
            stmt.close();
        }
}
