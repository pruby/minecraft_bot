package net.minecraft.src;

import net.minecraft.src.MinecraftBot.MBVec;

public class MBSquare {

	private MinecraftBot mb;
	private MBVec initVec;
	private boolean left;
	private int ix = 1, iy = -1, iz = 1;
	private int dx, dy, dz;
	private int cx = 0, cy = 0, cz = 0;
	private int yaw;

	MBSquare(MinecraftBot parMb){
		
		this(parMb, 3, 3, 1);
		
	}

	MBSquare(MinecraftBot parMb, int parZ, int parX, int parY){
		
		this(parMb, parZ, parX, parY, false);
		
	}

	MBSquare(MinecraftBot parMb, int parZ, int parX, int parY, boolean parLeft){
		
		mb = parMb;
		initVec = mb.new MBVec(0, -1, 0);
		yaw = mb.tools.rY(mb.mc.thePlayer.rotationYaw);
		left = parLeft;
		dx = parX;
		dy = -parY;
		dz = parZ;
		
	}
	
	public MBVec next(){
		 
	 	if((ix > 0 && dx - 1 > cx) || (ix < 0 && cx > 0)){
			cx += ix;
		}
		else{
			ix = -ix;
			if((iz > 0 && dz - 1 > cz) || (iz < 0 && cz > 0)){
				cz += iz;
			}
			else{
				iz = -iz;
				if(dy < cy - 1){
					cy += iy;
				}
				else{
					dx = 0;
					dy = 0;
					dz = 0;
					return null;
				}
			}
		}
		
		return finalVec(cx, cy, cz);
	}
	
	private MBVec finalVec(int parX, int parY, int parZ){
		
		MBVec finalVec = initVec.getVec();
		int buffx = 0, buffy, buffz = 0;
		
		switch (yaw){

		case 0:{
			if(left) buffx = parX;
			else buffx = -parX;
			buffz = parZ;
			break;
		}
		case 90:{
			if(left) buffz = parX;
			else buffz = -parX;
			buffx = -parZ;
			break;
		}
		case 180:{
			if(left) buffx = -parX;
			else buffx = parX;
			buffz = -parZ;
			break;
		}
		case 270:{
			if(left) buffz = -parX;
			else buffz = parX;
			buffx = parZ;
			break;
		}
		}
		
		buffy = parY;
		
		return mb.new MBVec(finalVec.xCoord + buffx, finalVec.yCoord + buffy, finalVec.zCoord + buffz);
	}
}
