package com.example.expensetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "KhataBook.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_CUSTOMERS = "customers";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_AMOUNT = "amount";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_CUSTOMERS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NAME + " TEXT, "
                + COLUMN_AMOUNT + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CUSTOMERS);
        onCreate(db);
    }

    // Insert a new customer
    public void addCustomer(String name, String amount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_AMOUNT, amount);
        db.insert(TABLE_CUSTOMERS, null, values);
        db.close();
    }

    public String getCustomerAmount(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT amount FROM customers WHERE name = ?", new String[]{name});

        String amount = "0"; // Default value
        if (cursor.moveToFirst()) {
            amount = cursor.getString(0);
        }

        cursor.close();
        db.close();
        return amount;
    }

    // Retrieve all customers in the format "Name - ₹Amount"
    public ArrayList<String> getAllCustomers() {
        ArrayList<String> customers = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM customers", null); // ✅ Fetch only names

        if (cursor.moveToFirst()) {
            do {
                customers.add(cursor.getString(0)); // ✅ Store only name
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return customers;
    }

    // Update an existing customer (assumes names are unique)
    public void updateCustomer(String oldName, String newName, String newAmount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, newName);
        values.put(COLUMN_AMOUNT, newAmount);
        db.update(TABLE_CUSTOMERS, values, COLUMN_NAME + "=?", new String[]{oldName});
        db.close();
    }

    // Delete a customer by name
    public void deleteCustomer(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CUSTOMERS, COLUMN_NAME + "=?", new String[]{name});
        db.close();
    }
}
