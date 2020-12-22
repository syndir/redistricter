package cse416.ravens.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.*;

@JsonIgnoreProperties({ "id", "numIterations", "defaultNumIterations" })
public class Parameters {
    private Integer id;
    private String state;
    private CompactnessMeasure compactnessMeasure;
    private Double populationVariance;
    private Map<Demographic, Boolean> selectedDemographics;
    private Integer numSeedPlans;
    private Integer numIterations;
    private static Integer defaultNumIterations;

    public Parameters(Map<Demographic, Boolean> selectedDemographics, int numSeedPlans, int numIterations, CompactnessMeasure compactnessMeasure, double populationVariance) {
        this.compactnessMeasure = compactnessMeasure;
        this.populationVariance =  populationVariance;
        this.selectedDemographics = selectedDemographics;
        this.numSeedPlans = numSeedPlans;
        this.numIterations = numIterations;
        this.state = RedistricterController.getCurrentState().getState().name();
    }

    public Parameters(Integer id, String state, Map<Demographic, Boolean> selectedDemographics, int numSeedPlans, int numIterations, CompactnessMeasure compactnessMeasure, double populationVariance) {
        this.id = id;
        this.compactnessMeasure = compactnessMeasure;
        this.populationVariance =  populationVariance;
        this.selectedDemographics = selectedDemographics;
        this.numSeedPlans = numSeedPlans;
        this.numIterations = numIterations;
        this.state = state;
    }

    public Parameters() {}

    //GETTERS/SETTERS
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public CompactnessMeasure getCompactnessMeasure() {
        return compactnessMeasure;
    }

    public void setCompactnessMeasure(CompactnessMeasure compactnessMeasure) {
        this.compactnessMeasure = compactnessMeasure;
    }

    public Double getPopulationVariance() {
        return populationVariance;
    }

    public void setPopulationVariance(Double populationVariance) {
        this.populationVariance = populationVariance;
    }

    public Map<Demographic, Boolean> getSelectedDemographics() {
        return selectedDemographics;
    }

    public void setSelectedDemographics(Map<Demographic, Boolean> selectedDemographics) {
        this.selectedDemographics = selectedDemographics;
    }

    public Integer getNumSeedPlans() {
        return numSeedPlans;
    }

    public void setNumSeedPlans(Integer numSeedPlans) {
        this.numSeedPlans = numSeedPlans;
    }

    public Integer getNumIterations() { 
        return this.numIterations;
    }

    public void setNumIterations(Integer numIterations) {
        this.numIterations = numIterations;
    }

    public static Integer getDefaultNumIterations() {
        return Parameters.defaultNumIterations;
    }

    public static void setDefaultNumIterations(Integer numIterations) {
        Parameters.defaultNumIterations = numIterations;
    }

    public String toString() {
        return new String("state: " + state + ", seeds: " + numSeedPlans + ", iterations: " + numIterations + ", compactness: " + compactnessMeasure.name() +
                ", population variance: " + populationVariance + ", demographics: (" + selectedDemographics.toString() + ")");
    }
}
