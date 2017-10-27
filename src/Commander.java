import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import bwapi.Color;
import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UpgradeType;

public class Commander {
	private static Game game;
	private static Player me;
	public HashSet<Unit> squad = new HashSet<Unit>();
	
	public Commander(Game theGame, Player me) {
		this.game = theGame;
		this.me = me;
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
		if(squadSizeDiff <= 0) {
			if(squadSizeDiff < -(nrOfGoodies))
				threatLevel -= 2;
			else
				threatLevel -= 1;
		}
		else
			threatLevel += 1;
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
	
	public void sendMarines(HashSet<Unit> squad, HashSet<Position> enemyBuildingMemory) {
    	if(squad.size() < 10)
    		return;
    	if(me.getUpgradeLevel(UpgradeType.Terran_Infantry_Weapons) == 0)
    		return;
    	if(enemyBuildingMemory.size() > 0)
    	{
    		Position enemyPosition = null;
    		while(enemyPosition == null)
    			enemyPosition = enemyBuildingMemory.iterator().next();
    		for(Unit myUnit: squad) {
    			if(!myUnit.isIdle())
    				continue;
    			Unit closeEnemy = seeEnemy(myUnit);
    			if(closeEnemy == null) {
    				myUnit.attack(enemyPosition);
    				game.drawLineMap(myUnit.getPosition(), enemyPosition, Color.Red);
    			}
    			else {
    				myUnit.attack(closeEnemy);
    				game.drawLineMap(myUnit.getPosition(), closeEnemy.getPosition(), Color.Red);

    			}
    		}
    	}
    } 
	
	public void evaluateGame() {
		if(squad.size() > 0)
    		for(Unit myUnit: squad) {
    			game.drawTextMap(myUnit.getPosition(), this.evaluateThreat(myUnit) + " ");
    		}
	}
}
