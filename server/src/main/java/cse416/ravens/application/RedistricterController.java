package cse416.ravens.application;

import java.math.BigInteger;
import java.util.*;
import javax.annotation.PostConstruct;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

@RestController
public class RedistricterController {
	private static State currentState;

	@PostConstruct
	public void initialize() {
		if(SeaWulfHandler.createConnection() == false) {
			DebugHelper.log("FAILED to create SSH connection to SeaWulf!");
		}
	}

	@PutMapping("/selectState")
	public boolean selectState(@RequestBody String state) {
		DebugHelper.log("Got select state request: " + state);
		RedistricterController.currentState = Database.getInstance().findStateByID(state);
		DebugHelper.log("currentState updated");
		return true;
	}

	@PostMapping("/createJob")
	@ResponseBody
	public Integer createJob(@RequestBody Parameters parameters) {
		Database db = Database.getInstance();
		Job newJob = new Job(parameters);
		BigInteger lastID = null;

		parameters.setNumIterations(Parameters.getDefaultNumIterations());
		DebugHelper.log("Got createJob request with parameters: [" + parameters + " ]");
		if(RedistricterController.getCurrentState() == null) {
			return null;
		}
		if((lastID = db.insertParametersIntoDB(parameters)) == null) {
			return null;
		}
		newJob.getParameters().setState(currentState.getState().name());
		newJob.setStatus(Status.PENDING);
		newJob.setParametersID(lastID.longValue());
		lastID = null;
		if((lastID = db.insertJobIntoDB(newJob)) == null) {
			return null;
		}
		newJob.setJobID(lastID.intValue());
		Job.execute(newJob);
		DebugHelper.log("returning job id " + newJob.getJobID());
		return newJob.getJobID();
	}

	@PostMapping("/abortJob")
	public boolean abortJob(@RequestBody Integer jobID) {
		Database db = Database.getInstance();
		Job toAbort = db.findJobByID(jobID);

		DebugHelper.log("got abortJob request for JobID=" + jobID);
		if(toAbort == null) {
			DebugHelper.log("no such job found to abort");
			return false;
		}
		if(toAbort.getStatus() == Status.PENDING || toAbort.getStatus() == Status.RUNNING) {
			Job.cancel(toAbort);
		}
		db.removeJobFromDB(toAbort);
		return true;
	}

	@DeleteMapping("/deleteJob")
	public boolean deleteJob(@RequestParam Integer jobID) {
		Database db = Database.getInstance();
		Job toDelete = db.findJobByID(jobID);

		DebugHelper.log("got deleteJob request for JobID=" + jobID);
		if(toDelete == null) {
			DebugHelper.log("no such job found to delete");
			return false;
		}
		if(toDelete.getStatus() == Status.PENDING || toDelete.getStatus() == Status.RUNNING) {
			Job.cancel(toDelete);
		}
		db.removeJobFromDB(toDelete);
		return true;
	}

	@GetMapping("/getBoxAndWhiskerData")
	@ResponseBody
	public BoxAndWhiskerData getBoxAndWhiskerData(@RequestParam Integer jobID) {
        DebugHelper.log("got BoxAndWhisker request for JobID=" + jobID);
        Database db = Database.getInstance();
        Job selectedJob = db.findJobByID(jobID);
        BoxAndWhiskerData plot = selectedJob.getBoxAndWhiskerData(new HashMap<Integer, DistrictingPlan>(), new HashMap<Integer, District>());

        return plot;
	}

	@GetMapping("/getHistory")
	@ResponseBody
	public Map<Integer, Map<Status, Parameters>> getHistory() {
		Database db = Database.getInstance();
		Map<Integer, Map<Status, Parameters>> history = new HashMap<>();

		DebugHelper.log("got getHistory request");
		if(currentState == null) {
			DebugHelper.log("No current state selected; history will not be printed.");
			return null;
		}
		// resultJobIDs = (List<Integer>)db.performSingleStatementSelect("SELECT job_id FROM jobs WHERE job_status != 'ABORTED' AND state='" + currentState.getState().name() + "';");
		history = (Map<Integer, Map<Status, Parameters>>)db.findJobsByState(currentState.getState().name());

		DebugHelper.log("resulting history is:\n" + history.toString());
		return history;
	}

	@PostMapping("/applyFilter")
	@ResponseBody
	public String applyFilter(@RequestBody Map<String, Object> filterParameters) {
		Database db = Database.getInstance();
		MapFilter filterType = null;
		DistrictingPlan plan = null;
		Job job = null;
		Integer jobID = null;
		List<String> plans = new ArrayList<String>();
		String planRepresentation = "";

		DebugHelper.log("got applyFilter request for filter=" + filterParameters.get("filter").toString());
		if(currentState == null) {
			return planRepresentation;
		}
		jobID = (Integer)filterParameters.get("jobID");
		if(jobID != null) {
			job = db.findJobByID(jobID);
		}
		filterType = MapFilter.valueOf(filterParameters.get("filter").toString());
		switch(filterType) {
			case FILTER_DP_ORIGINAL:
				if(RedistricterController.getCurrentState() == null) {
					return planRepresentation;
				}
				plan = db.findDistrictingPlanByID(RedistricterController.getCurrentState().getCurrentlyEnactedPlan());
				break;
			case FILTER_DP_AVERAGE:
			case FILTER_DP_BEST:
			case FILTER_DP_RANDOM:
			case FILTER_DP_WORST:
				plan = db.findDistrictingPlanByID(job.getDistrictingPlan(filterType));
				break;
			default:
				return planRepresentation;
		}

		if(plan.getGeoJSON() != null) {
			DebugHelper.log("Found already existant client geojson, returning...");
			return plan.getGeoJSON();
		}

		for(Integer district : plan.getDistricts()) {
			DebugHelper.log("looking up district=" + district);
			plans.add(Database.getInstance().findDistrictByID(district).getClientGeoJSON());
		}

		planRepresentation = 
		"{" +
			"\"type\" : \"FeatureCollection\"," +
			"\"features\" : [";
		for(String geoJSON : plans) {
			planRepresentation += 
			"{" +
				"\"type\" : \"Feature\"," +
				"\"properties\" : {}," +
				"\"geometry\" : " +
					geoJSON +
				"" +
			"},";
		}
		planRepresentation = planRepresentation.substring(0, planRepresentation.length() - 1);
		planRepresentation += "]}";
		db.performSingleStatementUpdate("UPDATE districting_plans SET geojson='" + planRepresentation + "' WHERE id=" + plan.getId() + ";");

		DebugHelper.log("resulting plan map is:\n" + planRepresentation.toString());
		return planRepresentation;
	}

