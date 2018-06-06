import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import bwapi.UnitType;

/* Units:
 * 	Terran_Firebat
 	Terran_Ghost
 	Terran_Goliath
 	Terran_Marine
 	Terran_Medic
 	Terran_Siege_Tank_Tank_Mode
 	Terran_Vulture
 	Terran_Vulture_Spider_Mine
 	Terran_Battlecruiser
 	Terran_Dropship
 	Terran_Nuclear_Missile
 	Terran_Science_Vessel
 	Terran_Valkyrie
 	Terran_Wraith 
 */

public class Individual {
	public static Logger logger = Logger.getLogger(ExampleBot.class.getName());
	public float fitnessScore;
	public ArrayList<Integer> unitsGenome;
	public Integer enemyRace = 0; //1 terran, 2 protoss, 3 zerg
	public Individual(ArrayList<Integer> existingGenome) {
		this.unitsGenome = existingGenome;
	} 
	public Individual() {
		this.unitsGenome = new ArrayList<>();
		//bad code?
		for(int i = 0; i < JsonParser.unitList.length; i ++) {
			this.unitsGenome.add(0);
		}
	}
	public void calculateFitness(float buildingToKillRatio, int buildingScore, int killScore) {
		fitnessScore = buildingToKillRatio * buildingScore + (1 - buildingToKillRatio) * killScore;
	}
	
	public static Individual createNew(Individual a, Individual b) {
		Random rn = new Random();
		Individual newIndividual = new Individual();
		//make sure a > b
		logger.log(Level.INFO, "New individual is being created from: " + a + " " + b);
		if(b.fitnessScore > a.fitnessScore) {
			newIndividual = a;
			a = b;
			b = a;
			newIndividual =  new Individual();
		}
		
		float dominantRatio = b.fitnessScore / a.fitnessScore;
		logger.log(Level.INFO, "New individual, start setting genes: " + newIndividual.unitsGenome.size());
		for(int i = 0; i < JsonParser.unitList.length; i ++) {
			if(rn.nextFloat() > dominantRatio / 2)
				newIndividual.unitsGenome.set(i, a.unitsGenome.get(i));
			else
				newIndividual.unitsGenome.set(i, b.unitsGenome.get(i));
		}
		return newIndividual;
	}
	
	public void mutate(float mutationFactor, int mutationImpact) {
		Random rn = new Random();
		if(mutationFactor < rn.nextFloat())
			return;
		//randomly pick one of the genes
		int gene = rn.nextInt(JsonParser.unitList.length);
		if(rn.nextFloat() > 0.5f || gene == 0) 
			unitsGenome.set(gene, unitsGenome.get(gene) + rn.nextInt(mutationImpact));
		else
			unitsGenome.set(gene, unitsGenome.get(gene) - rn.nextInt(mutationImpact));
		if(unitsGenome.get(gene) < 0)
			unitsGenome.set(gene, 0);
		}
	}




