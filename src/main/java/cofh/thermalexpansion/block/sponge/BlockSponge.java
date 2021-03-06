package cofh.thermalexpansion.block.sponge;

import static cofh.lib.util.helpers.ItemHelper.ShapedRecipe;

import cofh.api.tileentity.ISidedTexture;
import cofh.core.render.IconRegistry;
import cofh.lib.util.helpers.StringHelper;
import cofh.thermalexpansion.ThermalExpansion;
import cofh.thermalexpansion.block.BlockTEBase;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

public class BlockSponge extends BlockTEBase {

	public BlockSponge() {

		super(Material.sponge);
		setHardness(0.6F);
		setStepSound(soundTypeGrass);
		setBlockName("thermalexpansion.sponge");
	}

	@Override
	public TileEntity createNewTileEntity(World world, int metadata) {

		if (metadata >= Types.values().length) {
			return null;
		}
		switch (Types.values()[metadata]) {
		case CREATIVE:
			if (!enable[Types.CREATIVE.ordinal()]) {
				return null;
			}
			return new TileSpongeCreative(metadata);
		case BASIC:
			return new TileSponge(metadata);
		case MAGMATIC:
			return new TileSpongeMagmatic(metadata);
		default:
			return null;
		}
	}

	@Override
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {

		if (enable[0]) {
			list.add(new ItemStack(item, 1, 0));
		}
		for (int i = 1; i < Types.values().length; i++) {
			list.add(new ItemStack(item, 1, i));
		}
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase living, ItemStack stack) {

		if (world.getBlockMetadata(x, y, z) == 0 && !enable[0]) {
			world.setBlockToAir(x, y, z);
			return;
		}
		TileEntity tile = world.getTileEntity(x, y, z);

		if (stack.stackTagCompound != null && stack.stackTagCompound.hasKey("Fluid")) {
			FluidStack fluid = FluidStack.loadFluidStackFromNBT(stack.stackTagCompound.getCompoundTag("Fluid"));

			if (fluid != null) {
				((TileSponge) tile).setFluid(fluid);
			}
		} else if (tile instanceof TileSponge) {
			((TileSponge) tile).absorb();
		}
		super.onBlockPlacedBy(world, x, y, z, living, stack);
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random rand) {

		TileEntity tile = world.getTileEntity(x, y, z);

		if (tile instanceof TileSponge) {
			((TileSponge) tile).placeAir();
		}
	}

	@Override
	public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {

		ISidedTexture tile = (ISidedTexture) world.getTileEntity(x, y, z);
		return tile == null ? null : tile.getTexture(side, renderPass);
	}

	@Override
	public IIcon getIcon(int side, int metadata) {

		return IconRegistry.getIcon("Sponge", metadata);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister ir) {

		IconRegistry.addIcon("Sponge" + 0, "thermalexpansion:sponge/Sponge_Creative", ir);
		IconRegistry.addIcon("Sponge" + 1, "thermalexpansion:sponge/Sponge_Basic", ir);
		IconRegistry.addIcon("Sponge" + 2, "thermalexpansion:sponge/Sponge_Magmatic", ir);

		IconRegistry.addIcon("Sponge" + 9, "thermalexpansion:sponge/Sponge_Basic_Soaked", ir);
		IconRegistry.addIcon("Sponge" + 10, "thermalexpansion:sponge/Sponge_Magmatic_Soaked", ir);
	}

	@Override
	public NBTTagCompound getItemStackTag(World world, int x, int y, int z) {

		NBTTagCompound tag = super.getItemStackTag(world, x, y, z);
		TileSponge tile = (TileSponge) world.getTileEntity(x, y, z);

		if (tile != null) {
			FluidStack fluid = tile.getFluid();

			if (fluid != null) {
				tag = new NBTTagCompound();
				tag.setTag("Fluid", fluid.writeToNBT(new NBTTagCompound()));
			}
		}
		return tag;
	}

	/* IInitializer */
	@Override
	public boolean initialize() {

		TileSponge.initialize();
		TileSpongeMagmatic.initialize();
		TileSpongeCreative.initialize();

		spongeBasic = new ItemStack(this, 1, Types.BASIC.ordinal());
		spongeMagmatic = new ItemStack(this, 1, Types.MAGMATIC.ordinal());

		GameRegistry.registerCustomItemStack("spongeBasic", spongeBasic);
		GameRegistry.registerCustomItemStack("spongeMagmatic", spongeMagmatic);

		return true;
	}

	@Override
	public boolean postInit() {

		if (enable[Types.BASIC.ordinal()]) {
			GameRegistry.addRecipe(ShapedRecipe(spongeBasic, new Object[] { "SWS", "WBW", "SWS", 'S', Items.string, 'W', "dustWood", 'B', "slimeball" }));
		}
		if (enable[Types.MAGMATIC.ordinal()]) {
			GameRegistry.addRecipe(ShapedRecipe(spongeMagmatic,
					new Object[] { "SWS", "WBW", "SWS", 'S', Items.string, 'W', "dustWood", 'B', Items.magma_cream }));
		}
		return true;
	}

	public static enum Types {
		CREATIVE, BASIC, MAGMATIC
	}

	public static final String[] NAMES = { "creative", "basic", "magmatic" };
	public static boolean[] enable = new boolean[Types.values().length];

	static {
		String category = "Sponge.";

		enable[0] = ThermalExpansion.config.get(category + StringHelper.titleCase(NAMES[0]), "Enable", true);
		for (int i = 1; i < Types.values().length; i++) {
			enable[i] = ThermalExpansion.config.get(category + StringHelper.titleCase(NAMES[i]), "Recipe.Enable", true);
		}
	}

	public static ItemStack spongeCreative;
	public static ItemStack spongeBasic;
	public static ItemStack spongeMagmatic;

}
