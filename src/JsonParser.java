import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import com.google.gson.Gson;

import bwapi.UnitType;





public class JsonParser{
	private static Gson gson = new Gson();
	
	public static void init() {
		stringToUnitType.put("Terran_Marine", UnitType.Terran_Marine);
		stringToUnitType.put("Terran_Medic", UnitType.Terran_Medic);
		stringToUnitType.put("Terran_Vulture", UnitType.Terran_Vulture);
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
	
	public static HashMap<UnitType, Double> loadBuild(String fileName) throws FileNotFoundException{
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		String json = new String();
		String jsonLine = new String();
		HashMap<UnitType, Double> result = new HashMap<UnitType, Double>();
		try {
			while((jsonLine = reader.readLine()) != null) {
				json += "\n" + jsonLine;
			}
		}
		 catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		}
			
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
