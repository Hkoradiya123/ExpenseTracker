package com.example.expensetracker;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HomeDetailsActivity extends AppCompatActivity {

    ListView customerListView;
    Button addCustomerButton;
    TextView headerText;
    ArrayList<String> customers;
    ArrayAdapter<String> adapter;
    FirebaseFirestore db;

    FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.details_home);

        // Initialize Views
        customerListView = findViewById(R.id.customerListView);
        addCustomerButton = findViewById(R.id.addCustomerButton);
        headerText = findViewById(R.id.headerText);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        customers = new ArrayList<>();
        adapter = new ArrayAdapter<String>(this, R.layout.item_customer, R.id.customerName, customers) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView nameText = view.findViewById(R.id.customerName);
                TextView amountText = view.findViewById(R.id.customerAmount);

                String customerName = customers.get(position);
                getCustomerAmount(customerName, amountText);

                nameText.setText(customerName);
                return view;
            }
        };
        customerListView.setAdapter(adapter);

        loadCustomers();

        addCustomerButton.setOnClickListener(v -> showAddCustomerDialog());

        customerListView.setOnItemClickListener((parent, view, position, id) -> showEditCustomerDialog(position));

        customerListView.setOnItemLongClickListener((parent, view, position, id) -> {
            showDeleteCustomerDialog(position);
            return true;
        });
    }

    private void loadCustomers() {
        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("customers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    customers.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        customers.add(document.getString("name"));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading customers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("HomeDetailsActivity", "Error loading customers", e);
                });
    }

    private void getCustomerAmount(String customerName, TextView amountText) {
        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("customers")
                .whereEqualTo("name", customerName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        amountText.setText("₹" + getAmountAsString(doc));
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading customer amount", Toast.LENGTH_SHORT).show();
                    Log.e("HomeDetailsActivity", "Error loading customer amount", e);
                });
    }

    private String getAmountAsString(DocumentSnapshot doc) {
        Object amountObj = doc.get("amount");

        if (amountObj instanceof String) {
            return (String) amountObj;
        } else if (amountObj instanceof Double) {
            return String.format("%.2f", (Double) amountObj);
        } else if (amountObj instanceof Long) {
            return String.valueOf(amountObj);
        } else {
            return "0.00";
        }
    }

    private void showAddCustomerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Customer");

        View customLayout = getLayoutInflater().inflate(R.layout.dialog_add_customer, null);
        builder.setView(customLayout);

        EditText nameInput = customLayout.findViewById(R.id.customerNameInput);
        EditText amountInput = customLayout.findViewById(R.id.customerAmountInput);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = nameInput.getText().toString();
            String amount = amountInput.getText().toString();

            if (!name.isEmpty() && !amount.isEmpty()) {
                addCustomer(name, amount);
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void addCustomer(String name, String amount) {
        String userId = currentUser.getUid();

        Map<String, Object> customer = new HashMap<>();
        customer.put("name", name);
        customer.put("amount", Double.parseDouble(amount));

        db.collection("users").document(userId).collection("customers")
                .add(customer)
                .addOnSuccessListener(documentReference -> {
                    customers.add(name);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error adding customer", Toast.LENGTH_SHORT).show();
                    Log.e("HomeDetailsActivity", "Error adding customer", e);
                });
    }

    private void showDeleteCustomerDialog(int position) {
        String customerName = customers.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Customer");
        builder.setMessage("Are you sure you want to delete " + customerName + "?");

        builder.setPositiveButton("Delete", (dialog, which) -> deleteCustomer(customerName));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showEditCustomerDialog(int position) {
        String customerName = customers.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Customer");

        View customLayout = getLayoutInflater().inflate(R.layout.dialog_add_customer, null);
        builder.setView(customLayout);

        EditText nameInput = customLayout.findViewById(R.id.customerNameInput);
        EditText amountInput = customLayout.findViewById(R.id.customerAmountInput);

        nameInput.setText(customerName);

        // Fetch current amount from Firestore
        String userId = currentUser.getUid();
        db.collection("users").document(userId).collection("customers")
                .whereEqualTo("name", customerName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        amountInput.setText(getAmountAsString(doc));
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading customer data", Toast.LENGTH_SHORT).show();
                    Log.e("HomeDetailsActivity", "Error loading customer data", e);
                });

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = nameInput.getText().toString();
            String newAmount = amountInput.getText().toString();

            if (!newName.isEmpty() && !newAmount.isEmpty()) {
                updateCustomer(customerName, newName, newAmount, position);
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void updateCustomer(String oldName, String newName, String newAmount, int position) {
        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("customers")
                .whereEqualTo("name", oldName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            doc.getReference().update("name", newName, "amount", Double.parseDouble(newAmount));
                        }
                        customers.set(position, newName);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating customer", Toast.LENGTH_SHORT).show();
                    Log.e("HomeDetailsActivity", "Error updating customer", e);
                });
    }


    private void deleteCustomer(String name) {
        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("customers")
                .whereEqualTo("name", name)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            doc.getReference().delete();
                        }
                        customers.remove(name);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting customer", Toast.LENGTH_SHORT).show();
                    Log.e("HomeDetailsActivity", "Error deleting customer", e);
                });
    }
}
