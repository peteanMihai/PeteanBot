import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import java.util.logging.*;
public class ExampleBot extends DefaultBWListener {

	public  Logger logger = Logger.getLogger(ExampleBot.class.getName());
    private Mirror mirror = new Mirror();

    private Game game;
    
    private Player self;
    
    
    //battle logistics as of now
    private static Commander commander;
    
    //building stack & other stuff
    private static Builder builder;
    
    //strategy decider / ai? deprecated/unused
    private static StrategyController strategyController;
    
    private static EAController eaSquad;
    
    private static  Iterator<Individual> itIndividual;
    
    public static Individual individual;
    
    private boolean bScouted = false;
    
    //bad scouting strats
    private Unit scout = null;
    
    private ArrayList<Unit> bunkers = new ArrayList<Unit>();
    
    private Stack<TilePosition> startingLocations = new Stack<TilePosition>();

    public void run() {
    	mirror.getModule().setEventListener(this);
        mirror.startGame();	
    }
    

   
    
    public void scout() {
    	for(Unit myUnit: self.getUnits()) 
    		if(myUnit.getType().canMove() && scout == null) {
    			scout = myUnit;
    			scout.stop();
    			builder.workers.remove(scout);
    			logger.log(Level.INFO, myUnit.getID() + " started scouting!");
    		}
    	if(commander.enemyBuildingMemory.size() > 0) {
    		scout.move(self.getStartLocation().toPosition());
    		builder.workers.add(scout);
    		logger.log(Level.INFO, scout.getID() + " found the enemy!");
    		scout = null;
    		bScouted = true;
    		
    	}
    	if(scout == null)
    		return;
    	if(scout.isIdle())
    		scout.move(startingLocations.pop().toPosition());
    }

    
    

    public void evaluateGame() {
		try {
			builder.evaluateGame();
			commander.evaluateGame();
			strategyController.evaluateGame();
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
    }
    
    @Override
    public void onUnitCreate(Unit unit) {
    	if(unit.getType() == UnitType.Terran_Refinery)
    		builder.gasExtractors.add(unit);
    }
    
    @Override
    public void onUnitComplete(Unit unit) {
    	
        if(unit.getType().isBuilding()) {
            builder.areBeingBuilt.remove(unit);
            builder.occupiedLocations.clear();
        }
        if(unit.getType() == UnitType.Terran_SCV) {
        	builder.workers.add(unit);
        }
        if(unit.getType() == UnitType.Terran_Barracks)
        	builder.barrackCount ++;

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
    	 if(unit.getType() == UnitType.Terran_Refinery)
         	builder.gasExtractors.remove(unit);
    }

    @Override
    public void onStart() {
        game = mirror.getGame();
        for(Player a: game.getPlayers()) {
        	 logger.log(Level.INFO, "Player " + a.getName() + " is playing " + a.getRace());
        }
        game.setLocalSpeed(0);
        self = game.self();
        //Use BWTA to analyze map
        //This may take a few minutes if the map is processed first time!
        logger.log(Level.INFO, "Analyzing map...");
        BWTA.readMap();
        BWTA.analyze();
        logger.log(Level.INFO, "Map data ready");
        bScouted = false;
        scout = null;
        bunkers = new ArrayList<Unit>();
        startingLocations = new Stack<TilePosition>();
         
        logger.log(Level.INFO, "EASquad setting");
        if(itIndividual == null) {
        	SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
        	Timestamp ts = new Timestamp(System.currentTimeMillis());
        	logger.log(Level.INFO, "Logging EASquad to file");
        	try {
				eaSquad.writeResults("EASquadLog" + sdf.format(ts)+ ".txt");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				logger.log(Level.INFO, e.toString());
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				logger.log(Level.INFO, e.toString());
			}
        	logger.log(Level.INFO, "Trying EASquad Crossover");
        	eaSquad.crossOverPopulation();
        	itIndividual = eaSquad.population.iterator();
        }
        
        individual = itIndividual.next();
        logger.log(Level.INFO, "Got EASquad individual" + individual.unitsGenome.toString());
        //initialize commander
        commander = new Commander(game, self, individual.unitsGenome);
        //initialize builder
        builder = new Builder(game,self);
        //initialize strategy controller;
        strategyController = new StrategyController(game, self, builder, commander);
        builder.setCommander(commander);
        commander.setBuilder(builder);
        Builder.gas = 0;
        Builder.minerals = 0;
        //initialize starting locations for scout
        for(TilePosition location: game.getStartLocations()) {
    		if(location != self.getStartLocation())
    			startingLocations.push(location);
    	}
       
        logger.log(Level.INFO, "initialization complete");
    } 

    @Override
    public void onEnd(boolean isWinner) {
    	//code for measuring individuals, calculating fitness, etc.
    	individual.calculateFitness(0.2f, self.getBuildingScore(), self.getKillScore());
    }
    
    @Override
    public void onFrame() {
    	
    	//debug business
    	int gasMiners = 0;
    	for(Unit u: builder.workers) {
    		game.drawTextMap(u.getPosition(),"" + u.isIdle() + " " + u.getID());
    		if(u.isGatheringGas())
    			gasMiners++;
    	}
        game.drawTextScreen(10, 10, "Is supply blocked: " + (self.supplyUsed() >= self.supplyTotal()));
        game.drawTextScreen(10, 20, "Worker count: " + builder.workers.size());
        game.drawTextScreen(10, 30, "Squad size: " + commander.squad.size());
        game.drawTextScreen(10, 40, "buildOrder (" + builder.buildOrder.size() + ")" + builder.buildOrder);
    	game.drawTextScreen(10, 50, "beingBuilt: " + builder.areBeingBuilt);
    	game.drawTextScreen(10, 60, "gas minners: " + gasMiners);    	
        if(scout != null)
        	game.drawCircleMap(scout.getPosition(), 3, Color.Green);
       
        
        if(!bScouted)
        	this.scout();
        
        this.evaluateGame();
       
        //logger.log(Level.INFO, "evaluate game");
        //logger.log(Level.INFO, "trainingbois");
       	}


    
    public static void main(String[] args) {
    	eaSquad = new EAController(5, 0.1f);
    	itIndividual = eaSquad.population.iterator();
    	assert(itIndividual.hasNext());
        new ExampleBot().run();
    }
}