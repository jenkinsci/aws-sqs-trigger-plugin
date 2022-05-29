package io.jenkins.plugins.sqs;

import lombok.extern.java.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Log
public class AllTriggers {

    public static final AllTriggers INSTANCE = new AllTriggers();

    private AllTriggers() {
    }

    private List<SqsTrigger> triggerList = new CopyOnWriteArrayList();

    public void add(SqsTrigger trigger) {
        log.fine(() -> "Add SQS trigger, url: " + trigger.getSqsTriggerQueueUrl() + " job: " + trigger.getJobFullName() + ".");
        triggerList.add(trigger);
    }

    public void remove(SqsTrigger trigger) {
        log.fine(() -> "Remove SQS trigger, url: " + trigger.getSqsTriggerQueueUrl() + " job: " + trigger.getJobFullName() + ".");
        triggerList.remove(trigger);
    }

    public List<SqsTrigger> getAll() {
        return triggerList;
    }

}
