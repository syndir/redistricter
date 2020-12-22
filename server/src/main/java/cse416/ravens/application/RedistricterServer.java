package cse416.ravens.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import java.util.*;
import java.util.concurrent.Callable;

@SpringBootApplication
@EnableScheduling
public class RedistricterServer {
    public static void main(String[] args) {
        SpringApplication.run(RedistricterServer.class, args);
    }

    /* run with 5 minutes between end/begin of next invocation */
    // @Scheduled(initialDelay = 1000, fixedDelay=5*60*1000)
    @Scheduled(initialDelay=2500, fixedDelay=30*1000) /* testing version */
    public static void checkForResults() {
        Job job;
        Map<Job, Status> jobsToUpdate = new HashMap<>();

        DebugHelper.log("checkForResults() - ENTER");
        jobsToUpdate.putAll(SeaWulfHandler.pollSeaWulfJobs());
        jobsToUpdate.putAll(RedistricterAlgorithm.getInstance().pollLocalJobs());
        for(Map.Entry<Job, Status> jobEntry : jobsToUpdate.entrySet()) {
            job = jobEntry.getKey();
            if(jobEntry.getValue() == Status.RUNNING) {
                job.updateStatus(Status.RUNNING);
            }
            else if(jobEntry.getValue() == Status.FINISHED) {
                job.updateStatus(Status.PROCESSING);
            }
            else {
                job.updateStatus(Status.ERROR);
            }
        }
        for(Map.Entry<Job, Status> jobEntry : jobsToUpdate.entrySet()) {
            Status status = jobEntry.getValue();
            if(jobEntry.getValue() != Status.FINISHED) {
                continue;
            }
            
            Callable<String> taskToExecute = () -> {
                Job jobToUpdate = jobEntry.getKey();
                DebugHelper.log("want to update jobID=" + jobToUpdate.getJobID() + " to STATUS=" + status.name());
                String results = "";
                if(jobToUpdate.getSlurmJobNumber() != null && jobToUpdate.getSlurmJobNumber() > 0) {
                    results = SeaWulfHandler.retrieveResultsForJob(jobToUpdate);
                    jobToUpdate.processResults(results);
                }
                else {
                    results = RedistricterAlgorithm.getInstance().retrieveResultsForJob(jobToUpdate);
                    jobToUpdate.processResults(results);
                }
                jobToUpdate.updateStatus(status);
                return "";
            };
            ExecutorHelper.getInstance().submit(taskToExecute);
        }
        DebugHelper.log("checkForResults - EXIT");
    }

    @Configuration
    @PropertySource(value = "application.properties")
    class PropertiesHelper {
        @Value("${localRunThreshold}")
        public void setLocalRunThreshold(int localRunThreshold) {
            Job.setLocalRunThreshold(localRunThreshold);
        }

        @Value("${maxServerProcessingThreads}")
        public void setMaxServerProcessingThreads(Integer maxServerProcessingThreads) {
            ExecutorHelper.setMaxServerProcessingThreads(maxServerProcessingThreads);
        }

        @Value("${debuglevel}")
        public void setDebugLevel(Integer debugLevel) {
            DebugHelper.setDebugLevel(debugLevel);;
        }

        @Value("${debugFile}")
        public void setDebugFile(String debugFile) {
            DebugHelper.setDebugFile(debugFile);
        }

        @Value("${sshKeyFile}")
        public void setSshKeyFile(String sshKeyFile) {
            SeaWulfHandler.setSshKeyFile(sshKeyFile);
        }

        @Value("${sshUser}")
        public void setSshUser(String sshUser) {
            SeaWulfHandler.setSshUser(sshUser);
        }

        @Value("${sshHost}")
        public void setSshHost(String sshHost) {
            SeaWulfHandler.setSshHost(sshHost);
        }

        @Value("${sshPort}")
        public void setSshPort(Integer sshPort) {
            SeaWulfHandler.setSshPort(sshPort);
        }

        @Value("${sshEnabled}")
        public void setSshEnabled(Boolean sshEnabled) {
            SeaWulfHandler.setSshEnabled(sshEnabled);
        }

        @Value("${localJobsEnabled}")
        public void setLocalJobsEnabled(Boolean localJobsEnabled) {
            RedistricterAlgorithm.setLocalJobsEnabled(localJobsEnabled);
        }

        @Value("${numberOfIterations}")
        public void setNumberOfIterations(Integer numberOfIterations) {
            Parameters.setDefaultNumIterations(numberOfIterations);
        }
    }
}
