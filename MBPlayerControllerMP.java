package net.minecraft.src;

import net.minecraft.client.*;

public class MBPlayerControllerMP extends PlayerControllerMP
{
	
	public boolean flag = true;

	public MBPlayerControllerMP(Minecraft par1Minecraft, NetClientHandler par2NetClientHandler) {
		
		super(par1Minecraft, par2NetClientHandler);
		
	}
	
    /**
     * Called when a player completes the destruction of a block
     */
	@Override
    public boolean onPlayerDestroyBlock(int par1, int par2, int par3, int par4)
    {
        if(flag) return super.onPlayerDestroyBlock(par1, par2, par3, par4);
        else return false;
    }

    /**
     * Resets current block damage and isHittingBlock
     */
	@Override
    public void resetBlockRemoving()
    {
        if(flag) super.resetBlockRemoving();
    }
	
}
