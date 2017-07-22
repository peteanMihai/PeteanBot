import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;

import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

public class ExampleBot extends DefaultBWListener {

    private Mirror mirror = new Mirror();

    private Game game;

    private Player self;
    private boolean bOneExtractor = false;
    private int barrackCount = 0;
    private boolean bSupplyBlocked = false;
    private boolean bScouted = false;
    private Unit scout = null;
    private HashSet<Builder> builders = new HashSet<Builder>();
    private HashSet<Unit> squad = new HashSet<Unit>();
    private HashSet<Position> enemyBuildingMemory = new HashSet<Position>();
    private HashSet<Unit> workers = new HashSet<Unit>();
    private HashSet<Unit> gasExtractors = new HashSet<Unit>();
    private Stack<TilePosition> startingLocations = new Stack<TilePosition>();
    private ArrayDeque<UnitType> buildOrder = new ArrayDeque<UnitType>();

    public void run() {
    	mirror.getModule().setEventListener(this);
        mirror.startGame();	
    }

    public void sendMarines() {
    	if(squad.size() < 10)
    		return;
    	if(self.getUpgradeLevel(UpgradeType.Terran_Infantry_Weapons) == 0)
    		return;
    	game.setLocalSpeed(-1);
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
    
    public void trainMarines() {
    	for(Unit myUnit: self.getUnits()) {
    		if(myUnit.getType() == UnitType.Terran_Barracks)
    			myUnit.train(UnitType.Terran_Marine);
    	}
    }
    
    public void scout() {
    	for(Unit myUnit: self.getUnits()) 
    		if(myUnit.getType().canMove() && scout == null) {
    			scout = myUnit;
    			scout.stop();
    			workers.remove(scout);
    		}
    	if(enemyBuildingMemory.size() > 0) {
    		scout.move(self.getStartLocation().toPosition());
    		workers.add(scout);
    		scout = null;
    		bScouted = true;
    	}
    	if(scout.isIdle())
    		scout.move(startingLocations.pop().toPosition());
    }
    
    public void upgrades() {
    	for(Unit myUnit: self.getUnits()) {
    		if(myUnit.getType() == UnitType.Terran_Engineering_Bay) {
    			if(myUnit.isIdle() && self.getUpgradeLevel(UpgradeType.Terran_Infantry_Weapons) == 0){
    				myUnit.upgrade(UpgradeType.Terran_Infantry_Weapons);
    			}
    			else
    				myUnit.upgrade(UpgradeType.Terran_Infantry_Armor);
    		}
    	}
    }
    
    public void buildClose(UnitType building) {
    	for (Unit myUnit : workers) {    		
    			if(!myUnit.isIdle() && !myUnit.isGatheringMinerals())
    				continue;
    			TilePosition buildTile = this.getBuildTile(myUnit, building, self.getStartLocation());
    			builders.add(new Builder(myUnit,building));
    			workers.remove(myUnit);
    			myUnit.build(building, buildTile);
    			break;
    		}
    }
   
    public void sendIdleMine() {
    	for(Builder builder: builders)
    		if(builder.getBuilder().isIdle()) {
    			workers.add(builder.getBuilder());
    			builders.remove(builder);
    		}
        //if it's a worker and it's idle, send it to the closest mineral patch
    	for(Unit myUnit : workers) { 
    		
    		if (myUnit.isIdle()) {
    			
    			
    			Unit closestMineral = null;
    			//find the closest mineral
    			for (Unit neutralUnit : game.neutral().getUnits()) 
    				if (neutralUnit.getType().isMineralField()) 
    					if (closestMineral == null || myUnit.getDistance(neutralUnit) < myUnit.getDistance(closestMineral)) 
    						closestMineral = neutralUnit;
    					
        //if a mineral patch was found, send the worker to gather it
        if (closestMineral != null)
            myUnit.gather(closestMineral, false);
    		}
    	}
    	
      
    }

    public void evaluateGame() {
    	boolean alreadyBuilding = false;
    		for(Builder builder: builders)
        		if(builder.getBuilding( )== UnitType.Terran_Supply_Depot)
        			alreadyBuilding = true;
    	
    	if(self.supplyTotal() < self.supplyUsed()  + 10 && !alreadyBuilding && !buildOrder.contains(UnitType.Terran_Supply_Depot))
			buildOrder.addFirst(UnitType.Terran_Supply_Depot);
    		
		boolean bHaveEngineeringBay = false;
		for(Unit myUnit: self.getUnits()) {
			if(myUnit.getType() == UnitType.Terran_Engineering_Bay)
				bHaveEngineeringBay = true;
			if(myUnit.getType() != UnitType.Terran_SCV) {
				continue;
			}
			if(myUnit.getBuildType() == UnitType.Terran_Engineering_Bay)
				bHaveEngineeringBay = true;
		}	
			if(!bHaveEngineeringBay && !buildOrder.contains(UnitType.Terran_Engineering_Bay)) {
				buildOrder.addFirst(UnitType.Terran_Engineering_Bay);
			}
		boolean bHavebarrack = false;
		for(Unit myUnit: self.getUnits()) {
			if(myUnit.getType() != UnitType.Terran_SCV) {
				continue;
			}
			if(myUnit.getBuildType() == UnitType.Terran_Barracks)
				bHavebarrack = true;
		}

		if(self.minerals() > 300 && squad.size() < 30 && barrackCount < 3 && !bHavebarrack) 
			buildOrder.addFirst(UnitType.Terran_Barracks);
		System.out.println(buildOrder.toString());
    }
    
    @Override
    public void onUnitCreate(Unit unit) {
    	
    		
    }
    
    @Override
    public void onUnitComplete(Unit unit) {
        if(unit.getType() == UnitType.Terran_SCV) {
        	workers.add(unit);
        }
        if(unit.getType() == UnitType.Terran_Barracks)
        	barrackCount ++;
        if(unit.getType() == UnitType.Terran_Marine)
        	squad.add(unit);
        if(unit.getType() == UnitType.Terran_Refinery)
        	gasExtractors.add(unit);
    }
    
    @Override
    public void onUnitDestroy(Unit unit) {
    	if(unit.getType() == UnitType.Terran_SCV) {
        	workers.remove(unit);
        }
    	if(unit.getType() == UnitType.Terran_Barracks)
        	barrackCount --;
    	 if(unit.getType() == UnitType.Terran_Marine) {
         	squad.remove(unit);
    	 }
    	 if(unit.getType() == UnitType.Terran_Refinery)
         	gasExtractors.remove(unit);
    }

    @Override
    public void onStart() {
    	
        game = mirror.getGame();
        game.setLocalSpeed(0);
        self = game.self();
        
        //Use BWTA to analyze map
        //This may take a few minutes if the map is processed first time!
        System.out.println("Analyzing map...");
        BWTA.readMap();
        BWTA.analyze();
        System.out.println("Map data ready");
        
        int i = 0;
        
        for(BaseLocation baseLocation : BWTA.getBaseLocations()){
        	System.out.println("Base location #" + (++i) + ". Printing location's region polygon:");
        	for(Position position : baseLocation.getRegion().getPolygon().getPoints()){
        		System.out.print(position + ", ");
        	}
        }
        
        
        //initialize starting locations for scout
        for(TilePosition location: game.getStartLocations()) {
    		if(location != self.getStartLocation())
    			startingLocations.push(location);
    	}
        buildOrder.addFirst(UnitType.Terran_Refinery);
        System.out.println("initialization complete");
    }

    @Override
    public void onFrame() {
    	
    	//game.setTextSize(10);
        game.drawTextScreen(10, 10, "Is supply blocked: " + bSupplyBlocked);
        game.drawTextScreen(10, 20, "Worker count: " + workers.size());
        game.drawTextScreen(10, 30, "Squad size: " + squad.size());
        game.drawTextScreen(10, 40, "Builders size: " + builders.size());
        if(scout != null)
        	game.drawCircleMap(scout.getPosition(), 3, Color.Green);
        bSupplyBlocked = self.supplyTotal() <= self.supplyUsed() ? true : false;
        if(buildOrder.size() > 0)
        	if(self.minerals() > buildOrder.peekLast().mineralPrice()) {
        		buildClose(buildOrder.removeLast());
        	}
        
        /* Old building algorithm. Don't talk to me
        if(bSupplyBlocked && self.minerals() >= 100) {
        	boolean alreadyBuilding = false;
        	for(Builder builder: builders)
        		if(builder.getBuilding( )== UnitType.Terran_Supply_Depot)
        			alreadyBuilding = true;
        	if(!alreadyBuilding)
        		this.buildClose(UnitType.Terran_Supply_Depot);
        }
        
        if(self.minerals() > 150 && workers.size() > 10 && barrackCount < 4 && !bSupplyBlocked)
        	this.buildClose(UnitType.Terran_Barracks);
        	
        if(self.minerals() > 150 && workers.size() > 12 && !bOneExtractor){
        	this.buildClose(UnitType.Terran_Refinery);
        	bOneExtractor = true;
        }
        */
        this.evaluateGame();
        for (Unit u : game.enemy().getUnits()) {
        	//if this unit is in fact a building
        	if (u.getType().isBuilding()) {
        		//check if we have it's position in memory and add it if we don't
        		if (!enemyBuildingMemory.contains(u.getPosition())) enemyBuildingMemory.add(u.getPosition());
        	}
        }
        
        //iterate through my units
        for (Unit myUnit : self.getUnits()) {
            //if there's enough minerals, train an SCV
            if (myUnit.getType() == UnitType.Terran_Command_Center && self.minerals() >= 50 && workers.size() < 15) {
                myUnit.train(UnitType.Terran_SCV);
            }
         }
        if(!bScouted)
        	this.scout();
        this.sendIdleMine();
        if(barrackCount > 0)
        	trainMarines();
        this.sendMarines();
        this.upgrades();
        //draw my units on screen
        //game.drawTextScreen(10, 25, units.toString());
    }

 // Returns a suitable TilePosition to build a given building type near
 // specified TilePosition aroundTile, or null if not found. (builder parameter is our worker)
 public TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile) {
 	TilePosition ret = null;
 	int maxDist = 3;
 	int stopDist = 40;

 	// Refinery, Assimilator, Extractor
 	if (buildingType.isRefinery()) {
 		for (Unit n : game.neutral().getUnits()) {
 			if ((n.getType() == UnitType.Resource_Vespene_Geyser) &&
 					( Math.abs(n.getTilePosition().getX() - aroundTile.getX()) < stopDist ) &&
 					( Math.abs(n.getTilePosition().getY() - aroundTile.getY()) < stopDist )
 					) return n.getTilePosition();
 		}
 	}

 	while ((maxDist < stopDist) && (ret == null)) {
 		for (int i=aroundTile.getX()-maxDist; i<=aroundTile.getX()+maxDist; i++) {
 			for (int j=aroundTile.getY()-maxDist; j<=aroundTile.getY()+maxDist; j++) {
 				if (game.canBuildHere(new TilePosition(i,j), buildingType, builder, false)) {
 					// units that are blocking the tile
 					boolean unitsInWay = false;
 					for (Unit u : game.getAllUnits()) {
 						if (u.getID() == builder.getID()) continue;
 						if ((Math.abs(u.getTilePosition().getX()-i) < 4) && (Math.abs(u.getTilePosition().getY()-j) < 4)) unitsInWay = true;
 					}
 					if (!unitsInWay) {
 						return new TilePosition(i, j);
 					}
 					// creep for Zerg
 					if (buildingType.requiresCreep()) {
 						boolean creepMissing = false;
 						for (int k=i; k<=i+buildingType.tileWidth(); k++) {
 							for (int l=j; l<=j+buildingType.tileHeight(); l++) {
 								if (!game.hasCreep(k, l)) creepMissing = true;
 								break;
 							}
 						}
 						if (creepMissing) continue;
 					}
 				}
 			}
 		}
 		maxDist += 2;
 	}

 	if (ret == null) game.printf("Unable to find suitable build position for "+buildingType.toString());
 	return ret;
 }
    
    public static void main(String[] args) {
        new ExampleBot().run();
    }
}