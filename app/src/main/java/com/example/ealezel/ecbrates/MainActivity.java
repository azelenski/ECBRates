package com.example.ealezel.ecbrates;

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ealezel.ecbrates.classes.Rate;


import org.apache.commons.io.IOUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends ListActivity {
    private final String urlContent = "http://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";
    private final String internalFile = "rates.xml";
    public static final String PREFS_NAME = "SharedPrefRatesDate";
    public static String lastUpdateDate = null;
    public ArrayList<Rate> rates;

    private TextView textHeader;
    private SimpleDateFormat dateFormat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textHeader = (TextView) findViewById(R.id.mainText);

        SharedPreferences storedData = getSharedPreferences(PREFS_NAME, 0);
        lastUpdateDate = storedData.getString("lastUpdate", null);

        Date dateLast = StringToDateParse(lastUpdateDate);

        Date today = new Date();

        long dif = 2;
        if (dateLast != null) {
            dif = today.getHours() - dateLast.getHours();
        }

        // 1 day && isOnline
        if (dif > 24 && IsOnline()) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(urlContent);

                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setDoInput(true);
                        conn.setDoOutput(true);
                        InputStream inputStream = conn.getInputStream();

                        String content = IOUtils.toString(inputStream, "UTF8");

                        SaveToStorage(content);

                        lastUpdateDate = ParseForDate(content);

                        rates = ParseForRates(content);

                        if (lastUpdateDate != null) {
                            updateDate(lastUpdateDate);
                        }

                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
            thread.start();

            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            String cachedContent = ReadFromStorage();
            if (cachedContent == null)
            {
                Toast.makeText(getApplicationContext(), "No content available, check your internet connection.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            rates = ParseForRates(cachedContent);
        }

        String format = "yyyy-MM-dd";
        dateFormat = new SimpleDateFormat(format);
        textHeader.setText(dateFormat.format(dateLast));

        setListAdapter(new RateAdapter(rates.toArray(new Rate[rates.size()])));

        AdapterView.OnItemClickListener itemListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Rate selectedRate = (Rate) parent.getItemAtPosition(position);
                Toast.makeText(getApplicationContext(), "Selected: " + selectedRate.Currency,
                        Toast.LENGTH_SHORT).show();
            }
        };
        getListView().setOnItemClickListener(itemListener);
//        ListView lv = getListView();
//        LayoutInflater inflater = getLayoutInflater();
//
//        View header = inflater.inflate(R.layout.activity_date, lv, false);
//        lv.addHeaderView(header, null, false);
    }

    private Rate getModel(int position) {
        return(((RateAdapter)getListAdapter()).getItem(position));
    }
    private void SaveToStorage(String xml)
    {
        FileOutputStream outputStream = null;
        try {
            outputStream = openFileOutput(internalFile, Context.MODE_PRIVATE);
            outputStream.write(xml.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String ReadFromStorage()
    {
        BufferedReader input = null;
        String output = null;
        try {
            String filePath = getFilesDir() + "/" + internalFile;
            File file = new File(filePath);

            input = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            StringBuffer buffer = new StringBuffer();
            while ((line = input.readLine()) != null) {
                buffer.append(line);
            }

            input.close();
            output = buffer.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return output;
    }

    private void updateDate(String date) {
        SharedPreferences storedData = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor storedDataEditor = storedData.edit();
        storedDataEditor.putString("lastUpdate", date);
        storedDataEditor.commit();

    }
    private String ParseForDate(String xmlContent)
    {
        String lastDate = null;
        if(xmlContent != null) {
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser rateParser = factory.newPullParser();

                rateParser.setInput(new StringReader(xmlContent));
                int eventType = rateParser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG &&
                            rateParser.getName().equals("Cube") &&
                            rateParser.getAttributeCount() > 0 &&
                            rateParser.getAttributeName(0).equals("time")) {
                        lastDate = rateParser.getAttributeValue(0);
                        break;
                    }
                    eventType = rateParser.next();
                }
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return lastDate;
    }
    private ArrayList<Rate> ParseForRates(String xmlContent)
    {
        ArrayList<Rate> rateList = new ArrayList<>();
        if(xmlContent != null) {
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser rateParser = factory.newPullParser();

                rateParser.setInput(new StringReader(xmlContent));
                int eventType = rateParser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG &&
                            rateParser.getName().equals("Cube") &&
                            rateParser.getAttributeCount() == 2 &&
                            rateParser.getAttributeName(0).equals("currency")) {
                        rateList.add(new Rate(rateParser.getAttributeValue(0), rateParser.getAttributeValue(1)));
                    }
                    eventType = rateParser.next();
                }
            }catch (XmlPullParserException e) {
                e.printStackTrace();
            }catch(IOException e)
            {
                e.printStackTrace();
            }
        }
        return rateList;
    }
    private Date StringToDateParse(String dateString)
    {
        Date date = null;

        if(dateString != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            try {
                date = format.parse(dateString);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return date;
    }
    public boolean IsOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    // Adapter
    class RateAdapter extends ArrayAdapter<Rate> {

        private LayoutInflater mInflater;

        RateAdapter(Rate[] list) {
            super(MainActivity.this, R.layout.activity_date, list);
            mInflater = LayoutInflater.from(MainActivity.this);
        }
        public View getView(int position, View convertView,
                            ViewGroup parent) {
            ViewHolder holder;
            View row=convertView;
            if(row==null){

                row = mInflater.inflate(R.layout.activity_date, parent, false);
                holder = new ViewHolder();
                holder.currencyView = (TextView) row.findViewById(R.id.currencyTextView);
                holder.rateView = (TextView) row.findViewById(R.id.rateTextView);
                row.setTag(holder);
            }
            else{

                holder = (ViewHolder)row.getTag();
            }

            Rate rate = getModel(position);

            holder.currencyView.setText(rate.Currency);
            holder.rateView.setText(rate.Rate);

            return row;
        }

        class ViewHolder {
            public TextView currencyView, rateView;
        }
    }
}
