package cse416.ravens.application;

import java.util.*;

public class AlgorithmResults {
	private Map<String, Map<Integer, List<String>>> generatedPlans;

	public void setGeneratedPlans(Map<String, Map<Integer, List<String>>> generatedPlans) {
		this.generatedPlans = generatedPlans;
	}

	public Map<String, Map<Integer, List<String>>> getGeneratedPlans() {
		return generatedPlans;
	}
}
