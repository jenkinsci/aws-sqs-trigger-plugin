package io.jenkins.plugins.sqs;

import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsImpl;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import hudson.model.FreeStyleProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsSessionRule;

import static org.assertj.core.api.Assertions.assertThat;


public class SqsTriggerUITest {

    @Rule
    public JenkinsSessionRule sessions = new JenkinsSessionRule();
    private AmazonWebServicesCredentials awsCredentials;
    private FreeStyleProject project;


    @Before
    public void setup() throws Throwable {
        sessions.then(r -> {
            awsCredentials = new AWSCredentialsImpl(CredentialsScope.GLOBAL, "aws", "awskey", "awssecret", "aws");
            SystemCredentialsProvider systemCredentialsProvider = r.getInstance().getExtensionList(SystemCredentialsProvider.class).get(0);
            systemCredentialsProvider.getCredentials().add(awsCredentials);
            systemCredentialsProvider.save();
            project = r.getInstance().createProject(FreeStyleProject.class, "test");
        });
    }

    @Test
    public void uiAndStorage() throws Throwable {

        sessions.then(r -> {


            HtmlForm config = r.createWebClient().goTo(project.getUrl() + "/configure").getFormByName("config");
            HtmlCheckBoxInput sqsTriggerCheckBox = config.getInputByName("io-jenkins-plugins-sqs-SqsTrigger");

            sqsTriggerCheckBox.setChecked(true);
            HtmlTextInput queueNameInput = config.getInputByName("_.sqsTriggerQueueUrl");
            queueNameInput.setText("some-aws-queue-url");
            HtmlSelect credentials = config.getSelectByName("_.sqsTriggerCredentialsId");
            credentials.setSelectedAttribute("aws", true);
            HtmlCheckBoxInput skipIfLastBuildNotFinished = config.getInputByName("_.sqsSkipIfLastBuildNotFinished");
            skipIfLastBuildNotFinished.setChecked(true);

            r.submit(config);
        });
        sessions.then(r -> {
            HtmlForm config = r.createWebClient().goTo(project.getUrl() + "/configure").getFormByName("config");
            HtmlCheckBoxInput sqsTriggerCheckBox = config.getInputByName("io-jenkins-plugins-sqs-SqsTrigger");
            assertThat(sqsTriggerCheckBox.isChecked()).isTrue();

            HtmlTextInput queueNameInput = config.getInputByName("_.sqsTriggerQueueUrl");
            assertThat(queueNameInput.getText()).isEqualTo("some-aws-queue-url");

            HtmlSelect credentials = config.getSelectByName("_.sqsTriggerCredentialsId");
            assertThat(credentials.getSelectedOptions().get(0).getValueAttribute()).isEqualTo("aws");

            HtmlCheckBoxInput skipIfLastBuildNotFinished = config.getInputByName("_.sqsSkipIfLastBuildNotFinished");
            assertThat(skipIfLastBuildNotFinished.isChecked()).isTrue();

        });

    }


}
