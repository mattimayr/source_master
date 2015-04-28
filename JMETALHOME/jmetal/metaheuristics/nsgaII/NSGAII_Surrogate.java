//  NSGAII.java
//
//  Author:
//       Antonio J. Nebro <antonio@lcc.uma.es>
//       Juan J. Durillo <durillo@lcc.uma.es>
//
//  Copyright (c) 2011 Antonio J. Nebro, Juan J. Durillo
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jmetal.metaheuristics.nsgaII;

import jmetal.core.*;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.Distance;
import jmetal.util.JMException;
import jmetal.util.Ranking;
import jmetal.util.comparators.CrowdingComparator;

/** 
 *  Implementation of NSGA-II.
 *  This implementation of NSGA-II makes use of a QualityIndicator object
 *  to obtained the convergence speed of the algorithm. This version is used
 *  in the paper:
 *     A.J. Nebro, J.J. Durillo, C.A. Coello Coello, F. Luna, E. Alba 
 *     "A Study of Convergence Speed in Multi-Objective Metaheuristics." 
 *     To be presented in: PPSN'08. Dortmund. September 2008.
 */

public class NSGAII_Surrogate extends Algorithm {
  /**
   * Constructor
   * @param problem Problem to solve
   */
  public NSGAII_Surrogate(Problem problem) {
    super (problem) ;
  } // NSGAII

