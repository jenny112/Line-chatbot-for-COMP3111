package com.example.bot.spring;

import lombok.extern.slf4j.Slf4j;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import java.sql.*;
import java.net.URISyntaxException;
import java.net.URI;

import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

@Slf4j
public class SQLDatabaseEngine {


	ArrayList<String[]> searchBookingRecord(String id) throws SQLException, URISyntaxException {

		ArrayList<String[]> recordArr = new ArrayList<String[]>();
		int index = 0;

		Connection connection = getConnection();
		PreparedStatement stmt = connection.prepareStatement("SELECT T.TourName, B.TourFee, B.Date FROM BookingRecord B, Tour T, Customer C WHERE T.TID = B.TID AND C.CID = B.CID AND C.LineID = '" + id + "'" );
		ResultSet rs = stmt.executeQuery();

		while (rs.next()) {
			String[] temp = new String[3];
			temp[0] = rs.getString(1);				    //tour name
			temp[1] = String.valueOf(rs.getDouble(2));  //tour fee
			temp[2] = rs.getString(3);				    //date
			recordArr.add(temp);
			index++;
		}

		return recordArr;
	}




	ArrayList<Tour> searchTour(String text) throws Exception {		//search a tour information		
		int index = -1;
		ArrayList<Tour> tourArr = new ArrayList<Tour>();
		Connection connection = getConnection();
		PreparedStatement stmt = connection.prepareStatement("SELECT * FROM Tour T, TourDetail TD WHERE T.TID = TD.TID AND date > current_date - 3");
		ResultSet rs = stmt.executeQuery();
		String previousTourID = "";
		while (rs.next()) {	
			String tourID = rs.getString(1);
			String tourName  = rs.getString(2);
			String tourDescription = rs.getString(3);
			int days = rs.getInt(4);
			String departureDate = rs.getString(5);
			double weekdayPrice = rs.getDouble(6);	
			double weekendPrice = rs.getDouble(7);					
			Date startDate = rs.getDate(9);
			String hotel = rs.getString(12);
			int capacity = rs.getInt(13);

			DateFormat df = new SimpleDateFormat("yyyyMMdd");	
			String month = df.format(startDate);
			ArrayList<java.util.Date> tourAvailableDate;
			ArrayList<java.util.Date> tourConfirmedDate;
			
			//if keyword matched, get the tour information
			if (tourName.toLowerCase().contains(text.toLowerCase()) || tourDescription.toLowerCase().contains(text.toLowerCase())
				|| Integer.toString(days).equals(text) || departureDate.toLowerCase().contains(text.toLowerCase())
				|| String.valueOf(weekdayPrice).equals(text) || String.valueOf(weekendPrice).equals(text)
				|| df.format(startDate).equals(text) || month.substring(4,6).equals(text) 
				|| hotel.toLowerCase().equals(text.toLowerCase())) {

				if (!previousTourID.equals(tourID)){
					tourArr.add(new Tour(tourName, tourID, tourDescription, weekendPrice, weekdayPrice, days, capacity));
					++index;
				}

				tourAvailableDate = tourArr.get(index).getAvailableDate();
				tourConfirmedDate = tourArr.get(index).getConfirmedDate();
				PreparedStatement stmt2 = connection.prepareStatement("SELECT Date, SUM(NoOfAdults), SUM(NoOfChildren), SUM(NoOfToddler) FROM BookingRecord WHERE TID = '" + tourID + "' GROUP BY Date");
				ResultSet rs2 = stmt2.executeQuery();
				boolean dateNotExistInResult = true;

				if(!rs2.isBeforeFirst()) {
					tourAvailableDate.add(startDate);
				}

				while (rs2.next()) {
					Date bookDate = rs2.getDate(1);
					int availableCapacity = capacity - (rs2.getInt(2) + rs2.getInt(3) + rs2.getInt(4));

					if(bookDate.equals(startDate) && (availableCapacity == 0)) {
						dateNotExistInResult = false;
						tourConfirmedDate.add(startDate);
					}
					else if(bookDate.equals(startDate) && (availableCapacity > 0)){
						dateNotExistInResult = false;
						tourAvailableDate.add(startDate);
						tourArr.get(index).setCapacity(availableCapacity);
					}
				}

				if (dateNotExistInResult) {
					tourAvailableDate.add(startDate);
				}

				previousTourID = tourID;
			}			
		}

		return tourArr;		
	}

