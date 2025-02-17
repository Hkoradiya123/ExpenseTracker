package com.example.expensetracker;

public class Customer {
    private String id;
    private String name;
    private double amount;

    // ✅ Required for Firestore deserialization
    public Customer() {}

    public Customer(String id, String name, double amount) {
        this.id = id;
        this.name = name;
        this.amount = amount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    // ✅ Ensures ListView/Spinner shows name instead of object reference
    @Override
    public String toString() {
        return name + " (₹" + amount + ")";
    }
}
