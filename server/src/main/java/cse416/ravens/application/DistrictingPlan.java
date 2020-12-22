package cse416.ravens.application;

import java.util.*;

public class DistrictingPlan {
    private Integer id;
    private List<Integer> districts;
    private List<Integer> sortedDistricts;
    private Double variance;
    private String geoJSON;

    public DistrictingPlan() {}

    //NOTE: Callers responsibility to call BOTH Job.sortDistricts(enacted) and Job.sortDistricts(this) BEFORE calling this method
    public Map<Integer,Integer> associatePlanWithEnacted(DistrictingPlan enacted) {
        Database db = Database.getInstance();
        Map<Integer,Integer> association = new HashMap<>();
        District district;
        List<String> precinctList;
        Precinct precinct;
        Integer index = null;
        DebugHelper.log("associatePlanWithEnacted() - ENTER (id=" + id +", enacted=" + enacted.toString() + ")");

        if(enacted == null || enacted.getSortedDistricts() == null || sortedDistricts == null){
            DebugHelper.log("Issue getting sorted districts, make sure all districts are sorted BEFORE calling associatePlanWithEnacted()");
            return null;
        }

        //For each district
        for(Integer districtID : sortedDistricts) {
            district = db.findDistrictByID(districtID);
            precinctList = district.getPrecincts();
            //For each precinct in each district
            for(String precinctID : precinctList) {
                precinct = db.findPrecinctByID(precinctID);
                //look for what district contains that precinct in enacted
                index = findPrecinctInEnacted(precinct,enacted, association);
                if(index != null) {
                    association.put(districtID, index);
                    break;
                }
            }
        }

        DebugHelper.log("associatePlanWithEnacted - EXIT (association=" + association +")");
        return association;
    }

    public static Integer findPrecinctInEnacted(Precinct precinct, DistrictingPlan enacted, Map<Integer,Integer> association){
        Database db = Database.getInstance();
        District district;
        List<String> precinctIDList;
        List<String> precinctNameList = new ArrayList<>();
        List<Integer> enactedDistrictIDs = db.findDistrictingPlanByID(enacted.getId()).getSortedDistricts();

        DebugHelper.log("findPrecinctInEnacted() - ENTER (precinct=" + precinct.toString() + ", enacted=" + enacted.toString() + ", association=" + association.toString() + ")");
        //For each district in enacted plan
        for(Integer districtID : enactedDistrictIDs) {
            if(association.containsValue(districtID)) {
                continue;
            }
            district = db.findDistrictByID(districtID);
            precinctIDList = district.getPrecincts();
            //Create list of precinct names for each district in enacted precinct
            for(String precinctID : precinctIDList) {
                precinctNameList.add(db.findPrecinctByID(precinctID).getPrecinctName());
            }
            //If district has a matching precinct, return district ID
            if (precinctNameList.contains(precinct.getPrecinctName()) && !association.containsValue(districtID)) {
                DebugHelper.log("findPrecicntInEnacted() - EXIT (districtID=" + districtID + ")");
                return districtID;
            }
        }
        //If no district has a matching precinct, return null
        DebugHelper.log("findPrecicntInEnacted() - EXIT (districtID=null)");
        return null;
    }

    //GETTERS/SETTERS
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<Integer> getDistricts() {
        return districts;
    }

    public void setDistricts(List<Integer> districts) {
        this.districts = districts;
    }

    public List<Integer> getSortedDistricts() {
        return sortedDistricts;
    }

    public void setSortedDistricts(List<Integer> sortedDistricts) {
        this.sortedDistricts = sortedDistricts;
    }

    public Double getVariance() {
        return variance;
    }

    public void setVariance(Double variance) {
        this.variance = variance;
    }

    public String getGeoJSON() {
        return geoJSON;
    }

    public void setGeoJSON(String geoJSON) { 
        this.geoJSON = geoJSON;
    }
}
