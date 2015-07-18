package jmetal.problems;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;
import jmetal.util.comparators.DominanceComparator;

/**
 * @author Mayr Matthias
 *
 */
public class SurrogateWrapper2 extends Problem {
	
	// the real Problem and a SolutionSet for saving solutions of the real problem
	private Problem problem;
	private SolutionSet realSolutions;
	
	//a surrogate for every objective
	private Surrogate surrogateOF1;
	private Surrogate surrogateOF2;
	
	//max evaluations, the current number of evaluation and the used method
	private int maxEvaluations;
	private int numberOfEval;
	private int method;
	private int populationSize;
	
	//components for method 1
	private int computeCounter;
	private int percentOfSolutionComparisms;
	private double epsilon;
	private SolutionSet roundSolutions;
	
	//components for method 2
	private int modelInitCounter;
	private int realInitCounter;
	private int modelCounter;
	private int realCounter;
	boolean useOF1Linear;
	boolean useOF2Linear;
	boolean useOF1Neural;
	boolean useOF2Neural;
	
	//components for method 3
	private SolutionSet offSprings;
	private DominanceComparator comparator;
	
	//components for method 4
	private int trainSetSize;
	private SolutionSet solutionsToCompare;
	
	//constructors
	public SurrogateWrapper2(Problem problem, int populationSize) {
		this.problem = problem;
		this.populationSize = populationSize;
		this.method = 1;
		maxEvaluations = 1000;
		initialize();
	}
	
	public SurrogateWrapper2(Problem problem, int maxEvaluations, int populationSize) {
		this.problem = problem;
		this.populationSize = populationSize;
		this.maxEvaluations = maxEvaluations;
		this.method = 1;
		initialize();
	}
	
	public SurrogateWrapper2(Problem problem, int maxEvaluations, int method, int populationSize) {
		this.problem = problem;
		this.populationSize = populationSize;
		this.maxEvaluations = maxEvaluations;
		this.method = method;
		initialize();
	}
	
	//initialize method to initialize the needed components
	private void initialize() {
		//delegate problem variables
		solutionType_ = problem.getSolutionType();
		numberOfConstraints_ = problem.getNumberOfConstraints();
		numberOfObjectives_ = problem.getNumberOfObjectives();
		numberOfVariables_ = problem.getNumberOfVariables();
		problemName_ = "SurrogateWrapper";
		
		numberOfEval_ = 0;

		realSolutions = new SolutionSet(maxEvaluations);
		surrogateOF1 = new Surrogate();
		surrogateOF2 = new Surrogate();
		
		//initialize the needed components depending on the used method
		switch(method) {
			case 1:
				modelInitCounter = 20;
				computeCounter = modelInitCounter;
				percentOfSolutionComparisms = 10; //if 0 the last solution will be taken, if 1 the best will be taken
				epsilon = 0.5;
				roundSolutions = new SolutionSet(computeCounter);
				break;
			case 2:
				modelInitCounter = 20;
				realInitCounter = 5;
				realCounter = realInitCounter;
				modelCounter = modelInitCounter;
				useOF1Linear = false;
				useOF2Linear = false;
				useOF1Neural = false;
				useOF2Neural = false;
				break;
			case 3:
				comparator = new DominanceComparator();
				trainSetSize = 20;
				offSprings = new SolutionSet(2);
				break;
			case 4:
				classifyingSurrogate = new Surrogate();
				trainSetSize = 10;
				solutionsToCompare = new SolutionSet(2);
				break;
		}			
	}
	
	//choose the used method
	public void evaluate(Solution solution) throws JMException {
		switch(method) {
			case 1: 
				useMethod1(solution);
				break;
			case 2: 
				useMethod2(solution);
				break;
			case 3:
				useMethod3(solution);
				break;
			case 4:
				useMethod4(solution);
				break;
			default:
				useMethod1(solution);
				break;
		}
	}
	
