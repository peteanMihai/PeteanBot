import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import com.google.gson.Gson;

import bwapi.UnitType;





public class FileParser{
	private static Gson gson = new Gson();
	public static UnitType[] unitList = {
			UnitType.Terran_Firebat,
			UnitType.Terran_Ghost,
			UnitType.Terran_Goliath,
			UnitType.Terran_Marine,
			UnitType.Terran_Medic,
			UnitType.Terran_Siege_Tank_Tank_Mode,
			UnitType.Terran_Vulture,
			UnitType.Terran_Battlecruiser,
			UnitType.Terran_Dropship,
			UnitType.Terran_Science_Vessel,
			UnitType.Terran_Valkyrie,
			UnitType.Terran_Wraith
	};
	
	
	public static void init() {
	 	stringToUnitType.put("Terran_Firebat", UnitType.Terran_Firebat);
	 	stringToUnitType.put("Terran_Ghost", UnitType.Terran_Ghost);
	 	stringToUnitType.put("Terran_Goliath", UnitType.Terran_Goliath);
	 	stringToUnitType.put("Terran_Marine", UnitType.Terran_Marine);
	 	stringToUnitType.put("Terran_Medic", UnitType.Terran_Medic);
	 	stringToUnitType.put("Terran_Siege_Tank_Tank_Mode", UnitType.Terran_Siege_Tank_Tank_Mode);
	 	stringToUnitType.put("Terran_Vulture", UnitType.Terran_Vulture);
	 	stringToUnitType.put("Terran_Battlecruiser", UnitType.Terran_Battlecruiser);
	 	stringToUnitType.put("Terran_Dropship", UnitType.Terran_Dropship);
	 	stringToUnitType.put("Terran_Science_Vessel", UnitType.Terran_Science_Vessel);
	 	stringToUnitType.put("Terran_Valkyrie", UnitType.Terran_Valkyrie);
	 	stringToUnitType.put("Terran_Wraith", UnitType.Terran_Wraith);
	};

	
	//oh god
	public static HashMap<String, UnitType> stringToUnitType = new HashMap<>();
	
	public static void saveSquad(HashMap<UnitType, Integer> squad, String filename) throws FileNotFoundException{
		PrintWriter writer = new PrintWriter(filename);
		String jsonResult = gson.toJson(squad);
		writer.write(jsonResult);
		writer.flush();
		writer.close();
	}
	
	public static ArrayList<Individual> readPopulationFromFile(String filename) throws IOException{
		ArrayList<Individual> population = new ArrayList<>();
		Scanner in = new Scanner(new FileReader(filename));
		ArrayList<Individual> result = new ArrayList<>();
		while(in.hasNextLine()) {
			Individual individual = new Individual();
			String line = in.nextLine();
			if(line.contains("GENERATION")) {
				result.clear();
				result.addAll(population);
				String[] tokLine = line.split(" ");
				EAController.generationCounter = Integer.parseInt(tokLine[1]);
				population.clear();
				continue;
			}
			int endOfGenomeAndFitness = line.indexOf("TIME");
			line = line.substring(0, endOfGenomeAndFitness);
			line = line.replace(",", "");
			line = line.replace("[", "");
			line = line.replace("]", "");
			line = line.replace("FITNESS: ", "");
			System.out.println(line);
			String[] tokLine = line.split(" ");
			for(int i = 0; i < unitList.length; i++) {
				individual.unitsGenome.set(i, Integer.parseInt(tokLine[i]));
			}
			individual.fitnessScore = Integer.parseInt(tokLine[unitList.length]);
			System.out.println("Got individual genome and fitness: " + individual.unitsGenome + " " + individual.fitnessScore);
			population.add(individual);
		}
		return result;
	}
	
	public static HashMap<UnitType, Integer> loadSquadInd(ArrayList<Integer> genome){
		//if we try to access the global one it crashes!
		UnitType[] unitList = {
				UnitType.Terran_Firebat,
				UnitType.Terran_Ghost,
				UnitType.Terran_Goliath,
				UnitType.Terran_Marine,
				UnitType.Terran_Medic,
				UnitType.Terran_Siege_Tank_Tank_Mode,
				UnitType.Terran_Vulture,
				UnitType.Terran_Battlecruiser,
				UnitType.Terran_Dropship,
				UnitType.Terran_Science_Vessel,
				UnitType.Terran_Valkyrie,
				UnitType.Terran_Wraith
		}; 
		ArrayList<UnitType> safeUnitList = new ArrayList<>();
		HashMap<UnitType, Integer> result = new HashMap<UnitType, Integer>();
		for(int i = 0; i < unitList.length; i ++) {
			safeUnitList.add(unitList[i]);
		}
		System.out.println(safeUnitList.toString());
		for(int i = 0; i < unitList.length; i++) {
				if(genome.get(i) != null && genome.get(i) > 0 && FileParser.unitList[i] != null) {
					result.put(safeUnitList.get(i), genome.get(i).intValue());
					
				}
			}
		System.out.println("result: " + result);	
		return result;
		
	}
	
	public static HashMap<UnitType, Integer> loadSquad(String filename) throws FileNotFoundException{
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String json = new String();
		String jsonLine = new String();
		try {
			while((jsonLine = reader.readLine()) != null) {
				json += "\n" + jsonLine;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		HashMap<String, Double> stringMapping = gson.fromJson(json, HashMap.class); 
		System.out.println("stringMapping keySet: " + stringMapping.keySet());
		for(String key: stringMapping.keySet())
			System.out.println("value: " + stringMapping.get(key));
		HashMap<UnitType, Integer> result = new HashMap<UnitType, Integer>();
		for(String key: stringMapping.keySet()) {
			//if this is a valid unit type
			for(String otherKey: stringToUnitType.keySet()) {
				System.out.println("value of stringToUnitType: " + stringToUnitType.get(otherKey));
				if(otherKey.equals(key)) {
					System.out.println("come on now");
					try {
					result.put(stringToUnitType.get(key), stringMapping.get(key).intValue());
					}
					catch(Exception e) {
						System.out.println(e.getMessage());
					}
				}
			}
		}
		System.out.println("result: " + result);	
		return result;
	}
}
