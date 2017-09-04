package com.jedk1.jedcore.ability.earthbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.util.RegenTempBlock;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LavaAbility;
import com.projectkorra.projectkorra.firebending.util.FireDamageTimer;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LavaFlux extends LavaAbility implements AddonAbility {

	private int speed;
	private int range;
	private long cooldown;
	private long duration;
	private long cleanup;
	private double damage;
	private boolean wave;

	private Location location;
	private Vector direction;
	private Vector blockdirection;
	private int step;
	private int counter;
	private long time;
	private boolean complete;

	Random rand = new Random();

	private List<Location> flux = new ArrayList<Location>();

	public LavaFlux(Player player) {
		super(player);

		if (!bPlayer.canBend(this) || !bPlayer.canLavabend()) {
			return;
		}

		setFields();
		time = System.currentTimeMillis();
		if (prepareLine()) {
			bPlayer.addCooldown(this);
			start();
		}
	}

	public void setFields() {
		speed = JedCore.plugin.getConfig().getInt("Abilities.Earth.LavaFlux.Speed");
		if (speed < 1) speed = 1;
		range = JedCore.plugin.getConfig().getInt("Abilities.Earth.LavaFlux.Range");
		cooldown = JedCore.plugin.getConfig().getLong("Abilities.Earth.LavaFlux.Cooldown");
		duration = JedCore.plugin.getConfig().getLong("Abilities.Earth.LavaFlux.Duration");
		cleanup = JedCore.plugin.getConfig().getLong("Abilities.Earth.LavaFlux.Cleanup");
		damage = JedCore.plugin.getConfig().getDouble("Abilities.Earth.LavaFlux.Damage");
		wave = JedCore.plugin.getConfig().getBoolean("Abilities.Earth.LavaFlux.Wave");
	}

	@Override
	public void progress() {
		if (player == null || !player.isOnline()) {
			remove();
			return;
		}
		if (!bPlayer.canBendIgnoreCooldowns(this)) {
			remove();
			return;
		}
		counter++;
		if (!complete) {
			if (speed <= 1 || counter % speed == 0) {
				for (int i = 0; i <= 2; i++) {
					step++;
					progressFlux();
				}
			}
		} else {
			if (System.currentTimeMillis() > time + duration) {
				for (Location location : flux) {
					new RegenTempBlock(location.getBlock(), Material.STONE, (byte) 0, cleanup + rand.nextInt(1000));
				}
				remove();
				return;
			}
		}
	}

	private boolean prepareLine() {
		direction = player.getEyeLocation().getDirection().setY(0).normalize();
		blockdirection = this.direction.clone().setX(Math.round(this.direction.getX()));
		blockdirection = blockdirection.setZ(Math.round(direction.getZ()));
		Location origin = player.getLocation().add(0, -1, 0).add(blockdirection.multiply(2));
		if (isEarthbendable(player, origin.getBlock())) {
			BlockIterator bi = new BlockIterator(player.getWorld(), origin.toVector(), direction, 0, range);

			while (bi.hasNext()) {
				Block b = bi.next();

				if (b != null && b.getY() > 1 && b.getY() < 255) {
					if (isWater(b)) break;
					while (!isEarthbendable(player, b)) {
						b = b.getRelative(BlockFace.DOWN);
						if (b == null || b.getY() < 1 || b.getY() > 255) {
							break;
						}
						if (isEarthbendable(player, b)) {
							break;
						}
					}

					while (!isTransparent(b.getRelative(BlockFace.UP))) {
						b = b.getRelative(BlockFace.UP);
						if (b == null || b.getY() < 1 || b.getY() > 255) {
							break;
						}
						if (isEarthbendable(player, b.getRelative(BlockFace.UP))) {
							break;
						}
					}

					if (isEarthbendable(player, b)) {
						flux.add(b.getLocation());
						Block left = b.getRelative(getLeftBlockFace(GeneralMethods.getCardinalDirection(blockdirection)), 1);
						expand(left);
						Block right = b.getRelative(getLeftBlockFace(GeneralMethods.getCardinalDirection(blockdirection)).getOppositeFace(), 1);
						expand(right);
					} else {
						break;
					}
				}
			}
			return true;
		}
		return false;
	}

	private void progressFlux() {
		for (Location location : flux) {
			if (flux.indexOf(location) <= step) {
				new RegenTempBlock(location.getBlock(), Material.STATIONARY_LAVA, (byte) 1, duration + cleanup);
				this.location = location;
				if (flux.indexOf(location) == step) {
					Block above = location.getBlock().getRelative(BlockFace.UP);
					ParticleEffect.LAVA.display((float) Math.random(), (float) Math.random(), (float) Math.random(), 0f, 2, above.getLocation(), 257D);
					applyDamageFromWave(above.getLocation());
					if (wave) {
						if (isTransparent(above)) {
							new RegenTempBlock(location.getBlock().getRelative(BlockFace.UP), Material.STATIONARY_LAVA, (byte) 1, (speed * 150));
						}
					}
				}
			}
		}
		if (step >= flux.size()) {
			wave = false;
			complete = true;
			time = System.currentTimeMillis();
		}
	}
	
	private void applyDamageFromWave(Location location) {
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1.5)) {
			if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId()) {
				DamageHandler.damageEntity(entity, damage, this);
				new FireDamageTimer(entity, player);
			}
		}
	}

	private void expand(Block block) {
		if (block != null && block.getY() > 1 && block.getY() < 255) {
			if (isWater(block)) return;
			while (!isEarthbendable(block)) {
				block = block.getRelative(BlockFace.DOWN);
				if (block == null || block.getY() < 1 || block.getY() > 255) {
					break;
				}
				if (isEarthbendable(block)) {
					break;
				}
			}

			while (!isTransparent(block.getRelative(BlockFace.UP))) {
				block = block.getRelative(BlockFace.UP);
				if (block == null || block.getY() < 1 || block.getY() > 255) {
					break;
				}
				if (isEarthbendable(block.getRelative(BlockFace.UP))) {
					break;
				}
			}

			if (isEarthbendable(block)) {
				flux.add(block.getLocation());
			} else {
				return;
			}
		}
	}

	public BlockFace getLeftBlockFace(BlockFace forward) {
		switch (forward) {
			case NORTH:
				return BlockFace.WEST;
			case SOUTH:
				return BlockFace.EAST;
			case WEST:
				return BlockFace.SOUTH;
			case EAST:
				return BlockFace.NORTH;
			case NORTH_WEST:
				return BlockFace.SOUTH_WEST;
			case NORTH_EAST:
				return BlockFace.NORTH_WEST;
			case SOUTH_WEST:
				return BlockFace.SOUTH_EAST;
			case SOUTH_EAST:
				return BlockFace.NORTH_EAST;

			default:
				return BlockFace.NORTH;
		}
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public String getName() {
		return "LavaFlux";
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public String getAuthor() {
		return JedCore.dev;
	}

	@Override
	public String getVersion() {
		return JedCore.version;
	}

	@Override
	public String getDescription() {
		return "* JedCore Addon *\n" + JedCore.plugin.getConfig().getString("Abilities.Earth.LavaFlux.Description");
	}

	@Override
	public void load() {
		if (JedCore.plugin.getConfig().get("Abilities.Earth.LavaFlux.Speed") instanceof String) {
			JedCore.plugin.getConfig().set("Abilities.Earth.LavaFlux.Speed", 1);
			JedCore.plugin.saveConfig();
			JedCore.plugin.reloadConfig();
		}
		return;
	}

	@Override
	public void stop() {
		return;
	}

	@Override
	public boolean isEnabled() {
		return JedCore.plugin.getConfig().getBoolean("Abilities.Earth.LavaFlux.Enabled");
	}
}