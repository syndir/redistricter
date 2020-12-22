package cse416.ravens.application;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import com.jcraft.jsch.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.regex.*;

@Component
public class SeaWulfHandler {
    /*
     * SSH keys must be generated with `ssh-keygen -f <outfilename> -t rsa -m PEM` or JSch will complain
     * SSH keys can be easily copied over to SeaWulf via `ssh-copy-id -i <keyfile> user@host`
     */

    private static Boolean sshEnabled = true;
    private static JSch sshConnection = null;
    private static Session sshSession = null;
    private static String sshKeyFile = null;
    private static String sshUser = null;
    private static String sshHost = null;
    private static Integer sshPort = null;

    /* fixedDelay=Long.MAX_VALUE will make it so this only runs once, 1 sec after startup */
    @Scheduled(initialDelay = 1000, fixedDelay=Long.MAX_VALUE)
    public static Boolean createConnection() {
        Properties sshConfig = null;

        DebugHelper.log("createConnection() - ENTER");
        if(sshEnabled == false) {
            DebugHelper.log("SSH is diabled in properties file.");
            DebugHelper.log("createConnection() - EXIT");
            return false;
        }
        if(sshKeyFile == null || sshUser == null || sshHost == null) {
            DebugHelper.log("Insufficient information for SSH connection");
            DebugHelper.log("createConnection() - EXIT");
            return false;
        }
        DebugHelper.log("Attempting to create SSH connection for " + sshUser + "@" + sshHost + ":" + sshPort + ", keyfile=" + sshKeyFile);
        try {
            sshConnection = new JSch();
            DebugHelper.log("JSch - adding identity");
            sshConnection.addIdentity(sshKeyFile);				/* for key-based authenticaion */
            DebugHelper.log("JSch - creating session");
            sshSession = sshConnection.getSession(sshUser, sshHost, sshPort);
            sshConfig = new Properties();
            sshConfig.put("StrictHostKeyChecking", "no");		/* Ignore UnkownHostKey errors */
            sshSession.setConfig(sshConfig);
            sshSession.setServerAliveInterval(1000*60*5);		/* send a keep-alive every 5 minutes */
            sshSession.connect(10000);							/* 10 second timeout */
            DebugHelper.log("JSch - connected");
        }
        catch (Exception e) {
            DebugHelper.log(e.getMessage());
            if(sshSession != null) {
                sshSession.disconnect();
                sshSession = null;
            }
            DebugHelper.log("createConnection() - EXIT");
            return false;
        }
        DebugHelper.log("createConnection() - EXIT");
        return true;
    }

    private static String readCommandResponse(Channel sshChannel, InputStream inStream) {
        String response = "";
        byte[] bytesReceived= new byte[1024];
        Integer numBytesReceived;

        try {
            while(true) {
                while(inStream.available()>0) {
                    numBytesReceived = inStream.read(bytesReceived, 0, 1024);
                    if(numBytesReceived < 0) {
                        break;
                    }
                    response += (new String(bytesReceived, 0, numBytesReceived));
                }
                if(sshChannel.isClosed()) {
                    break;
                }
            }
        }
        catch(IOException e) {
            DebugHelper.log(e.getMessage());
        }
        return response;
    }

    private static String sendShellCommand(String command) {
        Channel sshChannel = null;
        String output = "";
        InputStream inStream = null;
        OutputStream outStream = null;
        PrintStream printer = null;

        DebugHelper.log("sendShellCommand() - ENTER (command=" + command + ")");
        if(sshSession == null || sshSession.isConnected() == false) {
            SeaWulfHandler.createConnection();
        }
        try {
            DebugHelper.log("JSch - opening shell channel");
            sshChannel = sshSession.openChannel("shell");
            inStream = sshChannel.getInputStream();
            outStream = sshChannel.getOutputStream();
            printer = new PrintStream(outStream);

            sshChannel.connect();
            Thread.sleep(100);
            try {
                DebugHelper.log("JSch - sending shell command: " + command);
                printer.println(command);
                printer.flush();
                output = readCommandResponse(sshChannel, inStream);
            }
            catch(Exception e) {
                DebugHelper.log(e.getMessage());
            }
            finally {
                printer.close();
            }

            DebugHelper.log("JSch - disconnecting shell channel");
            sshChannel.disconnect();
        }
        catch(Exception e) {
            DebugHelper.log(e.getMessage());
        }
        DebugHelper.log("sendShellCommand() - EXIT (output='" + output.substring(0, Math.min(output.length(), 40)) + "...')");
        return output;
    }

