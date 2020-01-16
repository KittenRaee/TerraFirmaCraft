/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.objects.blocks.agriculture;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;

import net.dries007.tfc.api.types.ICrop;
import net.dries007.tfc.objects.blocks.BlocksTFC;
import net.dries007.tfc.objects.items.ItemSeedsTFC;
import net.dries007.tfc.objects.te.TEPlacedItemFlat;
import net.dries007.tfc.objects.te.TETickCounter;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.climate.ClimateTFC;
import net.dries007.tfc.world.classic.chunkdata.ChunkDataTFC;

@ParametersAreNonnullByDefault
public abstract class BlockCropTFC extends BlockBush implements IGrowable
{
    // stage properties
    public static final PropertyInteger STAGE_8 = PropertyInteger.create("stage", 0, 7);
    public static final PropertyInteger STAGE_7 = PropertyInteger.create("stage", 0, 6);
    public static final PropertyInteger STAGE_6 = PropertyInteger.create("stage", 0, 5);
    public static final PropertyInteger STAGE_5 = PropertyInteger.create("stage", 0, 4);

    /* true if the crop spawned in the wild, means it ignores growth conditions i.e. farmland */
    public static final PropertyBool WILD = PropertyBool.create("wild");

    // binary flags for state and metadata conversion
    private static final int META_WILD = 8;
    private static final int META_GROWTH = 7;

    private static final Map<ICrop, BlockCropTFC> MAP = new HashMap<>();

    public static BlockCropTFC get(ICrop crop)
    {
        return MAP.get(crop);
    }

    public static Set<ICrop> getCrops()
    {
        return MAP.keySet();
    }

    protected final ICrop crop;

    BlockCropTFC(ICrop crop)
    {
        super(Material.PLANTS);

        this.crop = crop;
        if (MAP.put(crop, this) != null)
        {
            throw new IllegalStateException("There can only be one.");
        }

        setSoundType(SoundType.PLANT);
        setHardness(0.6f);
    }

    @Nonnull
    public ICrop getCrop()
    {
        return crop;
    }

    @Override
    public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient)
    {
        return true;
    }

    @Override
    public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state)
    {
        return false;
    }

    @Override
    @Nonnull
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta)
    {
        return getDefaultState().withProperty(WILD, (meta & META_WILD) > 0).withProperty(getStageProperty(), meta & META_GROWTH);
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(getStageProperty()) + (state.getValue(WILD) ? META_WILD : 0);
    }

    @Override
    public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random random)
    {
        super.updateTick(worldIn, pos, state, random);
        if (!worldIn.isRemote)
        {
            // Attempt to grow
            float temp = ClimateTFC.getActualTemp(worldIn, pos);
            float rainfall = ChunkDataTFC.getRainfall(worldIn, pos);
            TETickCounter te = Helpers.getTE(worldIn, pos, TETickCounter.class);
            if (te != null)
            {
                if (crop.isValidForGrowth(temp, rainfall))
                {
                    // for every growth time, grow the plant
                    while (te.getTicksSinceUpdate() > crop.getGrowthTime())
                    {
                        grow(worldIn, random, pos, worldIn.getBlockState(pos));
                        te.reduceCounter((long) crop.getGrowthTime());
                    }
                }

                // If not valid conditions, die
                if (!crop.isValidConditions(temp, rainfall))
                {
                    worldIn.setBlockState(pos, BlocksTFC.PLACED_ITEM_FLAT.getDefaultState());
                    TEPlacedItemFlat tilePlaced = Helpers.getTE(worldIn, pos, TEPlacedItemFlat.class);
                    if (tilePlaced != null)
                    {
                        tilePlaced.setStack(getDeathItem(te));
                    }
                }
            }
        }
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state)
    {
        TETickCounter tile = Helpers.getTE(worldIn, pos, TETickCounter.class);
        if (tile != null)
        {
            tile.resetCounter();
        }
    }

    @Override
    public boolean hasTileEntity(IBlockState state)
    {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state)
    {
        return new TETickCounter();
    }

    @Override
    @Nonnull
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player)
    {
        return new ItemStack(ItemSeedsTFC.get(crop));
    }

    @Nonnull
    @Override
    public EnumPlantType getPlantType(IBlockAccess world, BlockPos pos)
    {
        return EnumPlantType.Crop;
    }

    public abstract PropertyInteger getStageProperty();

    /**
     * Gets the item stack that the crop will create upon dying and turning into a placed item
     */
    protected ItemStack getDeathItem(TETickCounter cropTE)
    {
        return ItemSeedsTFC.get(crop, 1);
    }
}
