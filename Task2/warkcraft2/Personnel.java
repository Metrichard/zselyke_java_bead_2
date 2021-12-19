package warkcraft2;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Personnel extends Unit {

    private static final int ATTACK_WAIT_MINIMUM = 100;
    private static final int ATTACK_WAIT_MAXIMUM = 200;

    private AtomicInteger health;
    private final int attackMin;
    private final int attackMax;
    private Personnel opponent;
    private static final Random random = new Random();

    public Personnel(int health, Base owner, int attackMin, int attackMax, UnitType unitType) {
        super(owner, unitType);
        this.health = new AtomicInteger(health);
        this.attackMin = attackMin;
        this.attackMax = attackMax;
    }

    /**
     * Starts a fight with an enemy army.
     * If the personnel is still alive and there is an army to attack,
     * the personnel will randomly select an opponent and start attacking.
     *
     * @param enemyArmy The enemy personnel
     */
    public void startWar(List<Personnel> enemyArmy){
        if(health.get() <= 0 || enemyArmy.size() <= 0)
            return;
        while(health.get() > 0 && !enemyArmy.isEmpty()) {
            opponent = enemyArmy.get(random.nextInt(enemyArmy.size()));
            new Thread(this::startAttacking);
        }
    }

    /**
     * Starts attacking the current opponent.
     * Keeps attacking the enemy until either this personnel or the enemy personnel is dead.
     */
    private void startAttacking(){
        while (health.get() > 0 && opponent.getHealth() > 0){
            opponent.loseHealth(getAttack());
            try {
                TimeUnit.MILLISECONDS.sleep(random.nextInt(200) + 100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        opponent = null;
    }

    public int getHealth() {
        return health.get();
    }

    public void loseHealth(int amount) {
        this.health.set(this.health.get() - amount);
        if(this.health.get() <= 0){
            this.getOwner().signalPersonnelDeath(this);
        }
    }

    public int getAttack(){
        return random.nextInt(attackMax) + attackMin;
    }

}
