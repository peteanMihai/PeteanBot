import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EAController {
	public  Logger logger = Logger.getLogger(ExampleBot.class.getName());
	public static float mutationFactor = 0.1f;
	public static int mutationImpact = 3;
	public static int initGenesLimit = 3;
	public static int initNrUnitsLimit = 10;
	public static int generationCounter = 0;
	public static String name;
	public ArrayList<Individual> population;
	
	public String fileName() {
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
    	Timestamp ts = new Timestamp(System.currentTimeMillis());
    	return sdf.format(ts);
	}
	
	public EAController(int populationLimit, float mutationFactor) {
		name = "EASquadLog" + fileName() + ".txt";
		Random rn = new Random();
		EAController.mutationFactor = mutationFactor;
		this.population = new ArrayList<Individual>(populationLimit);
		for(int i = 0; i < populationLimit; i++) {
			Individual ind = new Individual();
			for(int j = 0; j < initGenesLimit; j++) {
				ind.unitsGenome.set(rn.nextInt(JsonParser.unitList.length), rn.nextInt(initNrUnitsLimit));
			}
			this.population.add(ind);
		}
	}
	
	public void crossOverPopulation() {
		float averageFitness = averageFitness();
		logger.log(Level.INFO, "Got average fitness: " + averageFitness);
		Individual best = bestIndividual();
		logger.log(Level.INFO, "Got best individual: " + best);
		ArrayList<Individual> replacements = new ArrayList<>(population.size());
		logger.log(Level.INFO, "Initial population size: " + population.size());
		Individual newIndividual = null;
		population.sort((Individual a, Individual b) -> (int)(b.fitnessScore - a.fitnessScore));
		System.out.println("population: " + population);
		
		for(int i = 0; i < population.size() /2 ; i ++) {
				//good individual keeps living
				newIndividual = population.get(i);
				newIndividual.mutate(mutationFactor, mutationImpact);
				replacements.add(newIndividual);
				
				//crossover 2 good individuals
				newIndividual = Individual.createNew(population.get(i), population.get(i + 1));
				newIndividual.mutate(mutationFactor, mutationImpact);
				replacements.add(newIndividual);
			}
		
		logger.log(Level.INFO, "Population size before replacements: " + population.size());
		population = replacements;
		logger.log(Level.INFO, "Population size after replacements: " + population.size());
		//another generation
		generationCounter++;
	}
	
	public float averageFitness() {
		float sum  = 0;
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
	
	public void writeResults(String fileName) throws IOException {
		PrintWriter writer =  new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));
		writer.println("GENERATION: " + generationCounter + " DONE WITH: " + averageFitness() + " AVERAGE FITNESS");
		writer.close();
	}
}
