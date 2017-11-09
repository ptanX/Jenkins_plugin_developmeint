package org.jenkinsci.plugins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class BuilderOrPublisher extends Builder implements SimpleBuildStep{
    public String KubernetesUrl;
    public String Token;

    @DataBoundConstructor
    public BuilderOrPublisher(String KubernetesUrl, String Token) {
        this.KubernetesUrl = KubernetesUrl;
        this.Token = Token;
    }

    public String getKubernetesUrl() {
        return KubernetesUrl;
    }

    public String getToken() {
        return Token;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        String messages = null;
        listener.getLogger().println("Start get kubernetes information");
        try {
            messages  = this.getKubernetesInfo();
        }catch (Exception e){listener.getLogger().println("Loi khi build");}
//        listener.getLogger().println(messages);
        KubernetesInfoAction buildAction = new KubernetesInfoAction(messages, run);
        run.addAction(buildAction);
    }

    @Symbol("kubernetes")
    @Extension
    public static final class  DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Kubernetes information";
        }
    }

    public String getKubernetesInfo() throws Exception{
        String AuthorizationValue = "Bearer " + Token;
        URL obj = new URL(KubernetesUrl);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", AuthorizationValue);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        //return AuthorizationValue;
        return response.toString();
    }
}
