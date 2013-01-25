/*
 * Copyright (C) 2012 Electric Cloud, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.electriccloud.commander.android.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.xml.sax.SAXException;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.ProgressDialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.os.AsyncTask;
import android.os.Bundle;

import android.preference.PreferenceManager;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.webkit.WebView;

import android.widget.Toast;

import com.electriccloud.commander.android.app.R;
import com.electriccloud.commander.client.CommanderRequestFactory;
import com.electriccloud.commander.client.domain.Job;
import com.electriccloud.commander.client.domain.ObjectType;
import com.electriccloud.commander.client.requests.FindObjectsFilter;
import com.electriccloud.commander.client.requests.FindObjectsRequest;
import com.electriccloud.commander.client.requests.LoginRequest;
import com.electriccloud.commander.client.requests.impl.CommanderRequestFactoryImpl;
import com.electriccloud.commander.client.responses.FindObjectsResponse;
import com.electriccloud.commander.client.responses.impl.FindObjectsResponseImpl;
import com.electriccloud.commander.transport.CommanderObjectImpl;

/**
 * Main Activity for the commander mobile application.
 *
 * <p>This activity does the following:</p>
 *
 * <p>o Presents a WebView screen to users. This WebView has a list of HTML
 * content.</p>
 *
 * <p>o Uses AsyncTask to download and process the XML.</p>
 *
 * <p>o Monitors preferences and the device's network connection to determine
 * whether to refresh the WebView content.</p>
 */
