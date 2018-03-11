import java.io.FileNotFoundException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;

import com.google.gson.JsonObject;

import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

public class ExampleBot extends DefaultBWListener {

	
    private Mirror mirror = new Mirror();

    private Game game;
    
    private Player self;
    
    
    //battle logistics as of now
    private static Commander commander;
    
    //building stack & other stuff
    private static Builder builder;
    
    private boolean bScouted = false;
    
    //bad scouting strats
    private Unit scout = null;
    
    private ArrayList<Unit> bunkers = new ArrayList<Unit>();
    
    private Stack<TilePosition> startingLocations = new Stack<TilePosition>();

    public void run() {
    	mirror.getModule().setEventListener(this);
        mirror.startGame();	
    }
    
    private int cheapCounter = 0;
    //this is bad lul
    public void trainArmy() {
    	for(Unit myUnit: self.getUnits()) {
    		if(myUnit.getType() == UnitType.Terran_Barracks) {
    			cheapCounter ++;
    			if(cheapCounter < 4)
    				myUnit.train(UnitType.Terran_Marine);
    			else
    				myUnit.train(UnitType.Terran_Medic);
    			if(cheapCounter >= 4)
    				cheapCounter = 0;
    		}
    	}
    }
    
    public void scout() {
    	for(Unit myUnit: self.getUnits()) 
    		if(myUnit.getType().canMove() && scout == null) {
    			scout = myUnit;
    			scout.stop();
    			builder.workers.remove(scout);
    		}
    	if(commander.enemyBuildingMemory.size() > 0) {
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
    	for(Unit squadee: commander.squad) {
    		for(Unit bunker: bunkers)
    			commander.getInBunker(bunker, squadee);
    	}
    	
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
        if(unit.getType() == UnitType.Terran_Marine || unit.getType() == UnitType.Terran_Medic)
        	commander.squad.add(unit);
        if(unit.getType() == UnitType.Terran_Refinery)
        	builder.gasExtractors.add(unit);
        if(unit.getType() == UnitType.Terran_Bunker)
        	bunkers.add(unit);
    }
    
    @Override
    public void onUnitDestroy(Unit unit) {
    	if(unit.getPlayer().isEnemy(self))
    		if(unit.getType().isBuilding() || unit.getType().isWorker() || !unit.getType().canMove())
    			commander.enemyBuildingMemory.remove(unit.getPosition());
    	
    	
    	if(unit.getType() == UnitType.Terran_SCV) {
    		builder.workers.remove(unit);
        }
    	if(unit.getType() == UnitType.Terran_Barracks)
    		builder.barrackCount --;
    	 if(unit.getType() == UnitType.Terran_Marine) {
         	commander.squad.remove(unit);
    	 }
    	 if(unit.getType() == UnitType.Terran_Refinery)
         	builder.gasExtractors.remove(unit);
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
        
        bScouted = false;
        bunkers = new ArrayList<Unit>();
        startingLocations = new Stack<TilePosition>();
        
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
    	int gasMiners = 0;
    	for(Unit u: builder.workers) {
    		if(u.isGatheringGas())
    			gasMiners++;
    	}
        game.drawTextScreen(10, 10, "Is supply blocked: " + (self.supplyUsed() >= self.supplyTotal()));
        game.drawTextScreen(10, 20, "Worker count: " + builder.workers.size());
        game.drawTextScreen(10, 30, "Squad size: " + commander.squad.size());
        game.drawTextScreen(10, 40, "buildOrder: " + builder.buildOrder);
    	game.drawTextScreen(10, 50, "beingBuilt: " + builder.areBeingBuilt);
    	game.drawTextScreen(10, 60, "gas minners: " + gasMiners);    	
        if(scout != null)
        	game.drawCircleMap(scout.getPosition(), 3, Color.Green);
       
        if(builder.barrackCount > 0)
        	trainArmy();
        
        if(!bScouted)
        	this.scout();
        
        this.evaluateGame();
       
        //System.out.println("evaluate game");
        //System.out.println("trainingbois");
       	}


    
    public static void main(String[] args) {
        new ExampleBot().run();
    }
}