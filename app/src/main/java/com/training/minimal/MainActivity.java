package com.training.minimal;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {

    private android.app.AlertDialog dialog;

    private ExecutorService es = Executors.newScheduledThreadPool(30);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.bluetoothButton).setOnClickListener(v ->
                startActivityForResult(new Intent(getApplicationContext(),BluetoothActivity.class),25)
        );

        findViewById(R.id.printButton).setOnClickListener(v -> {
            TextInputEditText bluetoothEditText = findViewById(R.id.bluetoothEditText);
            if(bluetoothEditText.getText()!=null){
                String text = bluetoothEditText.getText().toString();
                if(text.trim().isEmpty()){
                    return;
                }
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if(bluetoothAdapter!=null){
                    if(!bluetoothAdapter.isEnabled()) bluetoothAdapter.enable();
                    bluetoothAdapter.cancelDiscovery();
                    setLoading(true);
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(text);
                    es.submit(new OpenPrinter(device));
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==25 && resultCode== Activity.RESULT_OK){
            if(data!=null){
                String serialNumber = data.getStringExtra("serial_number");
                if(serialNumber!=null&&!serialNumber.isEmpty()){
                    TextInputEditText bluetoothEditText = findViewById(R.id.bluetoothEditText);
                    bluetoothEditText.setText(serialNumber);
                }
            }
        }
    }

    private void setLoading(@NotNull Boolean isLoading){
        if(isLoading) {
            dialog = new SpotsDialog.Builder().setContext(this).build();
            dialog.setCancelable(false);
            dialog.show();
        } else {
            if(dialog!=null){
                dialog.dismiss();
                dialog = null;
            }
        }
    }

    private class OpenPrinter implements Runnable {

        private final BluetoothDevice device;

        private OpenPrinter(BluetoothDevice device) {
            this.device = device;
        }

        @Override
        public void run() {
            try{
                PrintUtils.print(device);
                new Handler(Looper.getMainLooper()).post(() -> {
                    //
                    setLoading(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    setLoading(false);
                    Toast.makeText(getApplicationContext(),"Error:"+e.getMessage(),Toast.LENGTH_LONG).show();
                });
            }
        }
    }
}