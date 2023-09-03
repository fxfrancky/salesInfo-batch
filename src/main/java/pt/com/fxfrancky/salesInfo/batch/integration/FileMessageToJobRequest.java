package pt.com.fxfrancky.salesInfo.batch.integration;

import lombok.Setter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Date;

@Component
@Setter
public class FileMessageToJobRequest {
    private Job job;
    private String fileName = "input.file.name";

    @Transformer
    public JobLaunchRequest jobLaunchRequest(Message<File> fileMessage){
        JobParametersBuilder jobParametersBuilder =
                new JobParametersBuilder();

        jobParametersBuilder.addString(fileName,
                fileMessage.getPayload().getAbsolutePath());
        jobParametersBuilder.addDate("uniqueness", new Date());

        return new JobLaunchRequest(job, jobParametersBuilder.toJobParameters());

//        var jobParameters = new JobParametersBuilder();
//        jobParameters.addString(fileName, fileMessage.getPayload().getAbsolutePath());
//        jobParameters.addDate("uniqueness", new Date());
//        return new JobLaunchRequest(job, jobParameters.toJobParameters());
    }
}
