package com.jedk1.jedcore.ability.earthbending;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.util.VersionUtil;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MetalHook extends MetalAbility implements AddonAbility {

	private long cooldown;
	private int range;
	private int maxhooks;
	private boolean nosource;

	private boolean canFly;
	private boolean hasFly;
	private boolean hasHook;
	private long time;

	private Location destination;

	private ConcurrentHashMap<Arrow, Boolean> hooks = new ConcurrentHashMap<Arrow, Boolean>();
	private List<UUID> hookIds = new ArrayList<UUID>();

	public MetalHook(Player player) {
		super(player);

		if (!bPlayer.canBend(this) || !bPlayer.canMetalbend()) {
			return;
		}

		if (hasAbility(player, MetalHook.class)) {
			MetalHook mh = (MetalHook) getAbility(player, MetalHook.class);
			mh.launchHook();
			return;
		}

		setFields();
		if (!hasRequiredInv()) return;
		canFly = player.getAllowFlight();
		hasFly = player.isFlying();
		player.setAllowFlight(true);
		start();
		launchHook();
	}

	public void setFields() {
		cooldown = JedCore.plugin.getConfig().getLong("Abilities.Earth.MetalHook.Cooldown");
		range = JedCore.plugin.getConfig().getInt("Abilities.Earth.MetalHook.Range");
		maxhooks = JedCore.plugin.getConfig().getInt("Abilities.Earth.MetalHook.MaxHooks");
		nosource = JedCore.plugin.getConfig().getBoolean("Abilities.Earth.MetalHook.RequireItem");
	}

	@Override
	public void progress() {
		if (player == null || !player.isOnline() || player.isDead()) {
			removeAllArrows();
			resetFlight();
			remove();
			return;
		}
		if (!bPlayer.canBendIgnoreBindsCooldowns(this) || hooks.isEmpty()) {
			removeAllArrows();
			resetFlight();
			remove();
			return;
		}
		if (player.isSprinting()) {
			removeAllArrows();
			resetFlight();
			remove();
			return;
		}
		if (player.isSneaking()) {
			player.setVelocity(new Vector());
			if (System.currentTimeMillis() > (time + 1000)) {
				removeAllArrows();
				resetFlight();
				remove();
				return;
			}
		} else {
			time = System.currentTimeMillis();
		}
		Vector target = new Vector();
		for (Arrow a : hooks.keySet()) {
			if (a != null) {
				if (a.isDead() || player.getWorld() != a.getWorld() || player.getLocation().add(0,1,0).distance(a.getLocation()) > range) {
					hooks.remove(a);
					hookIds.remove(a.getUniqueId());
					a.remove();
					continue;
				}
				Location loc = a.getLocation();
				Vector vec = a.getVelocity();
				Location loc2 = new Location(loc.getWorld(), loc.getX()+vec.getX(), loc.getY()+vec.getY(), loc.getZ()+vec.getZ());
				if (loc2.getBlock().getType() != Material.AIR) {
					hooks.replace(a, hooks.get(a), true);
					hasHook = true;
				} else {
					hooks.replace(a, hooks.get(a), false);
				}
				
				//Draws the particle lines.
				for (Location location : JCMethods.getLinePoints(player.getLocation().add(0, 1, 0), a.getLocation(), ((int) player.getLocation().add(0,1,0).distance(a.getLocation()) * 2))) {
					GeneralMethods.displayColoredParticle(location, "#CCCCCC");
				}
				if (hooks.get(a)) {
					target.add(GeneralMethods.getDirection(player.getLocation().add(0, 1, 0), a.getLocation()));
				}
			} else {
				hooks.remove(a);
			}
		}
		if (hasHook) {
			destination = player.getLocation().clone().add(target);
			//ParticleEffect.FLAME.display(0, 0, 0, 0, 1, destination, 257D);
			if (player.getLocation().distance(destination) > 2) {
				player.setFlying(false);
				double velocity = 0.8;
				player.setVelocity(target.clone().normalize().multiply(velocity));
			} else if (player.getLocation().distance(destination) < 2 && player.getLocation().distance(destination) >= 1) {
				player.setFlying(false);
				double velocity = 0.35;
				player.setVelocity(target.clone().normalize().multiply(velocity));
			} else {
				player.setVelocity(new Vector(0, 0, 0));
				if (player.getAllowFlight()) {
					player.setFlying(true);
				}
			}
		}
	}

	public void launchHook() {
		if (!hasRequiredInv()) return;

		Vector dir = GeneralMethods.getDirection(player.getEyeLocation(), VersionUtil.getTargetedLocation(player, range));

		if (!hookIds.isEmpty() && hookIds.size() > (maxhooks - 1)) {
			for (Arrow a : hooks.keySet()) {
				if (a.getUniqueId().equals(hookIds.get(0))) {
					hooks.remove(a);
					hookIds.remove(0);
					a.remove();
					break;
				}
			}
		}
		Arrow a = player.getWorld().spawnArrow(player.getEyeLocation().add(player.getLocation().getDirection().multiply(2)), dir, 3, 0f);
		a.setMetadata("metalhook", new FixedMetadataValue(JedCore.plugin, "1"));
		hooks.put(a, false);
		hookIds.add(a.getUniqueId());
	}

	public void removeAllArrows() {
		for (Arrow a : hooks.keySet()) {
			a.remove();
		}
	}

	public void resetFlight() {
		player.setAllowFlight(canFly);
		player.setFlying(hasFly);
	}

	public boolean hasRequiredInv() {
		if (nosource) return true;
		if (player.getInventory().getChestplate() != null) {
			Material[] chestplates = {Material.IRON_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE};
			Material playerChest = player.getInventory().getChestplate().getType();
			if (Arrays.asList(chestplates).contains(playerChest)) {
				return true;
			}
		}
		Material[] metals = {Material.IRON_INGOT, Material.IRON_BLOCK};
		for (ItemStack items : player.getInventory()) {
			if (items != null && Arrays.asList(metals).contains(items.getType())) {
				return true;
			}
		}
		return false;
	}

	public int getMaxHooks() {
		return this.maxhooks;
	}

	public void setMaxHooks(int maxhooks) {
		this.maxhooks = maxhooks;
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
		return "MetalHook";
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
		return "* JedCore Addon *\n" + JedCore.plugin.getConfig().getString("Abilities.Earth.MetalHook.Description");
	}

	@Override
	public void load() {
		return;
	}

	@Override
	public void stop() {
		return;
	}

	@Override
	public boolean isEnabled() {
		return JedCore.plugin.getConfig().getBoolean("Abilities.Earth.MetalHook.Enabled");
	}
}