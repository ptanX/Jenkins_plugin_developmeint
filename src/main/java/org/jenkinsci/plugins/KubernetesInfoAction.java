package org.jenkinsci.plugins;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Run;

import javax.annotation.CheckForNull;

public class KubernetesInfoAction implements Action{

    public String messages;
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

    public String getMessages() {
        return messages;
    }

    public Run<?, ?> getRun() {
        return run;
    }

    KubernetesInfoAction(final String messages, final Run<?, ?> run) {
        this.messages = messages;
        this.run = run;
    }
}
