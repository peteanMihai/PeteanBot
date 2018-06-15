import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
      
    private ArrayList<Unit> bunkers = new ArrayList<Unit>();
    
    public static Stack<TilePosition> startingLocations = new Stack<TilePosition>();

    public void run() {
    	mirror.getModule().setEventListener(this);
        mirror.startGame();	
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
        try{
        	
        	BWTA.readMap();
        	BWTA.analyze();
        }
        catch(Exception e) {
        	game.leaveGame();
        }
        
        logger.log(Level.INFO, "Map data ready");
       
        bunkers = new ArrayList<Unit>();
        startingLocations = new Stack<TilePosition>();
        establishLocations(game);
        logger.log(Level.INFO, "EASquad setting");
        if(itIndividual == null || itIndividual.hasNext() == false) {
        	
        	logger.log(Level.INFO, "Logging EASquad to file");
        	try {
				eaSquad.writeResults(eaSquad.name);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.log(Level.INFO, e.toString());
			}
        	logger.log(Level.INFO, "Trying EASquad Crossover");
        	eaSquad.crossOverPopulation();
        	logger.log(Level.INFO, "Done EASquad Crossover");
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

        logger.log(Level.INFO, "initialization complete");
    } 

    public static void establishLocations(Game game) {
        for(TilePosition location: game.getStartLocations()) {
    		//if(location != self.getStartLocation())
    			startingLocations.push(location);
    	}
       
    }
    
    @Override
    public void onEnd(boolean isWinner) {
    	//code for measuring individuals, calculating fitness, etc.
    	boolean won = self.isVictorious();
    	individual.calculateFitness(0.2f, self.getBuildingScore(), self.getKillScore(), game.elapsedTime(), won);
    	PrintWriter writer;
		try {
			String toPrint = "";
			writer = new PrintWriter(new BufferedWriter(new FileWriter(eaSquad.name, true)));
			toPrint += (individual.unitsGenome.toString() + " FITNESS: " + (int)individual.fitnessScore + " TIME ELAPSED: " + game.elapsedTime());
			if(won)
				toPrint += " WON GAME ";
			writer.println(toPrint);
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
    	reset();
    }
    
    public void reset() {
    	BWTA.cleanMemory();
    	commander.reset();
    	builder.reset();
    	commander = null;
    	builder = null;
    	strategyController  = null;
    	bunkers = new ArrayList<Unit>();
    	startingLocations = new Stack<TilePosition>();
    }
    
    public void displayDebugInformation() {
    	int gasMiners = 0;
    	for(Unit u: builder.workers) {
    		game.drawTextMap(u.getPosition(),"" + u.isIdle() + " " + u.getID());
    		if(u.isGatheringGas())
    			gasMiners++;
    	}
    	//debug business
        game.drawTextScreen(10, 10, "Is supply blocked: " + (self.supplyUsed() >= self.supplyTotal()));
        game.drawTextScreen(10, 20, "Worker count: " + builder.workers.size());
        game.drawTextScreen(10, 30, "Squad size: " + commander.squad.size());
        game.drawTextScreen(10, 40, "buildOrder (" + builder.buildOrder.size() + ")" + builder.buildOrder);
    	game.drawTextScreen(10, 50, "beingBuilt: " + builder.areBeingBuilt);
    	game.drawTextScreen(10, 60, "gas minners: " + gasMiners);    	
        if(commander.scout != null)
        	game.drawCircleMap(commander.scout.getPosition(), 3, Color.Green);
    }
    
    @Override
    public void onFrame() { 
        this.evaluateGame();
    }

    public void evaluateGame() {
    	displayDebugInformation();
    	if(game.elapsedTime() > 7200)
    		game.leaveGame();
			builder.evaluateGame();
			commander.evaluateGame();
			strategyController.evaluateGame();	
    }
    
    public static void main(String[] args) {
    	eaSquad = new EAController(0.5f);
    	itIndividual = eaSquad.population.iterator();
    	assert(itIndividual.hasNext());
        new ExampleBot().run();
    }
}