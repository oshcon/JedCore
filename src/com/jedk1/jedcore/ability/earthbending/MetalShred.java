package com.jedk1.jedcore.ability.earthbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.util.VersionUtil;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class MetalShred extends MetalAbility implements AddonAbility {

	private int selectRange;
	private int extendTick;
	private double damage;

	private boolean horizontal = false;
	private boolean started = false;
	private boolean stop = false;
	private boolean stopCoil = false;
	private boolean extending = false;
	private int length = 0;
	private int fullLength = 0;
	private long lastExtendTime;
	private Block source;
	private Block lastBlock;
	private List<TempBlock> tblocks = new ArrayList<TempBlock>();

	public MetalShred(Player player) {
		super(player);

		if (hasAbility(player, MetalShred.class)) {
			((MetalShred) getAbility(player, MetalShred.class)).remove();
		}

		if (!bPlayer.canBend(this)) {
			return;
		}

		setFields();

		if (selectSource()) {
			if (horizontal) {
				raiseBlock(source, GeneralMethods.getDirection(player.getLocation(), source.getLocation()));
			} else {
				shiftBlock(source, GeneralMethods.getDirection(player.getLocation(), source.getLocation()));
			}

			start();
		}
	}
	
	public void setFields() {
		selectRange = JedCore.plugin.getConfig().getInt("Abilities.Earth.MetalShred.SourceRange");
		extendTick = JedCore.plugin.getConfig().getInt("Abilities.Earth.MetalShred.ExtendTick");
		damage = JedCore.plugin.getConfig().getDouble("Abilities.Earth.MetalShred.Damage");
	}

	public boolean selectSource() {
		Block b = BlockSource.getEarthSourceBlock(player, selectRange, ClickType.SHIFT_DOWN);

		if (b == null || !isMetal(b))
			return false;

		source = b;

		if (source.getRelative(BlockFace.UP).getType() == Material.AIR)
			horizontal = true;

		return true;
	}

	@SuppressWarnings("deprecation")
	public void raiseBlock(Block b, Vector d) {
		Block up = b.getRelative(BlockFace.UP);
		Block away = b.getRelative(GeneralMethods.getCardinalDirection(d));
		Block awayup = away.getRelative(BlockFace.UP);
		Block deeperb = b.getRelative(BlockFace.DOWN);
		Block deepera = away.getRelative(BlockFace.DOWN);

		for (TempBlock tb : tblocks) {
			if (tb.getBlock().getType() != Material.AIR)
				tb.setType(Material.AIR);
		}

		if (!up.getType().isSolid()) {
			TempBlock tbu = new TempBlock(up, b.getType(), b.getData());
			tblocks.add(tbu);
		}

		if (!awayup.getType().isSolid()) {
			TempBlock tbau = new TempBlock(awayup, away.getType(), away.getData());
			tblocks.add(tbau);
		}

		if (isMetal(b)) {
			TempBlock tbd = new TempBlock(b, Material.AIR, (byte) 0);
			tblocks.add(tbd);
		}

		if (isMetal(away)) {
			TempBlock tba = new TempBlock(away, Material.AIR, (byte) 0);
			tblocks.add(tba);
		}

		if (isMetal(deeperb)) {
			TempBlock tbdb = new TempBlock(deeperb, Material.AIR, (byte) 0);
			tblocks.add(tbdb);
		}

		if (isMetal(deepera)) {
			TempBlock tbda = new TempBlock(deepera, Material.AIR, (byte) 0);
			tblocks.add(tbda);
		}

		playMetalbendingSound(b.getLocation());
	}

	@SuppressWarnings("deprecation")
	public void shiftBlock(Block b, Vector d) {
		Block under = b.getRelative(BlockFace.DOWN);
		Block side = b.getRelative(GeneralMethods.getCardinalDirection(d).getOppositeFace());
		Block underside = under.getRelative(GeneralMethods.getCardinalDirection(d).getOppositeFace());

		for (TempBlock tb : tblocks) {
			if (tb.getBlock().getType() != Material.AIR)
				tb.setType(Material.AIR);
		}

		if (!side.getType().isSolid()) {
			TempBlock tbs = new TempBlock(side, b.getType(), b.getData());
			tblocks.add(tbs);
		}

		if (!underside.getType().isSolid()) {
			TempBlock tbus = new TempBlock(underside, under.getType(), under.getData());
			tblocks.add(tbus);
		}

		if (isMetal(b)) {
			TempBlock tb1 = new TempBlock(b, Material.AIR, (byte) 0);
			tblocks.add(tb1);
		}

		if (isMetal(under)) {
			TempBlock tb2 = new TempBlock(under, Material.AIR, (byte) 0);
			tblocks.add(tb2);
		}

		playMetalbendingSound(b.getLocation());
	}

	private void peelCoil(Block b) {
		Block under = b.getRelative(BlockFace.DOWN);

		if (length <= 0)
			return;

		if (!b.getType().isSolid()) {
			TempBlock tbb = new TempBlock(b, Material.IRON_BLOCK, (byte) 0);
			tblocks.add(tbb);
		}

		else
			stopCoil = true;

		if (!under.getType().isSolid()) {
			TempBlock tbu = new TempBlock(under, Material.IRON_BLOCK, (byte) 0);
			tblocks.add(tbu);
		}

		else
			stopCoil = true;

		playMetalbendingSound(b.getLocation());

		length--;
	}

	public static void startShred(Player player) {
		if (hasAbility(player, MetalShred.class)) {
			((MetalShred) getAbility(player, MetalShred.class)).startShred();
		}
	}

	private void startShred() {
		if (!horizontal) {
			started = true;
			return;
		}

		started = true;
	}

	public static void extend(Player player) {
		if (hasAbility(player, MetalShred.class)) {
			((MetalShred) getAbility(player, MetalShred.class)).extend();
		}
	}

	private void extend() {
		if (extending) {
			extending = false;
			return;
		}

		if (!stop)
			return;

		lastExtendTime = System.currentTimeMillis();
		fullLength = length;
		if (lastBlock != null)
			lastBlock = lastBlock.getRelative(GeneralMethods.getCardinalDirection(GeneralMethods.getDirection(player.getLocation(), lastBlock.getLocation())).getOppositeFace());
		else {
			return;
		}
		extending = true;
	}

	@Override
	public void progress() {
		if (!player.isOnline() || player.isDead()) {
			remove();
			return;
		}

		if (!bPlayer.canBendIgnoreCooldowns(this)) {
			remove();
			return;
		}

		if (!player.isSprinting()) {
			if (started)
				stop = true;
		}

		if (!horizontal && stop && !stopCoil && extending && System.currentTimeMillis() > lastExtendTime + extendTick) {
			lastExtendTime = System.currentTimeMillis();
			if (length > 0) {

				Block b = lastBlock.getRelative(GeneralMethods.getCardinalDirection(GeneralMethods.getDirection(lastBlock.getLocation(), VersionUtil.getTargetedLocation(player, fullLength))));

				peelCoil(b);

				for (Entity e : GeneralMethods.getEntitiesAroundPoint(b.getLocation(), 2)) {
					DamageHandler.damageEntity(e, damage, this);
					e.setVelocity(e.getVelocity().add(player.getLocation().getDirection().add(new Vector(0, 0.1, 0))));
				}

				lastBlock = b;
			}

			return;
		}

		if (stop || !started)
			return;

		Block b;

		if (lastBlock != null) {
			b = lastBlock.getRelative(GeneralMethods.getCardinalDirection(player.getLocation().getDirection()));
		}

		else {
			b = source.getRelative(GeneralMethods.getCardinalDirection(player.getLocation().getDirection()));
		}

		if (!isMetal(b)) {
			if (b.getType() != Material.AIR) {
				remove();
				return;

			}
			return;
		}

		if (b.getLocation().getX() == player.getLocation().getBlockX() || b.getLocation().getZ() == player.getLocation().getBlockZ()) {
			if (horizontal)
				raiseBlock(b, GeneralMethods.getDirection(player.getLocation(), b.getLocation()));
			else
				shiftBlock(b, GeneralMethods.getDirection(player.getLocation(), b.getLocation()));

			length++;
			lastBlock = b;
		}
		return;
	}

	private void revertAll() {
		for (TempBlock tb : tblocks) {
			tb.revertBlock();
		}
	}

	@Override
	public void remove() {
		revertAll();
		super.remove();
	}
	
	@Override
	public long getCooldown() {
		return 0;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return "MetalShred";
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
		return "* JedCore Addon *\n" + JedCore.plugin.getConfig().getString("Abilities.Earth.MetalShred.Description");
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
		return JedCore.plugin.getConfig().getBoolean("Abilities.Earth.MetalShred.Enabled");
	}
}