    private static String sendExecCommand(String command) {
        ChannelExec sshExecChannel = null;
        String output = "";
        InputStream inStream = null;

        DebugHelper.log("sendExecCommand() - ENTER (command=" + command + ")");
        if(sshSession == null || sshSession.isConnected() == false) {
            SeaWulfHandler.createConnection();
        }
        try {
            DebugHelper.log("JSch - opening exec channel");
            sshExecChannel = (ChannelExec)sshSession.openChannel("exec");
            inStream = sshExecChannel.getInputStream();

            try {
                DebugHelper.log("JSch - sending exec command: " + command);
                sshExecChannel.setCommand(command);
                sshExecChannel.connect();
                output = readCommandResponse((Channel)sshExecChannel, inStream);
            }
            catch(Exception e) {
                DebugHelper.log(e.getMessage());
            }

            DebugHelper.log("JSch - disconnecting exec channel");
            sshExecChannel.disconnect();
        }
        catch(Exception e) {
            DebugHelper.log(e.getMessage());
        }
        DebugHelper.log("sendExecCommand() - EXIT (output='" + output.substring(0, Math.min(output.length(), 40)) + "...')");
        return output;
    }

    private static String createSlurmScript(Job job) {
        String slurmScript = "";

        DebugHelper.log("createSlurmScript() - ENTER (jobID=" + job.getJobID() + ", parameters=[" + job.getParameters() + "])");
        if(job == null || job.getParameters() == null) {
            return "";
        }
        slurmScript = "#!/usr/bin/env bash\n" +
            "#SBATCH --job-name=ravens_job" + job.getJobID() + "\n" +
            "#SBATCH --output=jobs/" + job.getJobID() + "/output.log\n" +
            "#SBATCH --ntasks-per-node=28\n" +
            "#SBATCH --nodes=1\n" +
            "#SBATCH --time=24:00:00\n" +
            "#SBATCH -p extended-28core\n" +
            "\n" +
            "module load python/3.8.6\n" +
            "module load gnu-parallel/6.0\n" +
            "cur_job=1\n" +
            "num_seeds=" + job.getParameters().getNumSeedPlans() + "\n"+
            "mkdir -p ~/jobs/" + job.getJobID() + "/output/\n" +
            "echo > ~/jobs/" + job.getJobID() + "/commands.txt\n" +
            "while [ \\$cur_job -le \\$num_seeds ]\n" +
            "do\n" +
            "	echo python3 algorithm/main.py ~/jobs/" + job.getJobID() + "/config.json \\$cur_job >> ~/jobs/" + job.getJobID() + "/commands.txt\n" +
            "	cur_job=\\$(expr \\$cur_job + 1)\n" +
            "done\n" +
            "echo -n RUNNING > ~/jobs/" + job.getJobID() + "/status\n" +
            "parallel --verbose --jobs 28 < ~/jobs/" + job.getJobID() + "/commands.txt\n" +
            "python3 algorithm/merge_dps.py ~/jobs/" + job.getJobID() + "/output/\n" +
            "echo -n FINISHED > ~/jobs/" + job.getJobID() + "/status\n";
        DebugHelper.log("createSlurmScript() - EXIT (slurmScript='" + slurmScript.substring(0, Math.min(slurmScript.length(), 40)) + "...')");
        return slurmScript;
    }

    private static String createCommandToExecute(Job job) {
        String command = null;

        if(job == null) {
            return null;
        }
        DebugHelper.log("createCommmandToExecute() - ENTER (jobID=" + job.getJobID() + ")");
        command = "mkdir -p ~/jobs/" + job.getJobID() + "/output;" +
            "cat << EOF > ~/jobs/" + job.getJobID() + "/launch.slurm\n" +
            SeaWulfHandler.createSlurmScript(job) +
            "\nEOF\n" +
            "cat << EOF > ~/jobs/" + job.getJobID() + "/config.json\n" +
            job.createConfigJSON() +
            "\nEOF\n" +
            "module load slurm; sbatch ~/jobs/" + job.getJobID() + "/launch.slurm; exit\n";
        DebugHelper.log("createCommandToExecute() - EXIT (command='" + command.substring(0, Math.min(command.length(), 40)) + "...')");
        return command;
    }

    public static Integer submitSeaWulfJob(Job job) {
        Database db = Database.getInstance();
        String response = "";
        Integer slurmJobID = -1;
        Pattern pattern = Pattern.compile("Submitted batch job \\d+");
        Matcher matcher;

        if(sshEnabled == false) {
            return -1;
        }
        if(job == null) {
            DebugHelper.log("submitSlurmScript() for null job!");
            return -1;
        }
        DebugHelper.log("submitSeaWulfJob() - ENTER (jobID=" + job.getJobID() +")");
        response = SeaWulfHandler.sendShellCommand(SeaWulfHandler.createCommandToExecute(job));
        DebugHelper.log("submitSlurmScript() - Got response: " + response);
        matcher = pattern.matcher(response);
        if(matcher.find()) {
            slurmJobID = Integer.valueOf(matcher.group().substring("Submitted batch job ".length()));
            DebugHelper.log("Got slurm job id=" + slurmJobID);
            job.setSlurmJobNumber(slurmJobID);
        }
        db.performSingleStatementUpdate("UPDATE jobs SET slurm_job=" + slurmJobID + " WHERE job_id=" + job.getJobID() + ";");
        DebugHelper.log("submitSeaWulfJob() - EXIT (slurmJobID=" + slurmJobID +")");
        return slurmJobID;
    }

