package com.example.expensetracker;

import static android.provider.Settings.Global.getString;

import android.util.Log;

import com.google.firebase.BuildConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper {

    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_CUSTOMERS = "customers";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_AMOUNT = "amount";
    private FirebaseFirestore db;
    private String userId;

    public DatabaseHelper() {
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        FirebaseFirestore db;

        if (BuildConfig.DEBUG) {
            db = FirebaseFirestore.getInstance();
            db.useEmulator("10.0.2.2", 8080);  // Use this for Android Emulator
        } else {
            db = FirebaseFirestore.getInstance(); // Use Firestore normally in production
        }
    }

    private CollectionReference getUserCollection() {
        return db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_CUSTOMERS);
    }

    // Add customer to Firestore

    public void addCustomerToFirestore(String userId, String name, String amount) {
        DocumentReference docRef = db.collection("users")
                .document(userId)
                .collection("customers")
                .document(name); // Use customer name as document ID

        Map<String, Object> customer = new HashMap<>();
        customer.put("name", name);
        customer.put("amount", amount);

        docRef.set(customer)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Customer Added Successfully..."))
                .addOnFailureListener(e -> Log.e("Firestore", "Error Adding Customer", e));
    }
    // Get customer amount by name
    public void getCustomerAmount(String name, FirestoreCallback<String> callback) {
        if (userId == null) return;
        getUserCollection().whereEqualTo(FIELD_NAME, name).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    String amount = "0";
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        amount = doc.getString(FIELD_AMOUNT);
                        break;
                    }
                    callback.onCallback(amount);
                });
    }

    // Retrieve all customer names
    public void getAllCustomers(FirestoreCallback<ArrayList<String>> callback) {
        if (userId == null) return;
        getUserCollection().get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<String> customers = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        customers.add(doc.getString(FIELD_NAME));
                    }
                    callback.onCallback(customers);
                });
    }

    // Update customer details
    public void updateCustomer(String oldName, String newName, String newAmount) {
        if (userId == null) return;
        getUserCollection().whereEqualTo(FIELD_NAME, oldName).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().update(FIELD_NAME, newName, FIELD_AMOUNT, newAmount);
                    }
                });
    }

    // Delete customer by name
    public void deleteCustomer(String name) {
        if (userId == null) return;
        getUserCollection().whereEqualTo(FIELD_NAME, name).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                });
    }

    // Callback interface for Firestore async operations
    public interface FirestoreCallback<T> {
        void onCallback(T result);
    }
}
