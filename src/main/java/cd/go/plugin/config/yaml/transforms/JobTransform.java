package cd.go.plugin.config.yaml.transforms;

import cd.go.plugin.config.yaml.YamlConfigException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cd.go.plugin.config.yaml.JSONUtils.*;
import static cd.go.plugin.config.yaml.YamlUtils.*;
import static cd.go.plugin.config.yaml.transforms.EnvironmentVariablesTransform.JSON_ENV_VAR_FIELD;

public class JobTransform extends ConfigurationTransform {
    private static final String YAML_JOB_TIMEOUT_FIELD = "timeout";
    private static final String JSON_JOB_TIMEOUT_FIELD = "timeout";
    private static final String YAML_JOB_TASKS_FIELD = "tasks";
    private static final String JSON_JOB_TASKS_FIELD = "tasks";
    private static final String YAML_JOB_RUN_INSTANCES_FIELD = "run_instances";
    private static final String JSON_JOB_RUN_INSTANCES_FIELD = "run_instance_count";
    private static final String JSON_JOB_NAME_FIELD = "name";
    private static final String YAML_JOB_TABS_FIELD = "tabs";
    private static final String JSON_JOB_TAB_NAME_FIELD = "name";
    private static final String JSON_JOB_TAB_PATH_FIELD = "path";
    private static final String JSON_JOB_TABS_FIELD = "tabs";
    private static final String JSON_JOB_RESOURCES_FIELD = "resources";
    private static final String YAML_JOB_RESOURCES_FIELD = "resources";
    private static final String JSON_JOB_ELASTIC_PROFILE_FIELD = "elastic_profile_id";
    private static final String YAML_JOB_ELASTIC_PROFILE_FIELD = "elastic_profile_id";
    private static final String YAML_JOB_ARTIFACTS_FIELD = "artifacts";
    private static final String JSON_JOB_ARTIFACTS_FIELD = "artifacts";
    private static final String JSON_JOB_ARTIFACT_SOURCE_FIELD = "source";
    private static final String YAML_JOB_ARTIFACT_SOURCE_FIELD = "source";
    private static final String JSON_JOB_ARTIFACT_DEST_FIELD = "destination";
    private static final String YAML_JOB_ARTIFACT_DEST_FIELD = "destination";
    private static final String JSON_JOB_ARTIFACT_ARTIFACT_ID_FIELD = "id";
    private static final String YAML_JOB_ARTIFACT_ARTIFACT_ID_FIELD = "id";
    private static final String JSON_JOB_ARTIFACT_STORE_ID_FIELD = "store_id";
    private static final String YAML_JOB_ARTIFACT_STORE_ID_FIELD = "store_id";

    private static final String JSON_JOB_PROP_NAME_FIELD = "name";
    private static final String JSON_JOB_PROP_SOURCE_FIELD = "source";
    private static final String YAML_JOB_PROP_SOURCE_FIELD = "source";
    private static final String JSON_JOB_PROP_XPATH_FIELD = "xpath";
    private static final String YAML_JOB_PROP_XPATH_FIELD = "xpath";
    private static final String EXTERNAL_ARTIFACT_TYPE_FIELD = "external";

    private EnvironmentVariablesTransform environmentTransform;
    private TaskTransform taskTransform;

    public JobTransform(EnvironmentVariablesTransform environmentTransform, TaskTransform taskTransform) {
        this.environmentTransform = environmentTransform;
        this.taskTransform = taskTransform;
    }

