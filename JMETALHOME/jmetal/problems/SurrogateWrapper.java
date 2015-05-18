package jmetal.problems;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;

/**
 * @author Mayr Matthias
 *
 */
public class SurrogateWrapper {

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
	
	//initial counters for method 2
	private int modelInitCounter;
	private int realInitCounter;
	
	private int computeCounter;
	
	public SurrogateWrapper(Problem problem) {
		this.problem = problem;
		maxEvaluations = 1000;
		initialize();
	}
	
	public SurrogateWrapper(Problem problem, int maxEvaluations) {
		this.problem = problem;
		this.maxEvaluations = maxEvaluations;
		numberOfEval = 1;
		initialize();
	}
	
	public SurrogateWrapper(Problem problem, int maxEvaluations, int method) {
		this.problem = problem;
		this.maxEvaluations = maxEvaluations;
		this.method = method;
		initialize();
	}
	
	private void initialize() {
		numberOfEval = 1;
		method = 1;
		realSolutions = new SolutionSet(maxEvaluations);
		surrogateOF1 = new Surrogate();
		surrogateOF2 = new Surrogate();
		
		computeCounter = 20;
		modelInitCounter = 20;
		realInitCounter = 5;
	}
	
	public void evaluate(Solution solution) throws JMException {
		switch(this.method) {
			case 1: 
				useMethod1(solution);
				break;
			case 2: 
				useMethod2(solution);
				break;
			default:
				useMethod1(solution);
		}
	}
	
	public void useMethod1(Solution solution) throws JMException {
		int numberOfInitialSolutions = 5;
		double[] fx  = new double[solution.getNumberOfObjectives()];
		double sol1, sol2;
		
		if(numberOfEval >= maxEvaluations - maxEvaluations * 0.1) {
			problem.evaluate(solution);
			realSolutions.add(solution);
		} else if(numberOfEval <= numberOfInitialSolutions) {
			problem.evaluate(solution);
			realSolutions.add(solution);
			surrogateOF1.fillTrainSet(0, solution);
			surrogateOF2.fillTrainSet(1, solution);
			if(numberOfEval == numberOfInitialSolutions) {
				System.out.println(numberOfInitialSolutions + " initial solutions are evaluated...");
			}
		} else {   
			// use model to compute new solutions  	
			sol1 = surrogateOF1.useLinearRegression(solution);
			sol2 = surrogateOF2.useLinearRegression(solution);
			solution.setObjective(0, sol1);
			solution.setObjective(1, sol2);
			computeCounter--;

			if(computeCounter == 0) {
	    		System.out.println(modelInitCounter + " model solutions are computed...");
	    		problem.evaluate(solution);
	    		realSolutions.add(solution);
	    		fx[0] = solution.getObjective(0);
	    		fx[1] = solution.getObjective(1);
	    		if(fx[0] < sol1) {
	    			System.out.println("Found better solution in objective 1 --> save to train set...");
	    			surrogateOF1.fillTrainSet(0, solution);
	    			computeCounter = modelInitCounter;
	    		} 
	    		if(fx[1] < sol2) {
	    			System.out.println("Found better solution in objective 2 --> save to train set...");
	    			surrogateOF2.fillTrainSet(1, solution);
	    			computeCounter = modelInitCounter;
	    		} else {
	    			System.out.println(modelInitCounter + " model solutions are computed...");
	    			computeCounter = modelInitCounter;
	    		}
			}
		}
		numberOfEval++;
	}
	
	public void useMethod2(Solution solution) {
		numberOfEval++;
	}
	
	public void useMethod3() {
	
		this.numberOfEval++;
	}

	public Problem getProblem() {
		return problem;
	}

	public void setProblem(Problem problem) {
		this.problem = problem;
	}
	
}