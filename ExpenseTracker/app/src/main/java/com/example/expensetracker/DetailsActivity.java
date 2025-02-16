package com.example.expensetracker;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DetailsActivity extends AppCompatActivity {

    TextView customerDetailsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        customerDetailsTextView = findViewById(R.id.customerDetailsTextView);

        // Retrieve and display customer details from intent extras
        String customerDetails = getIntent().getStringExtra("customer");
        if (customerDetails != null) {
            customerDetailsTextView.setText(customerDetails);
        }
    }
}
