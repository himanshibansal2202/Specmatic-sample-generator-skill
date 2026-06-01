package com.example.orderapi.model;

public class Order {
    private int id;
    private int productid;
    private int count;
    private String status;

    public Order() {}

    public Order(int id, int productid, int count, String status) {
        this.id = id;
        this.productid = productid;
        this.count = count;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProductid() { return productid; }
    public void setProductid(int productid) { this.productid = productid; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