  /**   
   * Runs the NSGA-II algorithm.
   * @return a <code>SolutionSet</code> that is a set of non dominated solutions
   * as a result of the algorithm execution
   * @throws JMException 
   */
  public SolutionSet execute() throws JMException, ClassNotFoundException {
    int populationSize;
    int maxEvaluations;
    int modelEvaluations;
    int initialModelEvaluations;
    int evaluations;
    double solutionRange;

    QualityIndicator indicators; // QualityIndicator object
    int requiredEvaluations; // Use in the example of use of the
    // indicators object (see below)

    SolutionSet population;
    SolutionSet offspringPopulation;
    SolutionSet union;
    SolutionSet parentPopulation;
    
    Solution parentSolution1;

    Operator mutationOperator;
    Operator crossoverOperator;
    Operator selectionOperator;

    Distance distance = new Distance();

    //Read the parameters
    populationSize = ((Integer) getInputParameter("populationSize")).intValue();
    maxEvaluations = ((Integer) getInputParameter("maxEvaluations")).intValue();
    indicators = (QualityIndicator) getInputParameter("indicators");

    //Initialize the variables
    population = new SolutionSet(populationSize);
    parentPopulation = new SolutionSet(populationSize);
    
    parentSolution1 = new Solution();
    
    evaluations = 0;
    initialModelEvaluations = 5;
    modelEvaluations = initialModelEvaluations;
    solutionRange = 0.3;

    requiredEvaluations = 0;

    //Read the operators
    mutationOperator = operators_.get("mutation");
    crossoverOperator = operators_.get("crossover");
    selectionOperator = operators_.get("selection");

    // Create the initial solutionSet
    Solution newSolution;
    for (int i = 0; i < populationSize; i++) {
      newSolution = new Solution(problem_);
      problem_.evaluate(newSolution);
      problem_.evaluateConstraints(newSolution);
      evaluations++;
      population.add(newSolution);
    } //for   
    
    population.printObjectivesToFile("POPULATION");
    
    parentSolution1 = new Ranking(population).getSubfront(0).get(0); 
    parentPopulation = getParentPopulation(solutionRange, population, parentSolution1);
    while(parentPopulation.size() < 2) {
    	parentSolution1 = (Solution) selectionOperator.execute(population);
  		parentPopulation = getParentPopulation(solutionRange, population, parentSolution1);
    }
    //parentPopulation.printObjectivesToFile("PARENTS1");
    
    // Generations 
    while (evaluations < maxEvaluations) {

      // Create the offSpring solutionSet      
      offspringPopulation = new SolutionSet(populationSize);
      Solution[] parents = new Solution[2];
      for (int i = 0; i < (populationSize / 2); i++) {
        if (evaluations < maxEvaluations) {
          //obtain parents
        	if(modelEvaluations > 0) {
        		parents[0] = parentSolution1;
        		parents[1] = (Solution) selectionOperator.execute(parentPopulation);
        		modelEvaluations--;
//          parents[0] = (Solution) selectionOperator.execute(population);
//          parents[1] = (Solution) selectionOperator.execute(population);
	          Solution[] offSpring = (Solution[]) crossoverOperator.execute(parents);
	          mutationOperator.execute(offSpring[0]);
	          mutationOperator.execute(offSpring[1]);
	          problem_.evaluate(offSpring[0]);
	          problem_.evaluateConstraints(offSpring[0]);
	          problem_.evaluate(offSpring[1]);
	          problem_.evaluateConstraints(offSpring[1]);
	          offspringPopulation.add(offSpring[0]);
	          offspringPopulation.add(offSpring[1]);
	          evaluations += 2;
        	} else {
        		parentSolution1 = (Solution) selectionOperator.execute(population);
        		parentPopulation = getParentPopulation(solutionRange, population, parentSolution1);
        		while(parentPopulation.size() < 2) {
        			parentSolution1 = (Solution) selectionOperator.execute(population);
        			parentPopulation = getParentPopulation(solutionRange, population, parentSolution1);
        		}
        		//parentPopulation.printObjectivesToFile("PARENTS" + evaluations);
        		parents[0] = parentSolution1;
        		parents[1] = (Solution) selectionOperator.execute(parentPopulation);
        		Solution[] offSpring = (Solution[]) crossoverOperator.execute(parents);
	          mutationOperator.execute(offSpring[0]);
	          mutationOperator.execute(offSpring[1]);
	          problem_.evaluate(offSpring[0]);
	          problem_.evaluateConstraints(offSpring[0]);
	          problem_.evaluate(offSpring[1]);
	          problem_.evaluateConstraints(offSpring[1]);
	          offspringPopulation.add(offSpring[0]);
	          offspringPopulation.add(offSpring[1]);
	          evaluations += 2;
						modelEvaluations = initialModelEvaluations;
        	}
        } // if                            
      } // for

      // Create the solutionSet union of solutionSet and offSpring
      union = ((SolutionSet) population).union(offspringPopulation);

      // Ranking the union
      Ranking ranking = new Ranking(union);

      int remain = populationSize;
      int index = 0;
      SolutionSet front = null;
      population.clear();

      // Obtain the next front
      front = ranking.getSubfront(index);

      while ((remain > 0) && (remain >= front.size())) {
        //Assign crowding distance to individuals
        distance.crowdingDistanceAssignment(front, problem_.getNumberOfObjectives());
        //Add the individuals of this front
        for (int k = 0; k < front.size(); k++) {
          population.add(front.get(k));
        } // for

        //Decrement remain
        remain = remain - front.size();

        //Obtain the next front
        index++;
        if (remain > 0) {
          front = ranking.getSubfront(index);
        } // if        
      } // while

      // Remain is less than front(index).size, insert only the best one
      if (remain > 0) {  // front contains individuals to insert                        
        distance.crowdingDistanceAssignment(front, problem_.getNumberOfObjectives());
        front.sort(new CrowdingComparator());
        for (int k = 0; k < remain; k++) {
          population.add(front.get(k));
        } // for

        remain = 0;
      } // if                               

      // This piece of code prints the feasible solutions of the algorithm every
      // 100 evaluations. It is meant to do convergence studies
      if (evaluations%100==0) {
            Ranking ranking2 = new Ranking(population);
            ranking2.getSubfront(0).printFeasibleFUN("FUN_NSGAII_"+evaluations) ;
      }            
      // This piece of code shows how to use the indicator object into the code
      // of NSGA-II. In particular, it finds the number of evaluations required
      // by the algorithm to obtain a Pareto front with a hypervolume higher
      // than the hypervolume of the true Pareto front.
      if ((indicators != null) &&
          (requiredEvaluations == 0)) {
        double HV = indicators.getHypervolume(population);
        if (HV >= (0.98 * indicators.getTrueParetoFrontHypervolume())) {
          requiredEvaluations = evaluations;
        } // if
      } // if
    } // while

    // Return as output parameter the required evaluations
    setOutputParameter("evaluations", requiredEvaluations);

    // Return the first non-dominated front
    Ranking ranking = new Ranking(population);
    ranking.getSubfront(0).printFeasibleFUN("FUN_NSGAII") ;
    
    population.printObjectivesToFile("FINALPARENTS");

    return ranking.getSubfront(0);
  } // execute

	private SolutionSet getParentPopulation(double solutionRange, SolutionSet population, Solution parentSolution1) {
		SolutionSet parentPopulation = new SolutionSet(population.size());
		double mostPromitingObjective1 = parentSolution1.getObjective(0);
    double mostPromitingObjective2 = parentSolution1.getObjective(1);
    
    
    double objective1 = 0;
    double objective2 = 0;
    
    for(int i = 0; i < population.size(); i++) {
    	objective1 = population.get(i).getObjective(0);
    	objective2 = population.get(i).getObjective(1);
    	if(objective1 > (mostPromitingObjective1 - solutionRange) && objective1 < (mostPromitingObjective1 + solutionRange)) {
    		if(objective2 > (mostPromitingObjective2 - solutionRange) && objective2 < (mostPromitingObjective2 + solutionRange)) {
    			parentPopulation.add(population.get(i));
    		}
    	}
    }
    return parentPopulation;
	}
} // NSGA-II
