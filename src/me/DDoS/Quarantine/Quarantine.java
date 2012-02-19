package me.DDoS.Quarantine;

import me.DDoS.Quarantine.leaderboard.Leaderboard;
import me.DDoS.Quarantine.zone.ZoneLoader;
import me.DDoS.Quarantine.listener.*;
import me.DDoS.Quarantine.zone.Zone;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import me.DDoS.Quarantine.gui.*;
import me.DDoS.Quarantine.permissions.Permissions;
import me.DDoS.Quarantine.permissions.PermissionsHandler;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author DDoS
 */
public class Quarantine extends JavaPlugin {

    public static final Logger log = Logger.getLogger("Minecraft");
    //
    private boolean WGOn = false;
    //
    private final Map<String, Zone> zones = new HashMap<String, Zone>();
    //
    private FileConfiguration config;
    //
    private Permissions permissions;
    //
    private GUIHandler guiHandler;

    public Quarantine() {

        checkForLibs();

    }

    @Override
    public void onEnable() {

        CommandExecutor ce = new QCommandExecutor(this);
        getCommand("qload").setExecutor(ce);
        getCommand("qunload").setExecutor(ce);
        getCommand("qrespawnmobs").setExecutor(ce);
        getCommand("qconvertinv").setExecutor(ce);
        getCommand("qjoin").setExecutor(ce);
        getCommand("qenter").setExecutor(ce);
        getCommand("qsetlobby").setExecutor(ce);
        getCommand("qsetentrance").setExecutor(ce);
        getCommand("qleave").setExecutor(ce);
        getCommand("qmoney").setExecutor(ce);
        getCommand("qkeys").setExecutor(ce);
        getCommand("qscore").setExecutor(ce);
        getCommand("qrank").setExecutor(ce);
        getCommand("qtop").setExecutor(ce);
        getCommand("qzones").setExecutor(ce);
        getCommand("qplayers").setExecutor(ce);

        config = getConfig();

        setupLeadboards();

        checkForWorldGuard();

        if (checkForSpout()) {

            guiHandler = new SpoutEnabledGUIHandler(this);

        } else {

            guiHandler = new TextGUIHandler(this);

        }

        permissions = new PermissionsHandler(this).getPermissions();

        loadStartUpZones();

        getServer().getPluginManager().registerEvents(new QListener(this), this);

        log.info("[Quarantine] Plugin enabled. v" + getDescription().getVersion() + ", by DDoS");

    }

    @Override
    public void onDisable() {

        unLoadAllZones();
        zones.clear();
        log.info("[Quarantine] Plugin disabled. v" + getDescription().getVersion() + ", by DDoS");

    }

    public Collection<Zone> getZones() {

        return zones.values();

    }

    public boolean hasZone(String zoneName) {

        return zones.containsKey(zoneName);

    }

    public boolean hasZones() {

        return zones.isEmpty();

    }

    public Zone getZone(String zoneName) {

        return zones.get(zoneName);

    }

    public Zone addZone(String zoneName, Zone zone) {

        return zones.put(zoneName, zone);

    }

    public void removeZone(String zoneName) {

        zones.remove(zoneName);

    }

    public boolean isWGOn() {

        return WGOn;

    }

    public Permissions getPermissions() {

        return permissions;

    }

    public FileConfiguration getConfigFile() {

        return config;

    }

    public GUIHandler getGUIHandler() {

        return guiHandler;

    }

    private void checkForWorldGuard() {

        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

        if (plugin != null && plugin instanceof WorldGuardPlugin) {

            log.info("[Quarantine] WorldGuard detected.");
            WGOn = true;

        } else {

            log.info("[Quarantine] No WorldGuard detected. This plugin will not work.");
            WGOn = false;

        }
    }

    private boolean checkForSpout() {

        PluginManager pm = getServer().getPluginManager();
        Plugin plugin = pm.getPlugin("Spout");

        if (plugin != null) {

            log.info("[Quarantine] Spout detected. Spout GUI enabled.");
            return true;

        } else {

            log.info("[Quarantine] No Spout detected. Spout GUI disabled.");
            return false;

        }
    }

