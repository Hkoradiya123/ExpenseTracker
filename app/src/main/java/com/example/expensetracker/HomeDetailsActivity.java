package com.example.expensetracker;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import android.util.Log;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HomeDetailsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    ListView customerListView;
    Button addCustomerButton;

    ArrayList<String> customers;
    ArrayAdapter<String> adapter;
    FirebaseFirestore db;

    FirebaseUser currentUser;

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.details_home);

        // Initialize Views
        customerListView = findViewById(R.id.customerListView);
        addCustomerButton = findViewById(R.id.addCustomerButton);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        //toolbar
        setSupportActionBar(toolbar);

        //navigation
        navigationView.bringToFront();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize Firestorm
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.user_nlogin), Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no user is logged in
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

    @Override
    public void onBackPressed() {
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
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
                    Toast.makeText(this, getString(R.string.error_adding_customer) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("HomeDetailsActivity", "Error Adding Customer", e); // Log the error for debugging
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

                        // Check if the amount is a String, Double, or Integer
                        Object amountObj = doc.get("amount");

                        if (amountObj instanceof String) {
                            amountText.setText("₹" + amountObj.toString());
                        } else if (amountObj instanceof Double) {
                            amountText.setText("₹" + String.format("%.2f", amountObj));
                        } else if (amountObj instanceof Long) {
                            amountText.setText("₹" + String.valueOf(amountObj));
                        } else {
                            amountText.setText("₹0.00");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.error_loading_amount), Toast.LENGTH_SHORT).show();
                    Log.e("HomeDetailsActivity", "Error Loading Amount", e); // Log error details
                });
    }


    private void showAddCustomerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.add_customer));

        View customLayout = getLayoutInflater().inflate(R.layout.dialog_add_customer, null);
        builder.setView(customLayout);

        EditText nameInput = customLayout.findViewById(R.id.customerNameInput);
        EditText amountInput = customLayout.findViewById(R.id.customerAmountInput);

        builder.setPositiveButton(getString(R.string.add), (dialog, which) -> {
            String name = nameInput.getText().toString();
            String amount = amountInput.getText().toString();

            if (!name.isEmpty() && !amount.isEmpty()) {
                addCustomer(name, amount);
            } else {
                Toast.makeText(this, getString(R.string.fill_up_fields), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.create().show();
    }

    private void addCustomer(String name, String amount) {
        String userId = currentUser.getUid();

        Map<String, Object> customer = new HashMap<>();
        customer.put("name", name);
        customer.put("amount", amount);

        db.collection("users").document(userId).collection("customers")
                .add(customer)
                .addOnSuccessListener(documentReference -> {
                    customers.add(name);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,getString(R.string.error_adding_customer), Toast.LENGTH_SHORT).show();
                    Log.e("HomeDetailsActivity", "Error Adding Customer", e); // Log the error for debugging
                });
    }

    private void showEditCustomerDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.edit_customer));

        View customLayout = getLayoutInflater().inflate(R.layout.dialog_edit_customer, null);
        builder.setView(customLayout);

        EditText nameInput = customLayout.findViewById(R.id.customerNameInput);
        EditText amountInput = customLayout.findViewById(R.id.customerAmountInput);

        String customerName = customers.get(position);
        nameInput.setText(customerName);

        db.collection("users").document(currentUser.getUid()).collection("customers")
                .whereEqualTo("name", customerName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        String amount = getAmountAsString(doc);
                        amountInput.setText(amount);
                    }
                });

        builder.setPositiveButton(getString(R.string.update), (dialog, which) -> {
            String newName = nameInput.getText().toString();
            String newAmount = amountInput.getText().toString();

            if (!newName.isEmpty() && !newAmount.isEmpty()) {
                updateCustomer(customerName, newName, newAmount);
            } else {
                Toast.makeText(this, getString(R.string.fill_up_fields), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.create().show();
    }

    // Helper method to safely get the amount as a string
    private String getAmountAsString(DocumentSnapshot doc) {
        Object amountObj = doc.get("amount");

        if (amountObj instanceof String) {
            return (String) amountObj;
        } else if (amountObj instanceof Double) {
            return String.format("%.2f", amountObj);
        } else if (amountObj instanceof Long) {
            return String.valueOf(amountObj);
        } else {
            return "0.00"; // Default value
        }
    }

    private void updateCustomer(String oldName, String newName, String newAmount) {
        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("customers")
                .whereEqualTo("name", oldName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            doc.getReference().update("name", newName, "amount", newAmount);
                        }
                        customers.set(customers.indexOf(oldName), newName);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.error_updating_customer), Toast.LENGTH_SHORT).show();
                    Log.e("HomeDetailsActivity", "Error Updating Customer", e); // Log the error for debugging
                });
    }

    private void showDeleteCustomerDialog(int position) {
        String customerName = customers.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.delete_customer));
        builder.setMessage(getString(R.string.are_you_sure_delete) + customerName + "?");

        builder.setPositiveButton(getString(R.string.delete), (dialog, which) -> deleteCustomer(customerName));
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
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
                    Toast.makeText(this, getString(R.string.error_deleting_customer), Toast.LENGTH_SHORT).show();
                    Log.e("HomeDetailsActivity", "Error Deleting Customer", e); // Log the error for debugging
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        return true;
    }
}
