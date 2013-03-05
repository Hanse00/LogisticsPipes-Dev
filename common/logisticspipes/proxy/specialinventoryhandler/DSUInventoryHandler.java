package logisticspipes.proxy.specialinventoryhandler;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import logisticspipes.utils.ItemIdentifier;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import powercrystals.minefactoryreloaded.api.IDeepStorageUnit;

public class DSUInventoryHandler extends SpecialInventoryHandler {

	private final IDeepStorageUnit _tile;
	private final boolean _hideOnePerStack;

	private DSUInventoryHandler(TileEntity tile, boolean hideOnePerStack, boolean hideOne, int cropStart, int cropEnd) {
		_tile = (IDeepStorageUnit)tile;
		_hideOnePerStack = hideOnePerStack || hideOne;
	}

	public DSUInventoryHandler() {
		_tile = null;
		_hideOnePerStack = false;
	}

	@Override
	public boolean init() {
		return _tile != null;
	}

	@Override
	public boolean isType(TileEntity tile) {
		return tile instanceof IDeepStorageUnit;
	}

	@Override
	public SpecialInventoryHandler getUtilForTile(TileEntity tile, boolean hideOnePerStack, boolean hideOne, int cropStart, int cropEnd) {
		return new DSUInventoryHandler(tile, hideOnePerStack, hideOne, cropStart, cropEnd);
	}


	@Override
	public int itemCount(ItemIdentifier itemIdent) {
		ItemStack items = _tile.getStoredItemType();
		if(items != null && ItemIdentifier.get(items) == itemIdent)
			return items.stackSize - (_hideOnePerStack?1:0);
		return 0;
	}

	@Override
	public ItemStack getMultipleItems(ItemIdentifier itemIdent, int count) {
		ItemStack items = _tile.getStoredItemType();
		if(items == null || ItemIdentifier.get(items) != itemIdent) {
			return null;
		}
		if(_hideOnePerStack)
			items.stackSize --; 
		if(count >= items.stackSize) {
			_tile.setStoredItemCount((_hideOnePerStack?1:0));
			return items;
		}
		ItemStack newItems = items.splitStack(count);
		_tile.setStoredItemCount(items.stackSize+(_hideOnePerStack?1:0));
		return newItems;
		
	}

	@Override
	public Set<ItemIdentifier> getItems() {
		Set<ItemIdentifier> result = new TreeSet<ItemIdentifier>();
		ItemStack items = _tile.getStoredItemType();
		if(items != null && items.stackSize > 0) {
			result.add(ItemIdentifier.get(items));
		}
		return result;
	}
	
	@Override
	public HashMap<ItemIdentifier, Integer> getItemsAndCount() {
		HashMap<ItemIdentifier, Integer> result = new HashMap<ItemIdentifier, Integer>();
		ItemStack items = _tile.getStoredItemType();
		if(items != null && items.stackSize > 0) {
			result.put(ItemIdentifier.get(items),items.stackSize-(_hideOnePerStack?1:0));
		}
		return result;
	}

	@Override
	public ItemStack getSingleItem(ItemIdentifier itemIdent) {
		return getMultipleItems(itemIdent,1);
	}

	@Override
	public boolean containsItem(ItemIdentifier itemIdent) {
		ItemStack items = _tile.getStoredItemType();
		return items != null && ItemIdentifier.get(items) == itemIdent;
	}

	@Override
	public boolean containsUndamagedItem(ItemIdentifier itemIdent) {
		ItemStack items = _tile.getStoredItemType();
		if(items != null && ItemIdentifier.getUndamaged(items) == itemIdent)
			return true;
		return false;		
	}

	@Override
	public int roomForItem(ItemIdentifier item) {
		return roomForItem(item, 0);
	}
	@Override
	public int roomForItem(ItemIdentifier itemIdent, int count) {
		if(itemIdent.tag != null || !itemIdent.tag.hasNoTags()) {
			return 0;
		}
		ItemStack items = _tile.getStoredItemType();
		if(items == null) {
			return _tile.getMaxStoredCount();
		}
		if(ItemIdentifier.get(items) == itemIdent) {
			return _tile.getMaxStoredCount() - items.stackSize;
		}
		return 0;
	}

	@Override
	public ItemStack add(ItemStack stack, ForgeDirection from, boolean doAdd) {
		if(stack.getTagCompound() != null || !stack.getTagCompound().hasNoTags()) {
			return null;
		}
		ItemStack items = _tile.getStoredItemType();
		if((items == null || items.stackSize == 0 ) && stack.stackSize < _tile.getMaxStoredCount()) {
			_tile.setStoredItemType(stack.itemID, stack.getItemDamage(), stack.stackSize);
			return stack;
		}
		if(items != null && !items.isItemEqual(stack)) {
			return null;
		}
		int toAdd = Math.min(_tile.getMaxStoredCount() - items.stackSize,stack.stackSize);
		if(toAdd == stack.stackSize) {
			_tile.setStoredItemType(stack.itemID, stack.getItemDamage(), stack.stackSize);
			return stack;
		}
		
		ItemStack itemsToAdd = (ItemStack) stack.copy().splitStack(toAdd);
		_tile.setStoredItemCount(_tile.getMaxStoredCount());
		return itemsToAdd;
	}
}