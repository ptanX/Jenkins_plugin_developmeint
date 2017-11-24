package org.jenkinsci.plugins;

import hudson.model.Action;
import hudson.model.Run;
import org.json.*;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.net.HttpURLConnection;
import java.net.URL;

public class EnvironmentInfo implements Action{

    private JSONArray ContainerInfo;
    private Run<?, ?> run;
    private String NodeName;
    private String ServiceName;
    private String Namespace;
    private String Token;
    private String KuberUrl;

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

    public Run<?, ?> getRun() {
        return run;
    }

    public String getNamespace() {
        return Namespace;
    }

    public String getServiceName() {
        return ServiceName;
    }

    public String getNodeName(){
        return NodeName;
    }

    public String getToken(){
        return Token;
    }

    public String getKuberUrl(){
        return KuberUrl;
    }


    @JavaScriptMethod
    public String deletePod(String podsName) throws Exception {
        getEnvInfoBuilder BuildInformation =new getEnvInfoBuilder(this.Namespace, this.ServiceName, this.NodeName) ;
        BuildInformation.turnOffSslValidation();
        String KubeUrl = this.KuberUrl + "/api/v1/namespaces/" + this.Namespace + "/pods/" + podsName;
        String AuthorizationValue = "Bearer " + this.Token;
        URL obj = new URL(KubeUrl);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("DELETE");
        con.setRequestProperty("Authorization", AuthorizationValue);
        String response = Integer.toString(con.getResponseCode());
        Thread.sleep(20000);
        this.refreshInformation();
        return response;
    }


    @JavaScriptMethod
    public void refreshInformation() throws Exception {
        getEnvInfoBuilder BuildInformation = new getEnvInfoBuilder(this.Namespace, this.ServiceName, this.NodeName) ;
        JSONObject ServicesInfo = new JSONObject();
        JSONObject ServiceInfo = new JSONObject();
        JSONArray PodsInfo = new JSONArray();
        PodsInfo = new JSONObject(BuildInformation.getKubeInfo(this.KuberUrl, this.Token, this.Namespace, "pods")).getJSONArray("items");
        ServicesInfo = new JSONObject(BuildInformation.getKubeInfo(this.KuberUrl, this.Token, this.Namespace,"services"));
        for(int i=0; i < ServicesInfo.getJSONArray("items").length(); i++){
            if(ServicesInfo.getJSONArray("items").getJSONObject(i).getJSONObject("metadata").get("name").equals(this.ServiceName)){
                ServiceInfo = ServicesInfo.getJSONArray("items").getJSONObject(i);
                break;
            }
        }
        JSONArray ContainerInfo = BuildInformation.createContainerInfo(PodsInfo, ServiceInfo);
        EnvironmentInfo buildAction = new EnvironmentInfo(ContainerInfo, this.run, this.Namespace, this.ServiceName, this.NodeName, this.Token, this.KuberUrl);
        run.replaceAction(buildAction);
    }


    @JavaScriptMethod
    public String getContainerInfo() {
        return ContainerInfo.toString();
    }


    EnvironmentInfo(final JSONArray ContainerInfo, final Run<?, ?> run,String Namespace, String ServiceName, String NodeName, String Token, String KubeUrl) {
        this.ContainerInfo = ContainerInfo;
        this.run = run;
        this.NodeName = NodeName;
        this.ServiceName = ServiceName;
        this.Namespace = Namespace;
        this.Token = Token;
        this.KuberUrl = KubeUrl;
    }
}
