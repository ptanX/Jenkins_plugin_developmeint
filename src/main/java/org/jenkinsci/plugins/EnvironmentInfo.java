package org.jenkinsci.plugins;

import hudson.model.Action;
import hudson.model.Run;
import org.json.*;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class EnvironmentInfo implements Action{

    private JSONArray ContainerInfo;
    private Run<?, ?> run;
    private String NodeName;
    private String ServiceName;
    private String Namespace;

    @Override
    public String getIconFileName() {
        return "clipboard.png";
    }

    @Override
    public String getDisplayName() {
        return "Server Information";
    }

    @Override
    public String getUrlName() {
        return "ServerInformation";
    }


    @JavaScriptMethod
    public String deletePod(String podsName) throws IOException {
        BuilderOrPublisher BuildInformation =new BuilderOrPublisher(this.Namespace, this.ServiceName, this.NodeName) ;
        String KubeUrl = BuildInformation.getKubeCloud().getServerUrl() + "/api/v1/Namespaces/" + "default" + "/pods/" + podsName;
        String Token = BuildInformation.getKubeToken(BuildInformation.getKubeCloud());
        String AuthorizationValue = "Bearer " + Token;
        URL obj = new URL(KubeUrl);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("DELETE");
        con.setRequestProperty("Authorization", AuthorizationValue);
        String response = Integer.toString(con.getResponseCode());
        return response;
    }

    @JavaScriptMethod
    public String getContainerInfo() {
        return ContainerInfo.toString();
    }

    @JavaScriptMethod
    public void refreshInformation() throws Exception {
        BuilderOrPublisher BuildInformation = new BuilderOrPublisher(this.Namespace, this.ServiceName, this.NodeName) ;
        JSONObject ServicesInfo = new JSONObject();
        JSONObject ServiceInfo = new JSONObject();
        JSONArray PodsInfo = new JSONArray();
        PodsInfo = new JSONObject(BuildInformation.getKubeInfo(Namespace, "pods")).getJSONArray("items");
        ServicesInfo = new JSONObject(BuildInformation.getKubeInfo(Namespace,"services"));
        for(int i=0; i < ServicesInfo.getJSONArray("items").length(); i++){
            if(ServicesInfo.getJSONArray("items").getJSONObject(i).getJSONObject("metadata").get("name").equals(ServiceName)){
                ServiceInfo = ServicesInfo.getJSONArray("items").getJSONObject(i);
                break;
            }
        }
        JSONArray ContainerInfo = BuildInformation.createContainerInfo(PodsInfo, ServiceInfo);
        EnvironmentInfo buildAction = new EnvironmentInfo(ContainerInfo, run, this.Namespace, this.ServiceName, this.NodeName);
        run.addAction(buildAction);
//        EnvironmentInfo buildAction = new EnvironmentInfo(Information, this.run);
//        this.run.replaceAction(buildAction);
    }

    public Run<?, ?> getRun() {
        return run;
    }

    EnvironmentInfo(final JSONArray ContainerInfo, final Run<?, ?> run,String Namespace, String ServiceName, String NodeName ) {
        this.ContainerInfo = ContainerInfo;
        this.run = run;
        this.NodeName = NodeName;
        this.ServiceName = ServiceName;
        this.Namespace = Namespace;
    }
}
