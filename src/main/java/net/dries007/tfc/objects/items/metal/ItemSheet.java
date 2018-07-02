/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.objects.items.metal;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.dries007.tfc.objects.Metal;
import net.dries007.tfc.objects.blocks.metal.BlockSheet;
import net.dries007.tfc.util.IPlacableItem;

import static net.dries007.tfc.objects.blocks.metal.BlockSheet.FACE;

public class ItemSheet extends ItemMetal implements IPlacableItem
{
    public ItemSheet(Metal metal, Metal.ItemType type)
    {
        super(metal, type);
    }

    @Override
    public boolean placeItemInWorld(World world, BlockPos pos, ItemStack stack, EnumFacing facing, EntityPlayer player)
    {
        if (world.getBlockState(pos).isNormalCube() && stack.getItem() instanceof ItemSheet)
        {
            ItemSheet sheet = (ItemSheet) stack.getItem();
            world.setBlockState(pos.offset(facing), BlockSheet.get(sheet.metal).getDefaultState().withProperty(FACE, facing));
            return true;
        }
        return false;
    }
}
