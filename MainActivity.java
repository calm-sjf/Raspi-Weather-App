package com.example.piotrskorupa.raspiweatherapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.example.piotrskorupa.raspiweatherapp.R.drawable.camera_error;

public class MainActivity extends AppCompatActivity implements WeatherServiceCallback{

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    public static double temperatureMain = 0.0, pressureMain = 0.0, humidityMain = 0.0;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS =0 ;
    public static String location;
    public static final String message = "Hi that is the weather in my place ";
    private String phoneNumber ="";
    private int PICK_CONTACT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent contactList = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                contactList.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                startActivityForResult(contactList, PICK_CONTACT);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PICK_CONTACT) {
            try {
                Uri contactData = data.getData();
                Cursor c = getContentResolver().query(contactData,null,null,null, null);

                if (resultCode == RESULT_OK) {
                    while (c.moveToNext()) {
                        phoneNumber = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));

                        if (pressureMain == 0 || humidityMain == 0){
                            throw new Exception("bad data from sensor");
                        }

                        String fullMessage = message + location + ": temperature " + temperatureMain + " C, pressure "
                                + pressureMain + " hPa, humidity " + humidityMain + " %.";

                        if (ContextCompat.checkSelfPermission(this,Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED){
                            SmsManager smsManager = SmsManager.getDefault();
                            smsManager.sendTextMessage(phoneNumber, null, fullMessage, null, null);

                            Toast.makeText(this, "SMS has been sent to: " + phoneNumber, Toast.LENGTH_SHORT).show();
                            phoneNumber = "";
                        }else{
                            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
                        }
                    }
                }
            }catch (Exception e){
                Toast.makeText(this, "SMS wasn't send", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settings = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settings);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void serviceSucces(Channel channel) {


        double tempInFarenheit = new Double(channel.getCondition().getTemperature());
        double tempInCelsjus = (tempInFarenheit - 32.0) / 1.800;
        Double temp = BigDecimal.valueOf(tempInCelsjus)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
        String description = channel.getCondition().getDescription();
        PlaceholderFragment.temperatureEdit2.setText(temp + " C. " + description);

        int pressure, humidity, windSpeed;
        humidity = channel.getAtmosphere().getHumidity();
        windSpeed = channel.getWind().getSpeed();

        pressure = channel.getAtmosphere().getPressure();
        pressure *= 33.8638; // pressure from inches to hpa

        PlaceholderFragment.pressureEdit2.setText(pressure + " hPa");
        PlaceholderFragment.humidityEdit2.setText(humidity + " %");
        PlaceholderFragment.windEdit.setText(windSpeed + "km/h");
    }

    @Override
    public void serviceFailure(Exception exception) {
        Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public static MqttConnector mqttConnector;

        public static EditText temperatureEdit, temperatureEdit2;
        public static EditText pressureEdit, pressureEdit2;
        public static EditText humidityEdit, humidityEdit2, windEdit;

        //private YahooWeatherService yahooService;

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            if (getArguments().getInt(ARG_SECTION_NUMBER) == 1)
            {
                View rootView = inflater.inflate(R.layout.fragment_main, container, false);
                temperatureEdit = (EditText) rootView.findViewById(R.id.temperature_edit_text);
                pressureEdit = (EditText) rootView.findViewById(R.id.pressure_edit_text);
                humidityEdit = (EditText) rootView.findViewById(R.id.humidity_edit_text);

                Button cameraButton = (Button) rootView.findViewById(R.id.camera_button);

                temperatureEdit.setEnabled(false);
                pressureEdit.setEnabled(false);
                humidityEdit.setEnabled(false);

                try {
                    startMqtt();

                } catch (MqttException e) {
                    e.printStackTrace();
                    Snackbar.make(getView(), "Something go wrong :(",
                            Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }

                cameraButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        mqttConnector.publishCamOnOff(true);
                        Intent camera = new Intent(getActivity(), CameraActivity.class);
                        startActivity(camera);
                    }
                });

                return rootView;
            }else{
                View rootView = inflater.inflate(R.layout.fragment_yahoo, container, false);

                final SharedPreferences[] sharedPref = {PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext())};
                String city = sharedPref[0].getString("city", "Siechnice").toString();
                String country = sharedPref[0].getString("country", "Poland").toString();

                location =  city + "," + country;

                final TextView locationText = (TextView) rootView.findViewById(R.id.location_text);
                temperatureEdit2 = (EditText) rootView.findViewById(R.id.temperature_edit_text2);
                pressureEdit2 = (EditText) rootView.findViewById(R.id.pressure_edit_text2);
                humidityEdit2 = (EditText) rootView.findViewById(R.id.humidity_edit_text2);
                windEdit = (EditText) rootView.findViewById(R.id.wind_edit_text);

                locationText.setText(location);

                temperatureEdit2.setEnabled(false);
                pressureEdit2.setEnabled(false);
                humidityEdit2.setEnabled(false);
                windEdit.setEnabled(false);

                final YahooWeatherService[] yahooService = {new YahooWeatherService((WeatherServiceCallback) getActivity(), location)};
                try{
                    yahooService[0].refreshWeather();
                }catch (Exception e){
                    Toast.makeText(getActivity(), "Bad location!", Toast.LENGTH_SHORT);
                }

                Button refresh = (Button) rootView.findViewById(R.id.button_refresh);
                refresh.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        sharedPref[0] = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                        String city = sharedPref[0].getString("city", "Siechnice").toString();
                        String country = sharedPref[0].getString("country", "Poland").toString();

                        location =  city + "," + country;
                        locationText.setText(location);
                        try{
                            yahooService[0] = new YahooWeatherService((WeatherServiceCallback) getActivity(), location);
                            yahooService[0].refreshWeather();
                        }catch (Exception e){
                            Toast.makeText(getActivity(), "Bad location!", Toast.LENGTH_SHORT);
                        }

                    }
                });

                return rootView;
            }

        }

        private void startMqtt() throws MqttException {
            mqttConnector = new MqttConnector(getActivity().getApplicationContext());
            mqttConnector.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean b, String s) {

                }

                @Override
                public void connectionLost(Throwable throwable) {

                }

                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                    Log.w("Debug", mqttMessage.toString());
                    if (topic.toString().equals("SENSORS/PRESSURE")) {
                        pressureEdit.setText(mqttMessage.toString() + " hPa");
                        pressureMain = Double.parseDouble(mqttMessage.toString());
                    }
                    else if (topic.toString().equals("SENSORS/TEMPERATURE")){
                        temperatureEdit.setText(mqttMessage.toString() + " C");
                        temperatureMain = Double.parseDouble(mqttMessage.toString());
                    }
                    else if (topic.toString().equals("SENSORS/HUMIDITY"))
                    {
                        humidityEdit.setText(mqttMessage.toString() + " %");
                        humidityMain = Double.parseDouble(mqttMessage.toString());
                    }
                    else if (topic.toString().equals("SENSORS/CAMERA_PIC")) {
                        String imageString = mqttMessage.toString();
                        if (imageString.equals("CAMERA_ERROR")){
                            CameraActivity.cameraImage.setImageResource(R.drawable.camera_error);
                        }
                        else{
                            byte[] decodeString = Base64.decode(imageString, Base64.DEFAULT);
                            Bitmap decoded = BitmapFactory.decodeByteArray(decodeString, 0, decodeString.length);
                            CameraActivity.cameraImage.setImageBitmap(decoded);
                        }

                        Log.d("message", "I have got an message from camera. Should i have ?");
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                }
            });
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "SECTION 1";
                case 1:
                    return "SECTION 2";
            }
            return null;
        }
    }
}
