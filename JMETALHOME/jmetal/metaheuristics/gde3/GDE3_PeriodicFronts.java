//  GDE3.java
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

package jmetal.metaheuristics.gde3;

import jmetal.core.*;
import jmetal.util.Distance;
import jmetal.util.JMException;
import jmetal.util.Ranking;
import jmetal.util.comparators.CrowdingComparator;
import jmetal.util.comparators.DominanceComparator;

import java.util.Comparator;

/**
 * This class implements the GDE3 algorithm. 
 */
public class GDE3_PeriodicFronts extends Algorithm {
    
  /**
  * Constructor
  * @param problem Problem to solve
  */
  public GDE3_PeriodicFronts(Problem problem){
    super (problem) ;
  } // GDE3
  
  /**   
  * Runs of the GDE3 algorithm.
  * @return a <code>SolutionSet</code> that is a set of non dominated solutions
  * as a result of the algorithm execution  
   * @throws JMException 
  */  
  public SolutionSet execute() throws JMException, ClassNotFoundException {
    int populationSize ;
    int maxIterations  ;
    int evaluations    ;
    int iterations     ;
    
    //parameters for measuring the elapsed time
    long initTime;
	long currentTime;
	long elapsedTime = 0;
    
    SolutionSet population          ;
    SolutionSet offspringPopulation ;
    SolutionSet union               ;
    
    Distance   distance  ;
    Comparator dominance ;
    
    Operator selectionOperator ;
    Operator crossoverOperator ;
    
    distance  = new Distance()  ;               
    dominance = new DominanceComparator(); 
    
    Solution parent[] ;
    
    //Read the parameters
    populationSize = ((Integer)this.getInputParameter("populationSize")).intValue();
    maxIterations  = ((Integer)this.getInputParameter("maxIterations")).intValue();                             
   
    selectionOperator = operators_.get("selection");   
    crossoverOperator = operators_.get("crossover") ;
    
    //Initialize the variables
    population  = new SolutionSet(populationSize);  
    SolutionSet ranked = new SolutionSet(population.size());
    evaluations = 0;                
    iterations  = 0 ;

    // Create the initial solutionSet
    Solution newSolution;
    
    //Start measuring the time
  	initTime = System.currentTimeMillis();
  	
    for (int i = 0; i < populationSize; i++) {
      newSolution = new Solution(problem_);                    
      problem_.evaluate(newSolution);            
      problem_.evaluateConstraints(newSolution);
      evaluations++;
      population.add(newSolution);
      if(evaluations%100 == 0) {
  		currentTime = System.currentTimeMillis();  
  		elapsedTime = currentTime - initTime;
  		  
  		Ranking rank = new Ranking(population);		
  		ranked = rank.getSubfront(0);
  		ranked.printObjectivesToFile("GDE3_Problem_" + populationSize + "_" + maxIterations + "_" + evaluations + "_" + elapsedTime + "ms");
  	  }
    } //for       
  
    // Generations ...
    while (evaluations < maxIterations) {
      // Create the offSpring solutionSet      
      offspringPopulation  = new SolutionSet(populationSize * 2);        

      for (int i = 0; i < populationSize; i++){   
        // Obtain parents. Two parameters are required: the population and the 
        //                 index of the current individual
        parent = (Solution [])selectionOperator.execute(new Object[]{population, i});

        Solution child ;
        // Crossover. Two parameters are required: the current individual and the 
        //            array of parents
        child = (Solution)crossoverOperator.execute(new Object[]{population.get(i), parent}) ;

        problem_.evaluate(child) ;
        problem_.evaluateConstraints(child);
        evaluations++ ;
        
        // Dominance test
        int result  ;
        result = dominance.compare(population.get(i), child) ;
        if (result == -1) { // Solution i dominates child
          offspringPopulation.add(population.get(i)) ;
        } // if
        else if (result == 1) { // child dominates
          offspringPopulation.add(child) ;
        } // else if
        else { // the two solutions are non-dominated
          offspringPopulation.add(child) ;
          offspringPopulation.add(population.get(i)) ;
        } // else
        if(evaluations%100 == 0) {
    		currentTime = System.currentTimeMillis();  
    		elapsedTime = currentTime - initTime;
    		  
    		Ranking rank = new Ranking(offspringPopulation);		
    		ranked = rank.getSubfront(0);
    		ranked.printObjectivesToFile("GDE3_Problem_" + populationSize + "_" + maxIterations + "_" + evaluations + "_" + elapsedTime + "ms");
    	  }
      } // for 
      System.out.println("GDE: " + iterations);

      // Ranking the offspring population
      Ranking ranking = new Ranking(offspringPopulation);                        

      int remain = populationSize;
      int index  = 0;
      SolutionSet front = null;
      population.clear();

      // Obtain the next front
      front = ranking.getSubfront(index);

      while ((remain > 0) && (remain >= front.size())){                
        //Assign crowding distance to individuals
        distance.crowdingDistanceAssignment(front,problem_.getNumberOfObjectives());                
        //Add the individuals of this front
        for (int k = 0; k < front.size(); k++ ) {
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
      
      // remain is less than front(index).size, insert only the best one
      if (remain > 0) {  // front contains individuals to insert                        
        while (front.size() > remain) {
           distance.crowdingDistanceAssignment(front,problem_.getNumberOfObjectives());
           front.remove(front.indexWorst(new CrowdingComparator()));
        }
        for (int k = 0; k < front.size(); k++) {
          population.add(front.get(k));
        }
        
        remain = 0; 
      } // if                   
      if (evaluations%100==0) {                  
          Ranking ranking2 = new Ranking(population);
          ranking2.getSubfront(0).printFeasibleFUN("FUN_GDE3_"+evaluations) ;
          }
      iterations ++ ;
    } // while
    
    // Return the first non-dominated front
    Ranking ranking = new Ranking(population);
    ranking.getSubfront(0).printFeasibleFUN("FUN_GDE3") ;
    return ranking.getSubfront(0);
  } // execute
} // GDE3
