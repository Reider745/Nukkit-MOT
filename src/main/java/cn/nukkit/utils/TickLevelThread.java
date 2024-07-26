package cn.nukkit.utils;

import cn.nukkit.Server;
import cn.nukkit.level.Level;

public class TickLevelThread extends Thread {
    private final Server server;
    private final Level level;
    private long nextTick;
    private int tickCounter;
    private final boolean autoTickRate;
    private final int autoTickRateLimit, baseTickRate;

    public TickLevelThread(Server server, Level level){
        this.server = server;
        this.level = level;

        autoTickRate = server.getPropertyBoolean("auto-tick-rate", true);
        autoTickRateLimit = server.getPropertyInt("auto-tick-rate-limit", 20);
        baseTickRate = server.getPropertyInt("base-tick-rate", 1);
    }

    @Override
    public void run() {
        while (true){
            long tickTime = System.currentTimeMillis();

            long time = tickTime - this.nextTick;
            if (time < -25) {
                try {
                    Thread.sleep(Math.max(5, -time - 25));
                } catch (InterruptedException e) {
                    Server.getInstance().getLogger().logException(e);
                }
            }

            if ((tickTime - this.nextTick) < -25) {
                continue;
            }

            ++this.tickCounter;

            if (level.isBeingConverted || (level.getTickRate() > baseTickRate && --level.tickRateCounter > 0)) {
                continue;
            }

            try {
                long levelTime = System.currentTimeMillis();
                level.providerLock.readLock().lock();
                if (level.getProvider() == null) {//世界在其他线程上卸载
                    break;
                }

                level.doTick(this.tickCounter);
                int tickMs = (int) (System.currentTimeMillis() - levelTime);
                level.tickRateTime = tickMs;

                if (autoTickRate) {
                    if (tickMs < 50 && level.getTickRate() > baseTickRate) {
                        int r;
                        level.setTickRate(r = level.getTickRate() - 1);
                        if (r > baseTickRate) {
                            level.tickRateCounter = level.getTickRate();
                        }
                        server.getLogger().debug("Raising level \"" + level.getName() + "\" tick rate to " + level.getTickRate() + " ticks");
                    } else if (tickMs >= 50) {
                        if (level.getTickRate() == baseTickRate) {
                            level.setTickRate(Math.max(baseTickRate + 1, Math.min(this.autoTickRateLimit, tickMs / 50)));
                            server.getLogger().debug("Level \"" + level.getName() + "\" took " + tickMs + "ms, setting tick rate to " + level.getTickRate() + " ticks");
                        } else if ((tickMs / level.getTickRate()) >= 50 && level.getTickRate() < this.autoTickRateLimit) {
                            level.setTickRate(level.getTickRate() + 1);
                            server.getLogger().debug("Level \"" + level.getName() + "\" took " + tickMs + "ms, setting tick rate to " + level.getTickRate() + " ticks");
                        }
                        level.tickRateCounter = level.getTickRate();
                    }
                }
            } catch (Exception e) {
                server.getLogger().error(server.getLanguage().translateString("nukkit.level.tickError", new String[]{level.getFolderName(), Utils.getExceptionMessage(e)}));
            } finally {
                level.providerLock.readLock().unlock();
            }

            if ((this.nextTick - tickTime) < -1000) {
                this.nextTick = tickTime;
            } else {
                this.nextTick += 50;
            }
        }
    }

    public void load(){
        this.start();
    }
}
