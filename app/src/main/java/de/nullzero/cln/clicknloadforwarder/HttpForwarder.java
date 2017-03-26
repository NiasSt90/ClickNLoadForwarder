package de.nullzero.cln.clicknloadforwarder;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import fi.iki.elonen.NanoHTTPD;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.util.Map;

/**
 * User: Markus Schulz <msc@0zero.de>
 */
public class HttpForwarder extends NanoHTTPD {

    private final ConnectivityManager cm;
    private final WifiManager wifiManager;
    private final NotificationManager notifyManager;

    private Context context;

    public HttpForwarder(Context context, String hostname, int port) {
        super(hostname, port);
        this.context = context;
        cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        notifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private boolean isHomeNetwork(String homeSSID) {
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo.isConnected()) {
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            String ssid = connectionInfo.getSSID();
            if (homeSSID != null && ssid.startsWith("\"")) {
                return homeSSID.equalsIgnoreCase(ssid.substring(1, ssid.length() - 1));
            }
        }
        return false;
    }

    @Override
    public Response serve(IHTTPSession session) {
        return super.serve(session);
    }

    @Override
    public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> parms, Map<String, String> files) {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String homeSSID = prefs.getString(ForwarderService.homeSSID, null);
        String targetUrl = prefs.getString(ForwarderService.targetURL, null);;
        final String username = prefs.getString(ForwarderService.httpBasicAuthUsername, null);
        final String password = prefs.getString(ForwarderService.httpBasicAuthPassword, null);
        final boolean isHomeNetwork = isHomeNetwork(homeSSID);
        if (isHomeNetwork) {
            targetUrl = prefs.getString(ForwarderService.targetHomeURL, targetUrl);
        }

        HttpURLConnection client = null;
        try {
            if (username != null && password != null) {
                Authenticator.setDefault(new Authenticator() {
                    private int mCounter = 0;

                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (mCounter++ > 0) {
                            return null;
                        }
                        return new PasswordAuthentication(username, password.toCharArray());
                    }
                });
            }

            URL url = new URL(targetUrl + uri);
            client = (HttpURLConnection) url.openConnection();
            client.setRequestMethod(method.name());
            client.setDoOutput(true);
            client.setDoInput(true);
            client.setUseCaches(false);
            client.setConnectTimeout(10000);
            client.setReadTimeout(10000);

            writeRequest(parms, client);
            InputStream responseStream = null;
            int responseCode=client.getResponseCode();
            if (responseCode >= HttpsURLConnection.HTTP_OK && responseCode < HttpsURLConnection.HTTP_BAD_REQUEST) {
                responseStream = client.getInputStream();
            }
            else {
                responseStream = client.getErrorStream();
            }
            if (uri.contains("flash/add")) {
                notifyToUser(parms, responseCode,"", "");
            }
            return generateResponse(responseCode, responseStream);
        }
        catch (IOException e) {
            notifyToUser(parms, 500, "IO-Fehler", e.getMessage());
            return generateResponse(500, null);
        }
        catch (Exception e) {
            Log.e(getClass().getName(), e.getMessage(), e);
            notifyToUser(parms, 500, "Unbekannter-Fehler", e.getClass().getName() + ":" + e.getMessage());
            return generateResponse(500, null);
        }
        finally {
            if (client != null) {
                client.disconnect();
            }
        }
    }

    private void writeRequest(Map<String, String> parms, HttpURLConnection client) throws IOException {
        OutputStream os = client.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(getPostDataString(parms));
        writer.flush();
        writer.close();
        os.close();
    }

    private Response generateResponse(int responseCode, InputStream response) {
        Response.Status resStatus = Response.Status.BAD_REQUEST;
        for (Response.Status status : Response.Status.values()) {
            if (status.getRequestStatus() == responseCode) {
                resStatus = status;
                break;
            }
        }
        return new Response(resStatus, "application/x-www-form-urlencoded", response, -1) {};
    }

    private String getPostDataString(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(entry.getValue() != null ?
                    URLEncoder.encode(entry.getValue(), "UTF-8") : "");
        }

        return result.toString();
    }

    private void notifyToUser(Map<String, String> parms, int responseCode, String errorMsg, String errorDetails) {
        final NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_cloud_done_white_24dp);

        if (responseCode >= 200 && responseCode < 400) {
            builder.setContentTitle("Erfolgreich übertragen");
            bigTextStyle.bigText(parms.get("urls"));
            bigTextStyle.setSummaryText(parms.get("source"));
        }
        else
        if (responseCode == HttpsURLConnection.HTTP_UNAUTHORIZED) {
            builder.setContentTitle("Fehlerhafte Authentifizierung!");
            builder.setContentText("Prüfe Sie die Basic-Auth Daten!");
        }
        else
        if (responseCode == 500) {
            builder.setContentTitle(errorMsg);
            builder.setContentText(errorDetails);
        }
        builder.setStyle(bigTextStyle);
        notifyManager.notify(0, builder.build());
    }


}
