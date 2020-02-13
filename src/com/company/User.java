package com.company;

public class User {
    private String name;
    private String ipAddress;
    private String color;

    public User(String name, String ipAddress, String color) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getColor() { return color; }

    public void setColor(String color) {
        this.color = color;
    }
}
