package cse416.ravens.application;

public class Whisker {
	private Double q1;
	private Double q3;
	private Double median;
	private Double min;
	private Double max;

	//GETTERS/SETTERS
	public Double getQ1() {
		return q1;
	}

	public void setQ1(Double q1) {
		this.q1 = q1;
	}

	public Double getQ3() {
		return q3;
	}

	public void setQ3(Double q3) {
		this.q3 = q3;
	}

	public Double getMedian() {
		return median;
	}

	public void setMedian(Double median) {
		this.median = median;
	}

	public Double getMin() {
		return min;
	}

	public void setMin(Double min) {
		this.min = min;
	}

	public Double getMax() {
		return max;
	}

	public void setMax(Double max) {
		this.max = max;
	}
}