	boolean bookTour(String lineID, String tid, Date tourDate, int adult, int children, int toddler, double amountPaid, double tourFee, String request) throws Exception {
		boolean success = true;
		String bid = "";
		String cid = "";
		Connection connection = getConnection();

		//get previous Booking ID
		PreparedStatement getBID = connection.prepareStatement("SELECT BID FROM BookingRecord");
		ResultSet rs = getBID.executeQuery();
		while (rs.next()) {
			bid = rs.getString(1);
		}
		bid = "B" + nextID(bid);

		//get the Customer ID
		PreparedStatement getCID = connection.prepareStatement("SELECT CID FROM Customer WHERE LineID = '" + lineID + "'");
		ResultSet rs2 = getCID.executeQuery();
		while (rs2.next()) {
			cid = rs2.getString(1);
		}		

		try {
			String insertSQL = "INSERT INTO BookingRecord" + " VALUES" + "(?,?,?,?,?,?,?,?,?,?)";
			PreparedStatement stmt = connection.prepareStatement(insertSQL);
			stmt.setString(1, cid);
			stmt.setString(2, tid);
			stmt.setInt(3, adult);
			stmt.setInt(4, children);
			stmt.setInt(5, toddler);
			stmt.setDouble(6, tourFee);
			stmt.setDouble(7, amountPaid);
			stmt.setString(8, request);
			stmt.setString(9, bid);
			stmt.setDate(10, tourDate);
			stmt.executeUpdate();

		} catch (SQLException e) {
			success = false;
		}
		return success;
	}

	boolean checkExistingCustomer(String lineID, Customer customer) throws Exception {
		Connection connection = getConnection();
		PreparedStatement stmt = connection.prepareStatement("SELECT * FROM Customer WHERE LineID = '" + lineID + "'");
		ResultSet rs = stmt.executeQuery();

		//set customer's data if user exists in the database
		if (rs.next()) {
			customer.setCustomerID(rs.getString(1));	
			customer.setName(rs.getString(2));
			customer.setPhoneNo(rs.getString(3));
			customer.setAge(rs.getInt(4));
			customer.setLineID(lineID);
			return true;
		}
		return false;
	}

	boolean createCustomer(String lineID, String customerName, String phone, int age) throws Exception {
		boolean success = true;
		String cid = "";
		Connection connection = getConnection();
		PreparedStatement getCID = connection.prepareStatement("SELECT CID FROM Customer");
		ResultSet rs = getCID.executeQuery();
		while (rs.next()) {
			cid = rs.getString(1);
		}		
		cid = "A" + nextID(cid);

		try {
			String insertSQL = "INSERT INTO Customer" + " VALUES" + "(?,?,?,?,?)";
			PreparedStatement stmt = connection.prepareStatement(insertSQL);
			stmt.setString(1, cid);
			stmt.setString(2, customerName);
			stmt.setString(3, phone);
			stmt.setInt(4, age);
			stmt.setString(5, lineID);
			stmt.executeUpdate();

		} catch (SQLException e) {
			success = false;
		}
		return success;
	}

	private String nextID(String last_id) {
		String result = "";
		int parseInt = Integer.parseInt(last_id.substring(1, 7));
		String temp = String.valueOf(++parseInt);
		int fillZero = 6 - temp.length();
		for (int i = 0; i < fillZero; i++) {
			result = result + "0";
		}
		return result + temp;
	}

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
}


	/*		java.util.Date currentDate = new java.util.Date();
			Calendar c = Calendar.getInstance();
			c.setTime(currentDate);
			c.add(Calendar.DATE, -3);
			currentDate = new java.sql.Date(c.getTime().getTime());	
			if (currentDate.after(startDate)) {
				continue;
			}
	*/
