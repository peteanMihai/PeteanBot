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
    
    
    //battle logistics as of now
    private Commander commander;
    
    //building stack & other stuff
    private Builder builder;
    
    //bad global checks
    private boolean bOneExtractor = false;
    
    
    private boolean bScouted = false;
    
    //bad scouting strats
    private Unit scout = null;
    
    private HashSet<Position> enemyBuildingMemory = new HashSet<Position>();
    
    private HashSet<Unit> gasExtractors = new HashSet<Unit>();
    private Stack<TilePosition> startingLocations = new Stack<TilePosition>();

    public void run() {
    	mirror.getModule().setEventListener(this);
        mirror.startGame();	
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
    			builder.workers.remove(scout);
    		}
    	if(enemyBuildingMemory.size() > 0) {
    		scout.move(self.getStartLocation().toPosition());
    		builder.workers.add(scout);
    		scout = null;
    		bScouted = true;
    	}
    	if(scout.isIdle())
    		scout.move(startingLocations.pop().toPosition());
    }

    
    

    public void evaluateGame() {
		builder.evaluateGame();
    	commander.evaluateGame();
    
    	
    }
    
    @Override
    public void onUnitCreate(Unit unit) {
    	
    }
    
    @Override
    public void onUnitComplete(Unit unit) {
        if(unit.getType() == UnitType.Terran_SCV) {
        	builder.workers.add(unit);
        }
        if(unit.getType() == UnitType.Terran_Barracks)
        	builder.barrackCount ++;
        if(unit.getType() == UnitType.Terran_Marine)
        	commander.squad.add(unit);
        if(unit.getType() == UnitType.Terran_Refinery)
        	gasExtractors.add(unit);
    }
    
    @Override
    public void onUnitDestroy(Unit unit) {
    	if(unit.getType() == UnitType.Terran_SCV) {
    		builder.workers.remove(unit);
        }
    	if(unit.getType() == UnitType.Terran_Barracks)
    		builder.barrackCount --;
    	 if(unit.getType() == UnitType.Terran_Marine) {
         	commander.squad.remove(unit);
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

        //initialize commander
        commander = new Commander(game, self);
        //initialize builder
        builder = new Builder(game,self);
        
        
        //initialize starting locations for scout
        for(TilePosition location: game.getStartLocations()) {
    		if(location != self.getStartLocation())
    			startingLocations.push(location);
    	}
       
        System.out.println("initialization complete");
    }

    @Override
    public void onFrame() {
    	
    	//debug business
        game.drawTextScreen(10, 10, "Is supply blocked: " + (self.supplyUsed() >= self.supplyTotal()));
        game.drawTextScreen(10, 20, "Worker count: " + builder.workers.size());
        game.drawTextScreen(10, 30, "Squad size: " + commander.squad.size());
        if(scout != null)
        	game.drawCircleMap(scout.getPosition(), 3, Color.Green);
       
        this.evaluateGame();
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
        //System.out.println("evaluate game");
        for (Unit u : game.enemy().getUnits()) {
        	//if this unit is in fact a building
        	if (u.getType().isBuilding()) {
        		//check if we have it's position in memory and add it if we don't
        		if (!enemyBuildingMemory.contains(u.getPosition())) enemyBuildingMemory.add(u.getPosition());
        	}
        }
        //System.out.println("trainingbois");
        //iterate through my units
        for (Unit myUnit : self.getUnits()) {
            //if there's enough minerals, train an SCV
            if (myUnit.getType() == UnitType.Terran_Command_Center && self.minerals() >= 50 && builder.workers.size() < 15) {
                myUnit.train(UnitType.Terran_SCV);
            }
         }
        
        if(!bScouted)
        	this.scout();
        builder.sendIdleMine();
        if(builder.barrackCount > 0)
        	trainMarines();
        commander.sendMarines(commander.squad, enemyBuildingMemory);
        //draw my units on screen
        //game.drawTextScreen(10, 25, units.toString());
    }


    
    public static void main(String[] args) {
        new ExampleBot().run();
    }
}