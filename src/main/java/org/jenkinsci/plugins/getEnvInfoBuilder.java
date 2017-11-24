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
import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

public class getEnvInfoBuilder extends Builder implements SimpleBuildStep {
    public String Namespace;
    public String ServiceName;
    public String NodeName;

    @DataBoundConstructor
    public getEnvInfoBuilder(String Namespace, String ServiceName, String NodeName) {
        this.Namespace = Namespace;
        this.ServiceName = ServiceName;
        this.NodeName = NodeName;
    }


    public String getNamespace() {
        return Namespace;
    }

    public String getServiceName() {
        return ServiceName;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("Starting get Environment information ");
        JSONObject ServicesInfo = new JSONObject();
        JSONObject ServiceInfo = new JSONObject();
        JSONArray PodsInfo = new JSONArray();
        Thread.sleep(20000);
        try {
            PodsInfo = new JSONObject(this.getKubeInfo(this.getKubeCloud().getServerUrl(),this.getKubeToken(this.getKubeCloud()), Namespace, "pods")).getJSONArray("items");
            ServicesInfo = new JSONObject(this.getKubeInfo(this.getKubeCloud().getServerUrl(),this.getKubeToken(this.getKubeCloud()), Namespace,"services"));
        } catch (Exception e){listener.getLogger().println("Loi khi build");}
        for(int i=0; i < ServicesInfo.getJSONArray("items").length(); i++){
            if(ServicesInfo.getJSONArray("items").getJSONObject(i).getJSONObject("metadata").get("name").equals(ServiceName)){
                ServiceInfo = ServicesInfo.getJSONArray("items").getJSONObject(i);
                break;
            }
        }

        JSONArray ContainerInfo = this.createContainerInfo(PodsInfo, ServiceInfo);
        EnvironmentInfo buildAction = new EnvironmentInfo(ContainerInfo, run, this.Namespace, this.ServiceName, this.NodeName, this.getKubeToken(this.getKubeCloud()), this.getKubeCloud().getServerUrl());
        run.addAction(buildAction);
    }


    @Symbol("kubernetesinfo")
    @Extension
    public static final class  DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Kubernetes Information";
        }
    }


    public KubernetesCloud getKubeCloud() throws AbortException {
        Cloud cloud = Jenkins.getInstance().getCloud(this.getKubeCloudName());
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


    public JSONArray createContainerInfo(JSONArray PodsInfo, JSONObject ServiceInfo) {
        JSONArray ContainerInfo = new JSONArray();
        for(int i = 0; i < ServiceInfo.getJSONObject("spec").getJSONObject("selector").names().length(); i++){
            String Selector = ServiceInfo.getJSONObject("spec").getJSONObject("selector").names().getString(i);
            for(int j = 0; j < PodsInfo.length(); j++){
                if(PodsInfo.getJSONObject(j).getJSONObject("metadata").getJSONObject("labels").has(Selector)){
                    if (PodsInfo.getJSONObject(j).getJSONObject("metadata").getJSONObject("labels").get(Selector).
                            equals(ServiceInfo.getJSONObject("spec").getJSONObject("selector").get(Selector))){
                        JSONArray PodContainers = PodsInfo.getJSONObject(j).getJSONObject("spec").getJSONArray("containers");
                        for(int k = 0;k < PodContainers.length(); k++){
                            JSONObject PodContainerInfo = new JSONObject();
                            JSONArray InternalInfo = new JSONArray();
                            JSONArray ExternalInfo = new JSONArray();
                            PodContainerInfo.put("PodName",PodsInfo.getJSONObject(j).getJSONObject("metadata").get("name"));
                            PodContainerInfo.put("ContainerName", PodContainers.getJSONObject(k).get("name"));
                            if ((Boolean) PodsInfo.getJSONObject(j).getJSONObject("status").getJSONArray("containerStatuses").getJSONObject(0).get("ready")){
                                PodContainerInfo.put("ContainerStatus","Running");

                            } else {
                                PodContainerInfo.put("ContainerStatus","failed");
                            }
                            for(int x = 0; x < PodContainers.getJSONObject(k).getJSONArray("ports").length(); x++){
                                JSONObject InternalPort = new JSONObject();
                                JSONObject ExternalPort = new JSONObject();
                                InternalPort.put("Port", PodContainers.getJSONObject(k).getJSONArray("ports").getJSONObject(x).get("containerPort"));
                                if(PodsInfo.getJSONObject(j).getJSONObject("status").has("podIP")){
                                    InternalPort.put("InternalIP", PodsInfo.getJSONObject(j).getJSONObject("status").get("podIP"));
                                }
                                if (PodContainers.getJSONObject(k).getJSONArray("ports").getJSONObject(x).has("name")){
                                    InternalPort.put("Service",PodContainers.getJSONObject(k).getJSONArray("ports").getJSONObject(x).get("name"));
                                }else {
                                    InternalPort.put("Service","");
                                }
                                for(int y = 0; y < ServiceInfo.getJSONObject("spec").getJSONArray("ports").length(); y++){
                                    if(ServiceInfo.getJSONObject("spec").getJSONArray("ports").getJSONObject(y).get("targetPort").equals(InternalPort.get("Port"))){
                                        ExternalPort.put("Port", ServiceInfo.getJSONObject("spec").getJSONArray("ports").getJSONObject(y).get("nodePort"));
                                        ExternalPort.put("ExternalIP",PodsInfo.getJSONObject(j).getJSONObject("status").get("hostIP"));
                                        if (ServiceInfo.getJSONObject("spec").getJSONArray("ports").getJSONObject(y).has("name")){
                                            ExternalPort.put("Service",ServiceInfo.getJSONObject("spec").getJSONArray("ports").getJSONObject(y).get("name"));
                                        }else {
                                            ExternalPort.put("Service","");
                                        }
                                        break;
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
                }
            }
        }
        return ContainerInfo;
    }
}