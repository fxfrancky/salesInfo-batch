package pt.com.fxfrancky.salesInfo.batch;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;


import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import pt.com.fxfrancky.salesInfo.batch.dto.SalesInfoDTO;
import pt.com.fxfrancky.salesInfo.batch.processor.SalesInfoItemProcessor;
import pt.com.fxfrancky.salesInfo.domain.SalesInfo;

import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@RequiredArgsConstructor
public class SalesInfoJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory emf;
    private final SalesInfoItemProcessor salesInfoItemProcessor;

    @Bean
    public Job importSalesInfo(Step fromFileIntoDatabase){
        return new JobBuilder("importSalesInfo", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(fromFileIntoDatabase)
                .build();
    }

    @Bean
    public Step fromFileIntoDatabase(ItemReader<SalesInfoDTO> salesInfoDTOItemReader){
        return new StepBuilder("fromFileToDatabase",jobRepository)
                .<SalesInfoDTO, Future<SalesInfo>>chunk(100,transactionManager)
                .reader(salesInfoDTOItemReader)
                .processor(asyncItemProcessor())
                .writer(asyncItemWriter())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<SalesInfoDTO> salesInfosFileReader(@Value("#{jobParameters['input.file.name']}") String resource){
        return new FlatFileItemReaderBuilder<SalesInfoDTO>()
                .resource(new FileSystemResource(resource))
                .name("salesInfoFileReader")
                .delimited()
                .delimiter(",")
                .names(new String[]{"product","seller","sellerId","price","city","category"})
                .linesToSkip(1)
                .targetType(SalesInfoDTO.class)
                .build();
    }

    @Bean
    public JpaItemWriter<SalesInfo> salesInfoItemWriter(){
        return new JpaItemWriterBuilder<SalesInfo>()
                .entityManagerFactory(emf)
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor(){
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(15);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("Thread N-> :");
        return executor;
    }

    @Bean
    public AsyncItemProcessor<SalesInfoDTO,SalesInfo> asyncItemProcessor(){
        var asyncItemProcessor = new AsyncItemProcessor<SalesInfoDTO, SalesInfo>();
        asyncItemProcessor.setDelegate(salesInfoItemProcessor);
        asyncItemProcessor.setTaskExecutor(taskExecutor());
        return asyncItemProcessor;
    }

    @Bean
    public AsyncItemWriter<SalesInfo> asyncItemWriter(){
        var asyncWriter = new AsyncItemWriter<SalesInfo>();
        asyncWriter.setDelegate(salesInfoItemWriter());
        return asyncWriter;
    }
}
