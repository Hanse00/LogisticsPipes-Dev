/** 
 * Copyright (c) Krapht, 2011
 * 
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public 
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package net.minecraft.src.buildcraft.krapht.logic;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.ModLoader;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.mod_LogisticsPipes;
import net.minecraft.src.buildcraft.api.APIProxy;
import net.minecraft.src.buildcraft.core.CoreProxy;
import net.minecraft.src.buildcraft.krapht.GuiIDs;
import net.minecraft.src.buildcraft.krapht.IRequestItems;
import net.minecraft.src.buildcraft.krapht.IRequireReliableTransport;
import net.minecraft.src.buildcraft.krapht.LogisticsManager;
import net.minecraft.src.buildcraft.krapht.LogisticsRequest;
import net.minecraft.src.buildcraft.krapht.RoutedPipe;
import net.minecraft.src.buildcraft.krapht.network.NetworkConstants;
import net.minecraft.src.buildcraft.krapht.network.PacketPipeInteger;
import net.minecraft.src.buildcraft.transport.TileGenericPipe;
import net.minecraft.src.krapht.ItemIdentifier;

public class LogicSatellite extends BaseRoutingLogic implements IRequireReliableTransport{
	
	public static HashSet<LogicSatellite> AllSatellites = new HashSet<LogicSatellite>();
	
	private final LinkedList<ItemIdentifier> _lostItems = new LinkedList<ItemIdentifier>();
	
	public int satelliteId;
	
	public LogicSatellite(){
		throttleTime = 40;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);	
    	satelliteId = nbttagcompound.getInteger("satelliteid");
    	ensureAllSatelliteStatus();
    }

	@Override
    public void writeToNBT(NBTTagCompound nbttagcompound) {
		nbttagcompound.setInteger("satelliteid", satelliteId);
    	super.writeToNBT(nbttagcompound);
    }
	
	private int findId(int increment){
		int potentialId = satelliteId;
    	boolean conflict = true;
    	while(conflict){
    		potentialId += increment;
    		if (potentialId < 0){
    			return 0;
    		}
    		conflict = false;
	    	for (LogicSatellite sat : AllSatellites){
	    		if (sat.satelliteId == potentialId){
	    			conflict = true;
	    			break;
	    		}
	    	}
    	}
    	return potentialId;
	}
	
	private void ensureAllSatelliteStatus(){
		if (satelliteId == 0 && AllSatellites.contains(this)){
			AllSatellites.remove(this);
		}
		if (satelliteId != 0 && !AllSatellites.contains(this)){
			AllSatellites.add(this);
		}
	}
    
    public void setNextId(EntityPlayerMP player){
    	satelliteId = findId(1);
    	ensureAllSatelliteStatus();

		// Using existing BuildCraft packet system
    	PacketPipeInteger packet = new PacketPipeInteger(NetworkConstants.SATELLITE_PIPE_SATELLITE_ID, xCoord, yCoord, zCoord, satelliteId);
		CoreProxy.sendToPlayer((EntityPlayer) player, packet);
    }
    
    public void setPrevId(EntityPlayerMP player){
    	satelliteId = findId(-1);
    	ensureAllSatelliteStatus();

		// Using existing BuildCraft packet system
    	PacketPipeInteger packet = new PacketPipeInteger(NetworkConstants.SATELLITE_PIPE_SATELLITE_ID, xCoord, yCoord, zCoord, satelliteId);
		CoreProxy.sendToPlayer((EntityPlayer) player, packet);
    }

	@Override
	public void destroy() {
		if (AllSatellites.contains(this)){
			AllSatellites.remove(this);
		}
	}

	@Override
	public void onWrenchClicked(EntityPlayer entityplayer) {
		if(!APIProxy.isClient(entityplayer.worldObj)) {
			//GuiProxy.GuiSatellitePipe(this);
			entityplayer.openGui(mod_LogisticsPipes.instance, GuiIDs.GUI_SatelitePipe_ID, worldObj, xCoord, yCoord, zCoord);
		}
	}
	
	@Override
	public void throttledUpdateEntity() {
		super.throttledUpdateEntity();
		if (_lostItems.isEmpty()) return;
		
		Iterator<ItemIdentifier> iterator = _lostItems.iterator();
		while(iterator.hasNext()){
			LogisticsRequest request = new LogisticsRequest(iterator.next(), 1, (IRequestItems)this.getRoutedPipe());
			if (LogisticsManager.Request(request, ((RoutedPipe)((TileGenericPipe)this.container).pipe).getRouter().getRoutersByCost(), null)){
				iterator.remove();
			}
		}
	}
	
	
	@Override
	public void itemLost(ItemIdentifier item) {
		_lostItems.add(item);
	}

	@Override
	public void itemArrived(ItemIdentifier item) {}
}