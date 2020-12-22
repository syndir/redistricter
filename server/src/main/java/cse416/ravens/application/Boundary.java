package cse416.ravens.application;

import java.awt.geom.Point2D;
import java.util.*;

public class Boundary {
	Integer id;
	private List<Point2D.Double> vertices;

	//GETTERS/SETTERS
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public List<Point2D.Double> getVertices() {
		return vertices;
	}

	public void setVertices(List<Point2D.Double> vertices) {
		this.vertices = vertices;
	}
}
