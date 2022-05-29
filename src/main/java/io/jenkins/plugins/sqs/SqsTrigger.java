package io.jenkins.plugins.sqs;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.sqs.model.Message;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.Extension;
import hudson.model.*;
import hudson.security.ACL;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log
public class SqsTrigger extends Trigger<Job<?, ?>> {
    @DataBoundSetter
    @Getter
    @Setter
    private String sqsTriggerQueueUrl;
    @DataBoundSetter
    @Getter
    @Setter
    private String sqsTriggerCredentialsId;
    @DataBoundSetter
    @Getter
    @Setter
    private boolean sqsDisableConcurrentBuilds;

    @Override
    public void start(Job project, boolean newInstance) {
        super.start(project, newInstance);
        AllTriggers.INSTANCE.add(this);
    }

    @Override
    public void stop() {
        AllTriggers.INSTANCE.remove(this);
        super.stop();
    }

    public void buildJob(List<Message> messages) {
        log.fine(() -> "Receive " + messages.size() + " messages.");

        if (job != null && messages.size() > 0) {
            if (sqsDisableConcurrentBuilds) {
                log.fine(() -> "disableConcurrentBuilds is True.");
                if (job.isBuilding()) {
                    log.fine(() -> "Receive " + messages.size() + " messages. Ignore all because there's build running.");
                    return;
                } else {
                    log.fine(() -> "Receive " + messages.size() + " messages. Pick the first one and ignore others.");
                    Message message = messages.get(0);
                    buildOne(message);
                }
            } else {
                messages.forEach(message -> buildOne(message));
            }
        }
    }

    private void buildOne(Message message) {
        log.fine(() -> "Build triggered by " + message.getMessageId() + ".");
        List<ParameterValue> parameters = message
                .getMessageAttributes()
                .entrySet()
                .stream()
                .map(entry -> new StringParameterValue("sqs_" + entry.getKey(), entry.getValue().getStringValue()))
                .collect(Collectors.toList());
        parameters.add(new StringParameterValue("sqs_body", message.getBody()));
        parameters.add(new StringParameterValue("sqs_message_id", message.getMessageId()));
        ParameterizedJobMixIn.scheduleBuild2(job, 0,
                new CauseAction(new SqsCause(message.getMessageId())),
                new ParametersAction(parameters));
    }

    @DataBoundConstructor
    public SqsTrigger() {
    }


    public String getJobFullName() {
        return Optional.ofNullable(job).map(Job::getFullName).orElse("");
    }

    @Extension
    public static class SqsTriggerDescriptor extends TriggerDescriptor {

        @Override
        public boolean isApplicable(final Item item) {
            return Job.class.isAssignableFrom(item.getClass());
        }

        @Override
        public String getDisplayName() {
            return "AWS SQS Trigger";
        }

        private transient SqsPoller sqsPoller;

        public ListBoxModel doFillSqsTriggerCredentialsIdItems(@AncestorInPath Item item) {
            StandardListBoxModel result = new StandardListBoxModel();

            return result
                    .includeEmptyValue() // (3)
                    .includeMatchingAs(ACL.SYSTEM,
                            Jenkins.get(),
                            AmazonWebServicesCredentials.class,
                            Collections.emptyList(),
                            CredentialsMatchers.instanceOf(
                                    AmazonWebServicesCredentials.class));// (4)
        }

        public FormValidation doCheckSqsTriggerQueueUrl(@AncestorInPath Item item, @QueryParameter String sqsTriggerQueueUrl) {
            if (StringUtils.isEmpty(sqsTriggerQueueUrl)) {
                return FormValidation.error("Queue Url is Empty");
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckSqsTriggerCredentialsId(@AncestorInPath Item item) {
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok(); // (3)
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ)
                        && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return FormValidation.ok(); // (3)
                }
            }
            return FormValidation.ok();
        }

        public FormValidation doTestConnection(@QueryParameter("sqsTriggerQueueUrl") final String queueUrl,
                                               @QueryParameter("sqsTriggerCredentialsId") final String credentialsId) {
            try {
                AWSCredentials awsCredentials = AwsCredentialsHelper.getAWSCredentials((credentialsId));
                initService();
                sqsPoller.testConnection(queueUrl, awsCredentials);
                return FormValidation.ok("Success");
            } catch (Exception e) {
                return FormValidation.error("Error  : " + e.getMessage());
            }
        }

        private void initService() {
            if (sqsPoller == null) {
                sqsPoller = new SqsPollerImpl();
            }
        }
    }


    class SqsCause extends Cause {

        private String messageId;

        public SqsCause(String messageId) {
            this.messageId = messageId;
        }

        @Override
        public String getShortDescription() {
            return "SQS queue: " + SqsTrigger.this.sqsTriggerQueueUrl + ", Message: " + messageId;
        }
    }
}
