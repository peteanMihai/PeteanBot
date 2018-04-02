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

public class Commander {
	private static Game game;
	private static Player me;
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
		//idealSquad.put(UnitType.Terran_Marine, 20);
		//idealSquad.put(UnitType.Terran_Medic, 5);
		
	}
	
	public boolean validateSquad(HashSet<Unit> squad, HashMap<UnitType, Integer> idealSquad ) {
		//System.out.println("VALIDATING SQUAD");
		if(idealSquad.size() == 0) {	
			//System.out.println("MISSING IDEAL SQUAD!");
			return false;
		}
		HashMap<UnitType, Integer> squadMap = new HashMap<>();
		for(Unit unit: squad) {
			if(!unit.exists()) {
				squad.remove(unit);
				continue;
			}
			//System.out.println("PARSE SQUAD");
			if( squadMap.get(unit.getType()) != null) {
				int unitsOfType = squadMap.get(unit.getType());
				squadMap.put(unit.getType(), unitsOfType + 1);
			}
			else
				squadMap.put(unit.getType(), 0);
		}
		//System.out.println("idealSquad: " + idealSquad);
		//System.out.println("squad:" + squadMap);
		//iterate through ideal squad, if we have at least the specified units we can attack
		

		for(UnitType type: idealSquad.keySet()) {
			//System.out.println("PARSING IDEAL SQUAD");
			if(squadMap.get(type) == null) {
				//System.out.println("INVALID SQUAD");
				return false;
			}
			if(squadMap.get(type) < idealSquad.get(type)) {
				//System.out.println("INVALID SQUAD");
				return false;
			}
		}
		//System.out.println("GOOD FUCKING SQUAD");
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
		List<Unit> neighbours = unit.getUnitsInWeaponRange(unit.getType().groundWeapon());
		for(Unit neighbour: neighbours) {
			//neighbour is enemy
			if(neighbour.canAttack(unit) && neighbour.getPlayer().isEnemy(unit.getPlayer())){
				if(neighbour.getType().groundWeapon().damageAmount() > unit.getHitPoints())
					threatLevel += 3;
				else
					threatLevel += 1;
				nrOfBaddies ++;
			}
			if(neighbour.getPlayer() == unit.getPlayer())
				nrOfGoodies ++;
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
		game.drawTextMap(unit.getPosition().getX(), unit.getPosition().getY(), "" + threatLevel);
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
	
	public void defendBase() {
		for(Unit myUnit:squad) {
			myUnit.move(me.getStartLocation().toPosition());
		}
	}
	
	public TilePosition establishTarget() {
		for (Unit u : game.enemy().getUnits()) {
        	//if this unit is in fact a building
        	if (u.getType().isBuilding()) {
        		//check if we have it's position in memory and add it if we don't
        		if (!enemyBuildingMemory.contains(u.getPosition())) 
        			enemyBuildingMemory.add(u.getPosition());
        	}
        }
		return enemyBuildingMemory.iterator().next().toTilePosition();
	}
	
	public void evaluateGame() {
		myTarget = establishTarget();
		if(validateSquad(squad, idealSquad))
			sendMarines(squad, myTarget);
		for(Unit u: squad) {
			try {
				//evaluateThreat(u);
			}
			catch(Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}
}
