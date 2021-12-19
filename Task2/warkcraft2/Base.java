package warkcraft2;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Base {

    private static final int STARTER_PEASANT_NUMBER = 5;
    private static final int PEASANT_NUMBER_GOAL = 10;

    // lock to ensure only one unit can be trained at one time
    private final ReentrantLock trainingLock = new ReentrantLock();

    private final String name;
    private final Resources resources = new Resources();
    private final List<Peasant> peasants = Collections.synchronizedList(new LinkedList<>());
    private final List<Footman> footmen = Collections.synchronizedList(new LinkedList<>());
    private final List<Building> buildings = Collections.synchronizedList(new LinkedList<>());
    private final List<Personnel> army = Collections.synchronizedList(new LinkedList<>());

    public Base(String name){
        this.name = name;
        for (int i = 0; i < STARTER_PEASANT_NUMBER; i++) {
            Peasant peasant = Peasant.createPeasant(this);
            if(i < 3)
                peasant.startMining();
            else if(i < 4)
                peasant.startCuttingWood();
            peasants.add(peasant);
        }
    }

    private boolean hasAllBuildings(){
        return hasEnoughBuilding(UnitType.LUMBERMILL, 1) && hasEnoughBuilding(UnitType.BLACKSMITH, 1) && hasEnoughBuilding(UnitType.BARRACKS, 1) &&
                hasEnoughBuilding(UnitType.FARM, 1);
    }

    public void startPreparation(){
        new Thread(() -> {
            while (!hasAllBuildings()){
                if(!hasEnoughBuilding(UnitType.LUMBERMILL, 1)){
                    Peasant peasant = this.getFreePeasant();
                    if(peasant != null) {
                        peasant.tryBuilding(UnitType.LUMBERMILL);
                    }
                }
                if(!hasEnoughBuilding(UnitType.FARM, 3)){
                    Peasant peasant = this.getFreePeasant();
                    if(peasant != null) {
                        peasant.tryBuilding(UnitType.FARM);
                    }
                }
                if(!hasEnoughBuilding(UnitType.BARRACKS, 1)){
                    Peasant peasant = this.getFreePeasant();
                    if(peasant != null) {
                        peasant.tryBuilding(UnitType.BARRACKS);
                    }
                }
                if(!hasEnoughBuilding(UnitType.BLACKSMITH, 1)){
                    Peasant peasant = this.getFreePeasant();
                    if(peasant != null) {
                        peasant.tryBuilding(UnitType.BLACKSMITH);
                    }
                }
            }
        });
        AtomicInteger lumberCount = new AtomicInteger(0);
        AtomicInteger minerCount = new AtomicInteger(0);
        new Thread(() -> {
            while(resources.getCapacity() != PEASANT_NUMBER_GOAL){
                Peasant peasant = createPeasant();
                if(peasant == null)continue;
                if(lumberCount.get() < 1){
                    lumberCount.getAndIncrement();
                    peasant.startCuttingWood();
                }
                else if(minerCount.get() < 2){
                    minerCount.getAndIncrement();
                    peasant.startMining();
                }
                peasants.add(peasant);
            }
        });

        try {
            TimeUnit.SECONDS.sleep(20);
        } catch (InterruptedException e) {
        }

        for (int i = 0; i < peasants.size(); i++) {
            peasants.get(i).stopHarvesting();
        }

        System.out.println(this.name + " finished creating a base");
        System.out.println(this.name + " peasants: " + this.peasants.size());
        System.out.println(this.name + " footmen: " + this.footmen.size());
        for(Building b : buildings){
            System.out.println(this.name + " has a  " + b.getUnitType().toString());
        }
    }

    /**
     * Assemble the army - call the peasants and footmen to arms
     * @param latch
     */
    public void assembleArmy(CountDownLatch latch){
        army.addAll(peasants);
        army.addAll(footmen);
        System.out.println(this.name + " is ready for war");
        // the latch is used to keep track of both factions
        latch.countDown();
    }

    /**
     * Starts a war between the two bases.
     *
     * @param enemy Enemy base's personnel
     * @param warLatch Latch to make sure they attack at the same time
     */
    public void goToWar(List<Personnel> enemy, CountDownLatch warLatch){
        // This is necessary to ensure that both armies attack at the same time
        warLatch.countDown();
        try {
            // Waiting for the other army to be ready for war
            warLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // TODO Start attacking the enemy with every soldier on a separate thread
        // TODO Wait until the fight is resolved

        // If our army has no personnel, we failed
        if(army.isEmpty()){
            System.out.println(this.name + " has lost the fight");
        } else {
            System.out.println(this.name + " has won the fight");
        }
    }

    /**
     * Resolves the event when a personnel dies;
     * Remove it from the army and update the capacity.
     * @param p The fallen personnel
     */
    public void signalPersonnelDeath(Personnel p){
        resources.updateCapacity(-p.getUnitType().foodCost);
        switch (p.getUnitType()) {
            case PEASANT -> {
                peasants.remove((Peasant) p);
                army.remove(p);
            }
            case FOOTMAN -> {
                footmen.remove((Footman) p);
                army.remove(p);
            }
        }
        System.out.println(this.name + " has lost a " + p.getUnitType().toString());

    }

    /**
     * Returns a peasants that is currently free.
     * Being free means that the peasant currently isn't harvesting or building.
     *
     * @return Peasant object, if found one, null if there isn't one
     */
    private Peasant getFreePeasant(){
        for (int i = 0; i < peasants.size(); i++) {
            if(peasants.get(i).isFree())
                return peasants.get(i);
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
        try {
            if(trainingLock.tryLock() || trainingLock.tryLock(UnitType.PEASANT.buildTime, TimeUnit.MILLISECONDS)) {
                Peasant result;
                if (resources.canTrain(UnitType.PEASANT.goldCost, UnitType.PEASANT.woodCost, UnitType.PEASANT.foodCost)) {
                    sleepForMsec(UnitType.PEASANT.buildTime);
                    resources.removeCost(UnitType.PEASANT.goldCost, UnitType.PEASANT.woodCost);
                    resources.updateCapacity(UnitType.PEASANT.foodCost);
                    result = Peasant.createPeasant(this);
                    System.out.println(this.name + " created a peasant");
                    return result;
                }
            }
        } catch (InterruptedException e) {
        } finally {
            trainingLock.unlock();
        }
        return null;
    }

    private Footman createFootman(){
        try {
            if(trainingLock.tryLock() || trainingLock.tryLock(UnitType.FOOTMAN.buildTime, TimeUnit.MILLISECONDS)) {
                Footman result;
                if (resources.canTrain(UnitType.FOOTMAN.goldCost, UnitType.FOOTMAN.woodCost, UnitType.FOOTMAN.foodCost) &&
                        barracksIsBuilt()) {
                    sleepForMsec(UnitType.FOOTMAN.buildTime);
                    resources.removeCost(UnitType.FOOTMAN.goldCost, UnitType.FOOTMAN.woodCost);
                    resources.updateCapacity(UnitType.FOOTMAN.foodCost);
                    result = Footman.createFootman(this);

                    System.out.println(this.name + " created a footman");
                    return result;
                }
            }
        } catch (InterruptedException e) {
        }finally {
            trainingLock.unlock();
        }
        return null;
    }

    private Boolean barracksIsBuilt() {
        for (int i = 0; i < buildings.size(); i++) {
            if(buildings.get(i).getUnitType() == UnitType.BARRACKS)
                return true;
        }
        return false;
    }

    public Resources getResources(){
        return this.resources;
    }

    public List<Personnel> getArmy(){
        return this.army;
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
    private boolean hasEnoughBuilding(UnitType unitType, int required){
        int counter = 0;
        for (int i = 0; i < buildings.size(); i++) {
            if(buildings.get(i).getUnitType() == unitType)
                counter++;
        }
        return required == counter;
    }

    private static void sleepForMsec(int sleepTime) {
        try {
            TimeUnit.MILLISECONDS.sleep(sleepTime);
        } catch (InterruptedException e) {
        }
    }

}
