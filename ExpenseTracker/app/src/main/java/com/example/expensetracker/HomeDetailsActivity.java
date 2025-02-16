package com.example.expensetracker;

import android.content.DialogInterface;
import android.content.Intent;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class HomeDetailsActivity extends AppCompatActivity {

    ListView customerListView;
    Button addCustomerButton;
    TextView headerText;
    ArrayList<String> customers;
    ArrayAdapter<String> adapter;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.details_home);

        // Initialize Views
        customerListView = findViewById(R.id.customerListView);
        addCustomerButton = findViewById(R.id.addCustomerButton);
        headerText = findViewById(R.id.headerText);
        dbHelper = new DatabaseHelper(this);

        // Add space below title
        headerText.setPadding(0, 0, 0, 20);

        // Load customers from SQLite
        customers = dbHelper.getAllCustomers();

        // Insert initial data if empty
        if (customers.isEmpty()) {
            dbHelper.addCustomer("Rahul", "500");
            dbHelper.addCustomer("Priya", "1200");
            dbHelper.addCustomer("Amit", "300");
            customers = dbHelper.getAllCustomers();
        }

        // Set up adapter
        adapter = new ArrayAdapter<String>(this, R.layout.item_customer, R.id.customerName, customers) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView nameText = view.findViewById(R.id.customerName);
                TextView amountText = view.findViewById(R.id.customerAmount);

                String customerName = customers.get(position);
                String amount = dbHelper.getCustomerAmount(customerName);

                nameText.setText(customerName);
                amountText.setText("₹" + amount);

                return view;
            }
        };

        customerListView.setAdapter(adapter);

        // Add Customer Button Click Listener
        addCustomerButton.setOnClickListener(v -> showAddCustomerDialog());

        // Edit Customer on Click
        customerListView.setOnItemClickListener((parent, view, position, id) -> showEditCustomerDialog(position));

        // Delete Customer on Long Press
        customerListView.setOnItemLongClickListener((parent, view, position, id) -> {
            showDeleteCustomerDialog(position);
            return true;
        });
    }

    private void refreshList() {
        customers.clear();
        customers.addAll(dbHelper.getAllCustomers());
        adapter.notifyDataSetChanged();
    }

    private void showAddCustomerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.dialog_add_customer);
        builder.setTitle("Add New Customer");

        View customLayout = getLayoutInflater().inflate(R.layout.dialog_add_customer, null);
        builder.setView(customLayout);

        EditText nameInput = customLayout.findViewById(R.id.customerNameInput);
        EditText amountInput = customLayout.findViewById(R.id.customerAmountInput);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = nameInput.getText().toString();
            String amountString = amountInput.getText().toString();

            if (!name.isEmpty() && !amountString.isEmpty()) {
                try {
                    int amount = Integer.parseInt(amountString);
                    dbHelper.addCustomer(name, String.valueOf(amount));
                    refreshList();
                } catch (NumberFormatException e) {
                    Toast.makeText(HomeDetailsActivity.this, "Invalid amount format", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(HomeDetailsActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showEditCustomerDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setTitle("Edit Customer");

        View customLayout = getLayoutInflater().inflate(R.layout.dialog_edit_customer, null);
        builder.setView(customLayout);

        EditText nameInput = customLayout.findViewById(R.id.customerNameInput);
        EditText amountInput = customLayout.findViewById(R.id.customerAmountInput);

        String customerName = customers.get(position);
        String amount = dbHelper.getCustomerAmount(customerName);

        nameInput.setText(customerName);
        amountInput.setText(amount);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newName = nameInput.getText().toString();
            String newAmount = amountInput.getText().toString();

            if (!newName.isEmpty() && !newAmount.isEmpty()) {
                dbHelper.updateCustomer(customerName, newName, newAmount);
                refreshList();
            } else {
                Toast.makeText(HomeDetailsActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDeleteCustomerDialog(int position) {
        String customerName = customers.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Customer");
        builder.setMessage("Are you sure you want to delete " + customerName + "?");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            dbHelper.deleteCustomer(customerName);
            refreshList();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
