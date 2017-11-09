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
	
	public JsonParser() {
		stringToUnitType.put("Terran_Marine", UnitType.Terran_Marine);
		stringToUnitType.put("Terran_Medic", UnitType.Terran_Medic);
	};
	
	//oh god
	public static HashMap<String, UnitType> stringToUnitType = new HashMap<>();
	
	public static void SaveSquad(HashMap<UnitType, Integer> squad, String filename) throws FileNotFoundException{
		PrintWriter writer = new PrintWriter(filename);
		String jsonResult = gson.toJson(squad);
		writer.write(jsonResult);
		writer.flush();
		writer.close();
	}
	
	public static HashMap<UnitType, Integer> LoadSquad(String filename) throws FileNotFoundException{
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
			
		HashMap<String, Integer> stringMapping = gson.fromJson(json, HashMap.class); 
		System.out.println(stringMapping.keySet());
		HashMap<UnitType, Integer> result = new HashMap<UnitType, Integer>();
		for(String key: stringMapping.keySet()) {
			//if this is a valid unit type
			if(stringToUnitType.containsKey(key)) {
				System.out.println("come on now");
				result.put(stringToUnitType.get(key), stringMapping.get(key));
			}
		}
		System.out.println(result);	
		return result;
	}
}
