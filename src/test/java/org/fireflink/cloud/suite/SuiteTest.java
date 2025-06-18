package org.fireflink.cloud.suite;


import com.fasterxml.jackson.databind.JsonNode;
import org.fireflink.cloud.utils.GsheetUtil;
import org.fireflink.cloud.utils.LoginUtil;
import org.fireflink.cloud.utils.SuiteUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SuiteTest {

    @Test
    public void triggerMultipleSuitesInFireflink() throws InterruptedException {
        LoginUtil loginUtil=new LoginUtil();
        SuiteUtil suiteUtil=new SuiteUtil();
        GsheetUtil gsheetUtil=new GsheetUtil();
        JSONArray licDetails = gsheetUtil.getLicDetails();
        int threadCount = licDetails.length(); // One thread per item
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < licDetails.length(); i++) {
            final int index = i;
            executor.submit(() -> {
                JSONObject obj = licDetails.getJSONObject(index);
                System.out.println("************************************");
                System.out.println("Thread: " + Thread.currentThread().getName() + " -> " + obj.getString("userName"));
                try {
                    String accessToken = loginUtil.getBearerToken(
                            obj.getString("licId"),
                            obj.getString("userName"),
                            obj.getString("password")
                    );

                    JsonNode suiteDetails = suiteUtil.getSuiteDetails(
                            accessToken,
                            obj.getString("projectId"),
                            obj.getString("suiteId")
                    );

                    JsonNode selectedModules = suiteUtil.selectedModulesAndScripts(suiteDetails);
                    List<JsonNode> selectedMachines = suiteUtil.selectedMachines(suiteDetails);
                    JsonNode payload = suiteUtil.generateSuiteBody(selectedModules, selectedMachines);

                    String execId = suiteUtil.triggerSuite(
                            accessToken,
                            payload,
                            obj.getString("projectId"),
                            obj.getString("suiteId")
                    );

                    System.out.println("LIC ID: " + obj.getString("licId"));
                    System.out.println("PROJ ID: " + obj.getString("projectId"));
                    System.out.println("EXEC ID: " + execId);
                    System.out.println("************************************");
                } catch (Exception e) {
                    System.err.println("Error in thread " + Thread.currentThread().getName());
                    e.printStackTrace();
                }
            });
        }

        executor.shutdown(); // stop accepting new tasks
        executor.awaitTermination(10, TimeUnit.MINUTES);
    }
}
