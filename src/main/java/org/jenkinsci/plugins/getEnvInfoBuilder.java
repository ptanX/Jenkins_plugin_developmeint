package org.jenkinsci.plugins;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.*;
import hudson.model.*;
import hudson.security.ACL;
import hudson.slaves.Cloud;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.csanchez.jenkins.plugins.kubernetes.KubernetesCloud;
import org.csanchez.jenkins.plugins.kubernetes.KubernetesSlave;
import org.csanchez.jenkins.plugins.kubernetes.TokenProducer;
import org.jenkinsci.Symbol;
import org.json.*;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.*;

public class getEnvInfoBuilder extends Builder implements SimpleBuildStep {
    public String Namespace;
    public String NodeName;
    public String id;

    @DataBoundConstructor
    public getEnvInfoBuilder(String Namespace, String NodeName, String id) {
        this.Namespace = Namespace;
        this.NodeName = NodeName;
        this.id = id;
    }


    public String getNamespace() {
        return Namespace;
    }

    public String getId() {
        return id;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("Starting get Environment information ");
        JSONArray ServicesInfo = new JSONArray();
        JSONArray ServicesWithId = new JSONArray();
        JSONArray PodsInfo = new JSONArray();
        JSONArray PodsWithId = new JSONArray();
        Thread.sleep(20000);
        try {
            PodsInfo = new JSONObject(this.getKubeInfo(this.getKubeCloud().getServerUrl(),this.getKubeToken(this.getKubeCloud()), Namespace, "pods")).getJSONArray("items");
            ServicesInfo = new JSONObject(this.getKubeInfo(this.getKubeCloud().getServerUrl(),this.getKubeToken(this.getKubeCloud()), Namespace,"services")).getJSONArray("items");
        } catch (Exception e){listener.getLogger().println("Loi khi build");}
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
        JSONArray ContainerInfo = this.createContainerInfo(PodsWithId, ServicesWithId);
        EnvironmentInfo buildAction = new EnvironmentInfo(ContainerInfo, run, this.Namespace, this.id, this.NodeName, this.getKubeToken(this.getKubeCloud()), this.getKubeCloud().getServerUrl());
        run.addAction(buildAction);
    }


