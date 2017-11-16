package org.jenkinsci.plugins;
import hudson.XmlFile;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import hudson.tasks.*;
import hudson.model.*;
import jenkins.model.GlobalConfiguration;


import java.util.Collection;

public class getKubernetesInfo extends GlobalConfiguration{
    public XmlFile getKubernetesUrl(){
        return this.getConfigFile();
    }
}
