package it.ship.shipit.API_AsyncTask;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by NahitAkarsu on 10/14/15.
 */
public class APISearchAsync extends AsyncTask<String,Void,LinkedList>  {

    //public WeatherAdapter adapter;
    private static final String TAG = "APIAsyncTask";
    private static final String API_KEYI_FEDEX = "91a7sgudMXR6pvqc";
    private static final String API_KEYI_UPS = "91a7sgudMXR6pvqc";
    private static final String API_KEYI_USPS = "91a7sgudMXR6pvqc";

    //two addresses one used to find location and the other to find temperature, description and image data
    public static final String FEDEX_ADDR = "http://api.wunderground.com/api/"+API_KEYI_FEDEX+"/forecast10day/q/";
    public static final String UPS_ADDR = "http://api.wunderground.com/api/"+API_KEYI_UPS+"/forecast10day/q/";
    public static final String USPS_ADDR = "http://api.wunderground.com/api/"+API_KEYI_USPS+"/forecast10day/q/";

    //Use progress dialog to inform the user they should wait while asynctask is being handled
    ProgressDialog pd;
    public Context mContext;


    private WeatherSearchCompletionListener mCompletionListener;

    public interface WeatherSearchCompletionListener{
         void weatherDataFound(LinkedList result);
         void weatherDataNotFound();
    }
    //WeatherDataAsyncTask constructor
    public APISearchAsync(Context context, WeatherSearchCompletionListener completionListener){
        mContext = context;
        mCompletionListener = completionListener;
    }



    @Override
    protected void onPreExecute() {
        //on PreExecute show the progress dialogue to inform user that the data is loading
        pd = new ProgressDialog(mContext);
        pd.setTitle("Processing Weather Data...");
        pd.setMessage("Fetching Weather from WeatherUnderground");
        pd.setCancelable(false);
        pd.setIndeterminate(true);
        pd.show();
    }


    @Override
    protected LinkedList doInBackground(String... query) {

        //use sharedPreferences to choose between displaying fahrenheit or celsius and zip code
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = sharedPref.edit();

        //store the values of system preferences in two booleans
        boolean pressedFahrenheit = sharedPref.getBoolean("fPressed", true);
        boolean pressedCelsius = sharedPref.getBoolean("cPressed", false);
        //use the location found in that was passed onto the asynctask through the String query
        String locationAddr = query[0];

        //pull up the current zipCode entered into sharedPreferences
        String zipcode = sharedPref.getString("zipCode", null);
        String zipAddr = zipcode + ".json";

        //Data structure used to store the weather data received from the Json object
        LinkedList forecastDayList = new LinkedList();
        URL url;

        try {
            //this is the base url
            url = new URL(BASE_ADDR + locationAddr);
            //get the parsed json data from the location url address
            String getJsonLocation = getJSON(LOC_ADDR+locationAddr,1000);
            //store the parsed json object into a JSONObject
            JSONObject jsonObjectLocation = new JSONObject(getJsonLocation);

            //get the current observation key name object
            JSONObject locationObject = jsonObjectLocation.getJSONObject("current_observation");
            //get the json object display_location that is inside of current observation object
            JSONObject currentDisplayLocationObject = locationObject.getJSONObject("display_location");
            //get the string value that is inside of the key named "full"
            String location = currentDisplayLocationObject.getString("full");
            //add the object into the forecastDayList
            forecastDayList.add(location);

            //get the parsed json data from the forecast url address
            String getJsonForecast = getJSON(BASE_ADDR+locationAddr,1000);
            //store the parsed json object into a JSONObject jsonObjectForecast
            JSONObject jsonObjectForecast = new JSONObject(getJsonForecast);
            //get the forecast key name object
            JSONObject forecastObject = jsonObjectForecast.getJSONObject("forecast");
            //get the json object simpleforecast that is inside of forecast object
            JSONObject simpleForecastObject = forecastObject.getJSONObject("simpleforecast");
            //get the json array forecastday that is inside of simpleforecast object
            JSONArray forecastDays = simpleForecastObject.getJSONArray("forecastday");

            //loop through forecast data for 3 period (i.e. weather information for the next three days)
            for(int i=0; i<3; i++) {
                //if systemPreferences Fahrenheit is selected, pull the fahrenheit temperature accordingly
                if(pressedFahrenheit) {
                    //get the ith days forecast and pull the temperature, icon url and description from the json object forecastDayObject
                    JSONObject forecastDaysObject = forecastDays.getJSONObject(i);
                    JSONObject highFahrenheit = forecastDaysObject.getJSONObject("high");
                    String temperature = highFahrenheit.getString("fahrenheit") + "F";
                    String description = forecastDaysObject.getString("conditions");
                    String icon_url = forecastDaysObject.getString("icon_url");
                    forecastDayList.add(temperature);
                    forecastDayList.add(description);
                    forecastDayList.add(icon_url);
                }else {
                    //if systemPreferences Celsius is selected, pull the Celsius temperature accordingly
                    //get the ith days forecast and pull the temperature, icon url and description from the json object forecastDayObject
                    JSONObject forecastDaysObject = forecastDays.getJSONObject(i);
                    JSONObject highFahrenheit = forecastDaysObject.getJSONObject("high");
                    String temperature = highFahrenheit.getString("celsius") + "C";
                    String description = forecastDaysObject.getString("conditions");
                    String icon_url = forecastDaysObject.getString("icon_url");
                    forecastDayList.add(temperature);
                    forecastDayList.add(description);
                    forecastDayList.add(icon_url);
                }
            }


            //return linked list of all the forecast information pulled from the url
            return forecastDayList;

        } catch (MalformedURLException e1) {
            System.out.println("Malformed exception");
            return null;
        } catch (IOException e1) {
            System.out.println("I/O exception");
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
}

    /**
     *
     * @param url passed in from doInBackground
     * @param timeout used to not have ui lag
     * @return returns the parsed Json string from the given url
     */
    public String getJSON(String url, int timeout) {
        //initialze HTTPURL connection
        HttpURLConnection c = null;
        try {
            URL u = new URL(url);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.setConnectTimeout(timeout);
            c.setReadTimeout(timeout);
            c.connect();
            int status = c.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line+"\n");
                    }
                    br.close();
                    return sb.toString();
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(LinkedList result) {

        //after weather data is found dismiss the progress dialogue
        if (pd!=null) {
            pd.dismiss();
        }
        //if result returned is not null then send the result to completion listener weatherDataFound
        if (result != null) {
                mCompletionListener.weatherDataFound(result);
            //otehrwise send the weatherDataNotFound listener
        }else if(result == null){
            mCompletionListener.weatherDataNotFound();
        }
    }


}
