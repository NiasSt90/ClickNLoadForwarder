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
import fi.iki.elonen.NanoHTTPD;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.util.HashMap;
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
            return homeSSID != null && homeSSID.equals(connectionInfo.getSSID());
        }
        return false;
    }

    @Override
    public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> parms, Map<String, String> files) {

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_cloud_done_white_24dp);

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
        final NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
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

            URL url = new URL(targetUrl + "/flash/add");
            client = (HttpURLConnection) url.openConnection();
            client.setRequestMethod("POST");
            client.setDoOutput(true);
            client.setDoInput(true);
            client.setUseCaches(false);
            client.setConnectTimeout(10000);
            client.setReadTimeout(10000);
            Map<String, String> paramsToTransfer = new HashMap<>(3);
            paramsToTransfer.put("source", parms.get("source"));
            paramsToTransfer.put("passwords", parms.get("passwords"));
            paramsToTransfer.put("urls", parms.get("urls"));

            String response = "";
            OutputStream os = client.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(paramsToTransfer));
            writer.flush();
            writer.close();
            os.close();

            int responseCode=client.getResponseCode();
            if (responseCode >= HttpsURLConnection.HTTP_OK && responseCode < HttpsURLConnection.HTTP_BAD_REQUEST) {
                builder.setContentTitle("Erfolgreich übertragen");
                bigTextStyle.bigText(parms.get("urls"));
            }
            else {
                builder.setContentTitle("Fehler HTTP-Code " + responseCode);
                response = readResponseText(client.getErrorStream());
                if (responseCode == HttpsURLConnection.HTTP_UNAUTHORIZED) {
                    response = "Fehlerhafte Authentifizierung!";
                }
                bigTextStyle.bigText(response);
            }
            bigTextStyle.setSummaryText(parms.get("source"));
            builder.setStyle(bigTextStyle);
            notifyManager.notify(0, builder.build());
        }
        catch (IOException e) {
            builder.setContentTitle("IO-Fehler");
            builder.setContentText(e.getMessage())
                    .setStyle(bigTextStyle.bigText(e.getMessage()));
            notifyManager.notify(0, builder.build());
        }
        finally {
            if (client != null) {
                client.disconnect();
            }
        }
        return super.serve(uri, method, headers, parms, files);
    }

    private String readResponseText(InputStream inputStream) throws IOException {
        String response = "";
        String line;
        BufferedReader br=new BufferedReader(new InputStreamReader(inputStream));
        while ((line=br.readLine()) != null) {
            response+=line;
        }
        return response;
    }

    @Override
    public Response serve(IHTTPSession session) {
        return super.serve(session);
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
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }


}
