package com.example.iotdemo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iotdemo.models.Product;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ActualCart extends AppCompatActivity implements ProductAdapter.RemoveProductListener {

    private TextView textViewTotalAmount;
    private RecyclerView recyclerViewProducts;
    private ProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();
    private double totalAmount = 0.0;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actual_cart);

        textViewTotalAmount = findViewById(R.id.textViewTotalAmount);
        recyclerViewProducts = findViewById(R.id.recyclerViewProducts);
        recyclerViewProducts.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the ProductAdapter with a listener
        productAdapter = new ProductAdapter(this, productList, this);
        recyclerViewProducts.setAdapter(productAdapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("products");

        // Receive scanned ID from intent
        String scannedId = getIntent().getStringExtra("SCANNED_ID");
//        String scannedId = "2";
        if (scannedId != null) {
            fetchProductData(scannedId);
        }

        // Set up UPI payment button
        MaterialButton buttonUPI = findViewById(R.id.buttonUPI);
        buttonUPI.setOnClickListener(v -> initiateUPIPayment());
    }

    private void fetchProductData(String scannedId) {
        databaseReference.child(scannedId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                handleProductData(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ActualCart.this, "Error fetching data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleProductData(DataSnapshot dataSnapshot) {
        String name = dataSnapshot.child("name").getValue(String.class);
        String priceString = dataSnapshot.child("price").getValue(String.class);
        String weightString = dataSnapshot.child("weight").getValue(String.class);

        if (name != null && priceString != null) {
            try {
                double price = Double.parseDouble(priceString);
                double weight = weightString != null ? Double.parseDouble(weightString) : 0.0;

                Product product = new Product(name, priceString, weightString);
                productList.add(product);
                productAdapter.notifyDataSetChanged();
                updateTotalAmount(price);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price or weight format", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Product details are incomplete", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTotalAmount(double price) {
        totalAmount += price;
        textViewTotalAmount.setText("Total Amount: ₹" + String.format("%.2f", totalAmount));
    }

    @Override
    public void onProductRemoved(double price) {
        totalAmount -= price;
        textViewTotalAmount.setText("Total Amount: ₹" + String.format("%.2f", totalAmount));
    }

    private void initiateUPIPayment() {
        String amount = String.format("%.2f", totalAmount);
        String upiId = "mayuriphad5@oksbi"; // Replace with your UPI ID

        Uri uri = new Uri.Builder()
                .scheme("upi")
                .authority("pay")
                .appendQueryParameter("pa", upiId) // Payee UPI ID
                .appendQueryParameter("pn", "Receiver Name") // Optional: Payee Name
                .appendQueryParameter("mc", "") // Optional: Merchant Code
                .appendQueryParameter("tid", "") // Optional: Transaction ID
                .appendQueryParameter("tt", "02") // Optional: Transaction Type
                .appendQueryParameter("am", amount) // Amount
                .appendQueryParameter("cu", "INR") // Currency Code
                .appendQueryParameter("url", "") // Optional: URL
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        if (amount.equals("0.00")) {
            Toast.makeText(this, "No items in the cart", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Total Amount: ₹" + amount, Toast.LENGTH_SHORT).show();
            startActivityForResult(intent, 1);
        }
    }
}