	public static State getCurrentState() {
		return currentState;
	}

	@GetMapping("/setDebugLevel")
	public void setDebugLevel(@RequestParam Integer debugLevel) {
		DebugHelper.log("Setting debug level to " + debugLevel, true);
		DebugHelper.setDebugLevel(debugLevel);
	}

	/* Testing methods */
	@GetMapping("/testFindJobByID")
	@ResponseBody
	public Job testFindJobByID(@RequestParam Integer jobID) {
		DebugHelper.log("Got testFindJobByID request for jobID=" + jobID);
		return Database.getInstance().findJobByID(jobID);
	}

	@GetMapping("/testFindDPByID")
	@ResponseBody
	public DistrictingPlan testFindDPByID(@RequestParam Integer dpID) {
		DebugHelper.log("Got testFindDPByID request for districtingPlanID=" + dpID);
		return Database.getInstance().findDistrictingPlanByID(dpID);
	}

	@GetMapping("/testFindPrecinctByID")
	@ResponseBody
	public Precinct testFindPrecinctByID(@RequestParam String precinctID) {
		DebugHelper.log("Got testFindPrecinctByID request for precinctID=" + precinctID);
		return Database.getInstance().findPrecinctByID(precinctID);
	}

	@GetMapping("/testSortDistricts")
	@ResponseBody
	public List<Integer> testSortDistricts(@RequestParam Integer districtPlanID) {
		DebugHelper.log("Got testSortDistricts request for districtPlanID=" + districtPlanID);
		return Database.getInstance().findDistrictingPlanByID(districtPlanID).getSortedDistricts();
	}

	@GetMapping("/testGenerateGeoDP")
	@ResponseBody
	public String testGenerateGeo(@RequestParam Integer jobID) {
		DebugHelper.log("Got testGenerateGeoDP request for jobID=" + jobID);
		Job.generateGeoJSONFileForJob(Database.getInstance().findJobByID(jobID), new HashMap<Integer, District>());
		return "a";
	}

	@GetMapping("/testForcePopGeneration")
	@ResponseBody
	public String testForcePopGeneration(@RequestParam Integer districtingPlanID) {
		DebugHelper.log("Forcing population generation of districtID=" + districtingPlanID);
		Database db = Database.getInstance();
		DistrictingPlan dp = db.findDistrictingPlanByID(districtingPlanID);
		ObjectMapper objMapper = new ObjectMapper();

		for(Integer district : dp.getDistricts()) {
			Map<Demographic, Integer> totalPopulationByDemographic = new HashMap<>();
			Map<Demographic, Integer> votingAgePopulationByDemographic = new HashMap<>();
			Integer total = 0;
			for(String precinctID : db.findDistrictByID(district).getPrecincts()) {
				Precinct precinct = db.findPrecinctByID(precinctID);
				total += precinct.getTotalPopulations().get(Demographic.ALL);
				for(Map.Entry<Demographic, Integer> demo : precinct.getTotalPopulations().entrySet()) {
					totalPopulationByDemographic.put(demo.getKey(), totalPopulationByDemographic.getOrDefault(demo.getKey(), 0) + demo.getValue());
				}
				for(Map.Entry<Demographic, Integer> demo : precinct.getVotingAgePopulations().entrySet()) {
					votingAgePopulationByDemographic.put(demo.getKey(), votingAgePopulationByDemographic.getOrDefault(demo.getKey(), 0) + demo.getValue());
				}
			}
			try {
				db.performSingleStatementUpdate("UPDATE districts SET total_pop=" + total + ", total_pop_by_demo='" + objMapper.writeValueAsString(totalPopulationByDemographic) + 
					"', vap_pop_by_demo='" + objMapper.writeValueAsString(votingAgePopulationByDemographic) + "' WHERE id=" + district +";");
			}
			catch (Exception e) {
				DebugHelper.log(e.getMessage());
			}
		}
		return "a";
	}

	@GetMapping("/testSummaryFile")
	@ResponseBody
	public Boolean testSummaryFile(@RequestParam Integer jobID) {
		DebugHelper.log("Forcing summary file genearation of jobID=" + jobID);
		Database db = Database.getInstance();
		Job job = db.findJobByID(jobID);
		return job.generateSummaryFile(new HashMap<String, Precinct>(), new HashMap<Integer, DistrictingPlan>(), new HashMap<Integer, District>());
	}
}

