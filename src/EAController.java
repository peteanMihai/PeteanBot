import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Random;

public class EAController {
	public static float mutationFactor = 0.1f;
	public static int initGenesLimit = 3;
	public static int initNrUnitsLimit = 10;
	public static int generationCounter = 0;
	public ArrayList<Individual> population;
	
	public EAController(int populationLimit, float mutationFactor) {
		Random rn = new Random();
		EAController.mutationFactor = mutationFactor;
		this.population = new ArrayList<Individual>(populationLimit);
		for(int i = 0; i < populationLimit; i++) {
			Individual ind = new Individual();
			for(int j = 0; j < initGenesLimit; j++) {
				ind.unitsGenome.add(rn.nextInt(JsonParser.unitList.length), rn.nextInt(initNrUnitsLimit));
			}
			this.population.add(ind);
		}
	}
	
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
		//another generation
		generationCounter++;
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
		float max = -1;
		Individual best = null;
		for(Individual i : population) {
			if(i.fitnessScore > max) {
				max = i.fitnessScore;
				best = i;
			}
		}
		return best;
	}
	
	public void writeResults(String fileName) throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter(fileName);
		writer.println("Generation: " + generationCounter);
		for(Individual i : population) {
			writer.println(i.unitsGenome.toString() + " FITNESS: " + i.fitnessScore);
		}
		writer.close();
	}
}
