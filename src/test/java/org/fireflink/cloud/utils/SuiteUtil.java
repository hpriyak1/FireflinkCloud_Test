package org.fireflink.cloud.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.path.json.JsonPath;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class SuiteUtil {
    public static final String baseURI="https://app.fireflink.com/";
    public JsonNode getSuiteDetails(String accessToken, String projId, String suiteId)
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseURI + "project/optimize/v1/suite/" + suiteId))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + accessToken)
                .header("projectId", projId).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(response.body());
//        System.out.println("Suite Details "+ rootNode.toPrettyString());
        return rootNode;
    }
    public JsonNode selectedModulesAndScripts(JsonNode suiteDetails) {
        return suiteDetails.path("responseObject").path("selectedModulesAndScripts");
    }

    public List<JsonNode> selectedMachines(JsonNode suiteDetails) {
        ObjectMapper mapper = new ObjectMapper();

        // Extract the selected machines
        JsonNode selectedMachinesNode = suiteDetails.path("responseObject").path("machines").path("selectedMachines");
        // Create a list to hold the transformed machines
        List<JsonNode> transformedMachinesList = new ArrayList<>();

        for (JsonNode machineNode : selectedMachinesNode) {
            // Create a new ObjectNode for the transformed machine
            ObjectNode transformedMachine = mapper.createObjectNode();

            // Extract the clientId
            String clientId = machineNode.path("clientId").asText();
            transformedMachine.put("clientId", clientId);

            // Extract and transform machine instances
            ArrayNode machineInstances = mapper.createArrayNode();
            JsonNode machineInstancesNode = machineNode.path("machineInstances");

            for (JsonNode instanceNode : machineInstancesNode) {
                ObjectNode transformedInstance = mapper.createObjectNode();
                transformedInstance.put("clientId", instanceNode.path("clientId").asText());
                transformedInstance.put("numberOfRuns", instanceNode.path("numberOfRuns").asInt());
                transformedInstance.put("executionEnv", instanceNode.path("executionEnv").asText());
                transformedInstance.put("browserName", instanceNode.path("browserName").asText());
                transformedInstance.put("browserVersion", instanceNode.path("browserVersion").asText());
                transformedInstance.put("systemUrl", instanceNode.path("systemUrl").asText());

                // Extract and put machineInfo
                JsonNode machineInfoNode = instanceNode.path("machineInfo");
                ObjectNode machineInfo = mapper.createObjectNode();
                machineInfo.put("osName", machineInfoNode.path("osName").asText());
                machineInfo.put("osVersion", machineInfoNode.path("osVersion").asText());
                machineInfo.put("hostName", machineInfoNode.path("hostName").asText());
                transformedInstance.set("machineInfo", machineInfo);

                // Extract and put deviceInfo
                ArrayNode deviceInfoArray = mapper.createArrayNode();
                JsonNode deviceInfoNode = instanceNode.path("deviceInfo");
                for (JsonNode deviceNode : deviceInfoNode) {
                    ObjectNode deviceInfo = mapper.createObjectNode();
                    deviceInfo.put("name", deviceNode.path("name").asText());
                    deviceInfo.put("version", deviceNode.path("version").asText());
                    deviceInfo.put("serial_no", deviceNode.path("serial_no").asText());
                    deviceInfo.put("deviceUniqueId", deviceNode.path("deviceUniqueId").asText());
                    deviceInfo.put("type", deviceNode.path("type").asText());
                    deviceInfo.put("subType", deviceNode.path("subType").asText());
                    deviceInfo.put("platform", deviceNode.path("platform").asText());
                    deviceInfoArray.add(deviceInfo);
                }
                transformedInstance.set("deviceInfo", deviceInfoArray);

                transformedInstance.put("headless", instanceNode.path("headless").asBoolean());
                machineInstances.add(transformedInstance);
            }

            transformedMachine.set("machineInstances", machineInstances);
            transformedMachinesList.add(transformedMachine);
        }

        // Print for debugging purposes
        System.out.println("Machines: " + transformedMachinesList);

        return transformedMachinesList;
    }

    public JsonNode generateSuiteBody(JsonNode selectedModulesAndScriptsNode, List<JsonNode> selectedMachinesJsonNodes) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode rootNode = objectMapper.createObjectNode();
            boolean parallelExecution = false;
            for (JsonNode machineNode : selectedMachinesJsonNodes) {
                JsonNode machineInstancesNode = machineNode.path("machineInstances");
                if (machineInstancesNode.isArray() && machineInstancesNode.size() > 1) {
                    parallelExecution = true;
                    break;
                }
            }
            String isParallel = parallelExecution ? "true" : "false";
            ObjectNode executionTerminationNode = objectMapper.createObjectNode();
            executionTerminationNode.put("terminateScriptIfTakesMoreTime", "1");
            executionTerminationNode.put("terminateScriptUnit", "Hrs");
            executionTerminationNode.put("terminateScriptSendEmail", "Secs");
            executionTerminationNode.put("terminateSuiteIfTakesMoreTime", "1");
            executionTerminationNode.put("terminateSuitetUnit", "Hrs");
            executionTerminationNode.put("terminateSuiteSendEmail", "Secs");
            rootNode.set("executionTermination", executionTerminationNode);
            ObjectNode machinesNode = objectMapper.createObjectNode();
            machinesNode.put("multiple", isParallel);
            ArrayNode selectedMachinesNode = objectMapper.createArrayNode();
            selectedMachinesNode.addAll(selectedMachinesJsonNodes);
            machinesNode.set("selectedMachines", selectedMachinesNode);
            if (isParallel.equals("true")) {
                machinesNode.put("executionType", "SEND_SUITE_TO_ALL");
            }

            rootNode.set("machines", machinesNode);
            ObjectNode resultSettingNode = objectMapper.createObjectNode();
            resultSettingNode.put("captureScreenshots", "For Failed steps only");
            resultSettingNode.put("captureVideoForFailedTestScript", "For all scripts");
            resultSettingNode.put("displayLogsInScriptResult", "On Suite Execution Completion / Termination");
            rootNode.set("resultSetting", resultSettingNode);
            ObjectNode waitTimeNode = objectMapper.createObjectNode();
            waitTimeNode.put("implicitWait", "20");
            waitTimeNode.put("implicitWaitUnit", "Secs");
            waitTimeNode.put("explicitlyWait", "30");
            waitTimeNode.put("explicitlyWaitUnit", "Secs");
            waitTimeNode.put("delayBetweenSteps", "1");
            waitTimeNode.put("delayBetweenStepsUnit", "Secs");
            rootNode.set("waitTime", waitTimeNode);
            rootNode.set("selectedModulesAndScripts", selectedModulesAndScriptsNode);
            rootNode.put("deleteSpillOverMemory", false);
            String suitePayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
//            System.out.println("Suite Payload: " + suitePayload);
            return rootNode;

        } catch (Exception e) {
            return null;
        }
    }
    public String triggerSuite(String accessToken, JsonNode payload, String projId, String suiteId)
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();
        String body = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseURI + "dashboardexecution/optimize/v1/dashboard/execution/suite/" + suiteId))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + accessToken)
                .header("projectId", projId).POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return JsonPath.from(response.body()).get("responseObject.id");
    }
}
