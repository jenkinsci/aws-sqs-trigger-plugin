package io.jenkins.plugins.sqs;

import com.amazonaws.auth.AWSCredentials;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.security.ACL;
import jenkins.model.Jenkins;

import java.util.Collections;
import java.util.Optional;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

public class AwsCredentialsHelper {
    static public AWSCredentials getAWSCredentials(String credentialsId) {
        AmazonWebServicesCredentials credentials = CredentialsMatchers.firstOrNull(
                lookupCredentials(AmazonWebServicesCredentials.class,
                        Jenkins.getInstance(), ACL.SYSTEM,
                        Collections.<DomainRequirement>emptyList()),

                CredentialsMatchers.withId(credentialsId)
        );
        return Optional.ofNullable(credentials).map(AmazonWebServicesCredentials::getCredentials).orElse(null);

    }
}