    public JsonObject transform(Object yamlObject) {
        Map<String, Object> map = (Map<String, Object>) yamlObject;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            return transform(entry);
        }
        throw new RuntimeException("expected job hash to have 1 item");
    }

    public JsonObject transform(Map.Entry<String, Object> entry) {
        return transform(entry.getKey(), (Map<String, Object>) entry.getValue());
    }

    public JsonObject transform(String jobName, Map<String, Object> jobMap) {
        JsonObject jobJson = new JsonObject();
        jobJson.addProperty(JSON_JOB_NAME_FIELD, jobName);
        addOptionalInteger(jobJson, jobMap, JSON_JOB_TIMEOUT_FIELD, YAML_JOB_TIMEOUT_FIELD);
        addRunInstances(jobMap, jobJson);
        JsonArray jsonEnvVariables = environmentTransform.transform(jobMap);
        if (jsonEnvVariables != null && jsonEnvVariables.size() > 0)
            jobJson.add(JSON_ENV_VAR_FIELD, jsonEnvVariables);
        addTabs(jobJson, jobMap);
        addOptionalStringList(jobJson, jobMap, JSON_JOB_RESOURCES_FIELD, YAML_JOB_RESOURCES_FIELD);
        addOptionalString(jobJson, jobMap, JSON_JOB_ELASTIC_PROFILE_FIELD, YAML_JOB_ELASTIC_PROFILE_FIELD);
        addArtifacts(jobJson, jobMap);
        addTasks(jobJson, jobMap);
        return jobJson;
    }

    public Map<String, Object> inverseTransform(Map<String, Object> job) {
        if (job == null)
            return null;
        String jobName = (String) job.get(JSON_JOB_NAME_FIELD);
        Map<String, Object> inverseJob = new LinkedTreeMap<>();
        Map<String, Object> jobData = new LinkedTreeMap<>();

        addOptionalInt(jobData, job, JSON_JOB_TIMEOUT_FIELD, YAML_JOB_TIMEOUT_FIELD);

        addInverseRunInstances(jobData, job);

        Map<String, Object> yamlEnvVariables = environmentTransform.inverseTransform((List<Map<String, Object>>) job.get(JSON_ENV_VAR_FIELD));
        if (yamlEnvVariables != null && yamlEnvVariables.size() > 0)
            jobData.putAll(yamlEnvVariables);

        addInverseTabs(jobData, job);

        addOptionalList(jobData, job, JSON_JOB_RESOURCES_FIELD, YAML_JOB_RESOURCES_FIELD);
        addOptionalValue(jobData, job, JSON_JOB_ELASTIC_PROFILE_FIELD, YAML_JOB_ELASTIC_PROFILE_FIELD);

        addInverseArtifacts(jobData, job);
        addInverseTasks(jobData, job);
        inverseJob.put(jobName, jobData);
        return inverseJob;
    }

    private void addInverseRunInstances(Map<String, Object> jobData, Map<String, Object> job) {
        Object run = job.get(JSON_JOB_RUN_INSTANCES_FIELD);
        if (run == null || run.equals(0) || run.equals(0.0) || run.equals("0"))
            return;
        if (run instanceof String) {
            addOptionalValue(jobData, job, JSON_JOB_RUN_INSTANCES_FIELD, YAML_JOB_RUN_INSTANCES_FIELD);
        } else {
            addOptionalInt(jobData, job, JSON_JOB_RUN_INSTANCES_FIELD, YAML_JOB_RUN_INSTANCES_FIELD);
        }
    }

    private void addInverseTabs(Map<String, Object> jobData, Map<String, Object> job) {
        List<Map<String, Object>> tabs = (List<Map<String, Object>>) job.get(JSON_JOB_TABS_FIELD);
        if (tabs == null || tabs.isEmpty())
            return;

        Map<String, Object> inverseTabs = new LinkedTreeMap<>();
        for (Map<String, Object> tab : tabs) {
            inverseTabs.put((String) tab.get(JSON_JOB_TAB_NAME_FIELD), tab.get(JSON_JOB_TAB_PATH_FIELD));
        }

        jobData.put(YAML_JOB_TABS_FIELD, inverseTabs);
    }

    private void addInverseTasks(Map<String, Object> jobData, Map<String, Object> job) {
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) job.get(JSON_JOB_TASKS_FIELD);
        if (tasks == null)
            return;

        List<Map<String, Object>> inverseTasks = new ArrayList<>();

        for (Map<String, Object> task : tasks) {
            inverseTasks.add(null == task ? null : taskTransform.inverseTransform(task));
        }

        jobData.put(YAML_JOB_TASKS_FIELD, inverseTasks);
    }

    private void addInverseArtifacts(Map<String, Object> jobData, Map<String, Object> job) {
        List<Map<String, Object>> artifacts = (List<Map<String, Object>>) job.get(JSON_JOB_ARTIFACTS_FIELD);
        if (artifacts == null || artifacts.isEmpty())
            return;

        List<Map<String, Object>> inverseArtifacts = new ArrayList<>();
        for (Map<String, Object> artifact : artifacts) {
            Map<String, Object> inverseArtifact = new LinkedTreeMap<>();

            String type = (String) artifact.remove("type");
            inverseArtifact.put(type, artifact);
            inverseArtifacts.add(inverseArtifact);
            handleExternalArtifactConfiguration(type, inverseArtifact);
        }

        jobData.put(YAML_JOB_ARTIFACTS_FIELD, inverseArtifacts);
    }

    private void handleExternalArtifactConfiguration(String type, Map<String, Object> inverseArtifact) {
        if (!EXTERNAL_ARTIFACT_TYPE_FIELD.equals(type)) {
            return;
        }
        final Map<String, Object> artifactMap = (Map<String, Object>) inverseArtifact.get(EXTERNAL_ARTIFACT_TYPE_FIELD);
        final List<Map<String, String>> configuration = (List<Map<String, String>>) artifactMap.get(JSON_PLUGIN_CONFIGURATION_FIELD);

        final Map<String, Map<String, String>> result = new HashMap<>();
        for (Map<String, String> configProperty : configuration) {
            final boolean isEncryptedProperty = configProperty.containsKey(JSON_PLUGIN_CONFIG_ENCRYPTED_VALUE_FIELD);
            String sourceFieldToUse = isEncryptedProperty ? JSON_PLUGIN_CONFIG_ENCRYPTED_VALUE_FIELD : JSON_PLUGIN_CONFIG_VALUE_FIELD;
            String mapToPutIntoInResult = isEncryptedProperty ? YAML_PLUGIN_SEC_CONFIG_FIELD : YAML_PLUGIN_STD_CONFIG_FIELD;

            result.putIfAbsent(mapToPutIntoInResult, new HashMap<>());
            result.get(mapToPutIntoInResult).put(configProperty.get(JSON_PLUGIN_CONFIG_KEY_FIELD), configProperty.get(sourceFieldToUse));
        }

        artifactMap.put(JSON_PLUGIN_CONFIGURATION_FIELD, result);
    }

    private void addArtifacts(JsonObject jobJson, Map<String, Object> jobMap) {
        Object artifacts = jobMap.get(YAML_JOB_ARTIFACTS_FIELD);
        if (artifacts == null)
            return;
        if (!(artifacts instanceof List))
            throw new YamlConfigException("artifacts should be a list of hashes");
        JsonArray artifactArrayJson = new JsonArray();
        List<Object> artifactsList = (List<Object>) artifacts;
        for (Object artifactObj : artifactsList) {
            if (!(artifactObj instanceof Map))
                throw new YamlConfigException("artifact should be a hash - build:, test: or external:");

            Map<String, Object> artifactMap = (Map<String, Object>) artifactObj;
            for (Map.Entry<String, Object> artMap : artifactMap.entrySet()) {
                JsonObject artifactJson = new JsonObject();
                if ("build".equalsIgnoreCase(artMap.getKey()))
                    artifactJson.addProperty("type", "build");
                else if ("test".equalsIgnoreCase(artMap.getKey()))
                    artifactJson.addProperty("type", "test");
                else if (EXTERNAL_ARTIFACT_TYPE_FIELD.equalsIgnoreCase(artMap.getKey())) {
                    artifactJson.addProperty("type", EXTERNAL_ARTIFACT_TYPE_FIELD);
                } else
                    throw new YamlConfigException("expected build:, test:, or external: in artifact, got " + artMap.getKey());

                Map<String, Object> artMapValue = (Map<String, Object>) artMap.getValue();
                if (EXTERNAL_ARTIFACT_TYPE_FIELD.equalsIgnoreCase(artMap.getKey())) {
                    addRequiredString(artifactJson, artMapValue, JSON_JOB_ARTIFACT_ARTIFACT_ID_FIELD, YAML_JOB_ARTIFACT_ARTIFACT_ID_FIELD);
                    addRequiredString(artifactJson, artMapValue, JSON_JOB_ARTIFACT_STORE_ID_FIELD, YAML_JOB_ARTIFACT_STORE_ID_FIELD);
                    super.addConfiguration(artifactJson, (Map<String, Object>) artMapValue.get("configuration"));
                } else {
                    addRequiredString(artifactJson, artMapValue, JSON_JOB_ARTIFACT_SOURCE_FIELD, YAML_JOB_ARTIFACT_SOURCE_FIELD);
                    addOptionalString(artifactJson, artMapValue, JSON_JOB_ARTIFACT_DEST_FIELD, YAML_JOB_ARTIFACT_DEST_FIELD);
                }
                artifactArrayJson.add(artifactJson);
                break;// we read first hash and exit
            }
        }
        jobJson.add(JSON_JOB_ARTIFACTS_FIELD, artifactArrayJson);
    }

    private void addTabs(JsonObject jobJson, Map<String, Object> jobMap) {
        Object tabs = jobMap.get(YAML_JOB_TABS_FIELD);
        if (tabs == null)
            return;
        if (!(tabs instanceof Map))
            throw new YamlConfigException("tabs should be a hash");
        JsonArray tabsJson = new JsonArray();
        Map<String, String> tabsMap = (Map<String, String>) tabs;
        for (Map.Entry<String, String> tab : tabsMap.entrySet()) {
            String tabName = tab.getKey();
            String tabPath = tab.getValue();
            JsonObject tabJson = new JsonObject();
            tabJson.addProperty(JSON_JOB_TAB_NAME_FIELD, tabName);
            tabJson.addProperty(JSON_JOB_TAB_PATH_FIELD, tabPath);
            tabsJson.add(tabJson);
        }
        jobJson.add(JSON_JOB_TABS_FIELD, tabsJson);
    }

    private void addRunInstances(Map<String, Object> jobMap, JsonObject jobJson) {
        String runInstancesText = getOptionalString(jobMap, YAML_JOB_RUN_INSTANCES_FIELD);
        if (runInstancesText != null) {
            if ("all".equalsIgnoreCase(runInstancesText))
                jobJson.addProperty(JSON_JOB_RUN_INSTANCES_FIELD, "all");
            else {
                try {
                    jobJson.addProperty(JSON_JOB_RUN_INSTANCES_FIELD, NumberFormat.getInstance().parse(runInstancesText));
                } catch (ParseException e) {
                    throw new YamlConfigException(YAML_JOB_RUN_INSTANCES_FIELD + " must be 'all' or a number", e);
                }
            }
        }
    }

    private void addTasks(JsonObject jobJson, Map<String, Object> jobMap) {
        Object tasksObj = jobMap.get(YAML_JOB_TASKS_FIELD);
        if (tasksObj == null)
            throw new YamlConfigException("tasks are required in a job");
        JsonArray tasksJson = new JsonArray();
        List<Object> taskList = (List<Object>) tasksObj;
        addTasks(taskList, tasksJson);
        jobJson.add(JSON_JOB_TASKS_FIELD, tasksJson);
    }

    private void addTasks(List<Object> taskList, JsonArray tasksJson) {
        for (Object maybeTask : taskList) {
            if (maybeTask instanceof List) {
                List<Object> taskNestedList = (List<Object>) maybeTask;
                addTasks(taskNestedList, tasksJson);
            } else {
                JsonObject task = taskTransform.transform(maybeTask);
                tasksJson.add(task);
            }
        }
    }

}
