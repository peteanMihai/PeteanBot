import java.util.ArrayList;
import java.util.Random;

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
	
	public Integer fitnessScore;
	public ArrayList<Integer> unitsGenome;
	public Integer enemyRace = 0; //1 terran, 2 protoss, 3 zerg
	public Individual(ArrayList<Integer> existingGenome) {
		this.unitsGenome = existingGenome;
	}
	public Individual() {
		this.unitsGenome = new ArrayList<>(13);
	}
	public void calculateFitness(int buildingToKillRatio, int buildingScore, int killScore) {
		fitnessScore = buildingToKillRatio * buildingScore + (1 - buildingToKillRatio) * killScore;
	}
	
	public static Individual createNew(Individual a, Individual b) {
		Random rn = new Random();
		Individual newIndividual = new Individual();
		//make sure a > b
		
		if(b.fitnessScore > a.fitnessScore) {
			newIndividual = a;
			a = b;
			b = a;
			newIndividual = null;
		}
		
		int dominantRatio = b.fitnessScore / a.fitnessScore;
		
		for(int i = 0; i < a.unitsGenome.size(); i ++) {
			if(rn.nextFloat() > dominantRatio)
				newIndividual.unitsGenome.set(i, a.unitsGenome.get(i));
			else
				newIndividual.unitsGenome.set(i, b.unitsGenome.get(i));
		}
		return newIndividual;
	}
	
	public void mutate(float mutationFactor) {
		Random rn = new Random();
		if(mutationFactor < rn.nextFloat())
			return;
		//randomly pick one of the genes
		int gene = rn.nextInt() % JsonParser.unitList.length;
		if(rn.nextFloat() > 0.5f) 
			gene++;
		else
			gene--;
		if(gene < 0)
			gene = 0;
		}
	}




