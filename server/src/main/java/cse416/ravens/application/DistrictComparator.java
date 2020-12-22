package cse416.ravens.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DistrictComparator implements Comparator<District> {
    List<Demographic> targetDemographics = null;

    public void setTargetDemographics(Map<Demographic, Boolean> selectedDemos) {
        targetDemographics = new ArrayList<>();
        for(Demographic d : selectedDemos.keySet()) {
            if(selectedDemos.get(d) == true) {
                DebugHelper.log("sorting on " + d.name());
                targetDemographics.add(d);
            }
        }
    }

    @Override
    public int compare(District a, District b) {
        Map<Demographic, Integer> aVotingAgeDemos = a.getVotingAgePopulationByDemographic();
        Map<Demographic, Integer> bVotingAgeDemos = b.getVotingAgePopulationByDemographic();
        Double aSelectedPop = 0.0;
        Double bSelectedPop = 0.0;
        Double aPercentage;
        Double bPercentage;

        for(Demographic targetDemographic : targetDemographics) {
            aSelectedPop += aVotingAgeDemos.get(targetDemographic);
            bSelectedPop += bVotingAgeDemos.get(targetDemographic);
        }
        aPercentage = aSelectedPop / a.getTotalPopulation();
        bPercentage = bSelectedPop / b.getTotalPopulation();

        if(aPercentage < bPercentage)
        {
            return -1;
        }
        if(aPercentage > bPercentage)
        {
            return 1;
        }
        return 0;
    }
}
