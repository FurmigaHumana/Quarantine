package me.DDoS.Quarantine.zone.region;

import com.sk89q.worldedit.BlockVector;
import java.util.Iterator;
import me.DDoS.Quarantine.util.QUtil;
import me.DDoS.Quarantine.zone.location.BlockLocation;
import me.DDoS.Quarantine.zone.location.SpawnLocation;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/**
 *
 * @author DDoS 
 * 
 */
public class SubRegion {

    private final BlockLocation max;
    private final BlockLocation min;

    public SubRegion(World world, BlockVector pos1, BlockVector pos2) {

        this.max = new BlockLocation(world,
                Math.max(pos1.getBlockX(), pos2.getBlockX()),
                Math.max(pos1.getBlockY(), pos2.getBlockY()),
                Math.max(pos1.getBlockZ(), pos2.getBlockZ()));

        this.min = new BlockLocation(world, 
                Math.min(pos1.getBlockX(), pos2.getBlockX()),
                Math.min(pos1.getBlockY(), pos2.getBlockY()),
                Math.min(pos1.getBlockZ(), pos2.getBlockZ()));

    }

    public Iterator<SpawnLocation> spawnLocationIterator() {

        return new Iterator<SpawnLocation>() {

            private int nextX = min.getX();
            private int nextY = min.getY();
            private int nextZ = min.getZ();
            private final World world =  min.getWorld();

            @Override
            public boolean hasNext() {

                return nextX != Integer.MIN_VALUE;

            }

            @Override
            public SpawnLocation next() {

                if (!hasNext()) {

                    throw new java.util.NoSuchElementException();

                }

                SpawnLocation answer = new SpawnLocation(world, nextX, nextY, nextZ);

                while (!isSpawnable(answer)) {

                    increment();

                    if (nextX == Integer.MIN_VALUE) {

                        return null;

                    }

                    answer = new SpawnLocation(world, nextX, nextY, nextZ);

                }

                increment();
                return answer;

            }

            private void increment() {

                if (++nextX > max.getX()) {

                    nextX = min.getX();

                    if (++nextY > max.getY()) {

                        nextY = min.getY();

                        if (++nextZ > max.getZ()) {

                            nextX = Integer.MIN_VALUE;

                        }
                    }
                }
            }

            private boolean isSpawnable(SpawnLocation loc) {

                Block block = loc.getBlock();

                return ((block.getType().equals(Material.AIR) || block.getType().equals(Material.SNOW))
                        && QUtil.acceptsMobs(block.getRelative(BlockFace.DOWN))
                        && block.getRelative(BlockFace.UP).getType().equals(Material.AIR));

            }

            @Override
            public void remove() {

                throw new UnsupportedOperationException();

            }
        };
    }
}