    @Symbol("getEnvironmentInfo")
    @Extension
    public static final class  DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Server Information";
        }
    }


    public KubernetesCloud getKubeCloud() throws AbortException {
        Cloud cloud = Jenkins.getInstance().getCloud("kubernetes");
        if(cloud instanceof KubernetesCloud){
            return (KubernetesCloud) cloud;
        } else{
            return null;
        }
    }


    public String getKubeCloudName() throws AbortException {
        List<Node> nodes = Jenkins.getInstance().getNodes();
        for(Node node: nodes){
            if (node.getLabelString().equals(this.NodeName) && (node instanceof KubernetesSlave)){
                KubernetesSlave slave = (KubernetesSlave) node;
                return slave.getCloudName();
            }
        }
        return "Khong tim thay node";
    }


    private String getKubeToken(KubernetesCloud cloud) throws IOException {
        StandardCredentials standardCredential = this.getCredentials(cloud.getCredentialsId());
        return ((TokenProducer) standardCredential).getToken(cloud.getServerUrl(), cloud.getServerCertificate(), cloud.isSkipTlsVerify());
    }


    private StandardCredentials getCredentials(String credentials) {
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(StandardCredentials.class,
                        Jenkins.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList()),
                CredentialsMatchers.withId(credentials)
        );
    }


    public String getKubeInfo(String KuberUrl, String Token, String Namespace, String Factor) throws Exception{
        this.turnOffSslValidation();
        String AuthorizationValue = "Bearer " + Token;
        URL obj = new URL(KuberUrl + "/api/v1/namespaces/" + Namespace + "/" + Factor);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", AuthorizationValue);
        con.setRequestProperty("Accept", "application/json");
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }


    public void turnOffSslValidation() throws Exception{
        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
        };

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }


    public boolean compareServiceAndPods(JSONObject ServiceSelector, JSONObject PodLabels){
        Iterator<String> ListSelectors = ServiceSelector.keys();
        while(ListSelectors.hasNext()){
            String Key = ListSelectors.next();
            if (!(PodLabels.has(Key) && PodLabels.get(Key).equals(ServiceSelector.get(Key)))){
                return false;
            }
        }
        return true;
    }


    public JSONArray createContainerInfo(JSONArray PodsWithId, JSONArray ServicesWithId){
        JSONArray ContainerInfo = new JSONArray();
        if(PodsWithId.length() > 0){
            for(int i = 0; i < PodsWithId.length(); i++){
                JSONObject PodContainerInfo = new JSONObject();
                JSONObject PodContainer = PodsWithId.getJSONObject(i).getJSONObject("spec").getJSONArray("containers").getJSONObject(0);
                JSONArray InternalInfo = new JSONArray();
                JSONArray ExternalInfo = new JSONArray();
                JSONObject EnvironmentVariables = new JSONObject();
                PodContainerInfo.put("PodName",PodsWithId.getJSONObject(i).getJSONObject("metadata").get("name"));
                PodContainerInfo.put("ContainerName", PodContainer.get("name"));
                if ((Boolean) PodsWithId.getJSONObject(i).getJSONObject("status").getJSONArray("containerStatuses").getJSONObject(0).get("ready")){
                    PodContainerInfo.put("ContainerStatus","Running");

                } else {
                    PodContainerInfo.put("ContainerStatus","failed");
                }
                if(PodContainer.has("env")){
                    for(int y = 0; y < PodContainer.getJSONArray("env").length(); y++){
                        if(!PodContainer.getJSONArray("env").getJSONObject(y).get("name").equals("POD_IP")){
                            EnvironmentVariables.put((String) PodContainer.getJSONArray("env").getJSONObject(y).get("name"),
                                    PodContainer.getJSONArray("env").getJSONObject(y).get("value"));
                        }
                    }
                }
                PodContainerInfo.put("env", EnvironmentVariables);
                for(int j = 0; j < PodContainer.getJSONArray("ports").length(); j++){
                    JSONObject InternalPort = new JSONObject();
                    JSONObject ExternalPort = new JSONObject();
                    InternalPort.put("Port", PodContainer.getJSONArray("ports").getJSONObject(j).get("containerPort"));
                    if(PodsWithId.getJSONObject(i).getJSONObject("status").has("podIP")){
                        InternalPort.put("InternalIP", PodsWithId.getJSONObject(i).getJSONObject("status").get("podIP"));
                    }
                    if (PodContainer.getJSONArray("ports").getJSONObject(j).has("name")){
                        InternalPort.put("Service",PodContainer.getJSONArray("ports").getJSONObject(j).get("name"));
                    }else {
                        InternalPort.put("Service","");
                    }
                    for(int k = 0; k < ServicesWithId.length(); k++){
                        if(this.compareServiceAndPods(ServicesWithId.getJSONObject(k).getJSONObject("spec").getJSONObject("selector"),
                                PodsWithId.getJSONObject(i).getJSONObject("metadata").getJSONObject("labels"))){
                            for(int x = 0; x < ServicesWithId.getJSONObject(k).getJSONObject("spec").getJSONArray("ports").length(); x++){
                                if(ServicesWithId.getJSONObject(k).getJSONObject("spec").getJSONArray("ports").getJSONObject(x).get("targetPort").equals(InternalPort.get("Port"))){
                                    ExternalPort.put("Port", ServicesWithId.getJSONObject(k).getJSONObject("spec").getJSONArray("ports").getJSONObject(x).get("nodePort"));
                                    ExternalPort.put("ExternalIP",PodsWithId.getJSONObject(i).getJSONObject("status").get("hostIP"));
                                    if (ServicesWithId.getJSONObject(k).getJSONObject("spec").getJSONArray("ports").getJSONObject(x).has("name")){
                                        ExternalPort.put("Service",ServicesWithId.getJSONObject(k).getJSONObject("spec").getJSONArray("ports").getJSONObject(x).get("name"));
                                    }else {
                                        ExternalPort.put("Service","");
                                    }
                                }
                            }
                        }
                    }
                    InternalInfo.put(InternalPort);
                    ExternalInfo.put(ExternalPort);
                }
                PodContainerInfo.put("InternalInfo", InternalInfo);
                PodContainerInfo.put("ExternalInfo", ExternalInfo);
                ContainerInfo.put(PodContainerInfo);
            }
        }
        return ContainerInfo;
    }
}
