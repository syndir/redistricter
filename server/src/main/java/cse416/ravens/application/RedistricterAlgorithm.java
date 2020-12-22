package cse416.ravens.application;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class RedistricterAlgorithm {
	private static RedistricterAlgorithm algorithmHandler;
	private static Boolean localJobsEnabled = true;
	private List<Process> processes = new ArrayList<>();

	private RedistricterAlgorithm() {}

	public static RedistricterAlgorithm getInstance() {
		if(algorithmHandler == null) {
			algorithmHandler = new RedistricterAlgorithm();
		}
		return algorithmHandler;
	}

	private String executeLocalCommand(String command) {
		File commandFile = null;
		Writer streamWriter = null;
		PrintWriter printWriter = null;
		Process checkProcess;
		BufferedReader in = null;
		String result = "";
		String line;

		try {
			commandFile = File.createTempFile("script", null);
			streamWriter = new OutputStreamWriter(new FileOutputStream(commandFile));
			printWriter = new PrintWriter(streamWriter);
			printWriter.println(command);
		}
		catch (Exception e) {
			DebugHelper.log(e.getMessage());
		}
		finally {
			printWriter.close();
		}

		try {
			checkProcess = new ProcessBuilder("/bin/bash", commandFile.toString()).start();
			in = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));
			while((line = in.readLine()) != null) {
				result += line;
			}
			checkProcess.waitFor();
			in.close();
		}
		catch (Exception e) {
			DebugHelper.log(e.getMessage());
		}
		return result;
	}

	private String createLocalScript(Job job) {
		String localScript = "";

		if(job == null || job.getParameters() == null) {
			return "";
		}
		localScript = "#!/usr/bin/env bash\n" + 
					  "cur_job=1\n" +
					  "num_seeds=" + job.getParameters().getNumSeedPlans() + "\n"+
					  "mkdir -p ~/jobs/" + job.getJobID() + "/output/\n" +
					  "echo > ~/jobs/" + job.getJobID() + "/commands.txt\n" +
					  "while [ \\$cur_job -le \\$num_seeds ]\n" +
					  "do\n" +
					  "	echo python3 ~/algorithm/main.py ~/jobs/" + job.getJobID() + "/config.json \\$cur_job >> ~/jobs/" + job.getJobID() + "/commands.txt\n" +
					  "	cur_job=\\$(expr \\$cur_job + 1)\n" +
					  "done\n" +
					  "echo -n RUNNING > ~/jobs/" + job.getJobID() + "/status\n" +
					  "parallel --verbose < ~/jobs/" + job.getJobID() + "/commands.txt\n" +
					  "sleep 2\n" + 
					  "python3 ~/algorithm/merge_dps.py ~/jobs/" + job.getJobID() + "/output/\n" +
					  "echo -n FINISHED > ~/jobs/" + job.getJobID() + "/status\n";
		return localScript;
	}

	private File createFileToExecute(Job job) {
		File commandFile = null;
		Writer streamWriter = null;
		PrintWriter printWriter = null;

		try {
			commandFile = File.createTempFile("script", null);
			streamWriter = new OutputStreamWriter(new FileOutputStream(commandFile));
			printWriter = new PrintWriter(streamWriter);
		
			if(job == null) {
				return commandFile;
			}
			printWriter.println("#!/bin/bash");
			printWriter.println("cd ~");
			printWriter.println("mkdir -p ~/jobs/" + job.getJobID() + "/output");
			printWriter.println("cat << EOF > ~/jobs/" + job.getJobID() + "/launch.sh");
			printWriter.println(createLocalScript(job));
			printWriter.println("\nEOF");
			printWriter.println("cat << EOF > ~/jobs/" + job.getJobID() + "/config.json");
			printWriter.println(job.createConfigJSON());
			printWriter.println("\nEOF");
			printWriter.println("/bin/bash ~/jobs/" + job.getJobID() + "/launch.sh");
		}
		catch (Exception e) {
			DebugHelper.log(e.getMessage());
		}
		finally {
			printWriter.close();
		}
		return commandFile;
	}

	public Boolean submitLocalJob(Job job) {
		Database db = Database.getInstance();
		Process jobProcess;

		if(localJobsEnabled == false) {
			DebugHelper.log("Local jobs not enabled");
			return false;
		}
		if(job == null) {
			DebugHelper.log("submitLocalJob() for null job!");
			return false;
		}
		try {
			jobProcess = new ProcessBuilder("/bin/bash", createFileToExecute(job).toString()).start();
			processes.add(jobProcess);
			job.setLocalPID(jobProcess.pid());
			job.updateStatus(Status.RUNNING);
			db.performSingleStatementUpdate("UPDATE jobs SET local_pid=" + job.getLocalPID() + " WHERE job_id=" + job.getJobID() + ";");
		}
		catch (Exception e) {
			DebugHelper.log(e.getMessage());
			return false;
		}
		return true;
	}

	public Boolean cancelLocalJob(Job job) {
		if(localJobsEnabled == false) {
			DebugHelper.log("Local jobs are not enabled");
			return false;
		}
		if(job == null) {
			DebugHelper.log("cancelLocalJob() for null job!");
			return false;
		}
		DebugHelper.log("cancelLocalJob() for jobID=" + job.getJobID() + " with PID=" + job.getLocalPID());
		for(Process p : processes) {
			if(p.pid() == job.getLocalPID()) {
				DebugHelper.log("Found matching process to kill");
				p.descendants().forEach(child -> child.destroy());
				p.destroy();
				job.updateStatus(Status.ABORTED);
				processes.remove(p);
				return true;
			}
		}
		DebugHelper.log("NO MATCHING PROCESS FOUND TO KILL!");
		job.updateStatus(Status.ABORTED);
		return true;
	}

	@SuppressWarnings("unchecked")
	public Map<Job, Status> pollLocalJobs() {
		Database db = Database.getInstance();
		List<Integer> jobsToPoll = null;
		String status = "";
		Map<Job, Status> jobsToUpdate = new HashMap<>();

		DebugHelper.log("pollLocalJobs() invoked @ " + new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date()));
        if(localJobsEnabled == false) {
            return jobsToUpdate;
		}

		jobsToPoll = (List<Integer>)db.performSingleStatementSelect("SELECT job_id FROM jobs WHERE local_pid IS NOT NULL AND job_status in (\"PENDING\", \"RUNNING\");");
        for(Integer job : jobsToPoll) {
            status = executeLocalCommand("cat ~/jobs/" + job + "/status");
            DebugHelper.log("Got status=" + status + " for jobID=" + job);
            switch(status) {
                case "RUNNING":
                    if(db.findJobByID(job).getStatus() == Status.PENDING) {
                        jobsToUpdate.put(db.findJobByID(job), Status.RUNNING);
                    }
                    break;

                case "FINISHED":
                    jobsToUpdate.put(db.findJobByID(job), Status.FINISHED);
                    break;

                case "ABORTED":
                    jobsToUpdate.put(db.findJobByID(job), Status.ABORTED);
                    break;
                
                case "ERROR":
                    jobsToUpdate.put(db.findJobByID(job), Status.ERROR);
                    break;
            }
        }		
		return jobsToUpdate;
	}

	public String retrieveResultsForJob(Job job) {
		String resultsJSON = "{}";

		if(job == null) {
			DebugHelper.log("retrieving resultsfor a null job!");
			return "{}";
		}
		resultsJSON = executeLocalCommand("cat ~/jobs/" + job.getJobID() + "/output/results.json");
		return resultsJSON;
	}

	public List<Process> getProcesses() {
		return processes;
	}

	public static Boolean getLocalJobsEnabled() { 
		return localJobsEnabled;
	}

	public static void setLocalJobsEnabled(Boolean localJobsEnabled) {
		RedistricterAlgorithm.localJobsEnabled = localJobsEnabled;
	}
}
