package org.jenkinsci.plugins;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Run;
import org.json.*;

import javax.annotation.CheckForNull;

public class KubernetesInfoAction implements Action{

    public JSONArray ContainerInfo;
//    public String messages;
    public Run<?, ?> run;
    @Override
    public String getIconFileName() {
        return "package.png";
    }

    @Override
    public String getDisplayName() {
        return "Kubernetes Info";
    }

    @Override
    public String getUrlName() {
        return "KubernetesInfo";
    }

    public JSONArray getContainerInfo() {
//        JSONObject messagesJson = new JSONObject(messages);
//        JSONArray messagesJsonArray = messagesJson.getJSONArray("items");
        return ContainerInfo;
    }

    public Run<?, ?> getRun() {
        return run;
    }

    KubernetesInfoAction(final JSONArray ContainerInfo, final Run<?, ?> run) {
        this.ContainerInfo = ContainerInfo;
        this.run = run;
    }
}
