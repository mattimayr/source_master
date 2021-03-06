//  NSGAII_main.java
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

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.operators.crossover.CrossoverFactory;
import jmetal.operators.mutation.MutationFactory;
import jmetal.operators.selection.SelectionFactory;
import jmetal.problems.EBEs;
import jmetal.problems.ProblemFactory;
import jmetal.problems.SurrogateWrapper;
import jmetal.problems.SurrogateWrapperTestAP3;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.Configuration;
import jmetal.util.JMException;
import jmetal.util.Ranking;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

/** 
 * Class to configure and execute the NSGA-II algorithm.  
 *     
 * Besides the classic NSGA-II, a steady-state version (ssNSGAII) is also
 * included (See: J.J. Durillo, A.J. Nebro, F. Luna and E. Alba 
 *                  "On the Effect of the Steady-State Selection Scheme in 
 *                  Multi-Objective Genetic Algorithms"
 *                  5th International Conference, EMO 2009, pp: 183-197. 
 *                  April 2009)
 */ 

public class NSGAII_main_surrogate {
  public static Logger      logger_ ;      // Logger object
  public static FileHandler fileHandler_ ; // FileHandler object

  /**
   * @param args Command line arguments.
   * @throws JMException 
   * @throws IOException 
   * @throws SecurityException 
   * Usage: three options
   *      - jmetal.metaheuristics.nsgaII.NSGAII_main
   *      - jmetal.metaheuristics.nsgaII.NSGAII_main problemName
   *      - jmetal.metaheuristics.nsgaII.NSGAII_main problemName paretoFrontFile
   */
  public static void main(String [] args) throws 
                                  JMException, 
                                  SecurityException, 
                                  IOException, 
                                  ClassNotFoundException {
    Problem problem   ; // The problem to solve
    Algorithm algorithm ; // The algorithm to use
    Operator  crossover ; // Crossover operator
    Operator  mutation  ; // Mutation operator
    Operator  selection ; // Selection operator
    
    HashMap  parameters ; // Operator parameters
    
    QualityIndicator indicators ; // Object to get quality indicators
    int maxEvaluations = 10000;
    int populationSize = 100;
    int time = 0;
    
    // Logger object and file to store log messages
    logger_      = Configuration.logger_ ;
    fileHandler_ = new FileHandler("NSGAII_main.log"); 
    logger_.addHandler(fileHandler_) ;
        
    indicators = null ;
    if (args.length == 1) {
      Object [] params = {"Real"};
      problem = (new ProblemFactory()).getProblem(args[0],params);
    } // if
    else if (args.length == 2) {
      Object [] params = {"Real"};
      problem = (new ProblemFactory()).getProblem(args[0],params);
      indicators = new QualityIndicator(problem, args[1]) ;
    } // if
    else { // Default problem
    	problem = new EBEs("Real");
      //problem = new Kursawe("Real", 3);
      //problem = new Kursawe("BinaryReal", 3);
      //problem = new Water("Real");
      //problem = new ZDT3("Real", 30);
      //problem = new ConstrEx("Real");
      //problem = new DTLZ1("Real");
      //problem = new OKA2("Real") ;
    } // else
    SurrogateWrapper sw = new SurrogateWrapper(problem, 3, maxEvaluations, populationSize, 5, true);
    algorithm = new NSGAII(sw);
    //algorithm = new ssNSGAII(problem);

    // Algorithm parameters
    algorithm.setInputParameter("populationSize",populationSize);
    algorithm.setInputParameter("maxEvaluations",maxEvaluations);

    // Mutation and Crossover for Real codification 
    parameters = new HashMap() ;
    parameters.put("probability", 0.9) ;
    parameters.put("distributionIndex", 20.0) ;
    crossover = CrossoverFactory.getCrossoverOperator("SBXCrossover", parameters);                   

    parameters = new HashMap() ;
    parameters.put("probability", 1.0/problem.getNumberOfVariables()) ;
    parameters.put("distributionIndex", 20.0) ;
    mutation = MutationFactory.getMutationOperator("PolynomialMutation", parameters);                    

    // Selection Operator 
    parameters = null ;
    selection = SelectionFactory.getSelectionOperator("BinaryTournament2", parameters) ;                           

    // Add the operators to the algorithm
    algorithm.addOperator("crossover",crossover);
    algorithm.addOperator("mutation",mutation);
    algorithm.addOperator("selection",selection);

    // Add the indicator object to the algorithm
    algorithm.setInputParameter("indicators", indicators) ;
    
    // Execute the Algorithm
    long initTime = System.currentTimeMillis();
    SolutionSet population = algorithm.execute();
    long estimatedTime = System.currentTimeMillis() - initTime;
    
    SolutionSet realSolutions = new SolutionSet(maxEvaluations);
    realSolutions = sw.getRealSolutions();
    System.out.println("Size: " + realSolutions.size());
    SolutionSet ranked = new SolutionSet(maxEvaluations);

	Ranking rank = new Ranking(realSolutions);
	ranked = rank.getSubfront(0);
	if(time == 0)
		ranked.printObjectivesToFile(getObjectiveFileName(sw.getMethod(), maxEvaluations));
	else 
		ranked.printObjectivesToFile(getObjectiveFileNameTime(sw.getMethod(), time));

    
//    realSolutions.printObjectivesToFile("POPULATION");
// 	for(int i = 0; i < rank.getNumberOfSubfronts(); i++){
//    	rank.getSubfront(i).printObjectivesToFile("RANK" + i);
//    }
    
    
    // Result messages 
    logger_.info("Total execution time: "+estimatedTime + "ms");
    logger_.info("Variables values have been writen to file VAR");
    population.printVariablesToFile("VAR");    
    logger_.info("Objectives values have been writen to file FUN");
    population.printObjectivesToFile("FUN");
  
    if (indicators != null) {
      logger_.info("Quality indicators") ;
      logger_.info("Hypervolume: " + indicators.getHypervolume(population)) ;
      logger_.info("GD         : " + indicators.getGD(population)) ;
      logger_.info("IGD        : " + indicators.getIGD(population)) ;
      logger_.info("Spread     : " + indicators.getSpread(population)) ;
      logger_.info("Epsilon    : " + indicators.getEpsilon(population)) ;  
     
      int evaluations = ((Integer)algorithm.getOutputParameter("evaluations")).intValue();
      logger_.info("Speed      : " + evaluations + " evaluations") ;      
    } // if
    
    logger_.info("Quality indicators") ;
    indicators = new QualityIndicator(problem, "RANK0_Problem_10000");
	logger_.info("Hypervolume: " + indicators.getHypervolume(ranked));

  } //main
  
  private static String getObjectiveFileName(int method, int maxEvaluations) { 
	String fileName = "";
	switch(method) {
		case 1:
			fileName = "RANK0_SM1x_" + maxEvaluations;
			return fileName;
		case 2: 
			fileName = "RANK0_SM2_" + maxEvaluations;
			return fileName;
		case 3: 
			fileName = "RANK0_SM3TEST_" + maxEvaluations;
			return fileName;
		case 4: 
			fileName = "RANK0_SM4_" + maxEvaluations;
			return fileName;	
		default: 
			fileName = "RANK0_Problem_" + maxEvaluations;
			return fileName;
	}
  }
  
  private static String getObjectiveFileNameTime(int method, int time) { 
		String fileName = "";
		switch(method) {
			case 1:
				fileName = "RANK0_SM1x_" + time + "Min";
				return fileName;
			case 2: 
				fileName = "RANK0_SM2_" + time + "Min";
				return fileName;
			case 3: 
				fileName = "RANK0_SM3_" + time + "Min";
				return fileName;
			case 4: 
				fileName = "RANK0_SM4_" + time + "Min";
				return fileName;	
			default: 
				fileName = "RANK0_Problem_" + time + "Min";
				return fileName;
		}
	  }
} // NSGAII_main
