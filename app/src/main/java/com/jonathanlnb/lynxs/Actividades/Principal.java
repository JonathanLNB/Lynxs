package com.jonathanlnb.lynxs.Actividades;

import android.Manifest;
import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jonathanlnb.lynxs.Fragmentos.Vinculacion;
import com.jonathanlnb.lynxs.Herramientas.Tools;
import com.jonathanlnb.lynxs.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class Principal extends FragmentActivity implements OnMapReadyCallback, Vinculacion.OnFragmentInteractionListener {

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                pairedDeviceArrayList.add(device);
            }
        }
    };

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int ID_SERVICIO_JOB = 101;
    private final String UUID_STRING_WELL_KNOWN_SPP = "00001101-0000-1000-8000-00805F9B34FB";
    public JobInfo jobInfo;
    private FragmentManager fragmentManager = getSupportFragmentManager();
    private FragmentTransaction fragmentTransaction;
    private CountDownTimer contador = null;
    private BluetoothAdapter bluetoothAdapter;
    private GoogleMap mMap;
    private ArrayList<BluetoothDevice> pairedDeviceArrayList;
    private ArrayAdapter<BluetoothDevice> pairedDeviceAdapter;
    private SharedPreferences sharedPreferences;
    private UUID myUUID;

    private static ThreadConnectBTdevice myThreadConnectBTdevice;
    private ThreadConnected myThreadConnected;
    private String contactosArray[];
    private String contactos;
    private LatLng latLng;
    private float zoom = 15f;
    private boolean entro = false, entroL = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        ButterKnife.bind(this);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this,
                    "FEATURE_BLUETOOTH NOT support",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //using the well-known SPP UUID
        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this,
                    "Bluetooth is not supported on this hardware platform",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        sharedPreferences = getSharedPreferences(getString(R.string.shareKey), MODE_PRIVATE);
        contactos = sharedPreferences.getString("contactos", "");
        contactosArray = contactos.split("%");
        contador = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                System.out.println(contactos);
                for (int i = 0; i < contactosArray.length; i++) {
                    System.out.println("Hola: " + contactosArray[i].split("=")[0] + " " + contactosArray[i].split("=")[1]);
                    SmsManager sms = SmsManager.getDefault();
                    sms.sendTextMessage(contactosArray[i].split("=")[1], null, "¡" + contactosArray[i].split("=")[0] + " ayuda! está es mi ubicación " + latLng.latitude + ", " + latLng.longitude, null, null);
                }
            }
        };
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Turn ON BlueTooth if it is OFF
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        setup();
    }

    private void setup() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        bluetoothAdapter.startDiscovery();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        if (pairedDevices.size() > 0) {
            pairedDeviceArrayList = new ArrayList<BluetoothDevice>();

            for (BluetoothDevice device : pairedDevices) {
                pairedDeviceArrayList.add(device);
            }

            fragmentTransaction = fragmentManager.beginTransaction();
            Vinculacion d = new Vinculacion();
            fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
            fragmentTransaction.replace(R.id.lFragment, d).commit();

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            endJob();
        }
        if (myThreadConnectBTdevice != null) {
            myThreadConnectBTdevice.cancel();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                setup();
            } else {
                Toast.makeText(this,
                        "BlueTooth NOT enabled",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startThreadConnected(BluetoothSocket socket) {

        myThreadConnected = new ThreadConnected(socket);
        myThreadConnected.start();
    }

    public ArrayList<BluetoothDevice> getDispositivos() {
        return pairedDeviceArrayList;
    }

    public void setDispositivo(BluetoothDevice device) {
        myThreadConnectBTdevice = new ThreadConnectBTdevice(device);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            execJob();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void execJob() {
        ComponentName componentName = new ComponentName(Principal.this, MJobScheduler.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            jobInfo = new JobInfo.Builder(ID_SERVICIO_JOB, componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(false)
                    .setMinimumLatency(5 * 1000)
                    .build();
        } else {
            jobInfo = new JobInfo.Builder(ID_SERVICIO_JOB, componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(false)
                    .setPeriodic(5 * 1000)
                    .build();
        }
        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        int resultado = scheduler.schedule(jobInfo);
        if (resultado == JobScheduler.RESULT_SUCCESS) {
            Log.println(Log.ASSERT, "0000", "job success");
        } else {
            Log.println(Log.ASSERT, "0000", "job fail");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void endJob() {
        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        scheduler.cancel(ID_SERVICIO_JOB);
        Log.println(Log.ASSERT, "0000", "job terminado");
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    private class ThreadConnectBTdevice extends Thread {

        private BluetoothSocket bluetoothSocket = null;
        private final BluetoothDevice bluetoothDevice;


        private ThreadConnectBTdevice(BluetoothDevice device) {
            bluetoothDevice = device;

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void run() {
            boolean success = false;
            try {
                bluetoothSocket.connect();
                success = true;
            } catch (IOException e) {
                e.printStackTrace();

                final String eMessage = e.getMessage();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(Principal.this, "something wrong bluetoothSocket.connect(): \n" + eMessage, Toast.LENGTH_LONG);
                    }
                });

                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            if (success) {
                //connect successful
                final String msgconnected = "Dispositivo Conectado";

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(Principal.this, msgconnected, Toast.LENGTH_LONG).show();
                    }
                });

                startThreadConnected(bluetoothSocket);

            } else {
                Tools.showMessage(Principal.this, "Error");
                //fail
            }
        }

        public void cancel() {

            Toast.makeText(getApplicationContext(),
                    "close bluetoothSocket",
                    Toast.LENGTH_LONG).show();

            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }

    private class ThreadConnected extends Thread {
        private final BluetoothSocket connectedBluetoothSocket;
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;

        public ThreadConnected(BluetoothSocket socket) {
            connectedBluetoothSocket = socket;
            InputStream in = null;
            OutputStream out = null;

            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            String strRx = "";

            while (true) {
                try {
                    bytes = connectedInputStream.read(buffer);
                    final String strReceived = new String(buffer, 0, bytes);
                    final String strByteCnt = String.valueOf(bytes) + " bytes received.\n";

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (strReceived.equalsIgnoreCase("C")) {
                                contador.start();
                            } else {
                                if (contador != null)
                                    contador.cancel();
                            }
                        }
                    });

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();

                    final String msgConnectionLost = "Connection lost:\n"
                            + e.getMessage();
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(Principal.this, msgConnectionLost, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                connectedBluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static class MJobScheduler extends JobService {
        private static final int HANDLER_DELAY = 1000 * 25 * 1;
        private final String TAG = MJobScheduler.class.getSimpleName();
        Handler handler;
        boolean isWorking = false;
        private Context context;
        private Thread workerThread = null;

        @Override

        public void onCreate() {
            super.onCreate();
            context = getApplicationContext();
        }

        @Override
        public boolean onStartJob(final JobParameters params) {
            Log.println(Log.ASSERT, "empezo0000", "Job started0000");
            doWork(params);
            return true;
        }


        private void doWork(final JobParameters jobParameters) {
            if (workerThread == null || !workerThread.isAlive()) {
                workerThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.println(Log.ASSERT, "service000", "Intent recived000");

                        handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            public void run() {

                                myThreadConnectBTdevice.start();

                                handler.postDelayed(this, HANDLER_DELAY);
                            }
                        }, 0);
                        Log.println(Log.ASSERT, "termino", "Job finished!");
                        jobFinished(jobParameters, true);
                    }
                });
                workerThread.run();
            }
        }

        @Override
        public boolean onStopJob(JobParameters params) {
            Log.d(TAG, "Job cancelled before being completed.");
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
        } else {
            locationStart();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void locationStart() {
        LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Localizacion Local = new Localizacion();
        final boolean gpsEnabled = mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsEnabled) {
            if (!entroL) {
                Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(settingsIntent);
                entroL = true;
            } else {
                Tools.showMessage(this, getString(R.string.no_gps));
            }
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
            return;
        }
        mlocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 100, (LocationListener) Local);
        mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 100, (LocationListener) Local);

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1000) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationStart();
                return;
            }
        }
    }

    public void setLocation(Location loc) {
        //Obtener la direccion de la calle a partir de la latitud y la longitud
        if (loc.getLatitude() != 0.0 && loc.getLongitude() != 0.0) {
            latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
            //Aqui va lo bueno
        }
    }

    /* Aqui empieza la Clase Localizacion */
    public class Localizacion implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            // Este metodo se ejecuta cada vez que el GPS recibe nuevas coordenadas
            // debido a la deteccion de un cambio de ubicacion
            if (!entro) {
                setLocation(loc);
                entro = true;
            }

        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.AVAILABLE:
                    Log.d("debug", "LocationProvider.AVAILABLE");
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    Log.d("debug", "LocationProvider.OUT_OF_SERVICE");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.d("debug", "LocationProvider.TEMPORARILY_UNAVAILABLE");
                    break;
            }
        }
    }

    @OnClick(R.id.bAgregarContacto)
    public void agregarContacto() {
        Intent i = new Intent(this, AgregarContacto.class);
        startActivity(i);
    }

}
