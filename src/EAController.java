import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class EAController {
	public static float mutationFactor = 0.1f;
	public ArrayList<Individual> population;
	public void crossOverPopulation() {
		int averageFitness = averageFitness();
		Individual best = bestIndividual();
		ArrayList<Individual> replacements = new ArrayList<>(population.size());
		Individual newIndividual = null;
		for(Individual i: population) {
			if(i.fitnessScore >= averageFitness) {
				newIndividual = i;
				newIndividual.mutate(mutationFactor);
			}
			else {
				newIndividual = Individual.createNew(i, best);
			}
			replacements.add(newIndividual);
		}
		population = replacements;
	}
	
	public int averageFitness() {
		int sum  = 0;
		for(Individual i : population) {
			if(i.fitnessScore > 0) {
				sum += i.fitnessScore;
			}
		}
		return sum / population.size();
	}
	
	public Individual bestIndividual() {
		int max = -1;
		Individual best = null;
		for(Individual i : population) {
			if(i.fitnessScore > max) {
				max = i.fitnessScore;
				best = i;
			}
		}
		return best;
	}
	
	public void writeResults() throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter("resultsFile.txt");
		for(Individual i : population)
	}
}
