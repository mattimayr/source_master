package jmetal.qualityIndicator;

import java.io.IOException;

import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.problems.EBEs;

/**
 * @author Mayr Matthias
 * Main class to read fronts from file.
 */

public class calculateFrontHypervolume_Main {
	public static void main(String [] args) throws ClassNotFoundException, IOException {
		
		Problem problem = new EBEs("Real");
		String frontPath = "home\\matti\\1";
		String trueFrontPath = "home\\matti\\2";
		
		Hypervolume hv;
		double quality;
		
		FrontReader frontReader = new FrontReader(problem, frontPath);
		FrontReader trueFrontReader = new FrontReader(problem, trueFrontPath);
		
		SolutionSet front = new SolutionSet(frontReader.getNumberOfSolutions());
		SolutionSet trueFront = new SolutionSet(trueFrontReader.getNumberOfSolutions());
		
		front = frontReader.readFile();
		trueFront = trueFrontReader.readFile();
		
		hv = new Hypervolume();
		quality = hv.hypervolume(front.writeObjectivesToMatrix(), trueFront.writeObjectivesToMatrix(), problem.getNumberOfObjectives());
		System.out.printf("Hypervolume = %.2f\n", quality);
	}
}