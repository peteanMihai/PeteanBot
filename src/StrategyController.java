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
	
	public void macro() {
		if(builder.minerals > 500 && 
				   builder.gas > 200 && 
				   game.elapsedTime() > 40 &&
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
	
	//this is a strategy
	public void upgradeFirstTier() {
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
	
	public void evaluateGame() {
		macro();
		upgradeFirstTier();
	}
}
