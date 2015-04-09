package cofh.thermalexpansion.block.machine;

import cofh.api.item.IAugmentItem;
import cofh.core.util.CoreUtils;
import cofh.lib.util.helpers.MathHelper;
import cofh.thermalexpansion.ThermalExpansion;
import cofh.thermalexpansion.gui.client.machine.GuiFurnace;
import cofh.thermalexpansion.gui.container.machine.ContainerFurnace;
import cofh.thermalexpansion.item.TEAugments;
import cofh.thermalexpansion.util.crafting.FurnaceManager;
import cofh.thermalexpansion.util.crafting.FurnaceManager.RecipeFurnace;
import cpw.mods.fml.common.registry.GameRegistry;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class TileFurnace extends TileMachineBase {

	static final int TYPE = BlockMachine.Types.FURNACE.ordinal();

	public boolean foodBoost;

	public static void initialize() {

		defaultSideConfig[TYPE] = new SideConfig();
		defaultSideConfig[TYPE].numConfig = 4;
		defaultSideConfig[TYPE].slotGroups = new int[][] { {}, { 0 }, { 1 }, { 0, 1 } };
		defaultSideConfig[TYPE].allowInsertionSide = new boolean[] { false, true, false, true };
		defaultSideConfig[TYPE].allowExtractionSide = new boolean[] { false, true, true, true };
		defaultSideConfig[TYPE].allowInsertionSlot = new boolean[] { true, false, false };
		defaultSideConfig[TYPE].allowExtractionSlot = new boolean[] { true, true, false };
		defaultSideConfig[TYPE].sideTex = new int[] { 0, 1, 4, 7 };
		defaultSideConfig[TYPE].defaultSides = new byte[] { 1, 1, 2, 2, 2, 2 };

		String category = "Machine.Furnace";
		int basePower = MathHelper.clampI(ThermalExpansion.config.get(category, "BasePower", 20), 10, 500);
		ThermalExpansion.config.set(category, "BasePower", basePower);
		defaultEnergyConfig[TYPE] = new EnergyConfig();
		defaultEnergyConfig[TYPE].setParamsPower(basePower);

		sounds[TYPE] = CoreUtils.getSoundName(ThermalExpansion.modId, "blockMachineFurnace");

		GameRegistry.registerTileEntity(TileFurnace.class, "thermalexpansion.Furnace");
	}

	int outputTracker;

	public TileFurnace() {

		super();

		inventory = new ItemStack[1 + 1 + 1];
	}

	@Override
	public int getType() {

		return TYPE;
	}

	@Override
	protected boolean canStart() {

		if (inventory[0] == null) {
			return false;
		}
		if (foodBoost && !FurnaceManager.isFoodItem(inventory[0])) {
			return false;
		}
		RecipeFurnace recipe = FurnaceManager.getRecipe(inventory[0]);

		if (recipe == null || energyStorage.getEnergyStored() < recipe.getEnergy() * energyMod / processMod) {
			return false;
		}
		ItemStack output = recipe.getOutput();

		if (inventory[1] == null) {
			return true;
		}
		if (!inventory[1].isItemEqual(output)) {
			return false;
		}
		return inventory[1].stackSize + output.stackSize <= output.getMaxStackSize();
	}

	@Override
	protected boolean hasValidInput() {

		RecipeFurnace recipe = FurnaceManager.getRecipe(inventory[0]);

		if (foodBoost && !FurnaceManager.isFoodItem(inventory[0])) {
			return false;
		}
		return recipe == null ? false : recipe.getInput().stackSize <= inventory[0].stackSize;
	}

	@Override
	protected void processStart() {

		processMax = FurnaceManager.getRecipe(inventory[0]).getEnergy();

		if (foodBoost) {
			processMax /= 2;
		}
		processRem = processMax;
	}

	@Override
	protected void processFinish() {

		RecipeFurnace recipe = FurnaceManager.getRecipe(inventory[0]);
		ItemStack output = recipe.getOutput();

		if (inventory[1] == null) {
			inventory[1] = output;
		} else {
			inventory[1].stackSize += output.stackSize;
		}
		if (foodBoost && recipe.isOutputFood() && inventory[1].stackSize < inventory[1].getMaxStackSize()) {
			inventory[1].stackSize += output.stackSize;
		}
		inventory[0].stackSize -= recipe.getInput().stackSize;

		if (inventory[0].stackSize <= 0) {
			inventory[0] = null;
		}
	}

	@Override
	protected void transferProducts() {

		if (!augmentAutoTransfer) {
			return;
		}
		if (inventory[1] == null) {
			return;
		}
		int side;
		for (int i = outputTracker + 1; i <= outputTracker + 6; i++) {
			side = i % 6;

			if (sideCache[side] == 2) {
				if (transferItem(1, AUTO_EJECT[level], side)) {
					outputTracker = side;
					break;
				}
			}
		}
	}

	@Override
	public boolean isItemValid(ItemStack stack, int slot, int side) {

		return slot == 0 ? foodBoost ? FurnaceManager.isFoodItem(stack) : FurnaceManager.recipeExists(stack) : true;
	}

	/* GUI METHODS */
	@Override
	public Object getGuiClient(InventoryPlayer inventory) {

		return new GuiFurnace(inventory, this);
	}

	@Override
	public Object getGuiServer(InventoryPlayer inventory) {

		return new ContainerFurnace(inventory, this);
	}

	/* NBT METHODS */
	@Override
	public void readFromNBT(NBTTagCompound nbt) {

		super.readFromNBT(nbt);

		outputTracker = nbt.getInteger("Tracker");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {

		super.writeToNBT(nbt);

		nbt.setInteger("Tracker", outputTracker);
	}

	/* AUGMENT HELPERS */
	@Override
	protected boolean installAugment(int slot) {

		IAugmentItem augmentItem = (IAugmentItem) augments[slot].getItem();
		boolean installed = false;

		if (augmentItem.getAugmentLevel(augments[slot], TEAugments.MACHINE_FURNACE_FOOD) > 0) {
			foodBoost = true;
			installed = true;
		}
		return installed ? true : super.installAugment(slot);
	}

	@Override
	protected void resetAugments() {

		super.resetAugments();

		foodBoost = false;
	}

}