    @SuppressWarnings("unchecked")
    public static Map<Job, Status> pollSeaWulfJobs() {
        Database db = Database.getInstance();
        List<Integer> jobsToPoll = null;
        String status = "";
        Map<Job, Status> jobsToUpdate = new HashMap<>();

        DebugHelper.log("pollSeaWulfJobs() invoked @ " + new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date()));
        if(sshEnabled == false) {
            return jobsToUpdate;
        }
        /* retrieve a list of all jobs in the db that have slurm_job ids and are in PENDING or RUNNING states */
        jobsToPoll = (List<Integer>)db.performSingleStatementSelect("SELECT job_id FROM jobs WHERE slurm_job IS NOT NULL AND job_status in (\"PENDING\",\"RUNNING\");");
        for(Integer job : jobsToPoll) {
            Boolean errored = false, maybeErrored = false;

            /* check for job termination due to error, or if someone else killed our job */
            status = sendShellCommand("module load slurm; scontrol show job " + db.findJobByID(job).getSlurmJobNumber() + "; exit");
            DebugHelper.log("scontrol shows: " + status);
            if(status.contains("JobState=CANCELLED") || status.contains("JobState=FAILED") || status.contains("JobState=DEADLINE") ||
                status.contains("JobState=PREEMPTED") || status.contains("JobState=BOOT_FAIL") || status.contains("JobState=NODE_FAIL") ||
                status.contains("JobState=OUT_OF_MEMORY") || status.contains("JobState=TIMEOUT")) {
                    errored = true;
            }
            if(status.contains("Invalid job id specified")) {
                maybeErrored = true;
            }
            status = sendExecCommand("cat ~/jobs/" + job + "/status");
            DebugHelper.log("Got status=" + status + " for jobID=" + job);
            switch(status) {
                case "RUNNING":
                    if(db.findJobByID(job).getStatus() == Status.PENDING) {
                        jobsToUpdate.put(db.findJobByID(job), Status.RUNNING);
                    }
                    break;

                case "FINISHED":
                    jobsToUpdate.put(db.findJobByID(job), Status.FINISHED);
                    maybeErrored = false;
                    break;

                case "":
                default:
                    if(errored) {
                        jobsToUpdate.put(db.findJobByID(job), Status.ERROR);
                    }
                    break;
            }
            if(errored || maybeErrored) {
                jobsToUpdate.put(db.findJobByID(job), Status.ERROR);
            }
        }
        return jobsToUpdate;
    }

    public static String retrieveResultsForJob(Job job) {
        String resultsJSON = "{}";

        if(job == null) {
            DebugHelper.log("retrieving results for null job!");
            return "{}";
        }
        DebugHelper.log("retrieveResultsForJob() - ENTER (jobID=" + job.getJobID() + ")");
        resultsJSON = sendExecCommand("cat ~/jobs/" + job.getJobID() + "/output/results.json");
        DebugHelper.log("retrieveResultsForJob() - EXIT (resultsJSON='" + resultsJSON.substring(0, Math.min(40, resultsJSON.length())) + "...')");
        return resultsJSON;
    }

    public static Boolean cancelSeaWulfJob(Job job) {
        Database db = Database.getInstance();

        if(sshEnabled == false) {
            return false;
        }
        if(job == null) {
            DebugHelper.log("cancelSeaWulfJob() for null job!");
            return false;
        }
        DebugHelper.log("cancelSeaWulfJob() - ENTER (jobID=" + job.getJobID() +")");
        SeaWulfHandler.sendShellCommand("module load slurm; scancel " + job.getSlurmJobNumber() + "; echo -n ABORTED > ~/jobs/" + job.getJobID() + "/status; exit\n");
        job.setStatus(Status.ABORTED); /* perform job update here, because in the case where SSH is disabled, we don't want to mark a job as aborted without it actually being aborted */
        db.performSingleStatementUpdate("UPDATE jobs SET job_status=\"" + job.getStatus().name() + "\" WHERE job_id=" + job.getJobID() + ";");
        DebugHelper.log("cancelSeaWulfJob - EXIT");
        return true;
    }

    //GETTERS/SETTERS
    public static String getSshKeyFile() {
        return SeaWulfHandler.sshKeyFile;
    }

    public static void setSshKeyFile(String sshKeyFile) {
        SeaWulfHandler.sshKeyFile = sshKeyFile;
    }

    public static String getSshUser() {
        return SeaWulfHandler.sshUser;
    }

    public static void setSshUser(String sshUser) {
        SeaWulfHandler.sshUser = sshUser;
    }

    public static String getSshHost() {
        return SeaWulfHandler.sshHost;
    }

    public static void setSshHost(String sshHost) {
        SeaWulfHandler.sshHost = sshHost;
    }

    public static Integer getSshPort() {
        return SeaWulfHandler.sshPort;
    }

    public static void setSshPort(Integer sshPort) {
        SeaWulfHandler.sshPort = sshPort;
    }

    public static Boolean getSshEnabled() {
        return SeaWulfHandler.sshEnabled;
    }

    public static void setSshEnabled(Boolean sshEnabled) {
        SeaWulfHandler.sshEnabled = sshEnabled;
    }
}
