package com.example.iotdemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class WifiConnect extends AppCompatActivity {

    private WebSocketClient webSocketClient;
    private TextView connectingText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_connect);

        connectingText = findViewById(R.id.connecting_text);
        startBreathingAnimation();

        // Replace with your ESP32 IP address and port
        String esp32IP = "ws://192.168.4.1:8080";
        connectWebSocket(esp32IP);
    }

    private void startBreathingAnimation() {
        Animation breathingAnimation = AnimationUtils.loadAnimation(this, R.anim.breathing_animation);
        connectingText.startAnimation(breathingAnimation);
    }

    private void connectWebSocket(String uri) {
        try {
            URI websocketURI = new URI(uri);
            webSocketClient = new WebSocketClient(websocketURI) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    runOnUiThread(() -> {
                        Toast.makeText(WifiConnect.this, "Connected to ESP32", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onMessage(String message) {
                    runOnUiThread(() -> {
                        Toast.makeText(WifiConnect.this, "Received: " + message, Toast.LENGTH_LONG).show();
                        // Check if the message contains the scanned ID
                        if (message.contains("scanned_id")) { // Adjust condition based on your data format
                            Intent intent = new Intent(WifiConnect.this, ActualCart.class);
                            intent.putExtra("scanned_id", message);
                            startActivity(intent);
                        }
                    });
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    runOnUiThread(() -> {
                        Toast.makeText(WifiConnect.this, "Connection Closed: " + reason, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(Exception ex) {
                    runOnUiThread(() -> {
                        Toast.makeText(WifiConnect.this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            };
            webSocketClient.connect();
        } catch (Exception e) {
            Toast.makeText(this, "Error connecting to WebSocket: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }
}
