import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import bwapi.Game;
import bwapi.Player;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.UpgradeType;

public class Builder{
	private static Game game;
	private static Player me;
	//bad bad code
	public HashSet<Unit> gasExtractors;
    public ArrayList<UnitType> buildOrder;
    public HashSet<UnitType> areBeingBuilt;
	//had to give access
	public HashSet<Unit> workers;
	public int barrackCount = 0;
	public Builder(Game game, Player me) {
		this.game = game;
		this.me = me;		
		gasExtractors = new HashSet<Unit>();
		buildOrder = new ArrayList<UnitType>();
		areBeingBuilt = new HashSet<UnitType>();
		workers = new HashSet<Unit>();
		initBuildStack();
	}
	 // Returns a suitable TilePosition to build a given building type near
	 // specified TilePosition aroundTile, or null if not found. (builder parameter is our worker)
	public TilePosition  getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile) {
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

	 	if (ret == null) game.printf("Unable to find suitable build position for " + buildingType.toString());
	 	return ret;
	 }
	 
    public void buildClose(UnitType building, TilePosition place) {
    	for (Unit myUnit : workers) {    		
    			if(!myUnit.isIdle() && !myUnit.isGatheringMinerals())
    				continue;
    			TilePosition buildTile = this.getBuildTile(myUnit, building, place);
    			myUnit.build(building, buildTile);
    			break;
    		}
    }
    
    public void initBuildStack() {
    	buildOrder.add(UnitType.Terran_Barracks);
    	buildOrder.add(UnitType.Terran_Refinery);
    }
    
    public void sendIdleMine() {
        //if it's a worker and it's idle, send it to the closest mineral patch
    	for(Unit myUnit : workers) { 
    		if (myUnit.isIdle()) {
    			Unit closestMineral = null;
    			//find the closest mineral
    			for (Unit neutralUnit : game.neutral().getUnits()) 
    				if (neutralUnit.getType().isMineralField()) 
    					if (closestMineral == null || myUnit.getDistance(neutralUnit) < myUnit.getDistance(closestMineral)) 
    						{
    							closestMineral = neutralUnit;
    			        		//System.out.println("go mine ya fucker");
    						}
    					
        //if a mineral patch was found, send the worker to gather it
        if (closestMineral != null)
            myUnit.gather(closestMineral, false);
    		}
    	}
    }

    public void buildFromStack() {
    	long duration = 0;
    	
	   for(UnitType building: buildOrder) {
		    if(building == null)
		    	continue;
	    	if(me.minerals() > building.mineralPrice()) {
	    		long start,stop;
	    		start = System.currentTimeMillis();
	    		buildClose(building, me.getStartLocation());
	    		stop = System.currentTimeMillis();
	    		duration += stop - start;
	    		buildOrder.remove(building);
	    	}
	   }
	   game.drawTextScreen(10, 170, "findGoodTile: " + duration);
	}
    
    public void upgrades() {
    	for(Unit myUnit: me.getUnits()) {
    		if(myUnit.getType() == UnitType.Terran_Engineering_Bay) {
    			if(myUnit.isIdle() && me.getUpgradeLevel(UpgradeType.Terran_Infantry_Weapons) == 0){
    				myUnit.upgrade(UpgradeType.Terran_Infantry_Weapons);
    			}
    			else
    				myUnit.upgrade(UpgradeType.Terran_Infantry_Armor);
    		}
    	}
    }
    
    public void bunker() {
    	for(Unit myUnit: me.getUnits())
    		if(myUnit.getType() == UnitType.Terran_Bunker)
    			return;
    	if(buildOrder.contains(UnitType.Terran_Bunker) || game.elapsedTime() < 60 ||
    			areBeingBuilt.contains(UnitType.Terran_Bunker))
			return;
		buildOrder.add(UnitType.Terran_Bunker);
		buildOrder.add(UnitType.Terran_Bunker);
		buildOrder.add(UnitType.Terran_Bunker);
    }
    
    public void evaluateTech() {
    	boolean bHaveEngineeringBay = false;
		for(Unit myUnit: me.getUnits()) {
			if(myUnit.getType() == UnitType.Terran_Engineering_Bay)
				bHaveEngineeringBay = true;
		}
		if(!bHaveEngineeringBay && 
			!buildOrder.contains(UnitType.Terran_Engineering_Bay) && 
			!areBeingBuilt.contains(UnitType.Terran_Engineering_Bay) &&
			!alreadyBuilding(UnitType.Terran_Engineering_Bay) &&
			barrackCount > 1) {
				buildOrder.add(UnitType.Terran_Engineering_Bay);
		
		}
    	boolean bHaveAcademy = false;
		for(Unit myUnit: me.getUnits()) {
			if(myUnit.getType() == UnitType.Terran_Academy)
				bHaveAcademy = true;
		}
		if(!bHaveAcademy && 
			!buildOrder.contains(UnitType.Terran_Academy) && 
			!areBeingBuilt.contains(UnitType.Terran_Academy) &&
			!alreadyBuilding(UnitType.Terran_Academy)) {
				buildOrder.add(UnitType.Terran_Academy);
		
		}
    }
    
    public boolean startTechBuilding() { return false;}
    
    public boolean alreadyBuilding(UnitType someBuilding) {
    	for(Unit myUnit: this.workers) 
			if(myUnit.getType() == someBuilding)
				if( myUnit.getBuildType() == someBuilding)
					return true;
		return false;
    }
    
    public void supply() {
		if(me.supplyTotal() <= me.supplyUsed() + 10 && 
				!alreadyBuilding(UnitType.Terran_Supply_Depot) && 
				!buildOrder.contains(UnitType.Terran_Supply_Depot) && 
				!areBeingBuilt.contains(UnitType.Terran_Supply_Depot))
			buildOrder.add(UnitType.Terran_Supply_Depot);
    }
    
    public void minerals() {
    	 //iterate through my units
        for (Unit myUnit : me.getUnits()) {
            //if there's enough minerals, train an SCV
            if (myUnit.getType() == UnitType.Terran_Command_Center && me.minerals() >= 50 && workers.size() < 15) {
                myUnit.train(UnitType.Terran_SCV);
            }
         }
    }
    
    public void extractorCheck() {
    	if(gasExtractors.isEmpty() && !areBeingBuilt.contains(UnitType.Terran_Refinery) && !buildOrder.contains(UnitType.Terran_Refinery))
    		buildOrder.add(UnitType.Terran_Refinery);
    }
    
    public void factories() {
    	if(me.minerals() > 300 && barrackCount < 3 && !buildOrder.contains(UnitType.Terran_Barracks) && !areBeingBuilt.contains(UnitType.Terran_Barracks)) {
			buildOrder.add(UnitType.Terran_Barracks);
		}
    }
    
    public void mineGas() {
    	List<Unit> candidateGasWorkers = new ArrayList<Unit>();
    	int nrOfGasMiners = 0;
    	for(Unit worker: workers) {
    		if(worker.isGatheringGas())
    			nrOfGasMiners++;
    		else
    			if(candidateGasWorkers.size() < 2 && worker.isGatheringMinerals()) {
    				candidateGasWorkers.add(worker);
    			}
    	}
    	for(int i = 0; i < 2 - nrOfGasMiners; i ++) {
    		candidateGasWorkers.get(i).gather(gasExtractors.iterator().next());
    	}
    	 
    	/*	Bad algo for getting workers off gas
    	 		for(Unit worker:workers)
    			if(worker.isGatheringGas() && nrOfGasMiners > 3) {
    				worker.stop();
    				nrOfGasMiners -= 1;
    			}
    	*/
    }
    
    public void evaluateGame() {
    	//this cancer timing tho
    	
    	//train another worker for minerals
    	long startTime, stopTime, duration;
    	startTime = System.nanoTime();
    	minerals();
    	stopTime = System.nanoTime();
    	game.drawTextScreen(10, 70, "builderMinerals: " + (stopTime - startTime) / 1000000);
    	
    	startTime = System.nanoTime();
    	extractorCheck();
    	stopTime = System.nanoTime();
    	game.drawTextScreen(10, 80, "builderExtractorCheckTimer: " + (stopTime - startTime) / 1000000);
    	
    	startTime = System.nanoTime();
    	//evaluate what buildings are being built at this point
    	refreshAreBeingBuiltSet();
    	stopTime = System.nanoTime();
    	game.drawTextScreen(10, 90, "builderRefresh: " + (stopTime - startTime) / 1000000);
    	
    	startTime = System.nanoTime();
    	evaluateTech();
    	stopTime = System.nanoTime();
    	game.drawTextScreen(10, 100, "builderEvaluateTech: " + (stopTime - startTime) / 1000000);
    	
    	startTime = System.nanoTime();
    	upgrades();
    	stopTime = System.nanoTime();
    	game.drawTextScreen(10, 110, "builderUpgrades: " + (stopTime - startTime) / 1000000);
    	
    	startTime = System.nanoTime();
    	factories();
    	stopTime = System.nanoTime();
    	game.drawTextScreen(10, 120, "builderFactories: " + (stopTime - startTime) / 1000000);
    	
    	startTime = System.nanoTime();
    	bunker();
    	stopTime = System.nanoTime();
    	game.drawTextScreen(10, 130, "builderBunker: " + (stopTime - startTime) / 1000000);
    	
    	startTime = System.nanoTime();
    	supply();
    	stopTime = System.nanoTime();
    	game.drawTextScreen(10, 140, "builderSupply: " + (stopTime - startTime) / 1000000);
    	
    	startTime = System.nanoTime();
    	buildFromStack();
    	stopTime = System.nanoTime();
    	game.drawTextScreen(10, 150, "builderBuildFromStack: " + (stopTime - startTime) / 1000000);
    	
    	startTime = System.nanoTime();
    	//worker orders
    	sendIdleMine();
    	stopTime = System.nanoTime();
    	game.drawTextScreen(10, 160, "builderSendIdleMine: " + (stopTime - startTime) / 1000000);
    	
    	//mineGas();	
    	
    }
    public void refreshAreBeingBuiltSet() {
    	areBeingBuilt.clear();
    	for(Unit worker : workers)
    		if(worker.getBuildType() != null)
    			areBeingBuilt.add(worker.getBuildType());
    }
}
