package cofh.thermalexpansion.block.light;

import cpw.mods.fml.common.registry.GameRegistry;

import net.minecraft.nbt.NBTTagCompound;

public class TileLightFalse extends TileLight {

	public static void initialize() {

		GameRegistry.registerTileEntity(TileLightFalse.class, "thermalexpansion.LightFalse");
	}

	@Override
	public int getLightValue() {

		return 0;
	}

	public void cofh_validate() {

		int m = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
		worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, m & 3, 2);
		dim = true;
		NBTTagCompound tag = new NBTTagCompound();
		writeToNBT(tag);
		invalidate();
		TileLight tile = (TileLight) worldObj.getTileEntity(xCoord, yCoord, zCoord);
		tile.readFromNBT(tag);
		updateLighting();
		worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
	}

}
