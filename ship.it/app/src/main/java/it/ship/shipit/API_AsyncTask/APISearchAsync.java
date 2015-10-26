package it.ship.shipit.API_AsyncTask;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

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


    private APISearchCompletionListener mCompletionListener;

    public interface APISearchCompletionListener{
         void APIDataFound(LinkedList result);
         void APIDataNotFound();
    }
    //WeatherDataAsyncTask constructor
    public APISearchAsync(Context context, APISearchCompletionListener completionListener){
        mContext = context;
        mCompletionListener = completionListener;
    }



    @Override
    protected void onPreExecute() {
        //on PreExecute show the progress dialogue to inform user that the data is loading
        pd = new ProgressDialog(mContext);
        pd.setTitle("Processing Shipping Data...");
        pd.setMessage("Fetching Data from API");
        pd.setCancelable(false);
        pd.setIndeterminate(true);
        pd.show();
    }


    @Override
    protected LinkedList doInBackground(String... query) {

        //use sharedPreferences to choose between displaying fahrenheit or celsius and zip code
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = sharedPref.edit();

        String locationAddr = query[0];


        LinkedList apiDataList = new LinkedList();
        URL url;

        try {
            //this is the url connect to Fedex, Ups, Usps
            url = new URL(FEDEX_ADDR);
            url = new URL(UPS_ADDR);
            url = new URL(USPS_ADDR);

            //get the parsed json data for
            String getJsonFedex = getJSON(FEDEX_ADDR,1000);
            String getJsonUps = getJSON(UPS_ADDR,1000);
            String getJsonUsps = getJSON(USPS_ADDR,1000);


            //store the parsed json object into a JSONObject
            JSONObject jsonFedexLocation = new JSONObject(getJsonFedex);
            JSONObject jsonUpsLocation = new JSONObject(getJsonUps);
            JSONObject jsonUspsLocation = new JSONObject(getJsonUsps);

            //get the current observation key name object
            JSONObject fedexObject = jsonFedexLocation.getJSONObject("FEDEX");
            JSONObject upsObject = jsonUpsLocation.getJSONObject("UPS");
            JSONObject uspsObject = jsonUspsLocation.getJSONObject("USPS");


            apiDataList.add(fedexObject);
            apiDataList.add(upsObject);
            apiDataList.add(uspsObject);



            //return linked list of all the forecast information pulled from the url
            return apiDataList;

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
                mCompletionListener.APIDataFound(result);
            //otehrwise send the weatherDataNotFound listener
        }else if(result == null){
            mCompletionListener.APIDataNotFound();
        }
    }


}
