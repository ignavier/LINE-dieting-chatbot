package com.example.bot.spring;

import lombok.extern.slf4j.Slf4j;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.*;
import java.net.URISyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

@Slf4j
public class SQLDatabaseEngine {
	// Get connection to PostgreSQL Database
	private Connection getConnection() throws URISyntaxException, SQLException {
		Connection connection;
		URI dbUri = new URI(System.getenv("DATABASE_URL"));

		String username = dbUri.getUserInfo().split(":")[0];
		String password = dbUri.getUserInfo().split(":")[1];
		String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() +  "?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory";

		log.info("Username: {} Password: {}", username, password);
		log.info ("dbUrl: {}", dbUrl);
		
		connection = DriverManager.getConnection(dbUrl, username, password);

		return connection;
	}
	
	
	/* Writes input user info into the database
	 * Valid values for gender: male, female
	 * Unit for height: m
	 * Unit for weight: kg
	 */
	public void writeUserInfo(String userId, int age, String gender, double height, double weight, String[] allergies) throws Exception {
		Connection connection = null;
		PreparedStatement stmtUpdate = null;
		
		try {
			connection = this.getConnection();
			
			// Delete user info if it already exists
			try {
				stmtUpdate = connection.prepareStatement(
					"DELETE FROM userinfo " +
					"WHERE userId = ?"
				);
				stmtUpdate.setString(1, userId);
				stmtUpdate.executeUpdate();
			} catch (SQLException e) {
				log.info("Exception while deleting existing user info from database: {}", e.toString());
			}
			
			// Insert user info into the database
			try {
				stmtUpdate = connection.prepareStatement(
					"INSERT INTO userinfo " +
					"VALUES (?, ?, ?, ?, ?)"
				);
				stmtUpdate.setString(1, userId);
				stmtUpdate.setInt(2, age);
				stmtUpdate.setString(3, gender);
				stmtUpdate.setDouble(4, height);
				stmtUpdate.setDouble(5, weight);
				stmtUpdate.executeUpdate();
			} catch (SQLException e) {
				log.info("Exception while inserting user info into database: {}", e.toString());
			}
		} catch (SQLException e) {
			log.info("Exception while connecting to database: {}", e.toString());
		} finally {
			try {
				if (stmtUpdate != null) {stmtUpdate.close();}
				if (connection != null) {connection.close();}
			} catch (SQLException e) {  // Exception or IOException??
				log.info("Exception while closing connection to database: {}", e.toString());
			}
		}
	}

	
	public String readUserInfo(String[] columns) {
		String result = "test";
		return result;
	}
	
	// Deletes user info from database
	public void deleteUserInfo(String userId) throws Exception {
		Connection connection = null;
		PreparedStatement stmtDelete = null;
		
		try {
			connection = this.getConnection();
			
			// Delete user info if it already exists
			try {
				stmtDelete = connection.prepareStatement(
					"DELETE FROM userinfo " +
					"WHERE userId = ?"
				);
				stmtDelete.setString(1, userId);
				stmtDelete.executeUpdate();
			} catch (SQLException e) {
				log.info("Exception while deleting existing user info from database: {}", e.toString());
			}
		} catch (SQLException e) {
			log.info("Exception while connecting to database: {}", e.toString());
		} finally {
			try {
				if (stmtDelete != null) {stmtDelete.close();}
				if (connection != null) {connection.close();}
			} catch (SQLException e) {  // Exception or IOException??
				log.info("Exception while closing connection to database: {}", e.toString());
			}
		}
	}


	public String search(String text) throws Exception {
		//Write your code here
		String result = null;
		Connection connection = null;
		PreparedStatement stmtQuery = null;
		PreparedStatement stmtUpdate = null;

		ResultSet rs = null;
		try {
			connection = this.getConnection();
			stmtQuery = connection.prepareStatement(
				"SELECT response, hits FROM automatedreply "
				+ "WHERE ? LIKE concat('%', LOWER(keyword), '%')"
			);
			stmtQuery.setString(1, text.toLowerCase());
			rs = stmtQuery.executeQuery(); 
			while(result == null && rs.next()) {
				String response = rs.getString(1);
				int numHits = rs.getInt(2) + 1;  // rs.getInt(2) does not include current call
				result = response + " " + String.valueOf(numHits);

				// Perform update for number of hits in db
				try {
					stmtUpdate = connection.prepareStatement(
						"UPDATE automatedreply "
						+ "SET hits = hits + 1 "
						+ "WHERE ? LIKE concat('%', LOWER(keyword), '%')"
					);
					stmtUpdate.setString(1, text.toLowerCase());
				    // Call executeUpdate to execute our sql update statement
					stmtUpdate.executeUpdate();
				} catch (SQLException e) {
					log.info("Exception while updating number of hits in database: {}", e.toString());
				} finally {
					if(stmtUpdate != null)
						stmtUpdate.close();
				}
			}
		} catch (SQLException e) {
			log.info("Exception while connecting to database: {}", e.toString());
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmtQuery != null)
					stmtQuery.close();
				if (connection != null)
					connection.close();
			} catch (SQLException ex) {  // Exception or IOException??
				log.info("Exception while closing connection of database: {}", ex.toString());
			}
		}
		if (result != null)
			return result;
		throw new Exception("NOT FOUND");
	}
}