public class NetworkActivity
    extends Activity
{

    //~ Static fields/initializers ---------------------------------------------

    // ~ Static fields/initializers
    // ---------------------------------------------
    public static final String WIFI = "Wi-Fi";
    public static final String ANY  = "Any";

    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected = false;

    // Whether there is a mobile connection.
    private static boolean mobileConnected = false;

    // Whether the display should be refreshed.
    public static boolean refreshDisplay = true;

    // The user's current network preference setting.
    public static String                   sPref     = null;
    private static CommanderRequestFactory m_factory =
        new CommanderRequestFactoryImpl();

    //~ Instance fields --------------------------------------------------------

    private String m_CommanderServer = "http://192.168.1.127:8000";
    private String m_userName        = "";
    private String m_password        = "";

    // ~ Instance fields
    // --------------------------------------------------------
    // The BroadcastReceiver that tracks network connectivity changes.
    private NetworkReceiver      receiver = new NetworkReceiver();
    private CommanderServiceImpl service  = new CommanderServiceImpl();
    private List<Job>            m_jobs;
    private ProgressDialog       m_dialog;

    //~ Methods ----------------------------------------------------------------

    // ~ Methods
    // ----------------------------------------------------------------
    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Register BroadcastReceiver to track connection changes.
        IntentFilter filter = new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION);

        receiver = new NetworkReceiver();

        this.registerReceiver(receiver, filter);
        m_dialog = ProgressDialog.show(NetworkActivity.this, "",
                "Loading. Please wait...", true);

        // Get the message from the intent
        Intent intent = getIntent();

        m_CommanderServer = "http://"
                + intent.getStringExtra(HelloWorldAndroidActivity.SERVER)
                + ":8000";
        m_userName        = intent.getStringExtra(
                HelloWorldAndroidActivity.USER_NAME);
        m_password        = intent.getStringExtra(
                HelloWorldAndroidActivity.PASSWORD);
    }

    // Populates the activity's options menu.
    @Override public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.mainmenu, menu);

        return true;
    }

    @Override public void onDestroy()
    {
        super.onDestroy();

        if (receiver != null) {
            this.unregisterReceiver(receiver);
        }
    }

    // Handles the user's menu selection.
    @Override public boolean onOptionsItemSelected(MenuItem item)
    {

        switch (item.getItemId()) {

            case R.id.settings:

                Intent settingsActivity = new Intent(getBaseContext(),
                        SettingsActivity.class);

                startActivity(settingsActivity);

                return true;

            case R.id.refresh:
                loadPage();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Refreshes the display if the network connection and the
    // pref settings allow it.
    @Override public void onStart()
    {
        super.onStart();

        // Gets the user's network preference settings
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        // Retrieves a string value for the preferences. The second parameter
        // is the default value to use if a preference value is not found.
        sPref = "Any"; // sharedPrefs.getString("listPref", "Any");
        updateConnectedFlags();

        // Only loads the page if refreshDisplay is true. Otherwise, keeps
        // previous
        // display. 
        if (refreshDisplay) {
            loadPage();
        }
    }

    public void parseFindObjectsResponse(InputStream httpResponse)
    {
        org.dom4j.Document doc = null;

        try {
            doc = DocumentHelper.parseText(parseResponse(httpResponse));
        }
        catch (DocumentException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        org.dom4j.Node      n        = doc.selectSingleNode("//response");
        CommanderObjectImpl co       = new CommanderObjectImpl((Element) n);
        FindObjectsResponse response = new FindObjectsResponseImpl(
                ObjectType.job, co);

        m_jobs = response.getJobs();
    }

    // Uses AsyncTask subclass to download the XML feed from stackoverflow.com.
    // This avoids UI lock up. To prevent network operations from
    // causing a delay that results in a poor user experience, always perform
    // network operations on a separate thread from the UI.
    private void loadPage()
    {

        if ((((sPref.equals(WIFI)) || sPref.equals(ANY))
                    && (wifiConnected || mobileConnected))
                || ((sPref.equals(WIFI)) && (wifiConnected))) {

            // AsyncTask subclass
            new DownloadXmlTask().execute(m_CommanderServer);
        }
        else {
            showErrorPage();
        }
    }


    private String loadXmlFromNetwork(String urlString)
        throws XmlPullParserException, IOException
    {
        InputStream stream    = null;
        Calendar    rightNow  = Calendar.getInstance();
        DateFormat  formatter = new SimpleDateFormat("MMM dd h:mmaa");

        // Checks whether the user set the preference to include summary text
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        boolean           pref        = sharedPrefs.getBoolean("summaryPref",
                false);
        StringBuilder     htmlString  = new StringBuilder();

        htmlString.append("<h3>"
                + getResources().getString(R.string.page_title) + "</h3>");
        htmlString.append("<em>" + getResources().getString(R.string.updated)
                + " " + formatter.format(rightNow.getTime()) + "</em>");
        htmlString.append("<BR/>");

        try {
            LoginRequest loginRequest = m_factory.createLoginRequest();

            loginRequest.setUserName(m_userName);
            loginRequest.setPassword(m_password);
            stream = service.createStream(urlString,
                    service.createRequest(loginRequest));

            service.setSessionId(getSessionId(stream));

            htmlString.append("Your last 20 job names");
            htmlString.append("<BR/>");

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        }
        finally {

            if (stream != null) {
                stream.close();
            }
        }

        try {
            FindObjectsRequest fObjects = m_factory.createFindObjectsRequest(
                    ObjectType.job);

            fObjects.setMaxIds(20);

            fObjects.addFilter(new FindObjectsFilter.EqualsFilter(
                    "launchedByUser", m_userName));
            stream = service.createStream(urlString,
                    service.createRequest(fObjects));
            parseFindObjectsResponse(stream);

            for (Job job : m_jobs) {
                htmlString.append(job.getName());
                htmlString.append("<BR/>");
            }
        }
        finally {

            if (stream != null) {
                stream.close();
            }
        }

        return htmlString.toString();
    }

    private String parseResponse(InputStream is)
        throws IOException
    {

        // read it with BufferedReader
        BufferedReader br   = new BufferedReader(new InputStreamReader(is));
        StringBuilder  sb   = new StringBuilder();
        String         line;

        while ((line = br.readLine()) != null) {
            sb.append(line)
              .append("\n");
        }

        br.close();

        return sb.toString();
    }

    // Displays an error if the app is unable to load content.
    private void showErrorPage()
    {
        setContentView(R.layout.second);

        // The specified network connection is not available. Displays error
        // message.
        WebView myWebView = (WebView) findViewById(R.id.webview);

        myWebView.loadData(getResources().getString(R.string.connection_error),
            "text/html", null);
    }

    // Checks the network connection and sets the wifiConnected and
    // mobileConnected
    // variables accordingly.
    private void updateConnectedFlags()
    {
        ConnectivityManager connMgr    = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo         activeInfo = connMgr.getActiveNetworkInfo();

        if (activeInfo != null && activeInfo.isConnected()) {
            wifiConnected   = activeInfo.getType()
                    == ConnectivityManager.TYPE_WIFI;
            mobileConnected = activeInfo.getType()
                    == ConnectivityManager.TYPE_MOBILE;
        }
        else {
            wifiConnected   = false;
            mobileConnected = false;
        }
    }

    public String getSessionId(InputStream httpResponse)
    {
        Document document = null;
        Node     node     = null;

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory
                    .newInstance();
            DocumentBuilder        builder    = docFactory.newDocumentBuilder();

            document = builder.parse(httpResponse);

            XPath  xpath      = XPathFactory.newInstance()
                                            .newXPath();
            String expression = "//sessionId";

            node = (Node) xpath.evaluate(expression, document,
                    XPathConstants.NODE);
        }
        catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        catch (SAXException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        if (node != null) {
            return node.getTextContent();
        }

        return "";
    }

    //~ Inner Classes ----------------------------------------------------------

    // ~ Inner Classes
    // ----------------------------------------------------------
    /**
     * This BroadcastReceiver intercepts the
     * android.net.ConnectivityManager.CONNECTIVITY_ACTION, which indicates a
     * connection change. It checks whether the type is TYPE_WIFI. If it is, it
     * checks whether Wi-Fi is connected and sets the wifiConnected flag in the
     * main activity accordingly.
     */
    public class NetworkReceiver
        extends BroadcastReceiver
    {

        //~ Methods ------------------------------------------------------------

        // ~ Methods
        // ------------------------------------------------------------
        @Override public void onReceive(
                Context context,
                Intent  intent)
        {
            ConnectivityManager connMgr     = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo         networkInfo = connMgr.getActiveNetworkInfo();

            // Checks the user prefs and the network connection. Based on the
            // result, decides
            // whether
            // to refresh the display or keep the current display.
            // If the userpref is Wi-Fi only, checks to see if the device has a
            // Wi-Fi connection.
            if (WIFI.equals(sPref) && networkInfo != null
                    && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {

                // If device has its Wi-Fi connection, sets refreshDisplay
                // to true. This causes the display to be refreshed when the
                // user
                // returns to the app.
                refreshDisplay = true;
                Toast.makeText(context, R.string.wifi_connected,
                         Toast.LENGTH_SHORT)
                     .show();

                // If the setting is ANY network and there is a network
                // connection
                // (which by process of elimination would be mobile), sets
                // refreshDisplay to true.
            }
            else if (ANY.equals(sPref) && networkInfo != null) {
                refreshDisplay = true;

                // Otherwise, the app can't download content--either because
                // there is no network
                // connection (mobile or Wi-Fi), or because the pref setting is
                // WIFI, and there
                // is no Wi-Fi connection.
                // Sets refreshDisplay to false.
            }
            else {
                refreshDisplay = false;
                Toast.makeText(context, R.string.lost_connection,
                         Toast.LENGTH_SHORT)
                     .show();
            }
        }
    }

    // Implementation of AsyncTask used to make requests from
    // the Electric Commander Server 
    private class DownloadXmlTask
        extends AsyncTask<String, Void, String>
    {

        //~ Methods ------------------------------------------------------------

        // ~ Methods
        // ------------------------------------------------------------
        @Override protected String doInBackground(String... urls)
        {

            try {
                return loadXmlFromNetwork(urls[0]);
            }
            catch (IOException e) {
                return getResources().getString(R.string.connection_error);
            }
            catch (XmlPullParserException e) {
                return getResources().getString(R.string.xml_error);
            }
        }

        @Override protected void onPostExecute(String result)
        {
            m_dialog.hide();
            setContentView(R.layout.second);

            // Displays the HTML string in the UI via a WebView
            WebView myWebView = (WebView) findViewById(R.id.webview);

            myWebView.loadData(result, "text/html", null);
        }

        @Override protected void onPreExecute()
        {
            m_dialog.show();
        }
    }
}
