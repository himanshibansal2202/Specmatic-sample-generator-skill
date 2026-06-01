package com.example.orderapi.model;

public class Product {
    private int id;
    private String name;
    private String type;
    private int inventory;
    private String createdOn;

    public Product() {}

    public Product(int id, String name, String type, int inventory, String createdOn) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.inventory = inventory;
        this.createdOn = createdOn;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getInventory() { return inventory; }
    public void setInventory(int inventory) { this.inventory = inventory; }
    public String getCreatedOn() { return createdOn; }
    public void setCreatedOn(String createdOn) { this.createdOn = createdOn; }
}
