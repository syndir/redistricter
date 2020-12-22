package cse416.ravens.application;

import java.util.*;

public class District {
	private Integer districtID;
	private Integer name;
	private List<String> precincts;
	private List<Integer> neighboringDistricts;
	private Integer totalPopulation;
	private Double compactness;
	private Integer numberOfCounties;
	private Map<Demographic, Integer> totalPopulationByDemographic;
	private Map<Demographic, Integer> votingAgePopulationByDemographic;
	private String clientGeoJSON = "";
	private String realGeoJSON = "";

	public void processResultsForDistrict(Map<String, Precinct> preloadedPrecincts) {
		Database db = Database.getInstance();
		Set<String> counties = new HashSet<>();
		Integer total = 0;
		Precinct precinct;


		DebugHelper.log("processResultsForDistrict() - ENTER (district=" + districtID +")");
		if(preloadedPrecincts == null) {
			DebugHelper.log("Unable to process results with no precinct information!");
			return;
		}
		// DebugHelper.log("preloadedPrecincts contains: " + preloadedPrecincts.toString());
		totalPopulationByDemographic = new HashMap<>();
		votingAgePopulationByDemographic = new HashMap<>();
		for(String precinctID : precincts) {
			synchronized(preloadedPrecincts) {
				if(preloadedPrecincts.containsKey(precinctID)) {
					precinct = preloadedPrecincts.get(precinctID);
				}
				else {
					precinct = db.findPrecinctByID(precinctID);
					preloadedPrecincts.put(precinctID, precinct);
				}
			}
			counties.add(precinct.getCountyName());
			total += precinct.getTotalPopulations().get(Demographic.ALL);
			for(Map.Entry<Demographic, Integer> demo : precinct.getTotalPopulations().entrySet()) {
				totalPopulationByDemographic.put(demo.getKey(), totalPopulationByDemographic.getOrDefault(demo.getKey(), 0) + demo.getValue());
			}
			for(Map.Entry<Demographic, Integer> demo : precinct.getVotingAgePopulations().entrySet()) {
				votingAgePopulationByDemographic.put(demo.getKey(), votingAgePopulationByDemographic.getOrDefault(demo.getKey(), 0) + demo.getValue());
			}
		}
		setNumberOfCounties(counties.size());
		setTotalPopulation(total);
		DebugHelper.log("processResultsForDistrict() - EXIT");
	}

	//GETTERS/SETTERS
	public Integer getName() {
		return name;
	}

	public void setName(Integer name) {
		this.name = name;
	}

	public List<String> getPrecincts() {
		return precincts;
	}

	public void setPrecincts(List<String> precincts) {
		this.precincts = precincts;
	}

	public List<Integer> getNeighboringDistricts() {
		return this.neighboringDistricts;
	}

	public void setNeighboringDistricts(List<Integer> neighboringDistricts) {
		this.neighboringDistricts = neighboringDistricts;
	}

	public Integer getTotalPopulation() {
		return totalPopulation;
	}

	public void setTotalPopulation(Integer totalPopulation) {
		this.totalPopulation = totalPopulation;
	}

	public Double getCompactness() {
		return compactness;
	}

	public void setCompactness(Double compactness) {
		this.compactness = compactness;
	}

	public Integer getNumberOfCounties() {
		return numberOfCounties;
	}

	public void setNumberOfCounties(Integer numberOfCounties) {
		this.numberOfCounties = numberOfCounties;
	}

	public Map<Demographic, Integer> getTotalPopulationByDemographic() {
		return totalPopulationByDemographic;
	}

	public void setTotalPopulationByDemographic(Map<Demographic, Integer> totalPopulationByDemographic) {
		this.totalPopulationByDemographic = totalPopulationByDemographic;
	}

	public Map<Demographic, Integer> getVotingAgePopulationByDemographic() {
		return votingAgePopulationByDemographic;
	}

	public void setVotingAgePopulationByDemographic(Map<Demographic, Integer> votingAgePopulationByDemographic) {
		this.votingAgePopulationByDemographic = votingAgePopulationByDemographic;
	}

	public Integer getDistrictID() {
		return districtID;
	}

	public void setDistrictID(Integer districtID) {
		this.districtID = districtID;
	}

	public String getRealGeoJSON() {
		return realGeoJSON;
	}
	public void setRealGeoJSON(String realGeoJSON) {
		this.realGeoJSON = realGeoJSON;
	}

	public String getClientGeoJSON() {
		return clientGeoJSON;
	}

	public void setClientGeoJSON(String clientGeoJSON) {
		this.clientGeoJSON = clientGeoJSON;
	}
}
