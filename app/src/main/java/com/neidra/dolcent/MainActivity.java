package com.neidra.dolcent;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;
import com.neidra.dolcent.utils.MediaStoreLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ApiService apiService;
    private Retrofit retrofit;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        retrofit = RClient.getRetrofitInstance();
        apiService = retrofit.create(ApiService.class);

        webView = findViewById(R.id.webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        MainWebViewClient viewClient = new MainWebViewClient();
        webView.setWebViewClient(viewClient);

//        webView.loadUrl("https://food.tukir.biz.id/");
        webView.loadUrl("https://dolcent.neidra.my.id/");

    }

    public static String formatRupiah(String angka, String prefix) {
        // Remove all non-digit and non-comma characters
        String numberString = angka.replaceAll("[^,\\d]", "");
        String[] split = numberString.split(",");
        int sisa = split[0].length() % 3;
        String rupiah = split[0].substring(0, sisa);
        String ribuan = split[0].substring(sisa);

        // Use regex to find all groups of 3 digits
        StringBuilder ribuanBuilder = new StringBuilder();
        for (int i = 0; i < ribuan.length(); i += 3) {
            if (ribuanBuilder.length() > 0) {
                ribuanBuilder.append(".");
            }
            ribuanBuilder.append(ribuan.substring(i, Math.min(i + 3, ribuan.length())));
        }

        if (ribuanBuilder.length() > 0) {
            String separator = sisa > 0 ? "." : "";
            rupiah += separator + ribuanBuilder.toString();
        }

        // Append the decimal part if it exists
        if (split.length > 1) {
            rupiah += "," + split[1];
        }

        // Return the formatted string with prefix if provided
        return prefix == null ? rupiah : "Rp. " + rupiah;
    }

    private class MainWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            Uri uri = request.getUrl();
            Log.d(TAG, "request: " + request);
            Log.d(TAG, "url: " + url);

            if(url.startsWith("intent://cetak-struk")){
                String id = uri.getQueryParameter("id");
                Log.d(TAG, "masok: " + id);
                getDataStruk(id);

                return true;
            }

            if (url.startsWith("intent://list-bluetooth-device")){
                Log.d(TAG, "masok pilih: ");
                browseBluetoothDevice();
                return true;
            }
            return false;

        }
    }

    public void getDataStruk(String id){
        Call<GetStruk> call = apiService.getStruk(id);
        call.enqueue(new Callback<GetStruk>() {
            @Override
            public void onResponse(Call<GetStruk> call, Response<GetStruk> response) {

                Log.d(TAG, "onResponse: " + id);
                Penjualan penjualan = response.body().getPenjualan();
                List<Detail> detail = response.body().getDetail();


                Toast.makeText(MainActivity.super.getApplicationContext(), response.body().toString(), Toast.LENGTH_LONG).show();
                String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
                MediaStoreLog.append(MainActivity.super.getApplicationContext(), ts + " [API_GET_STRUK_SUCCESS] " + response.body().toString());
                printBluetooth(penjualan, detail);
            }

            @Override
            public void onFailure(Call<GetStruk> call, Throwable t) {
                Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
                String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
                MediaStoreLog.append(MainActivity.super.getApplicationContext(), ts + " [API_GET_STRUK_FAILURE] " + t.getMessage());
            }
        });
    }

    /*==============================================================================================
    ======================================BLUETOOTH PART============================================
    ==============================================================================================*/

    public interface OnBluetoothPermissionsGranted {
        void onPermissionsGranted();
    }

    public static final int PERMISSION_BLUETOOTH = 1;
    public static final int PERMISSION_BLUETOOTH_ADMIN = 2;
    public static final int PERMISSION_BLUETOOTH_CONNECT = 3;
    public static final int PERMISSION_BLUETOOTH_SCAN = 4;

    public OnBluetoothPermissionsGranted onBluetoothPermissionsGranted;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case MainActivity.PERMISSION_BLUETOOTH:
                case MainActivity.PERMISSION_BLUETOOTH_ADMIN:
                case MainActivity.PERMISSION_BLUETOOTH_CONNECT:
                case MainActivity.PERMISSION_BLUETOOTH_SCAN:
                    this.checkBluetoothPermissions(this.onBluetoothPermissionsGranted);
                    break;
            }
        }
    }

    public void checkBluetoothPermissions(OnBluetoothPermissionsGranted onBluetoothPermissionsGranted) {
        this.onBluetoothPermissionsGranted = onBluetoothPermissionsGranted;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH}, MainActivity.PERMISSION_BLUETOOTH);
        } else if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_ADMIN}, MainActivity.PERMISSION_BLUETOOTH_ADMIN);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, MainActivity.PERMISSION_BLUETOOTH_CONNECT);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, MainActivity.PERMISSION_BLUETOOTH_SCAN);
        } else {
            this.onBluetoothPermissionsGranted.onPermissionsGranted();
        }
    }

    private BluetoothConnection selectedDevice;

    public void browseBluetoothDevice() {
        this.checkBluetoothPermissions(() -> {
            final BluetoothConnection[] bluetoothDevicesList = (new BluetoothPrintersConnections()).getList();

            if (bluetoothDevicesList != null) {
                final String[] items = new String[bluetoothDevicesList.length + 1];
                items[0] = "Default printer";
                int i = 0;
                for (BluetoothConnection device : bluetoothDevicesList) {
                    items[++i] = device.getDevice().getName();
                }

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                alertDialog.setTitle("Bluetooth printer selection");
                alertDialog.setItems(
                        items,
                        (dialogInterface, i1) -> {
                            int index = i1 - 1;
                            if (index == -1) {
                                selectedDevice = null;
                            } else {
                                selectedDevice = bluetoothDevicesList[index];
                            }
//                            Button button = (Button) findViewById(R.id.button_bluetooth_browse);
//                            button.setText(items[i1]);
                        }
                );

                AlertDialog alert = alertDialog.create();
                alert.setCanceledOnTouchOutside(false);
                alert.show();
            }
        });

    }

    public void printBluetooth(Penjualan penjualan, List<Detail> details) {
        this.checkBluetoothPermissions(() -> {
            new AsyncBluetoothEscPosPrint(
                    this,
                    new AsyncEscPosPrint.OnPrintFinished() {
                        @Override
                        public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                            Log.e("Async.OnPrintFinished", "AsyncEscPosPrint.OnPrintFinished : An error occurred !");
                            Toast.makeText(MainActivity.super.getApplicationContext(), String.valueOf(codeException), Toast.LENGTH_LONG).show();
                            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
                            MediaStoreLog.append(MainActivity.super.getApplicationContext(), ts + " [BT_PRINTER_BLUETOOTH_ERROR] " + String.valueOf(codeException));
                        }

                        @Override
                        public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                            Log.i("Async.OnPrintFinished", "AsyncEscPosPrint.OnPrintFinished : Print is finished !");
                        }
                    }
            )
                    .execute(this.getAsyncEscPosPrinter(selectedDevice, penjualan, details));
        });
    }

    @SuppressLint("SimpleDateFormat")
    public AsyncEscPosPrinter getAsyncEscPosPrinter(DeviceConnection printerConnection, Penjualan penjualan, List<Detail> detail) {
        String item = "";
        for (Detail data : detail){
//            item += "[L]<b>"+ data.getQty() +"</b>[C]<b>" +data.getBarang()+"</b>[R]<b>"+ data.getTotal() +"</b>\n";
            item += "[L]<b>"+ data.getQty() +"</b><b>" +data.getBarang()+"</b>[R]<b>"+ formatRupiah(String.valueOf(data.getTotal()), "Rp. ") +"</b>\n";
        }

        String pelanggan = penjualan.getPelanggan() != null ? penjualan.getPelanggan() : "-";
        SimpleDateFormat format = new SimpleDateFormat("'on' yyyy-MM-dd 'at' HH:mm:ss");

        String discount = "0";
        if(penjualan.getId_discount() != null){

            if(penjualan.getTipe() == "1"){ //jika discount persentase
                int nominalDiscount = Integer.parseInt(penjualan.getSubtotal()) - Integer.parseInt(penjualan.getTotal());
                discount = String.valueOf(nominalDiscount);
            }else{ //jika discount nominal langsung
                discount = penjualan.getJumlah();
            }
        }

        String biayaLayanan = penjualan.getBiaya_layanan() != null ? penjualan.getBiaya_layanan() : "0";

        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 203, 48f, 32);
        return printer.addTextToPrint(
                "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, this.getApplicationContext().getResources().getDrawableForDensity(R.drawable.logo, DisplayMetrics.DENSITY_MEDIUM)) + "</img>\n" +
                        "[L]\n" +
                        "[C]<font size='big'>STRUK PENJUALAN</font>\n" +
                        "[C]<font size='wide'>DOLCENT</font>\n" +
                        "[C]" + penjualan.getKasir() + "\n" +
                        "[C]" + penjualan.getNohp() + "\n" +
                        "[C]" + penjualan.getAlamat() + "\n" +
                        "[L]\n" +
                        "[C]--------------------------------\n" +
                        "[C]\n" +
                        "[C]<u type='double'>" + format.format(new Date()) + "</u>\n" +
                        "[C]\n" +
                        "[C]================================\n" +
                        "[L]\n" +
                        "[L]<b>QTY</b>[C]<b>ITEM</b>[R]<b>TOTAL</b>\n" +
                        "[C]--------------------------------\n" +
                        item +
                        "[L]\n" +
                        "[C]--------------------------------\n" +
                        "[L]HARGA :[R]" + formatRupiah(String.valueOf(penjualan.getSubtotal()), "Rp. ") + "\n" +
                        "[L]Discount :[R]" + formatRupiah(discount, "Rp. ") + "\n" +
                        "[L]PPN :[R]" + formatRupiah(penjualan.getPpn(), "Rp. ") + "\n" +
                        "[L]Biaya Layanan :[R]" + formatRupiah(biayaLayanan, "Rp. ") + "\n" +
                        "[L]Total :[C][R]Rp. " + penjualan.getTotal() + "\n" +
                        "[C]================================\n" +
                        "[L]\n" +
                        "[L]<b>CUSTOMER</b>[C][R]" + pelanggan + "\n" +
                        "[L]<b>PEMBAYARAN</b>[C][R]" + penjualan.getNama_tipe() + "\n" +
                        "\n" +
                        "[C]--------------------------------\n" +
                        "[C]<font size='big'>TERIMA KASIH</font>\n"
        );
    }


    @Override
    public void onBackPressed() {
        if (webView.canGoBack()){
            webView.goBack();
        }else{
            super.onBackPressed();
        }
    }
}