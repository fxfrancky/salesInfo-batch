package pt.com.fxfrancky.salesInfo.batch.integration;

import lombok.RequiredArgsConstructor;
import org.aspectj.bridge.MessageHandler;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.launch.JobLaunchingGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;

@Component
@EnableIntegration
@IntegrationComponentScan
@RequiredArgsConstructor
public class SalesInfoIntegrationConfig {

    private final Job importSalesInfo;
    private final JobRepository jobRepository;

    @Value("${sales.info.directory}")
    private String salesDirectory;

    @Bean
    public IntegrationFlow integrationFlow(){
        // process the next file 5 seconds after the end of the previous file (process files 1 by 1 maxMessagesPerPoll)
        //we also need a channel where we are going to send the file
        return IntegrationFlow.from(fileReadingMessageSource(),
                sourcePolling -> sourcePolling.poller(Pollers.fixedDelay(Duration.ofSeconds(5)) .maxMessagesPerPoll(1)))
                .channel(fileIn())
                .handle(fileRenameProcessingHandler())
                .transform(fileMessageToJobRequest())
                .handle(jobLaunchingGateway())
                .log(LoggingHandler.Level.WARN, "headers.id + ': ' + payload")
                .get();
    }

    public FileReadingMessageSource fileReadingMessageSource(){
        var messageSource = new FileReadingMessageSource();
        messageSource.setDirectory(new File(salesDirectory));
        messageSource.setFilter(new SimplePatternFileListFilter("*.csv"));
        return messageSource;
    }

    public DirectChannel fileIn(){
        return new DirectChannel();
    }

    // Rename the file while processing it
    public FileWritingMessageHandler fileRenameProcessingHandler(){
        var fileWritingMessage = new FileWritingMessageHandler(new File(salesDirectory));
        fileWritingMessage.setFileExistsMode(FileExistsMode.REPLACE);
        fileWritingMessage.setDeleteSourceFiles(Boolean.TRUE);
        fileWritingMessage.setFileNameGenerator(new DefaultFileNameGenerator());
        fileWritingMessage.setFileNameGenerator(fileNameGenerator());
        fileWritingMessage.setRequiresReply(Boolean.FALSE);
        return  fileWritingMessage;
    }

    public DefaultFileNameGenerator fileNameGenerator(){
        var fileNameGenerator = new DefaultFileNameGenerator();
        fileNameGenerator.setExpression("payload.name + '.processing'");
        return fileNameGenerator;
    }

    public FileMessageToJobRequest fileMessageToJobRequest(){
        FileMessageToJobRequest fileMessageToJobRequest = new FileMessageToJobRequest();
        fileMessageToJobRequest.setJob(importSalesInfo);
        return fileMessageToJobRequest;
    }

    // To Launch Our ob
    public JobLaunchingGateway jobLaunchingGateway() {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SyncTaskExecutor());
        JobLaunchingGateway jobLaunchingGateway = new JobLaunchingGateway(jobLauncher);

        return jobLaunchingGateway;

    }
}
