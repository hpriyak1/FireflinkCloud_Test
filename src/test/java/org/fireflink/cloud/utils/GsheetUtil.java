package org.fireflink.cloud.utils;

import org.json.JSONArray;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GsheetUtil {
    public JSONArray getLicDetails(){
        try {
            String url = "https://script.google.com/macros/s/AKfycbzk96RBb4lOUhmLijmca6ny8t1X-Cv_op2OsBehzTpcMgjGGXKoKgovwQ4KTa4ZlvZC/exec";

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new JSONArray(response.body());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
