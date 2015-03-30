package jmetal.problems;

import java.util.HashMap;
import java.util.Map;

import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;
import weka.core.Utils;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.functions.VotedPerceptron;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;
import jmetal.util.comparators.DominanceComparator;
/**
 * @author Mayr Matthias
 *
 */
public class Surrogate {
	
	private Instance currentInstance;
	private Instances trainSet;
	private LinearRegression linearModel;
	private MultilayerPerceptron nnModel;
	
	private SolutionSet realSolutions;
	private int[][] classifier;
	
	private Map<SolutionSet, Integer> classifiedSolutions; 
	private DominanceComparator comperator;
	
	private int solutionSize;
	
	/**
	 * 
	 * @param numberOfObjectiveFunction which objective function is used
	 * @param solution the given solution
	 * @return Solution
	 */
	public double useLinearRegression(Solution solution) {	
		double sol = 0;
		if(linearModel == null) {
			linearModel = new LinearRegression();
		} else {
			try {
				//build a linear regression model on the given data
				linearModel.buildClassifier(trainSet); 
			
				//create a new instance and fill all attributes
				currentInstance = new Instance(solution.numberOfVariables());
				currentInstance.setDataset(trainSet);
				fillXAttributes(currentInstance, solution);
				
				//compute new value 
				sol = linearModel.classifyInstance(currentInstance);

			} catch (Exception e) {
				e.printStackTrace();
			}	
		}
		//add current instance to the data set
		//dataset.add(currentInstance);
		return sol;
	}
	
	public double useNeuralNetwork(Solution solution) {
		double sol = 0;
		if(nnModel == null) {
			nnModel = new MultilayerPerceptron();
		} else {
			try {
				nnModel.setAutoBuild(true);
				nnModel.setOptions(Utils.splitOptions("-L 0.5 -M 0.3 -N 100 -V 0 -S 0 -E 20 -H 4"));
				nnModel.buildClassifier(trainSet);
				
				currentInstance = new Instance(solution.numberOfVariables());
				currentInstance.setDataset(trainSet);
				fillXAttributes(currentInstance, solution);
				
				sol = nnModel.classifyInstance(currentInstance);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return sol;
	}
	
	public Solution useDominanceComperation(int numberOfObjectives) {
		Solution sol = new Solution(numberOfObjectives);
		
		
		
		return sol;
	}
	
	/**
	 * 
	 * @param solution the given solution
	 */
	public void fillTrainSet(int numberOfObjectiveFunction, Solution solution) {
		//add attributes to dataset and fill them initially
		if(trainSet == null) {
			FastVector attributes = new FastVector(solution.numberOfVariables());
			for(int i = 0; i < solution.numberOfVariables(); i++) {
				attributes.addElement(new Attribute("x["+i+"]"));
			}
			//create new attribute for the objective function and create the data set
			attributes.addElement(new Attribute("OF"));
			trainSet = new Instances("Data", attributes, 10000);
			currentInstance = new Instance(solution.numberOfVariables() + 1);
			currentInstance.setDataset(trainSet);
			//fill the currentInstance with the decision variable values
			fillXAttributes(currentInstance, solution);
			//save the value of the objective function tp the attribute and add the instance to the data set
			currentInstance.setValue(currentInstance.attribute(currentInstance.numAttributes() - 1), solution.getObjective(numberOfObjectiveFunction));
			trainSet.add(currentInstance);
			//set the class value of the data set to the objective function variable
			trainSet.setClassIndex(currentInstance.numAttributes() - 1);
		} else {
			currentInstance = new Instance(solution.numberOfVariables() + 1);
			currentInstance.setDataset(trainSet);
			fillXAttributes(currentInstance, solution);
			currentInstance.setValue(currentInstance.attribute(currentInstance.numAttributes() - 1), solution.getObjective(numberOfObjectiveFunction));
			trainSet.add(currentInstance);
			trainSet.setClassIndex(currentInstance.numAttributes() - 1);
		}
	}
	
	/**
	 * 
	 * @param currentInstance the current instance 
	 * @param solution given solution
	 */
	void fillXAttributes(Instance currentInstance, Solution solution) {
		//fill all x attributes with the given decision variable of the solution
		for(int i = 0; i < currentInstance.numAttributes() - 1; i++) {
			try {
				currentInstance.setValue(currentInstance.attribute(i), solution.getDecisionVariables()[i].getValue());
			} catch (JMException e) {
				e.printStackTrace();
			}
		}
	}
	
	public Instance getInstance() {
		return currentInstance;
	}

	public void setInstance(Instance instance) {
		this.currentInstance = instance;
	}

	public Instances getTrainSet() {
		return trainSet;
	}

	public void setTrainSet(Instances trainSet) {
		if(trainSet == null) {
			this.trainSet = new Instances(trainSet);
		}
		this.trainSet = trainSet;
	}

	public SolutionSet getRealSolutions() {
		if(realSolutions == null) {
			return null;
		} else {
		return realSolutions;
		}
	}

	public void setRealSolutions(SolutionSet realSolutions) {
		this.realSolutions = realSolutions;
	}

	public int getSolutionSize() {
		return solutionSize;
	}
	public void setSolutionSize(int solutionSize) {
		this.solutionSize = solutionSize;
	}
	public Map<SolutionSet, Integer> getClassifiedSolutions() {
		return classifiedSolutions;
	}

	public void setClassifiedSolutions(Map<SolutionSet, Integer> classifiedSolutions) {
		this.classifiedSolutions = classifiedSolutions;
	}

	public void addSolution(Solution solution) {
		if(realSolutions == null) {
			realSolutions =  new SolutionSet(10000); // TODO!!!
		}
		this.realSolutions.add(solution);
	}
	
	public void classifySolutions() {
		if(classifier == null) {
			classifier = new int[realSolutions.size()][realSolutions.size()];
		} else if(comperator == null) {
			comperator = new DominanceComparator();
		}	else {
			SolutionSet comparedSolutions = new SolutionSet(2);
			for(int i = 0; i < realSolutions.size(); i++) {
				Solution sol1 = realSolutions.get(i);
				for(int j = 0; j < realSolutions.size(); j++) {
					Solution sol2 = realSolutions.get(j);
					int flag = comperator.compare(sol1, sol2);
					System.out.println("Flag = " + flag);
					classifier[i][j] = flag;
					comparedSolutions.clear();
					comparedSolutions.add(sol1);
					comparedSolutions.add(sol2);
					classifiedSolutions.put(comparedSolutions, flag);
				}				
			}
		}
	}

}