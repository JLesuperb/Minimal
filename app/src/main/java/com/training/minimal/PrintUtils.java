package com.training.minimal;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PrintUtils {

    private static String[] binaryArray = { "0000", "0001", "0010", "0011",
            "0100", "0101", "0110", "0111", "1000", "1001", "1010", "1011",
            "1100", "1101", "1110", "1111" };

    public static Bitmap encodeToQrCode(String barcode) {
        QRCodeWriter writer = new QRCodeWriter();
        Bitmap bmp = null;
        try {
            BitMatrix matrix = writer.encode(barcode, BarcodeFormat.QR_CODE, 250, 250);
            bmp = Bitmap.createBitmap(250, 250, Bitmap.Config.RGB_565);
            for (int x = 0; x < 250; x++){
                for (int y = 0; y < 250; y++){
                    bmp.setPixel(x, y, matrix.get(x,y) ? Color.BLACK : Color.WHITE);
                }
            }
        } catch (WriterException ex) {
            //
        }
        return bmp;
    }

    @Nullable
    public static byte[] decodeBitmap(@NotNull Bitmap bmp){
        int bmpWidth = bmp.getWidth();
        int bmpHeight = bmp.getHeight();

        List<String> list = new ArrayList<>();
        StringBuffer sb;


        //int bitLen = bmpWidth / 8;
        int zeroCount = bmpWidth % 8;

        StringBuilder zeroStr = new StringBuilder();
        if (zeroCount > 0) {
            for (int i = 0; i < (8 - zeroCount); i++) {
                zeroStr.append("0");
            }
        }

        for (int i = 0; i < bmpHeight; i++) {
            sb = new StringBuffer();
            for (int j = 0; j < bmpWidth; j++) {
                int color = bmp.getPixel(j, i);

                int r = (color >> 16) & 0xff;
                int g = (color >> 8) & 0xff;
                int b = color & 0xff;

                // if color close to white，bit='0', else bit='1'
                if (r > 160 && g > 160 && b > 160)
                    sb.append("0");
                else
                    sb.append("1");
            }
            if (zeroCount > 0) {
                sb.append(zeroStr);
            }
            list.add(sb.toString());
        }

        List<String> bmpHexList = binaryListToHexStringList(list);
        String commandHexString = "1D763000";
        String widthHexString = Integer
                .toHexString(bmpWidth % 8 == 0 ? bmpWidth / 8
                        : (bmpWidth / 8 + 1));
        if (widthHexString.length() > 2) {
            Log.e("decodeBitmap error", " width is too large");
            return null;
        } else if (widthHexString.length() == 1) {
            widthHexString = "0" + widthHexString;
        }
        widthHexString = widthHexString + "00";

        String heightHexString = Integer.toHexString(bmpHeight);
        if (heightHexString.length() > 2) {
            Log.e("decodeBitmap error", " height is too large");
            return null;
        } else if (heightHexString.length() == 1) {
            heightHexString = "0" + heightHexString;
        }
        heightHexString = heightHexString + "00";

        List<String> commandList = new ArrayList<>();
        commandList.add(commandHexString+widthHexString+heightHexString);
        commandList.addAll(bmpHexList);

        return hexList2Byte(commandList);
    }

    @NotNull
    private static List<String> binaryListToHexStringList(@NotNull List<String> list) {
        List<String> hexList = new ArrayList<>();
        for (String binaryStr : list) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < binaryStr.length(); i += 8) {
                String str = binaryStr.substring(i, i + 8);

                String hexString = myBinaryStrToHexString(str);
                sb.append(hexString);
            }
            hexList.add(sb.toString());
        }
        return hexList;

    }

    @NotNull
    private static String myBinaryStrToHexString(@NotNull String binaryStr) {
        StringBuilder hex = new StringBuilder();
        String f4 = binaryStr.substring(0, 4);
        String b4 = binaryStr.substring(4, 8);
        String hexStr = "0123456789ABCDEF";
        for (int i = 0; i < binaryArray.length; i++) {
            if (f4.equals(binaryArray[i]))
                hex.append(hexStr.substring(i, i + 1));
        }
        for (int i = 0; i < binaryArray.length; i++) {
            if (b4.equals(binaryArray[i]))
                hex.append(hexStr.substring(i, i + 1));
        }

        return hex.toString();
    }

    @NotNull
    private static byte[] hexList2Byte(@NotNull List<String> list) {
        List<byte[]> commandList = new ArrayList<>();

        for (String hexStr : list) {
            commandList.add(hexStringToBytes(hexStr));
        }
        return sysCopy(commandList);
    }

    private static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    @NotNull
    private static byte[] sysCopy(@NotNull List<byte[]> srcArrays) {
        int len = 0;
        for (byte[] srcArray : srcArrays) {
            len += srcArray.length;
        }
        byte[] destArray = new byte[len];
        int destLen = 0;
        for (byte[] srcArray : srcArrays) {
            System.arraycopy(srcArray, 0, destArray, destLen, srcArray.length);
            destLen += srcArray.length;
        }
        return destArray;
    }

    public static void print(@NotNull BluetoothDevice device) throws IOException {
        BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        if(!socket.isConnected())socket.connect();
        OutputStream outputStream = socket.getOutputStream();

        outputStream.write("\n".getBytes());
        outputStream.flush();

        //Reset
        outputStream.write(0x1D);
        outputStream.write("B".getBytes());
        outputStream.write(0);
        outputStream.flush();

        //Center
        outputStream.write(0x1B);
        outputStream.write("a".getBytes());
        outputStream.write(1);
        outputStream.flush();

        //QrCode
        Bitmap bmp = encodeToQrCode("https://github.com/JLesuperb/Minimal");
        if (bmp != null ) {
            byte[] command = decodeBitmap(bmp);
            if(command!=null){
                outputStream.write(command);
                outputStream.flush();
            }
        }

        //Center
        outputStream.write(0x1B);
        outputStream.write("a".getBytes());
        outputStream.write(1);
        outputStream.flush();

        //Cut
        try {
            outputStream.write("\n".getBytes());
            outputStream.flush();
            outputStream.write(0x1D);
            outputStream.write("V".getBytes());
            outputStream.write(48);
            outputStream.write(0);
            outputStream.flush();
        } catch (Exception ignored){}

        socket.close();
    }
}
