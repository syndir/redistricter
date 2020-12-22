package cse416.ravens.application;

import org.apache.commons.math3.stat.*;

import java.util.*;

public class BoxAndWhiskerData {
    private Whisker[] whiskers;
    private List<Double> currentPlanDataPoints;
    public static List<Demographic> targetDemos = null;

    //Districts must all be sorted and setTargetDemographics must be called before calling this method
    public static BoxAndWhiskerData generateBoxAndWhiskerData(List<DistrictingPlan> plans, DistrictingPlan currentPlan, Map<Integer, District> districtMap) {
        BoxAndWhiskerData plot = new BoxAndWhiskerData();
        Whisker[] whiskerList = new Whisker[plans.get(0).getSortedDistricts().size()];
        List<Double> popPercents;
        double[] tempArray;
        List<Double> currentDataPoints;

        DebugHelper.log("generateBoxAndWhiskerData() - ENTER (plans=" + plans.toString() +", currentPlan=" + currentPlan.toString() + ")");
        if(districtMap == null) districtMap = new HashMap<>();

        if(currentPlan.getSortedDistricts() == null){
            DebugHelper.log("Sorted districts for enacted plan are null");
            DebugHelper.log("generateBoxAndWhiskerData() - EXIT (null)");
            return null;
        }
        for(int i = 0; i < plans.get(0).getSortedDistricts().size(); i++) {
            whiskerList[i] = new Whisker();
            popPercents = getPopPercents(plans, i, districtMap);
            Collections.sort(popPercents);
            tempArray = popPercents.stream().mapToDouble(d->d).toArray();
            whiskerList[i].setMin(StatUtils.min(tempArray));
            whiskerList[i].setMax(StatUtils.max(tempArray));
            whiskerList[i].setMedian(StatUtils.percentile(tempArray, 50));
            whiskerList[i].setQ1(StatUtils.percentile(tempArray, 25));
            whiskerList[i].setQ3(StatUtils.percentile(tempArray, 75));
            popPercents.clear();
        }
        plot.setWhiskers(whiskerList);
        
        currentDataPoints = getCurrentPlanPercents(currentPlan);
        plot.setCurrentPlanDataPoints(currentDataPoints);

        DebugHelper.log("generateBoxAndWhiskerData() - EXIT (plot=" + plot.toString() + ")");
        return plot;
    }

    public static List<Double> getCurrentPlanPercents(DistrictingPlan currentPlan) {
        DebugHelper.log("getCurrentPlanPercents() - ENTER (currentPlan=" + currentPlan.toString() + ")");
        List<Double> currentDataPoints = new ArrayList<>();
        District tempDistrict;
        Map<Demographic, Integer> votingAgeDemos;
        Double enactedPop = 0.0;
        Double enactedPercent;

        for(Integer enactedDistrictID : currentPlan.getSortedDistricts()) {
            tempDistrict = Database.getInstance().findDistrictByID(enactedDistrictID);

            votingAgeDemos = tempDistrict.getVotingAgePopulationByDemographic();
            for(Demographic targetDemographic : targetDemos) {
                enactedPop += votingAgeDemos.get(targetDemographic);
            }
            enactedPercent = enactedPop / tempDistrict.getTotalPopulation();
            currentDataPoints.add(enactedPercent);
            enactedPop = 0.0;
        }
        DebugHelper.log("getCurrentPlanPercents() - EXIT (currentDataPoints=" + currentDataPoints.toString() + ")");
        return currentDataPoints;
    }

    public static List<Double> getPopPercents(List<DistrictingPlan> plans, int i, Map<Integer, District> districtMap) {
        DebugHelper.log("getPopPercents() - ENTER");
        Database db = Database.getInstance();
        List<Integer> tempDistricts;
        District tempDistrict;
        Double population = 0.0;
        Double popPercent;
        Map<Demographic, Integer> votingAgeDemos;
        List<Double> popPercents = new ArrayList<>();
        
        if(districtMap == null) districtMap = new HashMap<>();
        for (DistrictingPlan plan : plans) {
            if (plan.getSortedDistricts() == null) {
                DebugHelper.log("Sorted districts for plan: " + plan.getId() + " are null");
            }
            tempDistricts = plan.getSortedDistricts();
            //Get district at ith position of sorted district list
            synchronized(districtMap) {
                if(districtMap.containsKey(tempDistricts.get(i))) {
                    tempDistrict = districtMap.get(tempDistricts.get(i));
                }
                else {
                    tempDistrict = db.findDistrictByID(tempDistricts.get(i));
                    districtMap.put(tempDistricts.get(i), tempDistrict);
                }
            }
            //Get population percentage for that district
            votingAgeDemos = tempDistrict.getVotingAgePopulationByDemographic();
            for(Demographic targetDemographic : targetDemos) {
                population += votingAgeDemos.get(targetDemographic);
            }
            popPercent = population / tempDistrict.getTotalPopulation();

            //Add popPercent of district to list of pop%s for each district at sorted list position i in each plan
            popPercents.add(popPercent);
            population = 0.0;
        }

        DebugHelper.log("getPopPercents() - EXIT (popPercents=" + popPercents.toString() + ")");
        return popPercents;
    }

    public static void setTargetDemosForPlot(Map<Demographic, Boolean> selectedDemos) {
        if(selectedDemos == null) {
            DebugHelper.log("setTargetDemosForPlot() - EXIT");
            return;
        }
        DebugHelper.log("setTargetDemosForPlot() - ENTER (selectedDemos=" + selectedDemos.toString() + ")");
        targetDemos = new ArrayList<>();
        for(Demographic d : selectedDemos.keySet()) {
            if(selectedDemos.get(d) == true) {
                DebugHelper.log("sorting on " + d.name());
                targetDemos.add(d);
            }
        }
        DebugHelper.log("setTargetDemosForPlot() - EXIT");
    }

    //GETTERS/SETTERS
    public Whisker[] getWhiskers() {
        return whiskers;
    }

    public void setWhiskers(Whisker[] whiskers) {
        this.whiskers = whiskers.clone();
    }

    public List<Double> getCurrentPlanDataPoints() {
        return currentPlanDataPoints;
    }

    public void setCurrentPlanDataPoints(List<Double> currentPlanDataPoints) {
        this.currentPlanDataPoints = currentPlanDataPoints;
    }
}
