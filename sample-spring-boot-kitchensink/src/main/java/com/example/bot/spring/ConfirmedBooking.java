package com.example.bot.spring;

public class ConfirmedBooking {

    private String CustomerID;
    private String TourID;
    private int noOfAdults;
    private int noOfChildren;
    private int noOfToodler;
    private double amountPaid;
    private double tourFee;
    private String special_request = "";

    public ConfirmedBooking(String customerID, String tourID, int noOfAdults, int noOfChildren, int noOfToodler, double amountPaid, double tourFee, String special_request) {
        CustomerID = customerID;
        TourID = tourID;
        this.noOfAdults = noOfAdults;
        this.noOfChildren = noOfChildren;
        this.noOfToodler = noOfToodler;
        this.amountPaid = amountPaid;
        this.tourFee = tourFee;
        this.special_request = special_request;
    }
    
    public ConfirmedBooking(String customerID, String tourID, int noOfAdults, int noOfChildren, int noOfToodler, double amountPaid, double tourFee) {
        CustomerID = customerID;
        TourID = tourID;
        this.noOfAdults = noOfAdults;
        this.noOfChildren = noOfChildren;
        this.noOfToodler = noOfToodler;
        this.amountPaid = amountPaid;
        this.tourFee = tourFee;
    }

    public String getCustomerID() {
        return CustomerID;
    }

    public void setCustomerID(String customerID) {
        CustomerID = customerID;
    }

    public String getTourID() {
        return TourID;
    }

    public void setTourID(String tourID) {
        TourID = tourID;
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

    public int getNoOfToodler() {
        return noOfToodler;
    }

    public void setNoOfToodler(int noOfToodler) {
        this.noOfToodler = noOfToodler;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public double getTourFee() {
        return tourFee;
    }

    public void setTourFee(double tourFee) {
        this.tourFee = tourFee;
    }

    public String getSpecial_request() {
        return special_request;
    }

    public void setSpecial_request(String special_request) {
        this.special_request = special_request;
    }
}