package warcraft;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Base {
    private AtomicInteger numberOfMiners = new AtomicInteger(0);
    private AtomicInteger numberOfLumberjacks = new AtomicInteger(0);

    private static final int STARTER_PEASANT_NUMBER = 5;
    private static final int PEASANT_NUMBER_GOAL = 10;

    // lock to ensure only one unit can be trained at one time
    private final ReentrantLock trainingLock = new ReentrantLock();

    private final String name;
    private final Resources resources = new Resources();
    private final List<Peasant> peasants = Collections.synchronizedList(new LinkedList<>());
    private final List<Building> buildings = Collections.synchronizedList(new LinkedList<>());

    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*4);

    public Base(String name){
        this.name = name;
        for(int i = 0; i < STARTER_PEASANT_NUMBER; i++) {
            Peasant peasant = createPeasant();
            if(i < 3 && peasant != null)
                peasant.startMining();
            else if(i < 4 && peasant != null)
                peasant.startCuttingWood();

            if(peasant != null)
                peasants.add(peasant);
        }
    }

    public void startPreparation(){
        executorService.execute(() -> {
            try {
                buildingThread();
            } catch (InterruptedException e) {
            }
        });

        executorService.execute(this::peasantThread);

        boolean farms = hasEnoughBuilding(UnitType.FARM, 3);
        boolean lumbers = hasEnoughBuilding(UnitType.LUMBERMILL, 1);
        boolean smiths = hasEnoughBuilding(UnitType.BLACKSMITH, 1);
        while(!farms && !lumbers && !smiths)
        {
            farms = hasEnoughBuilding(UnitType.FARM, 3);
            lumbers = hasEnoughBuilding(UnitType.LUMBERMILL, 1);
            smiths = hasEnoughBuilding(UnitType.BLACKSMITH, 1);
            try {
                TimeUnit.MILLISECONDS.sleep(200);
            } catch (InterruptedException e) {
            }
        }

        while (!(peasants.size() == PEASANT_NUMBER_GOAL) ) {
            try {
                TimeUnit.MILLISECONDS.sleep(200);
            } catch (InterruptedException e) {
            }
        }

        for(Peasant peasant : peasants) {
            peasant.stopHarvesting();
        }

        System.out.println(this.name + " finished creating a base");
        System.out.println(this.name + " peasants: " + this.peasants.size());
        for (int i = 0, buildingsSize = buildings.size(); i < buildingsSize; i++) {
            Building b = buildings.get(i);
            System.out.println(this.name + " has a  " + b.getUnitType().toString());
        }
        System.exit(0);
    }

    private void peasantThread() {

        while(peasants.size() < PEASANT_NUMBER_GOAL) {
            if(resources.getCapacity() + UnitType.PEASANT.foodCost < resources.getCapacityLimit()) {
                Peasant peasant = Peasant.createPeasant(this);
                if (numberOfLumberjacks.get() < 1) {
                    peasant.startCuttingWood();
                    numberOfLumberjacks.addAndGet(1);
                } else if (numberOfMiners.get() < 2) {
                    peasant.startMining();
                    numberOfMiners.addAndGet(1);
                }
                peasants.add(peasant);
            }
        }
        System.out.println("\t\tPeasant thread done\n");
    }

    private void buildingThread() throws InterruptedException {
        while(!Thread.interrupted()) {
            if (!hasEnoughBuilding(UnitType.FARM, 3)) {
                Peasant peasant = getFreePeasant();
                if (peasant != null) {
                    peasant.tryBuilding(UnitType.FARM);
                }
            }
            else if (!hasEnoughBuilding(UnitType.LUMBERMILL, 1)) {
                Peasant peasant = getFreePeasant();
                if (peasant != null) {
                    peasant.tryBuilding(UnitType.LUMBERMILL);
                }
            }
            else if (!hasEnoughBuilding(UnitType.BLACKSMITH, 1)) {
                Peasant peasant = getFreePeasant();
                if (peasant != null) {
                    peasant.tryBuilding(UnitType.BLACKSMITH);
                }
            }
        }
    }

    /**
     * Returns a peasants that is currently free.
     * Being free means that the peasant currently isn't harvesting or building.
     *
     * @return Peasant object, if found one, null if there isn't one
     */
    private Peasant getFreePeasant() {
        for (int i = 0, peasantsSize = peasants.size(); i < peasantsSize; i++) {
            Peasant peasant = peasants.get(i);
            if (peasant.isFree())
                return peasant;
        }
        return null;
    }

    /**
     * Creates a peasant.
     * A peasant could only be trained if there are sufficient
     * gold, wood and food for him to train.
     *
     * At one time only one Peasant can be trained.
     *
     * @return The newly created peasant if it could be trained, null otherwise
     */
    private Peasant createPeasant(){
        Peasant result;
        if(resources.canTrain(UnitType.PEASANT.goldCost, UnitType.PEASANT.woodCost, UnitType.PEASANT.foodCost)) {
            try {
                if(trainingLock.tryLock() || trainingLock.tryLock(1500, TimeUnit.MILLISECONDS))
                {
                    sleepForMsec(UnitType.PEASANT.buildTime);
                    resources.removeCost(UnitType.PEASANT.goldCost, UnitType.PEASANT.woodCost);
                    resources.updateCapacity(UnitType.PEASANT.foodCost);
                    result = Peasant.createPeasant(this);
                    return result;
                }
            } catch (InterruptedException e) {
            }finally {
                trainingLock.unlock();
            }
        }
        return null;
    }

    public Resources getResources(){
        return this.resources;
    }

    public List<Building> getBuildings(){
        return this.buildings;
    }

    public String getName(){
        return this.name;
    }

    /**
     * Helper method to determine if a base has the required number of a certain building.
     *
     * @param unitType Type of the building
     * @param required Number of required amount
     * @return true, if required amount is reached (or surpassed), false otherwise
     */
    private synchronized boolean hasEnoughBuilding(UnitType unitType, int required){
        int counter = 0;
        for (int i = 0, buildingsSize = buildings.size(); i < buildingsSize; i++) {
            Building building = buildings.get(i);
            if (building.getUnitType() == unitType)
                counter++;
        }
        return counter == required;
    }

    private static void sleepForMsec(int sleepTime) {
        try {
            TimeUnit.MILLISECONDS.sleep(sleepTime);
        } catch (InterruptedException e) {
        }
    }

}
