package cse416.ravens.application;

import java.util.*;

public class Precinct {
    private String precinctID;
    private String precinctName;
    private String countyName;
    private Integer countyID;
    private Map<Demographic, Integer> totalPopulations;
    private Map<Demographic, Integer> votingAgePopulations;
    private String boundary;
    //private Boundary boundary;
    private List<String> precinctNeighbors;

    //GETTERS/SETTERS
    public String getPrecinctID() {
        return precinctID;
    }

    public void setPrecinctID(String precinctID) {
        this.precinctID = precinctID;
    }

    public String getPrecinctName() {
        return precinctName;
    }

    public void setPrecinctName(String name) {
        this.precinctName = name;
    }

    public String getCountyName() {
        return countyName;
    }

    public void setCountyName(String countyName) {
        this.countyName = countyName;
    }

    public Integer getCountyID() {
        return countyID;
    }

    public void setCountyID(Integer countyID) {
        this.countyID = countyID;
    }

    public Map<Demographic, Integer> getTotalPopulations() {
        return totalPopulations;
    }

    public void setTotalPopulations(Map<Demographic, Integer> totalPopulations) {
        this.totalPopulations = totalPopulations;
    }

    public Map<Demographic, Integer> getVotingAgePopulations() {
        return votingAgePopulations;
    }

    public void setVotingAgePopulations(Map<Demographic, Integer> votingAgePopulations) {
        this.votingAgePopulations = votingAgePopulations;
    }

    public String getBoundary() {
        return boundary;
    }

    public void setBoundary(String boundary) {
        this.boundary = boundary;
    }

    public List<String> getPrecinctNeighbors() {
        return precinctNeighbors;
    }

    public void setPrecinctNeighbors(List<String> precinctNeighbors) {
        this.precinctNeighbors = precinctNeighbors;
    }
}
