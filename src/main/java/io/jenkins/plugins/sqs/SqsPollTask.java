package io.jenkins.plugins.sqs;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.sqs.model.Message;
import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import lombok.extern.java.Log;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Extension
@Restricted(NoExternalUse.class)
@Symbol("sqsPollTask")
@Log
public class SqsPollTask extends AsyncPeriodicWork {


    private transient SqsPoller sqsPoller;

    public SqsPollTask() {
        super("sqsPollTask");
    }

    @Override
    protected void execute(TaskListener listener) throws IOException, InterruptedException {


        List<SqsTrigger> triggers = AllTriggers.INSTANCE.getAll();
        log.fine(() -> "Find " + triggers.size() + " SQS triggers.");

        initService();

        triggers.stream()
                .parallel()
                .forEach(trigger -> {
                    String credentialsId = trigger.getSqsTriggerCredentialsId();
                    AWSCredentials awsCredentials = AwsCredentialsHelper.getAWSCredentials((credentialsId));
                    List<Message> messages = sqsPoller.getMessagesAndDelete(trigger.getSqsTriggerQueueUrl(), awsCredentials);
                    trigger.buildJob(messages);
                });


    }

    public void setSqsPoller(SqsPoller sqsPoller) {
        this.sqsPoller = sqsPoller;
    }

    private void initService() {
        if (sqsPoller == null) {
            sqsPoller = new SqsPollerImpl();
        }
    }

    @Override
    public long getRecurrencePeriod() {
        return TimeUnit.MINUTES.toMillis(1);
    }


}
