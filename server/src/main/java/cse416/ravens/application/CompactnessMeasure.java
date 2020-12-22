package cse416.ravens.application;

public enum CompactnessMeasure {
	HIGH_COMPACTNESS,
	MEDIUM_COMPACTNESS,
	LOW_COMPACTNESS,
	NO_COMPACTNESS;

	public static Double getCompactnessMeasureValue(CompactnessMeasure cm) {
		switch(cm) {
			case HIGH_COMPACTNESS:
				return 0.25;
			case MEDIUM_COMPACTNESS:
				return 0.5;
			case LOW_COMPACTNESS:
				return 0.75;
			case NO_COMPACTNESS:
			default:
				return 1.0;
		}
	}
}
