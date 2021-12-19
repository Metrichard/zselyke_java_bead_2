package warcraft;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Peasant extends Unit {

    private static final int HARVEST_WAIT_TIME = 100;
    private static final int HARVEST_AMOUNT = 10;

    private AtomicBoolean isHarvesting = new AtomicBoolean(false);
    private AtomicBoolean isBuilding = new AtomicBoolean(false);

    private Peasant(Base owner) {
        super(owner, UnitType.PEASANT);
    }

    public static Peasant createPeasant(Base owner){
        return new Peasant(owner);
    }

    /**
     * Starts gathering gold.
     */
    public void startMining() {
        if(isHarvesting.get())
            return;
        System.out.println("Peasant starting mining");
        isHarvesting.set(true);
        Thread t = new Thread(() -> {
            try {
                while(isHarvesting.get()) {
                    TimeUnit.MILLISECONDS.sleep(HARVEST_WAIT_TIME);
                    getOwner().getResources().addGold(HARVEST_AMOUNT);
                }
                Thread.currentThread().interrupt();
            } catch (InterruptedException e) {
            }
        });
        t.start();
    }

    /**
     * Starts gathering wood.
     */
    public void startCuttingWood(){
        if(isHarvesting.get())
            return;
        System.out.println("Peasant starting cutting wood");
        isHarvesting.set(true);
        Thread t = new Thread(() -> {
            try {
                while(isHarvesting.get()) {
                    TimeUnit.MILLISECONDS.sleep(HARVEST_WAIT_TIME);
                    getOwner().getResources().addWood(HARVEST_AMOUNT);
                }
                Thread.currentThread().interrupt();
            } catch (InterruptedException e) {
            }
        });
        t.start();
    }

    /**
     * Peasant should stop all harvesting once this is invoked
     */
    public void stopHarvesting(){
        this.isHarvesting.set(false);
    }

    /**
     * Tries to build a certain type of building.
     * Can only build if there are enough gold and wood for the building
     * to be built.
     *
     * @param buildingType Type of the building
     * @return true, if the building process has started
     *         false, if there are insufficient resources
     */
    public boolean tryBuilding(UnitType buildingType){
        if(getOwner().getResources().canBuild(getOwner().getResources().getGold(), getOwner().getResources().getWood()))
        {
            Thread t = new Thread(() -> {
                startBuilding(buildingType);
            });
            t.start();
            return true;
        }
        return false;
    }

    /**
     * Start building a certain type of building.
     * Keep in mind that a peasant can only build one building at one time.
     *
     * @param buildingType Type of the building
     */
    private void startBuilding(UnitType buildingType){
        if(!isBuilding.get()) {
            isBuilding.set(true);
            getOwner().getResources().removeCost(buildingType.goldCost, buildingType.woodCost);
            Building.createBuilding(buildingType, getOwner());
            try {
                TimeUnit.MILLISECONDS.sleep(buildingType.buildTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        isBuilding.set(false);
    }

    /**
     * Determines if a peasant is free or not.
     * This means that the peasant is neither harvesting, nor building.
     *
     * @return Whether he is free
     */
    public boolean isFree(){
        return !isHarvesting.get() && !isBuilding.get();
    }


}
