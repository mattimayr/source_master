//  MOEAD_main.java
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

package jmetal.metaheuristics.moead;

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.operators.crossover.CrossoverFactory;
import jmetal.operators.mutation.MutationFactory;
import jmetal.problems.EBEs;
import jmetal.problems.Kursawe;
import jmetal.problems.ProblemFactory;
import jmetal.problems.SurrogateWrapper2;
import jmetal.problems.ZDT.ZDT3;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.Configuration;
import jmetal.util.JMException;
import jmetal.util.Ranking;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
/**
 * This class executes the algorithm described in:
 *   H. Li and Q. Zhang, 
 *   "Multiobjective Optimization Problems with Complicated Pareto Sets,  MOEA/D 
 *   and NSGA-II". IEEE Trans on Evolutionary Computation, vol. 12,  no 2,  
 *   pp 284-302, April/2009.  
 */
public class MOEAD_main_surrogate {
  public static Logger      logger_ ;      // Logger object
  public static FileHandler fileHandler_ ; // FileHandler object

  /**
   * @param args Command line arguments. The first (optional) argument specifies 
   *      the problem to solve.
   * @throws JMException 
   * @throws IOException 
   * @throws SecurityException 
   * Usage: three options
   *      - jmetal.metaheuristics.moead.MOEAD_main
   *      - jmetal.metaheuristics.moead.MOEAD_main problemName
   *      - jmetal.metaheuristics.moead.MOEAD_main problemName ParetoFrontFile
   * @throws ClassNotFoundException 
 
   */
  public static void main(String [] args) throws JMException, SecurityException, IOException, ClassNotFoundException {
    Problem   problem   ;         // The problem to solve
    Algorithm algorithm ;         // The algorithm to use
    Operator  crossover ;         // Crossover operator
    Operator  mutation  ;         // Mutation operator
     
    QualityIndicator indicators ; // Object to get quality indicators
	int populationSize = 1000;
	int maxEvaluations = 10000;
	int time = 0;

    HashMap  parameters ; // Operator parameters

    // Logger object and file to store log messages
    logger_      = Configuration.logger_ ;
    fileHandler_ = new FileHandler("MOEAD.log"); 
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
      //problem = new ZDT3("ArrayReal", 30);
      //problem = new ConstrEx("Real");
      //problem = new DTLZ1("Real");
      //problem = new OKA2("Real") ;
    } // else

	SurrogateWrapper2 sw = new SurrogateWrapper2(problem, maxEvaluations, 1, populationSize);
    algorithm = new MOEAD(sw);
    //algorithm = new MOEAD_DRA(problem);
    
    // Algorithm parameters
    algorithm.setInputParameter("populationSize", populationSize);
    algorithm.setInputParameter("maxEvaluations", maxEvaluations);
    
    // Directory with the files containing the weight vectors used in 
    // Q. Zhang,  W. Liu,  and H Li, The Performance of a New Version of MOEA/D 
    // on CEC09 Unconstrained MOP Test Instances Working Report CES-491, School 
    // of CS & EE, University of Essex, 02/2009.
    // http://dces.essex.ac.uk/staff/qzhang/MOEAcompetition/CEC09final/code/ZhangMOEADcode/moead0305.rar
    algorithm.setInputParameter("dataDirectory",
    "/Users/antelverde/Softw/pruebas/data/MOEAD_parameters/Weight");

    algorithm.setInputParameter("finalSize", 300) ; // used by MOEAD_DRA

    algorithm.setInputParameter("T", 20) ;
    algorithm.setInputParameter("delta", 0.9) ;
    algorithm.setInputParameter("nr", 2) ;

    // Crossover operator 
    parameters = new HashMap() ;
    parameters.put("CR", 1.0) ;
    parameters.put("F", 0.5) ;
    crossover = CrossoverFactory.getCrossoverOperator("DifferentialEvolutionCrossover", parameters);                   
    
    // Mutation operator
    parameters = new HashMap() ;
    parameters.put("probability", 1.0/problem.getNumberOfVariables()) ;
    parameters.put("distributionIndex", 20.0) ;
    mutation = MutationFactory.getMutationOperator("PolynomialMutation", parameters);                    
    
    algorithm.addOperator("crossover",crossover);
    algorithm.addOperator("mutation",mutation);
    
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
    logger_.info("Objectives values have been writen to file FUN");
    population.printObjectivesToFile("FUN");
    logger_.info("Variables values have been writen to file VAR");
    population.printVariablesToFile("VAR");      
    
    if (indicators != null) {
      logger_.info("Quality indicators") ;
      logger_.info("Hypervolume: " + indicators.getHypervolume(population)) ;
      logger_.info("EPSILON    : " + indicators.getEpsilon(population)) ;
      logger_.info("GD         : " + indicators.getGD(population)) ;
      logger_.info("IGD        : " + indicators.getIGD(population)) ;
      logger_.info("Spread     : " + indicators.getSpread(population)) ;
    } // if 

	logger_.info("Quality indicators") ;
    indicators = new QualityIndicator(problem, "RANK0_MOEAD_Problem_10000");   
	logger_.info("Hypervolume: " + indicators.getHypervolume(ranked));	
  } //main
  
  private static String getObjectiveFileName(int method, int maxEvaluations) { 
	String fileName = "";
	switch(method) {
		case 1:
			fileName = "RANK0_MOEAD_SM1x_" + maxEvaluations;
			return fileName;
		case 2: 
			fileName = "RANK0_MOEAD_SM2_" + maxEvaluations;
			return fileName;
		case 3: 
			fileName = "RANK0_MOEAD_SM3_" + maxEvaluations;
			return fileName;
		case 4: 
			fileName = "RANK0_MOEAD_SM4_" + maxEvaluations;
			return fileName;	
		default: 
			fileName = "RANK0_MOEAD_Problem_" + maxEvaluations;
			return fileName;
	}
  }
  
  private static String getObjectiveFileNameTime(int method, int time) { 
		String fileName = "";
		switch(method) {
			case 1:
				fileName = "RANK0_MOEAD_SM1x_" + time + "Min";
				return fileName;
			case 2: 
				fileName = "RANK0_MOEAD_SM2_" + time + "Min";
				return fileName;
			case 3: 
				fileName = "RANK0_MOEAD_SM3_" + time + "Min";
				return fileName;
			case 4: 
				fileName = "RANK0_MOEAD_SM4_" + time + "Min";
				return fileName;	
			default: 
				fileName = "RANK0_MOEAD_Problem_" + time + "Min";
				return fileName;
		}
	}
} // MOEAD_main