package com.example.bot.spring;

public class Customer {
    private String customerID;
    private String name;
    private String phoneNo;
    private int age;
    private String lineID;
    private int status;

    public Customer() {
        this.name = null;
        this.customerID = null;
        this.lineID = null;
        this.phoneNo = null;
        this.age = -1;
        this.status = -2;
    }

    public String getCustomerID() {
        return customerID;
    }

    public void setCustomerID(String customerID) {
        this.customerID = customerID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getLineID() {
        return lineID;
    }

    public void setLineID(String lineID) {
        this.lineID = lineID;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

}