	public void useMethod1(Solution solution) throws JMException {
		int numberOfInitialSolutions = 5;
		double[] fx  = new double[solution.getNumberOfObjectives()];
		double sol1, sol2;
		
		//evaluating the last 10% with the real problem
		if(numberOfEval_ >= maxEvaluations - maxEvaluations * 0.1) {
			problem.evaluate(solution);
			realSolutions.add(solution);
		} else if(numberOfEval_ <= numberOfInitialSolutions) { //create the initial training set of the surrogates
			problem.evaluate(solution);
			realSolutions.add(solution);
			surrogateOF1.fillTrainSet(0, solution);
			surrogateOF2.fillTrainSet(1, solution);
			if(numberOfEval_ == numberOfInitialSolutions) {
				System.out.println(numberOfInitialSolutions + " initial solutions are evaluated...");
			}
		} else {   
			// use model to compute new solutions  	
			sol1 = surrogateOF1.useNeuralNetwork(solution);
			sol2 = surrogateOF2.useNeuralNetwork(solution);
			solution.setObjective(0, sol1);
			solution.setObjective(1, sol2);
			roundSolutions.add(solution);
			computeCounter--;
			
			//if one round of model solution evaluation is done do the error correction
			if(computeCounter == 0) {
	    		System.out.println(modelInitCounter + " model solutions are computed...");
				//check a given percentage of the generated model solutions against the real solution
				if(percentOfSolutionComparisms != 0 && percentOfSolutionComparisms != 1) {
					System.out.println("Comparing now " + percentOfSolutionComparisms + "% of the last generated solutions...");
					for(int i = 1; i <= roundSolutions.size(); i++) {
						if((i-1) % (100/percentOfSolutionComparisms) == 0) {
							solution = roundSolutions.get(i-1);
							sol1 = solution.getObjective(0);
							sol2 = solution.getObjective(1);
							problem.evaluate(solution);
							fx[0] = solution.getObjective(0);
							fx[1] = solution.getObjective(1);
							realSolutions.add(solution);
							if(Math.abs(fx[0] - sol1) > epsilon) {
								System.out.println("Found better solution in objective 1 --> save to train set...");
								surrogateOF1.fillTrainSet(0, solution);							
							} 
							if(Math.abs(fx[1] - sol2) > epsilon) {
								System.out.println("Found better solution in objective 2 --> save to train set...");
								surrogateOF2.fillTrainSet(1, solution);
							}
						}
					}
					System.out.println("Comparism is done...");
					computeCounter = modelInitCounter;
				} else { 
					switch(percentOfSolutionComparisms) {
						case 0: //evaluate the last solution
							problem.evaluate(solution);
							realSolutions.add(solution);
							fx[0] = solution.getObjective(0);
							fx[1] = solution.getObjective(1);
							break;
						case 1:	//evaluate the best solution within the last round
							solution = roundSolutions.best(comparator);
							realSolutions.add(solution);
							fx[0] = solution.getObjective(0);
							fx[1] = solution.getObjective(1);
							sol1 = surrogateOF1.useLinearRegression(solution);
							sol2 = surrogateOF2.useLinearRegression(solution);
							break;
					}
					if(Math.abs(fx[0] - sol1) > epsilon) {
						System.out.println("Found better solution in objective 1 --> save to train set...");
						surrogateOF1.fillTrainSet(0, solution);
						computeCounter = modelInitCounter;
					} 
					if(Math.abs(fx[1] - sol2) > epsilon) {
						System.out.println("Found better solution in objective 2 --> save to train set...");
						surrogateOF2.fillTrainSet(1, solution);
						computeCounter = modelInitCounter;
					} else {
						System.out.println(modelInitCounter + " model solutions are computed...");
						computeCounter = modelInitCounter;
					}
				}
				roundSolutions.clear();
			}
		}
		numberOfEval_++;
	}
	
