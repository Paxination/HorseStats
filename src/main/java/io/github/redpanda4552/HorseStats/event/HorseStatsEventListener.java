package io.github.redpanda4552.HorseStats.event;

import io.github.redpanda4552.HorseStats.HorseStatsMain;
import io.github.redpanda4552.HorseStats.utilities.Translate;
import net.minecraft.server.v1_7_R4.NBTBase;
import net.minecraft.server.v1_7_R4.NBTTagCompound;
import net.minecraft.server.v1_7_R4.NBTTagList;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftHorse;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * The main listener. Imports CraftBukkit code and can break with version changes.
 * Events are registered here if server CB precisely matches HorseStats CB.
 */
public class HorseStatsEventListener extends HorseStatsListenerBase {
		
	public HorseStatsEventListener(HorseStatsMain horseStatsMain) {
		super (horseStatsMain);
	}
	
	@EventHandler
	/**
	 * This event is called whenever an entity is hurt by another entity.
	 * The first if statement quickly drops this if it is not a player and horse.
	 * @param event - The EntityDamageByEntityEvent that triggered this.
	 */
	public void onHorseHit(EntityDamageByEntityEvent event) {
		//A horse is hit
		if (event.getEntity() instanceof Horse) {
			Horse h = (Horse) event.getEntity();
			//A player hit it
			if (event.getDamager() instanceof Player) {
				Player p = (Player) event.getDamager();
				//They held a lead
				if (p.getItemInHand().getType() == Material.LEASH) {
					event.setCancelled(true);
					displayStats(p, h);
				//They held an ender pearl
				} else if (p.getItemInHand().getType() == Material.ENDER_PEARL) {
					event.setCancelled(true);
					teleportToggle(p, h);
				//They held something else
				} else {
					if (main.configBoolean("horseGrief") == false) {
						//Kill this method before it is cancelled.
						if (main.override(p)) {
							return;
						}	
						
						if (p != h.getOwner() && h.getOwner() != null) {
							event.setCancelled(true);
							this.sendError(p, Translate.generic, "attack");
						}
					}
				}				
			}
			//An arrow hit it
			else if (event.getDamager() instanceof Arrow) { 
				if (main.configBoolean("horseGrief") == false) {
					Arrow a = (Arrow) event.getDamager();
					if (a.getShooter() instanceof Player) {
						Player p = (Player) a.getShooter();
						
						//Kill this method before it is cancelled.
						if (main.override(p)) {
							return;
						}
						
						if (h.getOwner() != null) {
							event.setCancelled(true);
							this.sendError(p, Translate.generic, "attack");
						}
					}
				}
			}
		}
	}
	
	/**
	 * Display the stats of a horse to a player.
	 * @param p - The player to send stats to.
	 * @param horse - The horse who's stats are to be fetched.
	 */
	public void displayStats(Player p, Horse horse) {
		//Because its still an issue, ownership correction is back.
		if (horse.getOwner() == null && horse.isTamed() == true) {
			horse.setTamed(false);
			this.sendNormal(p, Translate.horseStatsEventListener, "ownerFix");
		}
		
		//RAW data (heh)
		double healthMax = horse.getMaxHealth();
		double heartMax = healthMax / 2;
		double health = horse.getHealth();
		double heart = health / 2;
		double jump = 5.5 * (Math.pow(horse.getJumpStrength(), 2));
		boolean adult = horse.isAdult();
		boolean breed = horse.canBreed();
		float age = horse.getAge();
		AnimalTamer tamer = horse.getOwner();
		
		//Horse name
		String name = Translate.event("horse");
		if (horse.getCustomName() != null) {
			name = horse.getCustomName() + Translate.event("posessive");
		}
		
		//Teleport status
		boolean tpStatus = false;
		if (main.teleportQueue.containsValue(horse)) {
			tpStatus = true;
		}
		
		//Age status
		String ageTime = "";
		if (adult == false) {
			ageTime = " (" + Translate.event("adultMinutes") + age/-1200 + ")";
		}
		
		//Owner name
		String owner = Translate.event("none");
		if (tamer != null) {
			owner = tamer.getName();
		}
		
		//Message output
		this.sendStat(p, "========================");
		this.sendStat(p, name + " " + Translate.event("stats"));
		this.sendStat(p, "========================");
		//Using floats to reduce the number of extraneous decimals
		this.sendStat(p, Translate.event("maxHealth") + " " + (float) healthMax + " (" + (int) heartMax + " " + Translate.event("hearts") + ")");
		this.sendStat(p, Translate.event("health") + " " + (float) health + " (" + (int) heart + " " + Translate.event("hearts") + ")");
		this.sendStat(p, Translate.event("jump") + " " + (float) jump);
		this.sendStat(p, Translate.event("speed") + " " + (float) getSpeed(horse) * 43);
		this.sendStat(p, Translate.event("breed") + " " + breed);
		this.sendStat(p, Translate.event("teleportStatus") + " " + tpStatus);
		this.sendStat(p, Translate.event("isAdult") + " " + adult + ageTime);
		this.sendStat(p, Translate.event("owner") + " " + owner);
	}
	
	/**
	 * Toggles teleport selection for a horse.
	 * @param p - The player who attempted the toggle.
	 * @param horse - The horse who teleport is being toggled for.
	 */
	public void teleportToggle(Player p, Horse horse) {
		if (horse.getOwner() == p || main.override(p)) {
			if (main.teleportQueue.containsKey(p.getName())) {
				main.teleportQueue.remove(p.getName());
				this.sendNormal(p, Translate.horseStatsEventListener, "teleportDeselected");
			} else {
				main.teleportQueue.put(p.getName(), horse);
				this.sendNormal(p, Translate.horseStatsEventListener, "teleportSelected");
			}
		} else if (horse.getOwner() == null) { 
			this.sendError(p, Translate.horseStatsEventListener, "teleportUntame");
		} else {
			this.sendError(p, Translate.generic, "owner");
		}
	}
	
	/**
	 * The 'fragile' code used to retrieve horse speed. NBT stuff.
	 * Needs re-imported when NBT code changes, or build number changes (e.g. CB 1.7.2-R0.1 to CB 1.7.2-R0.2)
	 * @param horse - The horse that was hit with a lead.
	 * @return Double that represents horse speed.
	 */
	public double getSpeed(Horse horse) {
		CraftHorse cHorse = (CraftHorse) horse;
		NBTTagCompound compound = new NBTTagCompound();
		cHorse.getHandle().b(compound);
		double speed = -1;
		NBTTagList list = (NBTTagList) compound.get("Attributes");
		for(int i = 0; i < list.size() ; i++) {
			NBTBase base = list.get(i);
			if (base.getTypeId() == 10) {
				NBTTagCompound attrCompound = (NBTTagCompound)base;
				if (base.toString().contains("generic.movementSpeed")) {
					speed = attrCompound.getDouble("Base");
				}
			}
		}
		return speed;
	}		
}


