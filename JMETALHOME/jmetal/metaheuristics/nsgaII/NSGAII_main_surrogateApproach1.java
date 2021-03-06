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
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.encodings.variable.Real;
import jmetal.experiments.studies.BridgeStudy;
import jmetal.operators.crossover.CrossoverFactory;
import jmetal.operators.mutation.MutationFactory;
import jmetal.operators.selection.SelectionFactory;
import jmetal.problems.EBEs;
import jmetal.problems.ProblemFactory;
import jmetal.problems.SurrogateWrapper;
import jmetal.problems.ZDT.ZDT3;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.Configuration;
import jmetal.util.Distance;
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

public class NSGAII_main_surrogateApproach1 {
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
    int populationSize = 1000;
	int time = 0;
	
	int numberOfInitialSolutions = 5;
	int modelInitCounter = 20;
	double epsilon = 0.5;
	int machineLearningMethod = 0; //0 = LR, 1 = MLP
    
	
	if(args.length > 0 && args.length < 7) {
		maxEvaluations = Integer.parseInt(args[0]);
		populationSize = Integer.parseInt(args[1]);
		numberOfInitialSolutions = Integer.parseInt(args[2]);
		modelInitCounter = Integer.parseInt(args[3]);
		epsilon = Double.parseDouble(args[4]);
		machineLearningMethod = Integer.parseInt(args[5]);
    } else {
		System.out.println("Usage: java NSGAII_main_surrogateApproach1.java maxEvaluations populationsSize numberOfInitialSolutions modelInitCounter epsilon machineLearningMethod");
	}
    
    // Logger object and file to store log messages
    logger_      = Configuration.logger_ ;
    fileHandler_ = new FileHandler("NSGAII_main.log"); 
    logger_.addHandler(fileHandler_) ;
        
    indicators = null ;
	
	problem = new EBEs("Real");
	//problem = new Kursawe("Real", 3);
	//problem = new Kursawe("BinaryReal", 3);
	//problem = new Water("Real");
	//problem = new ZDT3("ArrayReal", 30);
	//problem = new ConstrEx("Real");
	//problem = new DTLZ1("Real");
	//problem = new OKA2("Real") ;
	
    SurrogateWrapper sw = new SurrogateWrapper(problem, maxEvaluations, populationSize, numberOfInitialSolutions, modelInitCounter, epsilon, machineLearningMethod);
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
		ranked.printObjectivesToFile(getObjectiveFileName(maxEvaluations, populationSize, numberOfInitialSolutions, modelInitCounter, epsilon, machineLearningMethod, estimatedTime));
	else 
		ranked.printObjectivesToFile(getObjectiveFileNameTime(time, populationSize, numberOfInitialSolutions, modelInitCounter, epsilon, machineLearningMethod));
    
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

  } //main
  
  private static String getObjectiveFileName(int maxEvaluations, int populationSize, int numberOfInitialSolutions, int modelInitCounter, double epsilon, int machineLearningMethod, long executionTime) { 
	String fileName = "";
	if(machineLearningMethod == 0)
		fileName = "RANK0_NSGAII_SM1LR_" + maxEvaluations + "_" + populationSize + "_" + numberOfInitialSolutions + "_" + modelInitCounter + "_" + epsilon + "_" + executionTime + "ms";
	else 
		fileName = "RANK0_NSGAII_SM1N_" + maxEvaluations + "_" + populationSize + "_" + numberOfInitialSolutions + "_" + modelInitCounter + "_" + epsilon + "_" + executionTime + "ms";
	return fileName;
  }
  
  private static String getObjectiveFileNameTime(int time, int populationSize, int numberOfInitialSolutions, int modelInitCounter, double epsilon, int machineLearningMethod) { 
		String fileName = "";
		if(machineLearningMethod == 0)
		fileName = "RANK0_NSGAII_SM1LR_" + time + "Min_" + populationSize + "_" + numberOfInitialSolutions + "_" + modelInitCounter + "_" + epsilon;
	else 
		fileName = "RANK0_NSGAII_SM1N_" + time + "Min_" + populationSize + "_" + numberOfInitialSolutions + "_" + modelInitCounter + "_" + epsilon;
	return fileName;
  }
} // NSGAII_main