	public void useMethod2(Solution solution) throws JMException {
		double sol1, sol2;
		
		if(numberOfEval_ >= maxEvaluations - maxEvaluations*0.1) {
	    	problem.evaluate(solution);
	    	realSolutions.add(solution);
	    } else if(realCounter > 0) {
	    	problem.evaluate(solution);
	    	realSolutions.add(solution);
	    	surrogateOF1.fillTrainSet(0, solution);
	    	surrogateOF2.fillTrainSet(1, solution);
	    	if(realCounter % 4 == 0) {
	    		surrogateOF1.addRealSolution(solution);
	    		surrogateOF2.addRealSolution(solution);
	    	}
	    	realCounter--;
	    	useOF1Linear = false;
	    	useOF1Neural = false;
	    	useOF2Linear = false;
	    	useOF2Neural = false;
	    }
	    else {
	    	if(useOF1Linear == false && useOF2Neural == false && useOF2Linear == false && useOF2Neural == false) {
	    		System.out.println(realInitCounter + " solutions are computed...");
	    		System.out.println("Starting to compute the mean squared errors...");
	    		modelCounter = modelInitCounter;
		    	double errorObjective1Neural = 0;
		    	double errorObjective2Neural = 0;
		    	double errorObjective1Linear = 0;
		    	double errorObjective2Linear = 0;
		    	for(int i = 0; i < surrogateOF1.getRealSolutions().size(); i++) {
		    		Solution real1 = surrogateOF1.getRealSolutions().get(i);
		    		Solution real2 = surrogateOF2.getRealSolutions().get(i);
		    		sol1 = surrogateOF1.useLinearRegression(real1);
		    		sol2 = surrogateOF2.useLinearRegression(real2);
		    		errorObjective1Linear += Math.pow(real1.getObjective(0) - sol1, 2);
		    		errorObjective2Linear += Math.pow(real1.getObjective(1) - sol2, 2);
		    		sol1 = surrogateOF1.useNeuralNetwork(real1);
		    		sol2 = surrogateOF2.useNeuralNetwork(real2);
		    		errorObjective1Neural += Math.pow(real1.getObjective(0) - sol1, 2);
		    		errorObjective2Neural += Math.pow(real1.getObjective(1) - sol2, 2);
		    	}	    	
		    	if(errorObjective1Linear < errorObjective1Neural) {
		    		useOF1Linear = true;
		    		System.out.println("Linear Regression got better objective 1 error -> use Linear Regression for objective 1...");
		    	} else {
		    		useOF1Neural = true;
		    		System.out.println("Neural network got better objective 1 error -> use Neural network for objective 1...");
		    	}
		    	if(errorObjective2Linear < errorObjective2Neural) {
		    		useOF2Linear = true;
		    		System.out.println("Linear Regression got better objective 2 error -> use Linear Regression for objective 2...");
		    	} else {
		    		useOF2Neural = true;
		    		System.out.println("Neural network got better objective 2 error -> use Neural network for objective 2...");
		    	}
	    	} else {
		    		if(modelCounter > 0) {
			    		if(useOF1Linear) {
			    			sol1 = surrogateOF1.useLinearRegression(solution);
			    			if(useOF2Linear) {
			    				sol2 = surrogateOF2.useLinearRegression(solution);
			    			}
			    			else {
			    				sol2 = surrogateOF2.useNeuralNetwork(solution);
			    			}
				        	solution.setObjective(0, sol1);
				        	solution.setObjective(1, sol2);
				        	modelCounter--;
				        	
			    		} else {
			    			sol1 = surrogateOF1.useNeuralNetwork(solution);
			    			if(useOF2Linear) {
			    				sol2 = surrogateOF2.useLinearRegression(solution);
			    			} else {
			    				sol2 = surrogateOF2.useNeuralNetwork(solution);
			    			}			        	
				        	solution.setObjective(0, sol1);
				        	solution.setObjective(1, sol2);
				        	modelCounter--;
			    		}
		    	} else {
		    		realCounter = realInitCounter;
		    		System.out.println(modelInitCounter + " model evaluations done...");
		    		surrogateOF1.emptyTrainSet();
		    		surrogateOF2.emptyTrainSet();
		    		surrogateOF1.emptyRealSolutions();
		    		surrogateOF2.emptyRealSolutions();
		    	}
	    	}
	    }
		numberOfEval_++;
	}
	
