import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import bwapi.Color;
import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.UpgradeType;
import bwapi.WeaponType;
import bwta.BWTA;
import bwta.Chokepoint;

import java.util.logging.*;
public class Commander {
	public  Logger logger = Logger.getLogger(ExampleBot.class.getName());
	private static Game game;
	private static Player me;
	public static int attackSquadSize = 15;
	public Builder builder;
	public HashSet<Unit> squad;
	public HashMap<UnitType, Integer> idealSquad;
	public HashSet<Position> enemyBuildingMemory;
	public TilePosition myTarget;
	public Commander(Game theGame, Player me) {
		this.game = theGame;
		this.me = me;
		squad = new HashSet<Unit>();
		idealSquad = new HashMap<>();
		enemyBuildingMemory = new HashSet<Position>();
		init();
	}
	
	public void setBuilder(Builder bld) {
		builder = bld;
	}
	
	public Builder getBuilder() {
		return builder;
	}
	
	public void init() {
		try {
			JsonParser.init();
			idealSquad = JsonParser.loadSquad("marineMedic.json");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		logger.log(Level.INFO, "Ideal squad loaded: " + idealSquad);
		//idealSquad.put(UnitType.Terran_Marine, 20);
		//idealSquad.put(UnitType.Terran_Medic, 5);
		
	}
	
	public void refreshSquad() {
		squad.clear();
		for(Unit u: me.getUnits())
			if((u.canAttack() && u.getType() != UnitType.Terran_SCV) || u.getType() == UnitType.Terran_Medic )
				squad.add(u);
		
	}
	
	public boolean validateSquad(HashSet<Unit> squad, HashMap<UnitType, Integer> idealSquad ) {
		if(idealSquad == null)
			return false;
		HashMap<UnitType, Integer> necesarryUnits = (HashMap<UnitType, Integer>) idealSquad.clone();
    	for(UnitType type: necesarryUnits.keySet())
    		for(Unit u : squad)
    				if(type == u.getType()) {
    					int oldValue = necesarryUnits.get(type);
    					necesarryUnits.put(type, oldValue - 1);
    				}
    	
    	for(UnitType type: necesarryUnits.keySet())
    		if(necesarryUnits.get(type) > 0)
    			return false;
    	return true;
	}
	
	public void getInBunker(Unit bunker, Unit unit) {
		if(bunker.getType() != UnitType.Terran_Bunker || !unit.canMove() || !bunker.canLoad(unit))
			return;
		unit.rightClick(bunker);
	}
	
	public int evaluateThreat(Unit unit) {
		int threatLevel = 0;
		int nrOfBaddies = 0;
		int nrOfGoodies = 0;
		
		WeaponType wepType = unit.getType().groundWeapon();
		//logger.log(Level.INFO, "got weapon type: " + wepType);
		ArrayList<Unit> neighbours = new ArrayList<Unit>();
		//logger.log(Level.INFO, "arraylist of neighbours initialized: ");
		unit.getUnitsInRadius(wepType.maxRange());
		//logger.log(Level.INFO, "got units in range: " + wepType);
		if(neighbours.size() == 0) {
			//logger.log(Level.INFO, "no neighbours!");
			game.drawTextMap(unit.getPosition() , "" + threatLevel);
			return 0;
		}
		//logger.log(Level.INFO, "obtained neighbours:" + neighbours.size());
		for(Unit neighbour: neighbours) {
			//neighbour is enemy
			if(neighbour.canAttack(unit) && neighbour.getPlayer().isEnemy(unit.getPlayer())){
				//logger.log(Level.INFO, "neighbour " + unit + " can attack " + unit);
				if(neighbour.getType().groundWeapon().damageAmount() > unit.getHitPoints())
					threatLevel += 3;
				else
					threatLevel += 1;
				nrOfBaddies ++;
			}
			if(neighbour.getPlayer() == unit.getPlayer()){
				nrOfGoodies ++;	
			}
		}
		int squadSizeDiff = nrOfBaddies - nrOfGoodies;
		if(squadSizeDiff < 0) {
			if(squadSizeDiff < -(nrOfGoodies))
				threatLevel -= 2;
			else
				threatLevel -= 1;
		}
		else
			threatLevel += 1;
		//logger.log(Level.INFO, "trying to draw unit threat level");
		game.drawTextMap(unit.getPosition() , "" + threatLevel);
		return threatLevel;
	}
	
	
	public Unit seeEnemy(Unit myUnit) {
    	int bestDistance = 999999;
    	Unit bestEnemy = null;
    	for(Unit enemy: myUnit.getUnitsInRadius(100)) {
    		if(enemy.getPlayer() == game.enemy())
    			if(bestDistance > myUnit.getDistance(enemy)) {
    				bestDistance = myUnit.getDistance(enemy);
    				bestEnemy = enemy;
    			}	    			
    	}
    	return bestEnemy;
    }
	
	public void sendMarines(HashSet<Unit> squad, TilePosition target) {
    	if(enemyBuildingMemory.size() > 0)
    	{
    		for(Unit myUnit: squad) {
    			if(!myUnit.isIdle())
    				continue;
    			Unit closeEnemy = seeEnemy(myUnit);
    			if(closeEnemy == null) {
    				myUnit.attack(target.toPosition());
    				game.drawLineMap(myUnit.getPosition(), target.toPosition(), Color.Red);
    			}
    			else {
    				myUnit.attack(closeEnemy);
    				game.drawLineMap(myUnit.getPosition(), closeEnemy.getPosition(), Color.Red);

    			}
    		}
    	}
    } 
	
	public void sendSquadChokePoint(HashSet<Unit> squad) {
		for(Chokepoint a : BWTA.getChokepoints()) {
			Position pos = a.getCenter();
		}
	}
	
	public void gatherAtPoint(HashSet<Unit> squad, TilePosition location) {
		//logger.log(Level.INFO, "Squad of " + squad.size() + " is going to " + location + " !");
		//game.setLocalSpeed(1);
		for(Unit u: squad) {
			if(!u.isIdle())
				continue;
			u.attack(location.toPosition());
		}
	}
	
	public void defendBase() {
		for(Unit u: me.getUnits())
			if(u.getType().isBuilding())
			{
				List<Unit> unitsInRange = u.getUnitsInRadius(20);
				for(Unit enemy: unitsInRange)
				{
					if(enemy.getPlayer().isEnemy(me))
						gatherAtPoint(squad, u.getTilePosition());
				}
			}
				
	}
	
	public TilePosition establishTarget() {
		for (Unit u : game.enemy().getUnits()) {
        	//if this unit is in fact a building
        	if (u.getType().isBuilding()) {
        		//check if we have it's position in memory and add it if we don't
        		if (!enemyBuildingMemory.contains(u.getPosition())) {
        			enemyBuildingMemory.add(u.getPosition());
        			logger.log(Level.INFO, "Found an enemy");
        		}
        		
        	}
        }
		if(enemyBuildingMemory.size() == 0) {
			return null;
		}
		return enemyBuildingMemory.iterator().next().toTilePosition();
	}
	
	public void evaluateGame() {
		myTarget = establishTarget();
		refreshSquad();
		defendBase();
		if(validateSquad(squad, idealSquad) && myTarget != null)
		{
			logger.log(Level.INFO, "Squad of " + squad.size() + " is attacking!");
			sendMarines(squad, myTarget);
		}
		for(Unit u: squad) {
			try {
				//logger.log(Level.INFO, "before evaluate");
				evaluateThreat(u);
				//logger.log(Level.INFO, "after evaluate");
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}
