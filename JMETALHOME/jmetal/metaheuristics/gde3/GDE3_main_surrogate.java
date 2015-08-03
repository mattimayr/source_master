//  GDE3_main.java
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

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.operators.crossover.CrossoverFactory;
import jmetal.operators.selection.SelectionFactory;
import jmetal.problems.EBEs;
import jmetal.problems.Kursawe;
import jmetal.problems.ProblemFactory;
import jmetal.problems.SurrogateWrapper2;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.Configuration;
import jmetal.util.JMException;
import jmetal.util.Ranking;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

/**
 * Class for configuring and running the GDE3 algorithm
 */
public class GDE3_main_surrogate {
  public static Logger      logger_ ;      // Logger object
  public static FileHandler fileHandler_ ; // FileHandler object

  /**
   * @param args Command line arguments.
   * @throws JMException 
   * @throws IOException 
   * @throws SecurityException 
   * Usage: three choices
   *      - jmetal.metaheuristics.nsgaII.NSGAII_main
   *      - jmetal.metaheuristics.nsgaII.NSGAII_main problemName
   *      - jmetal.metaheuristics.nsgaII.NSGAII_main problemName paretoFrontFile
   */
  public static void main(String [] args) throws JMException, SecurityException, IOException, ClassNotFoundException {
    Problem   problem   ;         // The problem to solve
    Algorithm algorithm ;         // The algorithm to use
    Operator  selection ;
    Operator  crossover ;
    
    HashMap  parameters ; // Operator parameters
    
    QualityIndicator indicators ; // Object to get quality indicators
	int maxEvaluations = 10000;
    int populationSize = 1000;
    int time = 0;

    // Logger object and file to store log messages
    logger_      = Configuration.logger_ ;
    fileHandler_ = new FileHandler("GDE3_main.log"); 
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
      //problem = new Water("Real");
      //problem = new ZDT1("ArrayReal", 100);
      //problem = new ConstrEx("Real");
      //problem = new DTLZ1("Real");
      //problem = new OKA2("Real") ;
    } // else
    
	SurrogateWrapper2 sw = new SurrogateWrapper2(problem, maxEvaluations, 3, populationSize);
    algorithm = new GDE3(sw);
    
    // Algorithm parameters
    algorithm.setInputParameter("populationSize", populationSize);
    algorithm.setInputParameter("maxIterations", maxEvaluations);
    
    // Crossover operator 
    parameters = new HashMap() ;
    parameters.put("CR", 0.5) ;
    parameters.put("F", 0.5) ;
    crossover = CrossoverFactory.getCrossoverOperator("DifferentialEvolutionCrossover", parameters);                   
    
    // Add the operators to the algorithm
    parameters = null ;
    selection = SelectionFactory.getSelectionOperator("DifferentialEvolutionSelection", parameters) ;

    algorithm.addOperator("crossover",crossover);
    algorithm.addOperator("selection",selection);
    
    // Execute the Algorithm 
    long initTime = System.currentTimeMillis();
    SolutionSet population = algorithm.execute();
    long estimatedTime = System.currentTimeMillis() - initTime;
    
	SolutionSet realSolutions = new SolutionSet(maxEvaluations);
    realSolutions = sw.getRealSolutions();
    System.out.println("Size: " + realSolutions.size());
    SolutionSet ranked = new SolutionSet(maxEvaluations);
	if(sw.getMethod() != 4) {
		Ranking rank = new Ranking(realSolutions);
		ranked = rank.getSubfront(0);
		if(time == 0)
			ranked.printObjectivesToFile(getObjectiveFileName(sw.getMethod(), maxEvaluations));
		else 
			ranked.printObjectivesToFile(getObjectiveFileNameTime(sw.getMethod(), time));
	} else {
		System.out.println("Size: " + population.size());
		System.out.println("Evaluating the solutions...");
		for(int i = 0; i < population.size(); i++) {
			problem.evaluate(population.get(i));
		}
		Ranking rank = new Ranking(population);
		ranked = rank.getSubfront(0);
		System.out.println("Size: " + ranked.size());
		if(time == 0)
			ranked.printObjectivesToFile(getObjectiveFileName(sw.getMethod(), maxEvaluations));
		else 
			ranked.printObjectivesToFile(getObjectiveFileNameTime(sw.getMethod(), time));
	}
    
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
    } // if        
	
	logger_.info("Quality indicators") ;
    indicators = new QualityIndicator(problem, "RANK0_GDE3_Problem_10000");
	logger_.info("Hypervolume: " + indicators.getHypervolume(ranked));
  }//main
  
  private static String getObjectiveFileName(int method, int maxEvaluations) { 
	String fileName = "";
	switch(method) {
		case 1:
			fileName = "RANK0_GDE3_SM1x_" + maxEvaluations;
			return fileName;
		case 2: 
			fileName = "RANK0_GDE3_SM2_" + maxEvaluations;
			return fileName;
		case 3: 
			fileName = "RANK0_GDE3_SM3_" + maxEvaluations;
			return fileName;
		case 4: 
			fileName = "RANK0_GDE3_SM4_" + maxEvaluations;
			return fileName;	
		default: 
			fileName = "RANK0_GDE3_Problem_" + maxEvaluations;
			return fileName;
	}
  }
  
  private static String getObjectiveFileNameTime(int method, int time) { 
	String fileName = "";
	switch(method) {
		case 1:
			fileName = "RANK0_GDE3_SM1x_" + time + "Min";
			return fileName;
		case 2: 
			fileName = "RANK0_GDE3_SM2_" + time + "Min";
			return fileName;
		case 3: 
			fileName = "RANK0_GDE3_SM3_" + time + "Min";
			return fileName;
		case 4: 
			fileName = "RANK0_GDE3_SM4_" + time + "Min";
			return fileName;	
		default: 
			fileName = "RANK0_GDE3_Problem_" + time + "Min";
			return fileName;
	}
  }
} // GDE3_main