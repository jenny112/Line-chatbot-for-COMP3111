package com.example.bot.spring;

<<<<<<< Updated upstream
public class Tour {
	int idInChatBot = -1; 		//Save tour's order of displaying for later use
	private String id;
	private String name;
	private String description;
	
	public Tour(int idInChatBot, String id, String name, String description) {
		this.idInChatBot = idInChatBot;
		this.id = id;
		this.name = name;
		this.description = description;
	}
	
	public String toString() {
		return id + " " + name + " " + description;
	}
	
	
	public String getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	public String getDescription() {
		return description;
	}
=======
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Tour {

    private String name;
    private String tourID;
    private String description;
    private double weekdayPrice;
    private double weekendPrice;
    private ArrayList<Date> availableDate;	//yyyyMMdd
    private ArrayList<Date> confirmedDate;	//yyyyMMdd
    private int days;
    private int capacity;

    public Tour(String name, String tourID, String description, double weekendPrice, double weekdayPrice, int days, int capacity) {
        this.name = name;
        this.tourID = tourID;
        this.description = description;
        this.weekdayPrice = weekdayPrice;
        this.weekendPrice = weekendPrice;
        availableDate = new ArrayList<Date>();
        confirmedDate = new ArrayList<Date>();
        this.days = days;
        this.capacity = capacity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTourID() {
        return tourID;
    }

    public void setTourID(String tourID) {
        this.tourID = tourID;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public double getWeekdayPrice() {
        return weekdayPrice;
    }

    public void setWeekdayPrice(double price) {
        weekdayPrice = price;
    }

    public double getWeekendPrice() {
        return weekendPrice;
    }

    public void setWeekendPrice(double price) {
        weekendPrice = price;
    }

    public ArrayList<Date> getAvailableDate(){
        return availableDate;
    }

    public ArrayList<Date> getConfirmedDate() {
        return confirmedDate;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
>>>>>>> Stashed changes
}