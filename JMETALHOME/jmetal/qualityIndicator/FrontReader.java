package jmetal.qualityIndicator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;

/**
 * @author Mayr Matthias
 * This class reads a front and saves it to a SolutionSet.
 */ 
 public class FrontReader {
	private String filePath;
	private int numberOfSolutions;
	
	private Solution solution;
	private SolutionSet front;
	
	private Problem problem;
	
	public FrontReader(Problem problem, String filePath) throws ClassNotFoundException, IOException {
		this.problem = problem;
		this.filePath = filePath;
		numberOfSolutions = getAmountOfSolutions();
		solution = new Solution(problem);
	}
	
	public SolutionSet readFile() throws NumberFormatException, IOException { 
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		front = new SolutionSet(numberOfSolutions);
		
		String line;
		while ((line = reader.readLine()) != null) {
			String[] objectiveFunctions = line.split("\\s+");
			for(int i = 0; i < objectiveFunctions.length; i++) {
				solution.setObjective(i, Double.parseDouble(objectiveFunctions[i]));
			}
			front.add(solution);
		}
		reader.close();
		return front;
	}	

	private int getAmountOfSolutions() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		int n = 0;
		while (reader.readLine() != null) {
			n++;
		}
		reader.close();
		return n;
	}
	
	public int getNumberOfSolutions() {
		return numberOfSolutions;
	}
	
	public void setProblem(Problem problem) {
		this.problem = problem;
	}
}
 