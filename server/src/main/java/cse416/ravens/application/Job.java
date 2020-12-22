package cse416.ravens.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import static java.lang.Math.abs;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.*;

@JsonIgnoreProperties({ "localRunThreshold", "parametersID", "slurmJobNumber", "localPID" })
public class Job {
    private static Integer localRunThreshold;
    private Integer jobID;
    private List<Integer> generatedDistrictingPlans;
    private Status status;
    private BoxAndWhiskerData boxAndWhiskerData;
    private Integer averageDistrictingPlan;
    private Integer maxDistrictingPlan;
    private Integer minDistrictingPlan;
    private Integer randomDistrictingPlan;
    private Parameters parameters;
    private Long parametersID;
    private Integer slurmJobNumber;
    private Long localPID;

    public Job(Parameters p) {
        this.parameters = p;
    }

    public Job() {
    }

    public Boolean generateSummaryFile(Map<String, Precinct> precinctMap, Map<Integer, DistrictingPlan> planMap, Map<Integer, District> districtMap) {
        DebugHelper.log("generateSummaryFile() - ENTER (jobID=" + getJobID() + ")");
        Database db = Database.getInstance();
        ObjectMapper objMapper = new ObjectMapper();
        State state = db.findStateByID(getParameters().getState());
        DistrictingPlan dp;
        String summary = "";

        if(precinctMap == null) precinctMap = new HashMap<>();
        if(planMap == null) planMap = new HashMap<>();
        if(districtMap == null) districtMap = new HashMap<>();

        summary = 
        "{ \"states\" : \n" +
        "[\n" +
        "{ \"stateName\" : \"" + getParameters().getState() + "\",\n" + 
        "   \"stateID\" : \"" + State.getStateAbbreviation(getParameters().getState()) + "\",\n" + 
        "   \"precinctsGeoJSON\" : {\n" +
        "       \"type\" : \"FeatureCollection\",\n" +
        "       \"description\" : \"" + getParameters().getState() + " State Precincts\",\n" + 
        "       \"features\" : [\n";
        
        
        try {
            Files.write(Paths.get("summaryJSONFiles/summary_" + getJobID() + ".json"), summary.getBytes());
        } catch (IOException e) {
            DebugHelper.log("FAILED to write summary file: " + e.getMessage());
            return false;
        }

        summary = "";
        try {
            for(String precinctID : state.getPrecincts()) {
                Precinct precinct;
                if(precinctMap.containsKey(precinctID)) {
                    precinct = precinctMap.get(precinctID);
                }
                else {
                    precinct = db.findPrecinctByID(precinctID);
                    precinctMap.put(precinctID, precinct);
                }
                summary +=
            "           {\n" +
            "               \"tyoe\" : \"Feature\",\n" +
            "               \"properties\" : {\n" +
            "                   \"precinct\" : \"" + precinct.getPrecinctName() + "\",\n" +
            "                   \"precinctID\" : \"" + precinct.getPrecinctID() + "\",\n" +
            "                   \"county\" : \"" + precinct.getCountyName() + "\",\n" +
            "                   \"countyID\" : \"" + String.format("%s%03d", State.getStateCode(getParameters().getState()), precinct.getCountyID()) + "\",\n" +
            "                   \"population\" : \"" + precinct.getTotalPopulations().getOrDefault(Demographic.ALL, 0) + "\",\n" +
            "                   \"votingAgePopulation\" : \"" + precinct.getVotingAgePopulations().getOrDefault(Demographic.ALL, 0) + "\",\n" +
            "                   \"adjacentPrecincts\" : " + objMapper.writeValueAsString(precinct.getPrecinctNeighbors()) + "\n" +
            "               },\n" +
            "               \"geometry\" : " + precinct.getBoundary() + "\n";
                try {
                    Files.write(Paths.get("summaryJSONFiles/summary_" + getJobID() + ".json"), summary.getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    DebugHelper.log("FAILED to write summary file: " + e.getMessage());
                    return false;
                }
                summary =
            "           },";
            }

            summary = summary.substring(0, summary.length() - 1);
            try {
                Files.write(Paths.get("summaryJSONFiles/summary_" + getJobID() + ".json"), summary.getBytes(), StandardOpenOption.APPEND);
            } catch (IOException e) {
                DebugHelper.log("FAILED to write summary file: " + e.getMessage());
                return false;
            }

            summary = 
            "       ]\n" +
            "   },\n" +
            "   \"averageDistricting\" : \"" + getAverageDistrictingPlan() + "\",\n" +
            "   \"extremeDistricting\" : \"" + getMaxDistrictingPlan() + "\",\n" +
            "   \"randomDistricting\" : \"" + getRandomDistrictingPlan() + "\",\n" +
            "   \"districtings\" : [";

            List<Integer> plans = new  ArrayList<>();
            plans.add(getAverageDistrictingPlan());
            plans.add(getMaxDistrictingPlan());
            plans.add(getRandomDistrictingPlan());

            for(Integer planID : plans) {
                DebugHelper.log("want planID=" + planID);
                dp = db.findDistrictingPlanByID(planID);

                summary +=
            "\n       {\n" +
            "           \"districtingID\" : \"" + dp.getId() + "\",\n" +
            "           \"constraints\" : {\n" +
            "               \"compactnessLimit\" : \"" + getParameters().getCompactnessMeasure().name() + "\",\n" +
            "               \"populationDifferenceLimit\" : \"" + getParameters().getPopulationVariance() + "\",\n" +
            "               \"minorityGroups\" : " + objMapper.writeValueAsString(getParameters().getSelectedDemographics().keySet()) + "\n" +
            "           },\n" +
            "           \"congressionalDistrictsGeoJSON\" : {\n" +
            "               \"type\" : \"FeatureCollection\",\n" +
            "               \"description\" : \"Congressional Districts\",\n" +
            "               \"features\" : [";

                for(Integer districtID : dp.getDistricts()) {
                    Integer minPop = 0, minVAP = 0;
                    District district = db.findDistrictByID(districtID);

                    List<String> neighbors = new ArrayList<>();
                    for(Integer nID : district.getNeighboringDistricts()) {
                        neighbors.add(nID.toString());
                    }
                    summary +=
            "\n                 {\n" +
            "                       \"type\" : \"Feature\",\n" +
            "                       \"properties\" : {\n" +
            "                           \"district\" : \"" + district.getDistrictID() + "\",\n" +
            "                           \"districtID\" : \"" + district.getDistrictID() + "\",\n" +
            "                           \"population\" : \"" + district.getTotalPopulation() + "\",\n" +
            "                           \"votingAgePopulation\" : \"" + district.getVotingAgePopulationByDemographic().get(Demographic.ALL) + "\",\n" +
            "                           \"adjacentDistricts\" : " + objMapper.writeValueAsString(neighbors) + ",\n" +
            "                           \"differentCounties\" : \"" + district.getNumberOfCounties() + "\",\n" +
            "                           \"precinctsInfo\" : [";
                    for(String precinctID : district.getPrecincts()) {
                        Integer pMinPop = 0, pMinVAP = 0;
                        for(Demographic d : getParameters().getSelectedDemographics().keySet()) {
                            pMinPop += precinctMap.get(precinctID).getTotalPopulations().get(d);
                            pMinVAP += precinctMap.get(precinctID).getVotingAgePopulations().get(d);
                        }
                        minPop += pMinPop;
                        minVAP += pMinVAP;
                        summary +=
            "                           {\n" +
            "                               \"precinctID\" : \"" + precinctID + "\",\n" +
            "                               \"minorityPopulation\" : \"" + pMinPop + "\",\n" +
            "                               \"minorityVotingAgePopulation\" : \"" + pMinVAP + "\"\n";

                        try {
                            Files.write(Paths.get("summaryJSONFiles/summary_" + getJobID() + ".json"), summary.getBytes(), StandardOpenOption.APPEND);
                        } catch (IOException e) {
                            DebugHelper.log("FAILED to write summary file: " + e.getMessage());
                            return false;
                        }
                        summary = 
            "                           },"; // precinctsInfo obj
                    }
                    summary = summary.substring(0, summary.length() - 1);
                    summary +=
            "\n                         ],\n" + // precinctsInfo array
            "                           \"minorityPopulation\" : \"" + minPop + "\",\n" +
            "                           \"minorityVotingAgePopulation\" : \"" + minVAP + "\"\n" +
            "                       },\n" + // properties
            "                       \"geometry\" : " + district.getRealGeoJSON();
                    try {
                        Files.write(Paths.get("summaryJSONFiles/summary_" + getJobID() + ".json"), summary.getBytes(), StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        DebugHelper.log("FAILED to write summary file: " + e.getMessage());
                        return false;
                    }
                    summary = 
            "\n                 },"; // features obj
                }
                summary = summary.substring(0, summary.length() - 1);
                summary +=
            "\n             ]}},"; // features array & congDistGeo obj & districting obj
            }
        }
        catch (Exception e) {
            DebugHelper.log("FAILED! " + e.getMessage());
        }

        summary = summary.substring(0, summary.length() - 1);                    
        summary += 
            "   ]\n" + // districtings array
            "}]}";

        try {
            Files.write(Paths.get("summaryJSONFiles/summary_" + getJobID() + ".json"), summary.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            DebugHelper.log("FAILED to write summary file: " + e.getMessage());
            return false;
        }
        DebugHelper.log("generateSummaryFile() - EXIT");
        return true;
    }

    public String createConfigJSON() {
        DebugHelper.log("createConfigJSON() - ENTER (jobID=" + getJobID() + ")");
        String config = "";
        if (getParameters() == null) {
            return "{}";
        }
        config = "{\n" + "  \"job\": " + getJobID() + ",\n" + "  \"state\": \""
                + State.getStateAbbreviation(getParameters().getState()) + "\",\n" + "  \"numIterations\": "
                + getParameters().getNumIterations() + ",\n" + "  \"numSeedPlans\": "
                + getParameters().getNumSeedPlans() + ",\n" + "  \"populationVariance\": "
                + (getParameters().getPopulationVariance() / 100.0) + ",\n" + "  \"compactnessMeasure\": "
                + CompactnessMeasure.getCompactnessMeasureValue(getParameters().getCompactnessMeasure()) + ",\n"
                + "  \"numDistricts\": " + State.getNumberOfDistricts(getParameters().getState()) + "\n" + "}";
        DebugHelper.log("createConfigJSON() - EXIT (config='" + config.substring(0, Math.min(40, config.length())) + "...')");
        return config;
    }

    public static void execute(Job job) {
        Thread thread = null;
        if (job == null) {
            return;
        }

        DebugHelper.log("execute() - ENTER (jobID=" + job.getJobID() + ")");
        if (job.getParameters().getNumSeedPlans() > Job.localRunThreshold) {
            thread = new Thread() {
                public void run() {
                    Integer slurmID;

                    DebugHelper.log("execute() request for SEAWULF job");
                    slurmID = SeaWulfHandler.submitSeaWulfJob(job);
                    if (slurmID < 0) {
                        DebugHelper.log("Error submiting job to SeaWulf!");
                    }
                }
            };
            thread.start();
        } else {
            thread = new Thread() {
                public void run() {
                    DebugHelper.log("execute() request for LOCAL SERVER job");
                    if (RedistricterAlgorithm.getInstance().submitLocalJob(job) == true) {
                        job.updateStatus(Status.RUNNING);
                    } else {
                        job.updateStatus(Status.ERROR);
                    }
                }
            };
            thread.start();
        }
        DebugHelper.log("execute() - EXIT");
    }

    public static Boolean cancel(Job job) {
        if (job == null) {
            return false;
        }

        DebugHelper.log("cancel() - ENTER (jobID=" + job.getJobID() + ")");
        if (job.getStatus() != Status.PENDING && job.getStatus() != Status.RUNNING) {
            DebugHelper.log("job is not in correct state for canceling");
            return false;
        }

        if (job.getSlurmJobNumber() != null && job.getSlurmJobNumber() > 0) {
            SeaWulfHandler.cancelSeaWulfJob(job);
        } else {
            RedistricterAlgorithm.getInstance().cancelLocalJob(job);
        }
        DebugHelper.log("cancel() - EXIT");
        return true;
    }

    public static Boolean updateJobs(Map<Job, Status> jobsToUpdate) {
        DebugHelper.log("updateJobs() - ENTER (jobsToUpdate=" + jobsToUpdate + ")");
        for (Entry<Job, Status> entry : jobsToUpdate.entrySet()) {
            DebugHelper.log("Attempting to update JobID=" + entry.getKey().getJobID());
            entry.getKey().updateStatus(entry.getValue());
        }
        DebugHelper.log("updateJobs() - EXIT");
        return true;
    }

    public Boolean updateStatus(Status status) {
        Database db = Database.getInstance();

        DebugHelper.log("updateStatus() - ENTER (JobID=" + jobID + ", Status=" + status.name() + ")");
        setStatus(status);
        db.performSingleStatementUpdate(
                "UPDATE jobs SET job_status=\"" + status.name() + "\" WHERE job_id=" + jobID + ";");
        DebugHelper.log("updateState() - EXIT");
        return true;
    }

    public Integer getDistrictingPlan(MapFilter filter) {
        switch (filter) {
            case FILTER_DP_AVERAGE:
                return getAverageDistrictingPlan();
            case FILTER_DP_BEST:
                return getMaxDistrictingPlan();
            case FILTER_DP_WORST:
                return getMinDistrictingPlan();
            case FILTER_DP_RANDOM:
                return getRandomDistrictingPlan();
            default:
                return null;
        }
    }

    public List<Double> initializeListToNumDistricts(Map<Integer, DistrictingPlan> planMap) {
        List<Double> list = new ArrayList<>();
        Database db = Database.getInstance();
        DistrictingPlan plan = null;
        if(planMap == null) planMap = new HashMap<>();

        synchronized(planMap) {
            if(planMap.containsKey(generatedDistrictingPlans.get(0))) {
                plan = planMap.get(generatedDistrictingPlans.get(0));
            }
            else {
                plan = db.findDistrictingPlanByID(generatedDistrictingPlans.get(0));
                planMap.put(generatedDistrictingPlans.get(0), plan);
            }
        } 
        for (int i = 0; i < plan.getDistricts().size(); i++) {
            list.add(0.0);
        }
        return list;
    }

    public List<Demographic> getTargetDemos() {
        Map<Demographic, Boolean> selectedDemos = parameters.getSelectedDemographics();
        List<Demographic> targetDemoList = new ArrayList<>();
        for (Demographic d : selectedDemos.keySet()) {
            if (selectedDemos.get(d) == true) {
                targetDemoList.add(d);
            }
        }
        return targetDemoList;
    }

    public List<DistrictingPlan> getSortedPlans(Map<Integer, DistrictingPlan> planMap, Map<Integer, District> districtMap) {
        DebugHelper.log("getSortedPlans() - ENTER");
        List<DistrictingPlan> plansToSort = new ArrayList<>();
        if(planMap == null) planMap = new HashMap<>();
        if(districtMap == null) districtMap = new HashMap<>();

        for (Integer y : generatedDistrictingPlans) {
            synchronized(planMap) {
                if(planMap.containsKey(y)) {
                    plansToSort.add(planMap.get(y));
                }
                else {
                    DistrictingPlan dp = Database.getInstance().findDistrictingPlanByID(y);
                    planMap.put(y, dp);
                    plansToSort.add(dp);
                }
            }
        }
        for (DistrictingPlan temp : plansToSort) {
            if (temp.getSortedDistricts() == null && temp != null) {
                sortDistricts(temp, districtMap);
            }
        }
        DebugHelper.log("getSortedPlans() - EXIT (plansToSort=" + plansToSort.toString() + ")");
        return plansToSort;
    }

    public List<Double> getSortedPlanAverages(Map<Integer, DistrictingPlan> planMap, Map<Integer, District> districtMap) {
        DebugHelper.log("getSortedPlanAverages() -- ENTER");
        Database db = Database.getInstance();
        Integer tempID;
        Double pop = 0.0, sortedPlanAverage;
        District tempDistrict;
        Map<Demographic, Integer> votingAgeDemos;
        List<Integer> tempDistricts;
        List<Demographic> targetDemographics = getTargetDemos();

        if (generatedDistrictingPlans.size() < 1) {
            DebugHelper.log("No DistrictingPlans for JobID=" + getJobID());
            return null;
        }
        if(planMap == null) planMap = new HashMap<>();
        if(districtMap == null) districtMap = new HashMap<>();
        List<DistrictingPlan> sortedPlans = getSortedPlans(planMap, districtMap);
        List<Double> sortedPlanAverages = initializeListToNumDistricts(planMap);


        // For each of the generated plans
        for (DistrictingPlan plan : sortedPlans) {
            tempDistricts = plan.getSortedDistricts();
            // For each district in each generated plan
            for (int i = 0; i < tempDistricts.size(); i++) {
                tempID = tempDistricts.get(i);
                synchronized(districtMap) {
                    if(districtMap.containsKey(tempID)) {
                        tempDistrict = districtMap.get(tempID);
                    }
                    else {
                        tempDistrict = db.findDistrictByID(tempID);
                        districtMap.put(tempID, tempDistrict);
                    }
                }
                // Get k: demographic, v: populationInDistrict map for the district
                votingAgeDemos = tempDistrict.getVotingAgePopulationByDemographic();
                // Add up target demo populations in district
                for (Demographic targetDemographic : targetDemographics) {
                    pop += votingAgeDemos.get(targetDemographic);
                }
                // Add population of target demos for district to rolling sum
                // of populations at sorted list index i
                sortedPlanAverage = sortedPlanAverages.get(i) + (pop / tempDistrict.getTotalPopulation());
                sortedPlanAverages.set(i, sortedPlanAverage);
                pop = 0.0;
            }
        }
        // Divide sum at each position in list by amount of districtingplans used in its
        // calculation
        for (int i = 0; i < sortedPlanAverages.size(); i++) {
            sortedPlanAverage = sortedPlanAverages.get(i) / sortedPlans.size();
            sortedPlanAverages.set(i, sortedPlanAverage);
        }
        DebugHelper.log("getSortedPlanAverages() -- EXIT (" + sortedPlanAverages + ")");
        return sortedPlanAverages;
    }

    public Double calculateVariance(Map<Integer, DistrictingPlan> planMap, DistrictingPlan plan, Map<Integer, District> districtMap) {
        DebugHelper.log("calculateVariance() - ENTER");
        Database db = Database.getInstance();
        Integer index = 0;
        Double pop, variance, sortedPlanAverage, sum = 0.0;
        District tempDistrict;
        List<Integer> sortedDistrictIDs;
        List<Demographic> targetDemographics = getTargetDemos();
        List<Double> sortedPlanAverages = getSortedPlanAverages(planMap, districtMap), percentages = new ArrayList<>();
        if(planMap == null) planMap = new HashMap<>();
        if(districtMap == null) districtMap = new HashMap<>();

        // If plan's variance has already been calculated, just return
        if (plan.getVariance() != null) {
            DebugHelper.log("calculateVariance() - EXIT (" + plan.getVariance() + ")");
            return plan.getVariance();
        }
        // get pop percentage for each district in plan
        sortedDistrictIDs = plan.getSortedDistricts();
        DebugHelper.log("sortedDistrictIDs = " + sortedDistrictIDs);
        for (Integer i : sortedDistrictIDs) {
            pop = 0.0;
            synchronized(districtMap) {
                if(districtMap.containsKey(i)){
                    tempDistrict = districtMap.get(i);
                }
                else {
                    tempDistrict = db.findDistrictByID(i);
                    districtMap.put(i, tempDistrict);
                }
            }
            Map<Demographic, Integer> votingAgeDemos = tempDistrict.getVotingAgePopulationByDemographic();
            for (Demographic targetDemographic : targetDemographics) {
                pop += votingAgeDemos.get(targetDemographic);
            }
            sortedPlanAverage = pop / tempDistrict.getTotalPopulation();
            percentages.add(sortedPlanAverage);
        }
        // get summation of square of difference between plan's pop percentage and the
        // expected value for that district
        for (Double percentage : percentages) {
            sum += Math.pow((percentage - sortedPlanAverages.get(index)), 2);
            index++;
        }
        variance = sum / (generatedDistrictingPlans.size() - 1);
        plan.setVariance(variance);
        db.performSingleStatementUpdate(
                "UPDATE districting_plans SET variance=" + variance + " WHERE id=" + plan.getId());

        DebugHelper.log("calculateVariance() - EXIT (" + variance + ")");
        return variance;
    }

    private Double calculateAverageVariance(Map<Integer, DistrictingPlan> planMap, Map<Integer, District> districtMap) {
        Double totalVariance = 0.0;
        Double averageVariance = 0.0;
        DistrictingPlan tempPlan;
        if(planMap == null) planMap = new HashMap<>();
        if(districtMap == null) districtMap = new HashMap<>();

        DebugHelper.log("calculateAverageVariance() - ENTER - genDP.size()=" + generatedDistrictingPlans.size());
        for (Integer planID : generatedDistrictingPlans) {
            synchronized(planMap) {
                if(planMap.containsKey(planID)) {
                    tempPlan = planMap.get(planID);
                }
                else {
                    tempPlan = Database.getInstance().findDistrictingPlanByID(planID);
                    planMap.put(planID, tempPlan);
                }
            }
            totalVariance += calculateVariance(planMap, tempPlan, districtMap);
        }
        try {
            averageVariance = totalVariance / generatedDistrictingPlans.size();
        } catch (Exception e) {
            DebugHelper.log(e.getMessage());
        }
        DebugHelper.log("calculateAverageVariance() - EXIT (" + averageVariance + ")");
        return averageVariance;
    }

    public DistrictingPlan calculateAverageDistrictingPlan(Map<Integer, DistrictingPlan> planMap, Map<Integer, District> districtMap) {
        DistrictingPlan avgPlan = null;
        Double averageVariance = calculateAverageVariance(planMap, districtMap);
        DistrictingPlan tempPlan;
        if(planMap == null) planMap = new HashMap<>();
        if(districtMap == null) districtMap = new HashMap<>();

        DebugHelper.log("calculateAverageDistrictingPlan() - ENTER (JobID=" + getJobID() +")");
        for (Integer planID : generatedDistrictingPlans) {
            synchronized(planMap) {
                if(planMap.containsKey(planID)) {
                    tempPlan = planMap.get(planID);
                }
                else {
                    tempPlan = Database.getInstance().findDistrictingPlanByID(planID);
                    planMap.put(planID, tempPlan);
                }
            }
            if ((avgPlan == null) || (abs(calculateVariance(planMap, tempPlan, districtMap) - averageVariance) < abs(
                    calculateVariance(planMap, avgPlan, districtMap) - averageVariance))) {
                avgPlan = tempPlan;
            }
        }
        if (avgPlan != null) {
            setAverageDistrictingPlan(avgPlan.getId());
        }

        DebugHelper.log("calculateAverageDistrictingPlan() - EXIT (avgPlan=" + avgPlan.toString() + ")");
        return avgPlan;
    }

    public List<DistrictingPlan> calculateExtremeDistrictingPlans(Map<Integer, DistrictingPlan> planMap, Map<Integer, District> districtMap) {
        Database db = Database.getInstance();
        List<DistrictingPlan> extremePlans = new ArrayList<>();
        DistrictingPlan mostExtreme = null;
        DistrictingPlan secondMostExtreme = null;
        DistrictingPlan tempPlan;

        DebugHelper.log("calculateExtremeDistrictingPlans() - ENTER (JobID=" + getJobID() + ")");
        if (generatedDistrictingPlans.size() < 1) {
            DebugHelper.log("No DistrictingPlans for JobID=" + getJobID());
            return null;
        }

        if(planMap == null) planMap = new HashMap<>();
        if(districtMap == null) districtMap = new HashMap<>();

        if (generatedDistrictingPlans.size() == 1) {
            tempPlan = db.findDistrictingPlanByID(generatedDistrictingPlans.get(0));
            setMinDistrictingPlan(tempPlan.getId());
            setMaxDistrictingPlan(tempPlan.getId());
            extremePlans.add(tempPlan);
            extremePlans.add(tempPlan);
            return extremePlans;
        }

        for (Integer planID : generatedDistrictingPlans) {
            synchronized(planMap) {
                if(planMap.containsKey(planID)) {
                    tempPlan = planMap.get(planID);
                }
                else {
                    tempPlan = db.findDistrictingPlanByID(planID);
                    planMap.put(planID, tempPlan);
                }
            }

            if ((mostExtreme == null) || (calculateVariance(planMap, tempPlan, districtMap) > calculateVariance(planMap, mostExtreme, districtMap))) {
                if (secondMostExtreme != null) {
                    secondMostExtreme = mostExtreme;
                }
                mostExtreme = tempPlan;
            } else if ((secondMostExtreme == null)
                    || (calculateVariance(planMap, tempPlan, districtMap) > calculateVariance(planMap, secondMostExtreme, districtMap))) {
                secondMostExtreme = tempPlan;
            }
        }
        if (mostExtreme != null) {
            setMinDistrictingPlan(mostExtreme.getId());
            extremePlans.add(mostExtreme);
        }
        if (secondMostExtreme != null) {
            setMaxDistrictingPlan(secondMostExtreme.getId());
            extremePlans.add(secondMostExtreme);
        }

        DebugHelper.log("calculateExtremeDistrictingPlans() - EXIT (extremePlans=" + extremePlans.toString() + ")");
        return extremePlans;
    }

    public Integer calculateRandomDistrictingPlan() {
        DebugHelper.log("calculateRandomDistrictingPlan() - ENTER");
        Integer randomPlan = null;
        Random random = new Random();

        if (generatedDistrictingPlans.size() <= 0) {
            return null;
        }

        do {
            randomPlan = generatedDistrictingPlans.get(random.nextInt(generatedDistrictingPlans.size()));
            if (generatedDistrictingPlans.size() < 4) {
                break;
            }
            if (randomPlan != minDistrictingPlan && randomPlan != maxDistrictingPlan
                    && randomPlan != averageDistrictingPlan) {
                break;
            }
        } while (true);
        setRandomDistrictingPlan(randomPlan);

        DebugHelper.log("calculateRandomDistrictingPlan() - EXIT");
        return randomPlan;
    }

    public void sortDistricts(DistrictingPlan plan, Map<Integer, District> districtMap) {
        List<Future<?>> futures = new ArrayList<>();
        List<District> districts = new ArrayList<District>();
        List<Integer> sortedDistricts = new ArrayList<Integer>();
        DistrictComparator districtComparator = new DistrictComparator();
        // if(districtMap == null) districtMap = new HashMap<>();

        DebugHelper.log("sortDistricts() - ENTER (DistrictingPlan=" + plan.getId() + ")");
        for (Integer i : plan.getDistricts()) {
            Callable<Void> c = () -> {
                District d;
                synchronized(districtMap) {
                    if(districtMap.containsKey(i)) {
                        d = districtMap.get(i);
                    }
                    else {
                        d = Database.getInstance().findDistrictByID(i);
                        districtMap.put(i, d);
                    }
                }
                
                synchronized (districts) {
                    districts.add(d);
                }
                return null;
            };
            futures.add(ExecutorHelper.getInstance().submit(c));
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
            }
        }
        districtComparator.setTargetDemographics(getParameters().getSelectedDemographics());
        Collections.sort(districts, districtComparator);
        for (District d : districts) {
            sortedDistricts.add(d.getDistrictID());
        }
        plan.setSortedDistricts(sortedDistricts);
        DebugHelper.log("sortDistricts() - EXIT");
    }

    public void computeDistrictingForClient(DistrictingPlan plan) {
        Database db = Database.getInstance();
        List<String> plans = new ArrayList<String>();
        String planRepresentation = "";

        DebugHelper.log("computeDistrictingForClient() - ENTER (plan=" + plan.toString() + ")");
        for(Integer district : plan.getDistricts()) {
			DebugHelper.log("looking up district=" + district);
			plans.add(db.findDistrictByID(district).getClientGeoJSON());
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
    }

    @SuppressWarnings("unchecked")
    public Boolean processResults(String resultsJSON) {
        DebugHelper.log("processResults() - ENTER (resultsJSON='" + resultsJSON.substring(0, Math.min(resultsJSON.length(), 40)) + "...')");
        Database db = Database.getInstance();
        ExecutorHelper eh = ExecutorHelper.getInstance();
        AlgorithmResults results = new AlgorithmResults();
        ObjectMapper objMapper = new ObjectMapper();
        List<Integer> plansForJob = new ArrayList<>();
        List<Future<?>> futures = new ArrayList<>();
        Map<String, Precinct> precinctMap = new HashMap<>();
        Map<Integer, DistrictingPlan> planMap = new HashMap<>();
        Map<Integer, District> districtMap = new HashMap<>();

        try {
            DebugHelper.log("BEGIN RESULTS PROCESSING");
            results.setGeneratedPlans(
                    (Map<String, Map<Integer, List<String>>>) objMapper.readValue(resultsJSON, Map.class));

            for (Map.Entry<String, Map<Integer, List<String>>> resultsEntry : results.getGeneratedPlans().entrySet()) {
                Map<Integer, List<String>> planLayout = resultsEntry.getValue(); // district# : [precincts]
                DistrictingPlan dp = new DistrictingPlan(); // dp : [ district#s ]

                dp.setDistricts(new ArrayList<Integer>());
                for (Map.Entry<Integer, List<String>> districtLayout : planLayout.entrySet()) {
                    Callable<String> callableTask = () -> {
                        District district = new District();

                        district.setPrecincts(districtLayout.getValue());
                        district.processResultsForDistrict(precinctMap);
                        synchronized (dp.getDistricts()) {
                            BigInteger lastID = db.insertDistrictIntoDB(district);
                            dp.getDistricts().add(lastID.intValue());
                        }
                        DebugHelper.log("1 LEAVING EXECUTOR");
                        return null;
                    };
                    futures.add(ExecutorHelper.getInstance().submit(callableTask));
                    DebugHelper.log("executor is currently running " + eh.getExecutor().getActiveCount() + " tasks");
                    DebugHelper.log("executor has " + eh.getExecutor().getQueue().size() + " tasks in queue");

                }
                /*
                 * This will force the main thread to wait until all tasks in the executor have
                 * finished
                 */
                for (Future<?> future : futures) {
                    future.get();
                }
                sortDistricts(dp, districtMap);
                synchronized (plansForJob) {
                    BigInteger lastID = db.insertDistrictingPlanIntoDB(dp);
                    plansForJob.add(lastID.intValue());
                }
            }
            setGeneratedDistrictingPlans(plansForJob);

        
            db.performSingleStatementUpdate("UPDATE jobs SET generated_plans='"
                    + objMapper.writeValueAsString(plansForJob) + "' WHERE job_id=" + this.getJobID() + ";");
            Callable<String> callableTask = () -> {
                calculateAverageDistrictingPlan(planMap, districtMap);
                return null;
            };
            futures.add(ExecutorHelper.getInstance().submit(callableTask));
            callableTask = () -> {
                calculateExtremeDistrictingPlans(planMap, districtMap);
                return null;
            };
            futures.add(ExecutorHelper.getInstance().submit(callableTask));
            for(Future<?> future : futures) {
                future.get();
            }
            calculateRandomDistrictingPlan(); /* calc random plan AFTER the others are determined */
            db.performSingleStatementUpdate("UPDATE jobs SET average_plan=" + getAverageDistrictingPlan()
                    + ", max_plan=" + getMaxDistrictingPlan() + ", min_plan=" + getMinDistrictingPlan()
                    + ", random_plan=" + getRandomDistrictingPlan() + " WHERE job_id=" + this.getJobID() + ";");
            Job.generateGeoJSONFileForJob(this, districtMap);
            computeDistrictingForClient(db.findDistrictingPlanByID(getAverageDistrictingPlan()));
            computeDistrictingForClient(db.findDistrictingPlanByID(getMaxDistrictingPlan()));
            computeDistrictingForClient(db.findDistrictingPlanByID(getMinDistrictingPlan()));
            computeDistrictingForClient(db.findDistrictingPlanByID(getRandomDistrictingPlan()));
            getBoxAndWhiskerData(planMap, districtMap);
            generateSummaryFile(precinctMap, planMap, districtMap);

        } catch (Exception e) {
            DebugHelper.log(e.getMessage());
        }
        DebugHelper.log("END RESULTS PROCESSING");
        DebugHelper.log("processResults() - EXIT");
        return true;
    }

    @SuppressWarnings("unchecked")
    public static void readGeoJSONResultsFiles(Job job, String path, Boolean isClientFile, Map<Integer, District> districtMap) {
        Database db = Database.getInstance();
        ObjectMapper objMapper = new ObjectMapper();
        District district;

        if (job == null) {
            return;
        }
        if(districtMap == null) districtMap = new HashMap<>();

        DebugHelper.log("readGeoJSONResultsFiles() - ENTER (jobID=" + job.getJobID() + ")");
        try {
            JsonNode root = objMapper.readTree(Paths.get(path).toFile());

            JsonNode average = root.get("average");
            Map<String, Object> avgPlan = objMapper.treeToValue(average, Map.class);
            for(Map.Entry<String, Object> e : avgPlan.entrySet()) {
                Map<String, Object> inner = (Map<String, Object>)((List<Object>)e.getValue()).get(0);
                List<Integer> neighboringDistricts = (List<Integer>)((Map<String, Object>)inner.get("properties")).get("neighbors");
                db.performSingleStatementUpdate("UPDATE districts SET neighboring_districts='" + objMapper.writeValueAsString(neighboringDistricts) + "'" +
                    (isClientFile == true ? ", client_geojson='" : ", real_geojson='") + objMapper.writeValueAsString(inner.get("geometry")) + "'" + 
                    " WHERE id=" + e.getKey() + ";");
                if(districtMap.containsKey(Integer.valueOf(e.getKey()))) {
                    district = districtMap.get(Integer.valueOf(e.getKey()));
                }
                else {
                    district = db.findDistrictByID(Integer.valueOf(e.getKey()));
                }
                district.setNeighboringDistricts(neighboringDistricts);
                districtMap.put(Integer.valueOf(e.getKey()), district);
            }

            JsonNode random = root.get("random");
            Map<String, Object> randomPlan = objMapper.treeToValue(random, Map.class);
            for(Map.Entry<String, Object> e : randomPlan.entrySet()) {
                Map<String, Object> inner = (Map<String, Object>)((List<Object>)e.getValue()).get(0);
                List<Integer> neighboringDistricts = (List<Integer>)((Map<String, Object>)inner.get("properties")).get("neighbors");
                db.performSingleStatementUpdate("UPDATE districts SET neighboring_districts='" + objMapper.writeValueAsString(neighboringDistricts) + "'" +
                    (isClientFile == true ? ", client_geojson='" : ", real_geojson='") + objMapper.writeValueAsString(inner.get("geometry")) + "'" + 
                    " WHERE id=" + e.getKey() + ";");
                if(districtMap.containsKey(Integer.valueOf(e.getKey()))) {
                    district = districtMap.get(Integer.valueOf(e.getKey()));
                }
                else {
                    district = db.findDistrictByID(Integer.valueOf(e.getKey()));
                }
                district.setNeighboringDistricts(neighboringDistricts);
                districtMap.put(Integer.valueOf(e.getKey()), district);
            }

            JsonNode max = root.get("extreme1");
            Map<String, Object> maxPlan = objMapper.treeToValue(max, Map.class);
            for(Map.Entry<String, Object> e : maxPlan.entrySet()) {
                Map<String, Object> inner = (Map<String, Object>)((List<Object>)e.getValue()).get(0);
                List<Integer> neighboringDistricts = (List<Integer>)((Map<String, Object>)inner.get("properties")).get("neighbors");
                db.performSingleStatementUpdate("UPDATE districts SET neighboring_districts='" + objMapper.writeValueAsString(neighboringDistricts) + "'" +
                    (isClientFile == true ? ", client_geojson='" : ", real_geojson='") + objMapper.writeValueAsString(inner.get("geometry")) + "'" + 
                    " WHERE id=" + e.getKey() + ";");
                if(districtMap.containsKey(Integer.valueOf(e.getKey()))) {
                    district = districtMap.get(Integer.valueOf(e.getKey()));
                }
                else {
                    district = db.findDistrictByID(Integer.valueOf(e.getKey()));
                }
                district.setNeighboringDistricts(neighboringDistricts);
                districtMap.put(Integer.valueOf(e.getKey()), district);
            }

            JsonNode min = root.get("extreme2");
            Map<String, Object> minPlan = objMapper.treeToValue(min, Map.class);
            for(Map.Entry<String, Object> e : minPlan.entrySet()) {
                Map<String, Object> inner = (Map<String, Object>)((List<Object>)e.getValue()).get(0);
                List<Integer> neighboringDistricts = (List<Integer>)((Map<String, Object>)inner.get("properties")).get("neighbors");
                db.performSingleStatementUpdate("UPDATE districts SET neighboring_districts='" + objMapper.writeValueAsString(neighboringDistricts) + "'" +
                    (isClientFile == true ? ", client_geojson='" : ", real_geojson='") + objMapper.writeValueAsString(inner.get("geometry")) + "'" + 
                    " WHERE id=" + e.getKey() + ";");
                if(districtMap.containsKey(Integer.valueOf(e.getKey()))) {
                    district = districtMap.get(Integer.valueOf(e.getKey()));                    }
                else {
                    district = db.findDistrictByID(Integer.valueOf(e.getKey()));
                }
                district.setNeighboringDistricts(neighboringDistricts);
                districtMap.put(Integer.valueOf(e.getKey()), district);
            }

        }
        catch (Exception e) {
            DebugHelper.log(e.getMessage());
        }
        DebugHelper.log("readGeoJSONResultsFiles() - EXIT");
    }

    public static void generateGeoJSONFileForJob(Job job, Map<Integer, District> districtMap) {
        Database db = Database.getInstance();
        ObjectMapper objMapper = new ObjectMapper();

        if(job == null) {
            return;
        }
        if(districtMap == null) districtMap = new HashMap<>();

        DebugHelper.log("generateGeoJSONFIleForDP() - ENTER (jobID=" + job.getJobID() + ")");
        try {
            Map<String, Map<Integer, List<String>>> plans = new HashMap<>();
            Map<Integer, List<String>> p = new HashMap<>();
            for(Integer districtID : db.findDistrictingPlanByID(job.getAverageDistrictingPlan()).getDistricts()) {
                p.put(districtID, db.findDistrictByID(districtID).getPrecincts());
            }
            plans.put("average", p);

            p = new HashMap<>();
            for(Integer districtID : db.findDistrictingPlanByID(job.getRandomDistrictingPlan()).getDistricts()) {
                p.put(districtID, db.findDistrictByID(districtID).getPrecincts());
            }
            plans.put("random", p);

            p = new HashMap<>();
            for(Integer districtID : db.findDistrictingPlanByID(job.getMaxDistrictingPlan()).getDistricts()) {
                p.put(districtID, db.findDistrictByID(districtID).getPrecincts());
            }
            plans.put("extreme1", p);

            p = new HashMap<>();
            for(Integer districtID : db.findDistrictingPlanByID(job.getMinDistrictingPlan()).getDistricts()) {
                p.put(districtID, db.findDistrictByID(districtID).getPrecincts());
            }
            plans.put("extreme2", p);

            objMapper.writeValue(Paths.get("job_" + job.getJobID() + ".json").toFile(), plans);
            
            DebugHelper.log("Launching python script to generate client-side geojson for jobID=" + job.getJobID());
            Process process = Runtime.getRuntime().exec(
                "python3 compute_districting_geojson.py static/" + State.getStateAbbreviation(job.getParameters().getState()) + 
                "_pop_simplified_geo_singles.geojson job_" + job.getJobID() + ".json out_" + job.getJobID() + "_client.json");
            process.waitFor();
            DebugHelper.log("Launching python script to generate real geojson for jobID=" + job.getJobID());
            process = Runtime.getRuntime().exec(
                "python3 compute_districting_geojson.py static/" + State.getStateAbbreviation(job.getParameters().getState()) +
                "_db.geojson job_" + job.getJobID() + ".json out_" + job.getJobID() + "_real.json");
            process.waitFor();

            readGeoJSONResultsFiles(job, "out_" + job.getJobID() + "_client.json", true, districtMap);
            readGeoJSONResultsFiles(job, "out_" + job.getJobID() + "_real.json", false, districtMap);
            Paths.get("job_" + job.getJobID() + ".json").toFile().delete();
            Paths.get("out_" + job.getJobID() + "_client.json").toFile().delete();
            Paths.get("out_" + job.getJobID() + "_real.json").toFile().delete();
        }
        catch (Exception e) {
            DebugHelper.log(e.getMessage());
        }

        DebugHelper.log("generateGeoJSONFileForDP() - EXIT");
    }

    //GETTERS/SETTERS
    public Integer getJobID() {
        return jobID;
    }

    public void setJobID(Integer jobID) {
        this.jobID = jobID;
    }

    public List<Integer> getGeneratedDistrictingPlans() {
        return generatedDistrictingPlans;
    }

    public void setGeneratedDistrictingPlans(List<Integer> generatedDistrictingPlans) {
        this.generatedDistrictingPlans = generatedDistrictingPlans;
    }

    public BoxAndWhiskerData getBoxAndWhiskerData(Map<Integer, DistrictingPlan> planMap, Map<Integer, District> districtMap) {
        DebugHelper.log("getBoxAndWhiskerData() - ENTER");
        Database db = Database.getInstance();
        ObjectMapper objMapper = new ObjectMapper();
        List<DistrictingPlan> plans;
        DistrictingPlan currentPlan;
        DistrictingPlan tempPlan;
        Integer currentPlanID;

        if(boxAndWhiskerData != null) {
            DebugHelper.log("getBoxAndWhiskerData() - EXIT (boxAndWhiskerData=" + boxAndWhiskerData + ")");
            return boxAndWhiskerData;
        }
        if(planMap == null) planMap = new HashMap<>();
        if(districtMap == null) districtMap = new HashMap<>();

        plans = new ArrayList<>();
        for(Integer i : generatedDistrictingPlans) {
            tempPlan = db.findDistrictingPlanByID(i);
            sortDistricts(tempPlan, districtMap);
            plans.add(tempPlan);
        }

        DebugHelper.log("... generatedDistrictingPlans successfully sorted");
        State tempState = db.findStateByID(parameters.getState());
        currentPlanID = tempState.getCurrentlyEnactedPlan();
        currentPlan = db.findDistrictingPlanByID(currentPlanID);
        DebugHelper.log("... attempting to sort currentPlan");
        sortDistricts(currentPlan, districtMap);
        DebugHelper.log("... currentPlan sorted");
        DebugHelper.log("... setting target demographics for plot");
        BoxAndWhiskerData.setTargetDemosForPlot(parameters.getSelectedDemographics());
        DebugHelper.log("... calling generateBoxAndWhiskerData()");
        boxAndWhiskerData = BoxAndWhiskerData.generateBoxAndWhiskerData(plans, currentPlan, districtMap);

        try {
            db.performSingleStatementUpdate("UPDATE jobs SET baw_data='" + objMapper.writeValueAsString(boxAndWhiskerData) + "' WHERE job_id=" + this.getJobID() +";");
        }
        catch (Exception e) {
            DebugHelper.log(e.getMessage());
        }
        DebugHelper.log("getBoxAndWhiskerData() - EXIT (boxAndWhiskerData=" + boxAndWhiskerData + ")");
        return boxAndWhiskerData;
    }

    public void setBoxAndWhiskerData(BoxAndWhiskerData boxAndWhiskerData) {
        this.boxAndWhiskerData = boxAndWhiskerData;
    }

    public Integer getAverageDistrictingPlan() {
        return averageDistrictingPlan;
    }

    public void setAverageDistrictingPlan(Integer averageDistrictingPlan) {
        this.averageDistrictingPlan = averageDistrictingPlan;
    }

    public Integer getMaxDistrictingPlan() {
        return maxDistrictingPlan;
    }

    public void setMaxDistrictingPlan(Integer maxDistrictingPlan) {
        this.maxDistrictingPlan = maxDistrictingPlan;
    }

    public Integer getMinDistrictingPlan() {
        return minDistrictingPlan;
    }

    public void setMinDistrictingPlan(Integer minDistrictingPlan) {
        this.minDistrictingPlan = minDistrictingPlan;
    }

    public Integer getRandomDistrictingPlan() {
        return randomDistrictingPlan;
    }

    public void setRandomDistrictingPlan(Integer randomDistrictingPlan) {
        this.randomDistrictingPlan = randomDistrictingPlan;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    public Long getParametersID() {
        return parametersID;
    }

    public void setParametersID(Long parametersID) {
        this.parametersID = parametersID;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Integer getSlurmJobNumber() {
        return this.slurmJobNumber;
    }

    public void setSlurmJobNumber(Integer slurmJobNumber) {
        this.slurmJobNumber = slurmJobNumber;
    }

    public Long getLocalPID() {
        return this.localPID;
    }

    public void setLocalPID(Long localPID) {
        this.localPID = localPID;
    }

    public static Integer getLocalRunThreshold() {
        return Job.localRunThreshold;
    }

    public static void setLocalRunThreshold(Integer level) {
        Job.localRunThreshold = level;
    }
}