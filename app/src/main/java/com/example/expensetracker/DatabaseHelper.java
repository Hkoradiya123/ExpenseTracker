package com.example.expensetracker;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private FirebaseUser currentUser;

    public DatabaseHelper() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    // Get reference to user's Firestore collection
    private CollectionReference getUserCollection() {
        if (currentUser == null) return null;
        return db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_CUSTOMERS);
    }

    // Store Google User in Firestore
    public void saveGoogleUser(FirebaseUser user) {
        if (user == null) return;

        DocumentReference userRef = db.collection(COLLECTION_USERS).document(user.getUid());

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", user.getDisplayName());
        userData.put("email", user.getEmail());

        userRef.set(userData)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Google User Added"))
                .addOnFailureListener(e -> Log.e("Firestore", "Failed to Add Google User", e));
    }

    // Add Customer to Firestore
    public void addCustomer(String name, String amount) {
        CollectionReference userCollection = getUserCollection();
        if (userCollection == null) return;

        Map<String, Object> customer = new HashMap<>();
        customer.put(FIELD_NAME, name);
        customer.put(FIELD_AMOUNT, amount);

        userCollection.document(name) // Customer name as document ID
                .set(customer)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Customer Added"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error Adding Customer", e));
    }

    // Get Customer Amount by Name
    public void getCustomerAmount(String name, FirestoreCallback<String> callback) {
        CollectionReference userCollection = getUserCollection();
        if (userCollection == null) return;

        userCollection.whereEqualTo(FIELD_NAME, name).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    String amount = "0";
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        amount = doc.getString(FIELD_AMOUNT);
                        break;
                    }
                    callback.onCallback(amount);
                });
    }

    // Retrieve All Customers
    public void getAllCustomers(FirestoreCallback<ArrayList<String>> callback) {
        CollectionReference userCollection = getUserCollection();
        if (userCollection == null) return;

        userCollection.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<String> customers = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        customers.add(doc.getString(FIELD_NAME));
                    }
                    callback.onCallback(customers);
                });
    }

    // Update Customer Data
    public void updateCustomer(String oldName, String newName, String newAmount) {
        CollectionReference userCollection = getUserCollection();
        if (userCollection == null) return;

        userCollection.whereEqualTo(FIELD_NAME, oldName).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().update(FIELD_NAME, newName, FIELD_AMOUNT, newAmount);
                    }
                });
    }

    // Delete Customer
    public void deleteCustomer(String name) {
        CollectionReference userCollection = getUserCollection();
        if (userCollection == null) return;

        userCollection.whereEqualTo(FIELD_NAME, name).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                });
    }

    // Callback Interface for Async Operations
    public interface FirestoreCallback<T> {
        void onCallback(T result);
    }
}
