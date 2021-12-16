package warcraft;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Peasant extends Unit {
    private Boolean isMining;
    private Boolean isCuttingWood;
    private Boolean isTryingToBuild;

    public void StartMining() {
        if(isCuttingWood)
            isCuttingWood = false;
        else if(isTryingToBuild)
            isTryingToBuild = false;
        isMining = true;
        ScheduledExecutorService excutor = Executors.newScheduledThreadPool(1);
        excutor.scheduleAtFixedRate(this::Mine, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void Mine(){

    }

    public void startCuttingWood() {
        if(isMining)
            isMining = false;
        else if(isTryingToBuild)
            isTryingToBuild = false;
        isCuttingWood = true;
        ScheduledExecutorService excutor = Executors.newScheduledThreadPool(1);
        excutor.scheduleAtFixedRate(this::CutWood, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void CutWood() {

    }

    public void TryBuilding() {
        if(isMining)
            isMining = false;
        else if(isCuttingWood)
            isCuttingWood = false;
        isTryingToBuild = true;

        //TODO build building, resource checking, and stuff

    }
}
