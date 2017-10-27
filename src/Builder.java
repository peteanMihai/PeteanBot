import java.util.ArrayDeque;
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
    public ArrayDeque<UnitType> buildOrder = new ArrayDeque<UnitType>();
	//had to give access
	public HashSet<Unit> workers = new HashSet<Unit>();
	public int barrackCount = 0;
	public Builder(Game game, Player me) {
		this.game = game;
		this.me = me;		
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
    			System.out.println(building + " " + place + " " + myUnit.getType()+ " is building: " + myUnit.getBuildType());
    			break;
    		}
    }
    
    public void initBuildStack() {
    	buildOrder.addFirst(UnitType.Terran_Refinery);
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
	    while(buildOrder.size() > 0)
	    	if(me.minerals() > buildOrder.peekLast().mineralPrice()) {
	    		buildClose(buildOrder.removeLast(), me.getStartLocation());
	    	}
	    	else
	    		break;
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
    	if(buildOrder.contains(UnitType.Terran_Bunker) || game.elapsedTime() < 60)
			return;
		buildOrder.add(UnitType.Terran_Bunker);
    }
    
    public void evaluateTech() {
    	boolean bHaveEngineeringBay = false;
		for(Unit myUnit: me.getUnits()) {
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
    }
    
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
				!buildOrder.contains(UnitType.Terran_Supply_Depot))
			buildOrder.addFirst(UnitType.Terran_Supply_Depot);
    }
    
    public void factories() {
    	boolean bHavebarrack = false;
		for(Unit myUnit: me.getUnits()) {
			if(myUnit.getBuildType() == UnitType.Terran_Barracks)
			bHavebarrack = true;
		}
    	if(me.minerals() > 300 && barrackCount < 3 && !buildOrder.contains(UnitType.Terran_Barracks)) {
			buildOrder.add(UnitType.Terran_Barracks);
		}
    }
    
    public void evaluateGame() {
    	
    	//initial buildOrderStackState?
    	int buildOrderLength = buildOrder.size();
    	upgrades();
    	factories();
    	bunker();
    	supply();
    	this.buildFromStack();
    	System.out.println("" + this.buildOrder);
    }
    
}