	public void useMethod3(Solution solution) throws JMException {
		int dominanceFlag;
		if(numberOfEval == populationSize/2)
			System.out.println("50% of the initial population has been finished!");
		if(numberOfEval_ < populationSize) {
			problem.evaluate(solution);
			realSolutions.add(solution);
			//save 10% to the train set
			if((numberOfEval_%10) == 0) {
				System.out.println("Filling into trainSet...");
				surrogateOF1.fillTrainSet(0, solution);
				surrogateOF2.fillTrainSet(1, solution);
			}
		} else {
			if(offSprings.size() < 1) {
				evaluateAndSetWithTheModel(solution);
				offSprings.add(solution);
			} else {
				evaluateAndSetWithTheModel(solution);
				offSprings.add(solution);
				
				dominanceFlag = comparator.compare(offSprings.get(0), offSprings.get(1));
//				if(dominanceFlag == 0) {
//					System.out.println("Dominance = 0");
//					problem.evaluate(offSprings.get(0));
//					problem.evaluate(offSprings.get(1));
//					realSolutions.add(offSprings.get(0));
//					realSolutions.add(offSprings.get(1));
//				}
				switch(dominanceFlag) {
					case -1:
						problem.evaluate(offSprings.get(0));
						realSolutions.add(offSprings.get(0));
						System.out.println("Dominance = -1");
						break;
					case 1:
						problem.evaluate(offSprings.get(1));
						realSolutions.add(offSprings.get(1));
						System.out.println("Dominance = 1");
						break;
				}		
				offSprings = new SolutionSet(2);
			}		
		}
		numberOfEval_++;
	}

	public void useMethod4(Solution solution) throws JMException {
		if(numberOfEval < trainSetSize) {
			if(numberOfEval == 0)
				System.out.println("Filling TrainSet for the classification...");
			if(solutionsToCompare.size() < 1) {
				problem.evaluate(solution);
				realSolutions.add(solution);
				solutionsToCompare.add(solution);
			} else {
				problem.evaluate(solution);
				realSolutions.add(solution);
				solutionsToCompare.add(solution);
				classifyingSurrogate.fillClassifyingTrainSet(solutionsToCompare.get(0), solutionsToCompare.get(1));
				solutionsToCompare = new SolutionSet(2);
			}
		} else {
			if(solutionsToCompare.size() < 1) {
				solutionsToCompare.add(solution);
				realSolutions.add(solution);
			}
			else {
				solutionsToCompare.add(solution);
				realSolutions.add(solution);
				classifyingSurrogate.useClassifier(solutionsToCompare.get(0), solutionsToCompare.get(1));
				solutionsToCompare = new SolutionSet(2);
			}
				
		}		
	}
	
	public Problem getProblem() {
		return problem;
	}

	public void setProblem(Problem problem) {
		this.problem = problem;
	}
	
	public SolutionSet getRealSolutions() {
		return realSolutions;
	}

	public void setRealSolutions(SolutionSet realSolutions) {
		this.realSolutions = realSolutions;
	}
	
	public int getMethod() {
		return method;
	}

	public void setMethod(int method) {
		this.method = method;
	}

	public void evaluateAndSetWithTheModel(Solution solution) {
		double sol1, sol2;
		
		sol1 = surrogateOF1.useNeuralNetwork(solution);
		sol2 = surrogateOF2.useNeuralNetwork(solution);
		solution.setObjective(0, sol1);
		solution.setObjective(1, sol2);
	}
	
}