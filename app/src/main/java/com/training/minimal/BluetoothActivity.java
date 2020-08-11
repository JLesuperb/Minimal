package com.training.minimal;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.textview.MaterialTextView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import dmax.dialog.SpotsDialog;

public class BluetoothActivity extends AppCompatActivity {

    private android.app.AlertDialog dialog;

    private ExecutorService es = Executors.newScheduledThreadPool(30);
    private DeviceAdapter adapter = new DeviceAdapter();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter.setOnClickListener(model -> {
            setLoading(true);
            es.submit(new OpenPrinter(model));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter!=null){
            if(!bluetoothAdapter.isEnabled()) bluetoothAdapter.enable();
            bluetoothAdapter.cancelDiscovery();
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            List<BluetoothDevice> models = new ArrayList<>(pairedDevices);
            adapter.add(models);
        }
    }

    private void setLoading(@org.jetbrains.annotations.NotNull Boolean isLoading){
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

        private BluetoothDevice model;

        OpenPrinter(BluetoothDevice model){
            this.model = model;
        }

        @Override
        public void run() {
            try{
                BluetoothSocket socket = model.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                if(!socket.isConnected())socket.connect();
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write("\n".getBytes());
                outputStream.flush();
                socket.close();
                new Handler(Looper.getMainLooper()).post(() -> {
                    setLoading(false);
                    Intent intent = new Intent();
                    intent.putExtra("serial_number",model.getAddress());
                    setResult(Activity.RESULT_OK,intent);
                    finish();
                    /*LocalData localData = new LocalData();
                    localData.setString("bluetooth_printer",model.getAddress());
                    NavHostFragment.findNavController(SettingBluetoothFragment.this).navigateUp();*/
                });
            } catch (IOException e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    setLoading(false);
                    Toast.makeText(getApplicationContext(),"Error:"+e.getMessage(),Toast.LENGTH_LONG).show();
                });
            }
        }
    }

    private static class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
        private OnClickListener onClickListener;
        private List<BluetoothDevice> models = new ArrayList<>();

        public void add(Collection<BluetoothDevice> collection){
            models.clear();
            models.addAll(collection);
            notifyDataSetChanged();
        }

        public void setOnClickListener(OnClickListener onClickListener) {
            this.onClickListener = onClickListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bleutooth, parent, false);
            return new ViewHolder(rootView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BluetoothDevice model = models.get(position);
            holder.nameTextView.setText(model.getName());
            holder.deviceTextView.setText(model.getAddress());
            holder.itemView.setOnClickListener(v -> {
                if(onClickListener!=null){
                    onClickListener.onClick(model);
                }
            });
        }

        @Override
        public int getItemCount() {
            return models.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            MaterialTextView nameTextView;
            MaterialTextView deviceTextView;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                nameTextView = itemView.findViewById(R.id.nameTextView);
                deviceTextView = itemView.findViewById(R.id.deviceTextView);
            }
        }

        interface OnClickListener{
            void onClick(BluetoothDevice model);
        }
    }

}
