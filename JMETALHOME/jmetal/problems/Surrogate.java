package jmetal.problems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	
	private Instances classifiedSolutions; 
	private DominanceComparator comperator;
	
	private int solutionSize;
	
	public Surrogate() {
		nnModel = new MultilayerPerceptron();
		linearModel = new LinearRegression();
	}
	
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
			//save the value of the objective function to the attribute and add the instance to the data set
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
	
	//WEKA relational attribute to create multi-instance input
		//create two instances add to instances trainset and add to a relational attribute. Last point is to create a nominal attribute with (-1,0,1)!!
		public void fillClassifyingTrainSet(int numberOfObjectiveFunction, Solution solution1, Solution solution2) {
			int flag = 0;
			comperator = new DominanceComparator();
			if(trainSet == null) {				
				flag = comperator.compare(solution1, solution2);
				
				//create x attributes for the solutions
				FastVector solutionAttributes = new FastVector(solution1.numberOfVariables());
				for(int i = 0; i < solution1.numberOfVariables(); i++) {
					solutionAttributes.addElement(new Attribute("x["+i+"]"));
				}
				//create two solutions
				Instance sol1 = new Instance(solution1.numberOfVariables());
				Instance sol2 = new Instance(solution2.numberOfVariables());
				
				//create relational attribute for the two solutions
				classifiedSolutions = new Instances("Solutions", solutionAttributes, 2);
				fillXAttributes(sol1, solution1);
				fillXAttributes(sol2, solution2);
				
				//add the two solutions to the relational attribute
				classifiedSolutions.add(sol1);
				classifiedSolutions.add(sol2);
				
				//create an vector for the two needed attributes of the classification
				FastVector instanceAttributes = new FastVector(2);
				Attribute solutions = new Attribute("Solutions", classifiedSolutions);
				instanceAttributes.addElement(solutions);
				
				//create an attribute for the dominance flag
				FastVector dominanceFlags = new FastVector(3);
				dominanceFlags.addElement(-1);
				dominanceFlags.addElement(0);
				dominanceFlags.addElement(1);
				Attribute dominanceFlag = new Attribute("DominanceFlag", dominanceFlags);
				
				//add the dominance flag to the trainset attributes
				instanceAttributes.addElement(dominanceFlag);
				
				trainSet = new Instances("Data", instanceAttributes, 10000);
				
				//set the class value of the data set to the dominance flag
				trainSet.setClassIndex(currentInstance.numAttributes() - 1);
				currentInstance = new Instance(3);
				currentInstance.setDataset(trainSet);
				
				//set the dominance flag 
				currentInstance.setValue(currentInstance.classAttribute(), flag);
				trainSet.add(currentInstance);	
				System.out.println(trainSet);
			} else {
				flag = comperator.compare(solution1, solution2);
				Instance sol1 = new Instance(solution1.numberOfVariables());
				Instance sol2 = new Instance(solution2.numberOfVariables());			
				fillXAttributes(sol1, solution1);
				fillXAttributes(sol2, solution2);
				classifiedSolutions.add(sol1);
				classifiedSolutions.add(sol2);
				
				currentInstance = new Instance(3);
				currentInstance.setDataset(trainSet);
				trainSet.setClassIndex(currentInstance.numAttributes() - 1);
				
				currentInstance.setValue(currentInstance.classAttribute(), flag);
				trainSet.add(currentInstance);
				System.out.println(trainSet);
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
		if(this.trainSet == null) {
			this.trainSet = new Instances(trainSet);
		}
		this.trainSet = trainSet;
	}

	public void emptyTrainSet() {
		this.trainSet = null;
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

	public void addRealSolution(Solution solution) {
		if(realSolutions == null) {
			realSolutions =  new SolutionSet(10000); // TODO!!!
		}
		this.realSolutions.add(solution);
	}
	
	public void emptyRealSolutions() {
		this.realSolutions = null;
	}
	
	public void emptyClassfiedSolutions() {
		for(int i = 0; i < classifiedSolutions.numInstances(); i++)
			this.classifiedSolutions.delete(i);
	}

}