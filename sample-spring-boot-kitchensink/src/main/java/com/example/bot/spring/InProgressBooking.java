package com.example.bot.spring;

import java.util.Calendar;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InProgressBooking {
	
	private Tour tour;
	private String lineID;
	private Date date;
	private int noOfAdults;
	private int noOfChildren;
	private int noOfToddler;
	private double tourFee;
	private double totalFee;
	private String specialRequest;

	public InProgressBooking() {
		this.tour = null;
		this.lineID = null;
		this.date = null;
		this.noOfAdults = -1;
		this.noOfChildren = -1;
		this.noOfToddler = -1;
		this.tourFee = -1.0;
		this.totalFee = -1.0;
		this.specialRequest = null;
	}

	public Tour getTour() {
		return tour;
	}

	public void setTour(Tour tour) {
		this.tour = tour;
	}

	public String getLineID() {
		return lineID;
	}

	public void setLineID(String lineID) {
		this.lineID = lineID;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;	
	}
	
	public int getNoOfAdults() {
		return noOfAdults;
	}

	public void setNoOfAdults(int noOfAdults) {
		this.noOfAdults = noOfAdults;
	}

	public int getNoOfChildren() {
		return noOfChildren;
	}

	public void setNoOfChildren(int noOfChildren) {
		this.noOfChildren = noOfChildren;
	}

	public int getNoOfToddler() {
		return noOfToddler;
	}

	public void setNoOfToddler(int noOfToddler) {
		this.noOfToddler = noOfToddler;
	}

	public double getTourFee() {
		return tourFee;
	}

	public void setTourFee() {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
			tourFee = tour.getWeekendPrice();
		} else {
			tourFee = tour.getWeekdayPrice();
		}
	}
	
	public double getTotalFee() {
		return totalFee;
	}
	
	public void setTotalFee(double totalFee) {
		this.totalFee = totalFee;
	}

	public String getSpecialRequest() {
		return specialRequest;
	}

	public void setSpecialRequest(String specialRequest) {
		this.specialRequest = specialRequest;
	}

	public void clearAllData() {
		this.tour = null;
		this.lineID = null;
		this.date = null;
		this.noOfAdults = -1;
		this.noOfChildren = -1;
		this.noOfToddler = -1;
		this.tourFee = -1.0;
		this.totalFee = -1.0;
		this.specialRequest = null;
	}

}