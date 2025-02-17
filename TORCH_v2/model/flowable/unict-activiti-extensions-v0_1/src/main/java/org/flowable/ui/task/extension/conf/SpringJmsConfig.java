package org.flowable.ui.task.extension.conf;

import javax.jms.ConnectionFactory;
import javax.sql.DataSource;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.executor.jms.HistoryJobMessageListener;
import org.flowable.spring.executor.jms.JobMessageListener;
import org.flowable.spring.executor.jms.MessageBasedJobManager;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@AutoConfigureAfter(ProcessEngineAutoConfiguration.class)
public class SpringJmsConfig {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SpringJmsConfig.class);
	
	public SpringJmsConfig(){
		LOGGER.info("Reading SpringJmsConfig configuration...");
	}
	
    @Bean
    public DataSource dataSource() {
		LOGGER.info("Creating dataSource bean...");
    	
        HikariDataSource dataSource = new HikariDataSource();
//        dataSource.setJdbcUrl("jdbc:h2:mem:flowable-spring-jms-test;DB_CLOSE_DELAY=1000");
//        dataSource.setDriverClassName("org.h2.Driver");
//        dataSource.setUsername("sa");
//        dataSource.setPassword("");
	      dataSource.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/flowable?characterEncoding=UTF-8&useSSL=false");
	      dataSource.setDriverClassName("com.mysql.jdbc.Driver");
	      dataSource.setUsername("root");
	      dataSource.setPassword("dev567!!ved89");      
//	      dataSource.setPassword("root");
	      	      
        return dataSource;
    }

    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager() {
		LOGGER.info("Creating transactionManager bean...");
    	
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(dataSource());
        return transactionManager;
    }
    
    @Bean 
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> customProcessEngineConfigurer() {
		LOGGER.info("Creating customProcessEngineConfigurer bean..."); 	
    	
        return configuration -> {
        	configuration.setDataSource(dataSource());
	        configuration.setTransactionManager(transactionManager());
	        configuration.setDatabaseSchemaUpdate(SpringProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
	        configuration.setAsyncExecutorMessageQueueMode(true);
	        configuration.setAsyncExecutorActivate(true);
	        
	        configuration.setAsyncHistoryEnabled(true);
	        configuration.setAsyncHistoryExecutorActivate(true);
	        configuration.setAsyncHistoryExecutorMessageQueueMode(true);
	        
	        configuration.setJobManager(jobManager());
	        
	        AsyncExecutor asyncExecutor = configuration.getProcessEngineConfiguration().getAsyncExecutor();
	        asyncExecutor.setAsyncJobLockTimeInMillis(15 * 60 * 1000);
	        asyncExecutor.setTimerLockTimeInMillis(20 * 60 * 1000);
	        
        };
    }

    @Bean
    public MessageBasedJobManager jobManager() {
    	LOGGER.info("Creating jobManager bean...");    	
    	
        MessageBasedJobManager jobManager = new MessageBasedJobManager();
        jobManager.setJmsTemplate(jmsTemplate());
        
        // History 
        jobManager.setHistoryJmsTemplate(historyJmsTemplate());
        
        return jobManager;
    }

    @Bean
    public ConnectionFactory connectionFactory() {
    	LOGGER.info("Creating connectionFactory bean...");      	
    	
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
        activeMQConnectionFactory.setUseAsyncSend(true);
        activeMQConnectionFactory.setAlwaysSessionAsync(true);
        activeMQConnectionFactory.setStatsEnabled(true);
        return new CachingConnectionFactory(activeMQConnectionFactory);
    }
    
    @Bean
    public JmsTemplate jmsTemplate() {
    	LOGGER.info("Creating jmsTemplate bean...");  
    	
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setDefaultDestination(new ActiveMQQueue("flowable-jobs"));
        jmsTemplate.setConnectionFactory(connectionFactory());
        return jmsTemplate;
    }  
    
    // History
    @Bean
    public JmsTemplate historyJmsTemplate() {
    	LOGGER.info("Creating historyJmsTemplate bean...");  
    	
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setDefaultDestination(new ActiveMQQueue("flowable-history-jobs"));
        jmsTemplate.setConnectionFactory(connectionFactory());
        return jmsTemplate;
    }

    @Bean
    public MessageListenerContainer messageListenerContainer(SpringProcessEngineConfiguration springProcessEngineConfiguration) {
    	LOGGER.info("Creating messageListenerContainer bean...");      	
    	
        DefaultMessageListenerContainer messageListenerContainer = new DefaultMessageListenerContainer();
        messageListenerContainer.setConnectionFactory(connectionFactory());
        messageListenerContainer.setDestinationName("flowable-jobs");
        messageListenerContainer.setMessageListener(jobMessageListener(springProcessEngineConfiguration));
        messageListenerContainer.setConcurrentConsumers(3);
        messageListenerContainer.start();
        
        return messageListenerContainer;
    }
    
    @Bean
  public MessageListenerContainer historyMessageListenerContainer(SpringProcessEngineConfiguration springProcessEngineConfiguration) {
  	LOGGER.info("Creating historyMessageListenerContainer bean...");      	
  	
      DefaultMessageListenerContainer historyMessageListenerContainer = new DefaultMessageListenerContainer();
      historyMessageListenerContainer.setConnectionFactory(connectionFactory());
      historyMessageListenerContainer.setDestinationName("flowable-history-jobs");
      historyMessageListenerContainer.setMessageListener(historyJobsMessageListener(springProcessEngineConfiguration));
      historyMessageListenerContainer.setConcurrentConsumers(10);
      historyMessageListenerContainer.start();
      
      return historyMessageListenerContainer;
  }
    
    @Bean
    public JobMessageListener jobMessageListener(SpringProcessEngineConfiguration springProcessEngineConfiguration) {
    	LOGGER.info("Creating jobMessageListener bean...");         
    	
    	JobMessageListener jobMessageListener = new JobMessageListener();
        ProcessEngineConfiguration processEngineConfiguration = springProcessEngineConfiguration;
        JobServiceConfiguration jobServiceConfiguration = (JobServiceConfiguration) processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG);
        jobMessageListener.setJobServiceConfiguration(jobServiceConfiguration);
        return jobMessageListener;
    }
    
    // History
    @Bean
    public HistoryJobMessageListener historyJobsMessageListener(SpringProcessEngineConfiguration springProcessEngineConfiguration) {
    	LOGGER.info("Creating historyJobMessageListener bean...");  
    	
    	HistoryJobMessageListener historyJobMessageListener = new HistoryJobMessageListener();
    	ProcessEngineConfiguration processEngineConfiguration = springProcessEngineConfiguration;
        JobServiceConfiguration jobServiceConfiguration = (JobServiceConfiguration) processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG);
    	historyJobMessageListener.setJobServiceConfiguration(jobServiceConfiguration);
    	return historyJobMessageListener;
    }
}
