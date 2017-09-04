package com.jedk1.jedcore.ability.waterbending.combo;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.ability.waterbending.WaterBlast;
import com.jedk1.jedcore.util.RegenTempBlock;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.waterbending.OctopusForm;
import com.projectkorra.projectkorra.waterbending.Torrent;
import com.projectkorra.projectkorra.waterbending.util.WaterReturn;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class WaterGimbal extends WaterAbility implements AddonAbility, ComboAbility {

	private int sourcerange;
	private long cooldown;
	private double ringsize;
	private double range;
	private double damage;
	private int speed;
	private int animspeed;
	private boolean plant;
	private boolean requireAdjacentPlants;
	private boolean canUseBottle;
	
	private int step;
	private double velocity = 0.15;
	private boolean initializing;
	private boolean leftvisible = true;
	private boolean rightvisible = true;
	private boolean rightconsumed = false;
	private boolean leftconsumed = false;
	private Block sourceblock;
	private TempBlock source;
	private Location sourceloc;
	private Location origin1;
	private Location origin2;
	private boolean usingBottle;
	
	Random rand = new Random();

	public WaterGimbal(Player player) {
		super(player);
		if (!bPlayer.canBendIgnoreBinds(this)) {
			return;
		}
		if (isLunarEclipse(player.getWorld())) {
			return;
		}
		if (hasAbility(player, WaterGimbal.class)) {
			return;
		}
		setFields();
		usingBottle = false;
		if (grabSource()) {
			start();
			initializing = true;
			if (hasAbility(player, Torrent.class)) {
				((Torrent) getAbility(player, Torrent.class)).remove();
			}
			if (hasAbility(player, OctopusForm.class)) {
				((OctopusForm) getAbility(player, OctopusForm.class)).remove();
			}
		}
	}
	
	public void setFields() {
		sourcerange = JedCore.plugin.getConfig().getInt("Abilities.Water.WaterCombo.WaterGimbal.SourceRange");
		cooldown = JedCore.plugin.getConfig().getLong("Abilities.Water.WaterCombo.WaterGimbal.Cooldown");
		ringsize = JedCore.plugin.getConfig().getDouble("Abilities.Water.WaterCombo.WaterGimbal.RingSize");
		range = JedCore.plugin.getConfig().getDouble("Abilities.Water.WaterCombo.WaterGimbal.Range");
		damage = JedCore.plugin.getConfig().getDouble("Abilities.Water.WaterCombo.WaterGimbal.Damage");
		speed = JedCore.plugin.getConfig().getInt("Abilities.Water.WaterCombo.WaterGimbal.Speed");
		animspeed = JedCore.plugin.getConfig().getInt("Abilities.Water.WaterCombo.WaterGimbal.AnimationSpeed");
		plant = JedCore.plugin.getConfig().getBoolean("Abilities.Water.WaterCombo.WaterGimbal.PlantSource");
		requireAdjacentPlants = JedCore.plugin.getConfig().getBoolean("Abilities.Water.WaterCombo.WaterGimbal.RequireAdjacentPlants");
		canUseBottle = JedCore.plugin.getConfig().getBoolean("Abilities.Water.WaterCombo.WaterGimbal.BottleSource");
	}


	@Override
	public void progress() {
		if (player == null || player.isDead() || !player.isOnline() || !player.isSneaking()) {
			remove();
			return;
		}
		if (!bPlayer.canBendIgnoreBinds(this) || !bPlayer.canBendIgnoreCooldowns(getAbility("OctopusForm"))) {
			remove();
			return;
		}
		if (hasAbility(player, OctopusForm.class)) {
			((OctopusForm) getAbility(player, OctopusForm.class)).remove();
		}
		if (leftconsumed && rightconsumed) {
			remove();
			return;
		}
		if (!initializing) {
			getGimbalBlocks(player.getLocation());
			if (!leftvisible && !leftconsumed) {
				if (origin1.getBlockY() <= player.getEyeLocation().getBlockY()) {
					new WaterBlast(player, origin1, range, damage, speed, this);
					leftconsumed = true;
				}
			}

			if (!rightvisible && !rightconsumed) {
				if (origin2.getBlockY() <= player.getEyeLocation().getBlockY()) {
					new WaterBlast(player, origin2, range, damage, speed, this);
					rightconsumed = true;
				}
			}
		} else {
			Vector direction = GeneralMethods.getDirection(sourceloc, player.getEyeLocation());
			sourceloc = sourceloc.add(direction.multiply(1).normalize());

			if (source == null || !sourceloc.getBlock().getLocation().equals(source.getLocation())) {
				if (source != null) {
					source.revertBlock();
				}
				if (isTransparent(sourceloc.getBlock())) {
					source = new TempBlock(sourceloc.getBlock(), Material.STATIONARY_WATER, (byte) 8);
				}
			}

			if (source != null && source.getLocation().distance(player.getLocation()) < 2.5) {
				source.revertBlock();
				initializing = false;
			}
		}
	}

	private boolean grabSource() {
		sourceblock = BlockSource.getWaterSourceBlock(player, sourcerange, ClickType.SHIFT_DOWN, true, true, plant);
		if (sourceblock != null) {
			if (isPlant(sourceblock)) {
				if (!requireAdjacentPlants || JCMethods.isAdjacentToThreeOrMoreSources(sourceblock, sourceblock.getType())) {
					playFocusWaterEffect(sourceblock);
					sourceloc = sourceblock.getLocation();
					sourceblock.setType(Material.AIR);
					return true;
				}
			} else {
				if (GeneralMethods.isAdjacentToThreeOrMoreSources(sourceblock) || (TempBlock.isTempBlock(sourceblock) && WaterAbility.isBendableWaterTempBlock(sourceblock))) {
					playFocusWaterEffect(sourceblock);
					sourceloc = sourceblock.getLocation();
					sourceblock.setType(Material.AIR);
					return true;
				}
			}
		}

		// Try to use bottles if no source blocks nearby.
		if (canUseBottle && hasWaterBottle(player)){
			Location eye = player.getEyeLocation();
			Location forward = eye.clone().add(eye.getDirection());

			if (isTransparent(eye.getBlock()) && isTransparent(forward.getBlock())) {
				sourceloc = forward;
				sourceblock = sourceloc.getBlock();
				usingBottle = true;
				WaterReturn.emptyWaterBottle(player);
				return true;
			}
		}
		return false;
	}

	// Custom function to see if player has bottle.
	// This is to get around the WaterReturn limitation since OctopusForm will currently be using the bottle.
	private boolean hasWaterBottle(Player player) {
		PlayerInventory inventory = player.getInventory();
		if(inventory.contains(Material.POTION)) {
			ItemStack item = inventory.getItem(inventory.first(Material.POTION));
			PotionMeta meta = (PotionMeta)item.getItemMeta();
			return meta.getBasePotionData().getType().equals(PotionType.WATER);
		}

		return false;
	}

	public static void prepareBlast(Player player) {
		if (hasAbility(player, WaterGimbal.class)) {
			((WaterGimbal) getAbility(player, WaterGimbal.class)).prepareBlast();
			if (hasAbility(player, OctopusForm.class)) {
				((OctopusForm) getAbility(player, OctopusForm.class)).remove();
			}
		}
	}

	public void prepareBlast() {
		if (leftvisible) {
			leftvisible = false;
			return;
		}
		if (rightvisible) {
			rightvisible = false;
			return;
		}
	}

	private void getGimbalBlocks(Location location) {
		List<Block> ring1 = new ArrayList<Block>();
		List<Block> ring2 = new ArrayList<Block>();
		Location l = location.clone().add(0, 1, 0);
		int count = 0;

		while (count < animspeed) {
			boolean completed = false;
			double angle =  3.0 + this.step * this.velocity;
			double xRotation = Math.PI / 2.82 * 2.1;
			Vector v1 = new Vector(Math.cos(angle), Math.sin(angle), 0.0D).multiply(ringsize);
			Vector v2 = new Vector(Math.cos(angle), Math.sin(angle), 0.0D).multiply(ringsize);
			rotateAroundAxisX(v1, xRotation);
			rotateAroundAxisX(v2, -xRotation);
			rotateAroundAxisY(v1, -((location.getYaw() * Math.PI / 180)-1.575));
			rotateAroundAxisY(v2, -((location.getYaw() * Math.PI / 180)-1.575));

			if (!ring1.contains(l.clone().add(v1).getBlock()) && !leftconsumed) {
				completed = true;
				Block block = l.clone().add(v1).getBlock();
				if (isTransparent(block)) {
					ring1.add(block);
				} else {
					for (int i = 0; i < 4; i++) {
						if (isTransparent(block.getRelative(BlockFace.UP, i))) {
							ring1.add(block.getRelative(BlockFace.UP, i));
							break;
						}
					}
				}
			}

			if (!ring2.contains(l.clone().add(v2).getBlock()) && !rightconsumed) {
				completed = true;
				Block block = l.clone().add(v2).getBlock();
				if (isTransparent(block)) {
					ring2.add(block);
				} else {
					for (int i = 0; i < 4; i++) {
						if (isTransparent(block.getRelative(BlockFace.UP, i))) {
							ring2.add(block.getRelative(BlockFace.UP, i));
							break;
						}
					}
				}
			}

			if (completed) {
				count++;
			}

			if (leftconsumed && rightconsumed) {
				break;
			}

			this.step++;
		}

		if (!leftconsumed) {
			if (!ring1.isEmpty()) {
				Collections.reverse(ring1);
				origin1 = ring1.get(0).getLocation();
			}
			for (Block block : ring1) {
				//new TempBlock(block, Material.STATIONARY_WATER, (byte) 8);
				//revert.put(block, System.currentTimeMillis() + 150L);
				new RegenTempBlock(block, Material.STATIONARY_WATER, (byte) 8, 150L);
				if (rand.nextInt(10) == 0) {
					playWaterbendingSound(block.getLocation());
				}
			}
		}

		if (!rightconsumed) {
			if (!ring2.isEmpty()) {
				Collections.reverse(ring2);
				origin2 = ring2.get(0).getLocation();
			}
			for (Block block : ring2) {
				//new TempBlock(block, Material.STATIONARY_WATER, (byte) 8);
				//revert.put(block, System.currentTimeMillis() + 150L);
				new RegenTempBlock(block, Material.STATIONARY_WATER, (byte) 8, 150L);
				if (rand.nextInt(10) == 0) {
					playWaterbendingSound(block.getLocation());
				}
			}
		}
	}

	private Vector rotateAroundAxisX(Vector v, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double y = v.getY() * cos - v.getZ() * sin;
		double z = v.getY() * sin + v.getZ() * cos;
		return v.setY(y).setZ(z);
	}

	private Vector rotateAroundAxisY(Vector v, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double x = v.getX() * cos + v.getZ() * sin;
		double z = v.getX() * -sin + v.getZ() * cos;
		return v.setX(x).setZ(z);
	}

	@Override
	public void remove() {
		if (source != null) {
			source.revertBlock();
		}
		if (player.isOnline() && !initializing) {
			bPlayer.addCooldown(this);
		}

		if (usingBottle) {
			new WaterReturn(player, sourceblock);
		}
		super.remove();
	}
	
	public Player getPlayer() {
		return player;
	}
	
	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return "WaterGimbal";
	}
	
	@Override
	public boolean isHiddenAbility() {
		return true;
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isSneakAbility() {
		return false;
	}

	@Override
	public Object createNewComboInstance(Player player) {
		return new WaterGimbal(player);
	}

	@Override
	public ArrayList<AbilityInformation> getCombination() {
		ArrayList<AbilityInformation> combination = new ArrayList<>();
		combination.add(new AbilityInformation("Torrent", ClickType.SHIFT_DOWN));
		combination.add(new AbilityInformation("Torrent", ClickType.SHIFT_UP));
		combination.add(new AbilityInformation("Torrent", ClickType.SHIFT_DOWN));
		combination.add(new AbilityInformation("Torrent", ClickType.SHIFT_UP));
		combination.add(new AbilityInformation("OctopusForm", ClickType.SHIFT_DOWN));
		return combination;
	}

	@Override
	public String getInstructions() {
		return "Torrent (Tap Shift) > Torrent (Tap Shift) > OctopusForm (Hold Shift) > OctopusForm (Left Click Multiple times)";
	}

	@Override
	public String getDescription() {
	   return "* JedCore Addon *\n" + JedCore.plugin.getConfig().getString("Abilities.Water.WaterCombo.WaterGimbal.Description");
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
	public void load() {
	}

	@Override
	public void stop() {
	}
	
	@Override
	public boolean isEnabled() {
		return JedCore.plugin.getConfig().getBoolean("Abilities.Water.WaterCombo.WaterGimbal.Enabled");
	}
}