import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

import bwapi.Game;
import bwapi.Player;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.UpgradeType;
import strategies.AStrategy;

public class StrategyController {
	private static int macroProductionBuildingsLimit = 3;
	private static int macroMineralThreshold = 500;
	private static int macroGasThreshhold = 200;
	private static int macroMinimumElapsedTime = 40;
	
	
	public  Logger logger = Logger.getLogger(ExampleBot.class.getName());
	private static Game game;
	private static Player me;
	public Builder builder;
	public Commander commander;
	public ArrayList<AStrategy> strategies;
	public StrategyController(Game game, Player player,Builder builder,Commander commander) {
		this.game = game;
		this.me = player;
		this.builder = builder;
		this.commander = commander;
		this.strategies = new ArrayList<AStrategy>();
	}
	
	public void addStrategy(AStrategy a) {
		strategies.add(a);
	}
	
	//this is a strategy to keep pumping out factories 
	public void macro() {
		if(builder.minerals > macroMineralThreshold && 
				   builder.gas > macroGasThreshhold && 
				   game.elapsedTime() > macroMinimumElapsedTime &&
				   builder.buildOrder.size() == 0){
			HashMap<UnitType, Integer> idealSquad = commander.idealSquad;
			HashSet<UnitType> productionList = new HashSet<UnitType>();
			for(UnitType u: idealSquad.keySet())
			{
				for(Unit building: me.getUnits())
				{
					if(building.canTrain(u))
						productionList.add(building.getType());
				}
			}
			for(UnitType building: productionList) {
				int counterBuilding = 0;
				for(Unit itUnits: me.getUnits()) {
					if(itUnits.getType() == building) {
						counterBuilding++;
					}
				}
				if(counterBuilding < macroProductionBuildingsLimit)
					builder.addToBuildStack(building);
			}
		}
	}
	
	//this is a hardcoded strategy
	public void upgradeBio() {
		if(builder.minerals > 500 && 
		   builder.gas > 300 && 
		   game.elapsedTime() > 60 &&
		   builder.buildOrder.size() == 0){
		   if(!builder.ownedBuildingTypes.contains(UnitType.Terran_Engineering_Bay) && !builder.alreadyBuilding(UnitType.Terran_Engineering_Bay)) {
			builder.addToBuildStack(UnitType.Terran_Engineering_Bay);
		   }
		   if(builder.ownedBuildingTypes.contains(UnitType.Terran_Engineering_Bay)) {
			   int wepUp = 0, armUp = 0;
			   wepUp = me.getUpgradeLevel(UpgradeType.Terran_Infantry_Weapons);
			   armUp = me.getUpgradeLevel(UpgradeType.Terran_Infantry_Armor);
			   if(armUp < wepUp)
				   builder.startUpgrade(UpgradeType.Terran_Infantry_Armor);
			   else
				   builder.startUpgrade(UpgradeType.Terran_Infantry_Weapons);
		   }
		}
		
	}
	//same for mech
	public void upgradeMech() {
		if(builder.minerals > 500 && 
		   builder.gas > 300 && 
		   game.elapsedTime() > 60 &&
		   builder.buildOrder.size() == 0){
		   if(!builder.ownedBuildingTypes.contains(UnitType.Terran_Armory) && !builder.alreadyBuilding(UnitType.Terran_Armory)) {
			builder.addToBuildStack(UnitType.Terran_Armory);
		   }
		   if(builder.ownedBuildingTypes.contains(UnitType.Terran_Armory)) {
			   int wepUp = 0, armUp = 0;
			   wepUp = me.getUpgradeLevel(UpgradeType.Terran_Vehicle_Weapons);
			   armUp = me.getUpgradeLevel(UpgradeType.Terran_Vehicle_Plating);
			   if(armUp < wepUp)
				   builder.startUpgrade(UpgradeType.Terran_Vehicle_Plating);
			   else
				   builder.startUpgrade(UpgradeType.Terran_Vehicle_Weapons);
		   }
		}
		
	}
	
	public void evaluateGame() {
		boolean haveBio = false;
		boolean haveMech = false;
		for(UnitType u : commander.idealSquad.keySet()) {
			if(u.isMechanical() && !u.isFlyer())
				haveMech = true;
			if(u.isOrganic())
				haveBio = true;
		}
		if(haveBio)
			upgradeBio();
		if(haveMech)
			upgradeMech();
		if(game.elapsedTime() % 60 != 0)
			return;
		macro();
	}
}