    public WorldGuardPlugin getWorldGuard() {

        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

        if (plugin != null && plugin instanceof WorldGuardPlugin) {

            return (WorldGuardPlugin) plugin;

        }

        return null;

    }

    private void unLoadAllZones() {

        for (Zone zone : zones.values()) {

            unloadZone(zone);

        }
    }

    public void unloadZone(Zone zone) {

        if (!WGOn) {
            return;
        }

        removePlayers(zone);
        saveZoneLocations(zone);
        stopMobCheckTask(zone);
        disconnectLB(zone);

    }

    private void saveZoneLocations(Zone zone) {

        zone.saveLocations(config);

    }

    private void stopMobCheckTask(Zone zone) {

        zone.stopMobCheckTask(getServer());

    }

    private void removePlayers(Zone zone) {

        zone.removeAllPlayers();

    }

    private void disconnectLB(Zone zone) {

        zone.disconnectLB();

    }

    private void loadStartUpZones() {

        if (!WGOn) {

            return;

        }

        for (String zoneToLoad : config.getStringList("Load_on_start")) {

            ZoneLoader loader = new ZoneLoader();
            Zone zone = loader.loadZone(this, zoneToLoad);

            if (zone == null) {

                log.info("[Quarantine] Couldn't load zone " + zoneToLoad + " on start up.");
                return;

            }

            zones.put(zoneToLoad, zone);

            log.info("[Quarantine] Loaded zone " + zoneToLoad + ".");

        }
    }

    private void setupLeadboards() {

        if (!config.getBoolean("Leaderboards.enabled")) {

            return;

        }

        Leaderboard.USE = true;
        String type = config.getString("Leaderboards.type", "invalid");

        if (type.equalsIgnoreCase("redis")) {

            Leaderboard.TYPE = "redis";
            Leaderboard.HOST = config.getString("Leaderboards.redis_db_info.host");
            Leaderboard.PORT = config.getInt("Leaderboards.redis_db_info.port");


        } else if (type.equalsIgnoreCase("mysql")) {

            Leaderboard.TYPE = "mysql";
            Leaderboard.HOST = config.getString("Leaderboards.mysql_db_info.host");
            Leaderboard.DB_NAME = config.getString("Leaderboards.mysql_db_info.name");
            Leaderboard.PORT = config.getInt("Leaderboards.mysql_db_info.port");
            Leaderboard.USER = config.getString("Leaderboards.mysql_db_info.user");
            Leaderboard.PASSWORD = config.getString("Leaderboards.mysql_db_info.password");

        } else {

            Leaderboard.USE = false;

        }
    }

    private void checkForLibs() {

        if (!new File("plugins/Quarantine/lib").exists()) {

            new File("plugins/Quarantine/lib").mkdir();

        }

        if (!new File("plugins/Quarantine/lib/jedis-2.0.0.jar").exists()) {

            log.info("[Quarantine] Downloading 'jedis-2.0.0.jar' library.");

            if (downloadJedisLib()) {

                log.info("[Quarantine] Downloading done.");

            }
        }
    }

    private boolean downloadJedisLib() {

        try {

            URL url = new URL("http://dl.dropbox.com/u/43006973/jedis-2.0.0.jar");
            URLConnection con = url.openConnection();
            DataInputStream dis = new DataInputStream(con.getInputStream());
            byte[] fileData = new byte[con.getContentLength()];

            for (int x = 0; x < fileData.length; x++) {

                fileData[x] = dis.readByte();

            }

            dis.close();
            FileOutputStream fos = new FileOutputStream(new File("plugins/Quarantine/lib/jedis-2.0.0.jar"));
            fos.write(fileData);
            fos.close();
            return true;

        } catch (Exception ex) {

            log.info("[Quarantine] Couldn't download 'jedis-2.0.0.jar' library. Error: " + ex.getMessage());
            return false;

        }
    }
}
