package org.jenkinsci.plugins;

import hudson.model.Action;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.json.*;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.net.HttpURLConnection;
import java.net.URL;

public class EnvironmentInfo implements Action{

    private JSONArray ContainerInfo;
    private Run<?, ?> run;
    private String NodeName;
    private String id;
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

    public String getId() {
        return id;
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
        getEnvInfoBuilder BuildInformation =new getEnvInfoBuilder(this.Namespace, this.NodeName, this.id) ;
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
        getEnvInfoBuilder BuildInformation = new getEnvInfoBuilder(this.Namespace, this.NodeName, this.id) ;
        JSONArray ServicesInfo = new JSONArray();
        JSONArray ServicesWithId = new JSONArray();
        JSONArray PodsInfo = new JSONArray();
        JSONArray PodsWithId = new JSONArray();
        PodsInfo = new JSONObject(BuildInformation.getKubeInfo(this.KuberUrl, this.Token, this.Namespace, "pods")).getJSONArray("items");
        ServicesInfo = new JSONObject(BuildInformation.getKubeInfo(this.KuberUrl, this.Token, this.Namespace,"services")).getJSONArray("items");
        for(int i=0; i < ServicesInfo.length(); i++){
            if(ServicesInfo.getJSONObject(i).getJSONObject("metadata").getJSONObject("labels").has("id")){
                if(ServicesInfo.getJSONObject(i).getJSONObject("metadata").getJSONObject("labels").get("id").equals(this.id)){
                    ServicesWithId.put(ServicesInfo.getJSONObject(i));
                }
            }
        }
        for(int i = 0; i < PodsInfo.length(); i++){
            if(PodsInfo.getJSONObject(i).getJSONObject("metadata").getJSONObject("labels").has("id")){
                if(PodsInfo.getJSONObject(i).getJSONObject("metadata").getJSONObject("labels").get("id").equals(this.id)){
                    PodsWithId.put(PodsInfo.getJSONObject(i));
                }
            }
        }
        JSONArray ContainerInfo = BuildInformation.createContainerInfo(PodsWithId, ServicesWithId);
        EnvironmentInfo buildAction = new EnvironmentInfo(ContainerInfo, this.run, this.Namespace, this.id, this.NodeName, this.Token, this.KuberUrl);
        run.replaceAction(buildAction);
    }


    @JavaScriptMethod
    public String getContainerInfo() {
        return ContainerInfo.toString();
    }


    EnvironmentInfo(final JSONArray ContainerInfo, final Run<?, ?> run,String Namespace, String id, String NodeName, String Token, String KubeUrl) {
        this.ContainerInfo = ContainerInfo;
        this.run = run;
        this.NodeName = NodeName;
        this.id = id;
        this.Namespace = Namespace;
        this.Token = Token;
        this.KuberUrl = KubeUrl;
    }
}
