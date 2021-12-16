package warcraft;

public enum UnitType {
    FARM(80,20,0,2000),
    LUMBERMILL(120,0,0,4000),
    BLACKSMITH(140,60,0,5000),
    PEASANT(75,0,1,1000);


    UnitType(int goldCost, int woodCost, int foodCost, int buildTime) {

    }
}
