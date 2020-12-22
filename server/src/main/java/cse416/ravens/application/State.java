package cse416.ravens.application;

import java.util.*;

public class State {
    private StateSelection state;
    private Boundary boundary;
    private Integer currentlyEnactedPlan;
    private List<String> precincts;

    public static String getStateAbbreviation(String state) {
        switch(StateSelection.valueOf(state)) {
            case ALABAMA:
                return "AL";
            case FLORIDA:
                return "FL";
            case PENNSYLVANIA:
                return "PA";
            default:
                return "ERROR";
        }
    }

    public static Integer getNumberOfDistricts(String state) {
        switch(StateSelection.valueOf(state)) {
            case ALABAMA:
                return 7;
            case FLORIDA:
                return 27;
            case PENNSYLVANIA:
                return 18;
            default:
                return 0;
        }
    }

    public static String getStateCode(String state) {
        switch(StateSelection.valueOf(state)) {
            case ALABAMA:
                return "01";
            case FLORIDA:
                return "12";
            case PENNSYLVANIA:
                return "42";
            default:
                return "";
        }
    }

    //GETTERS/SETTERS
    public StateSelection getState() {
        return state;
    }

    public void setState(StateSelection state) {
        this.state = state;
    }

    public Boundary getBoundary() {
        return boundary;
    }

    public void setBoundary(Boundary boundary) {
        this.boundary = boundary;
    }

    public Integer getCurrentlyEnactedPlan() {
        return currentlyEnactedPlan;
    }

    public void setCurrentlyEnactedPlan(Integer currentlyEnactedPlan) {
        this.currentlyEnactedPlan = currentlyEnactedPlan;
    }

    public List<String> getPrecincts() {
        return precincts;
    }

    public void setPrecincts(List<String> precincts) {
        this.precincts = precincts;
    }


}
