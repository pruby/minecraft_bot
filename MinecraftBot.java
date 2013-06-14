package net.minecraft.src;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import net.minecraft.client.*;
import net.minecraft.src.KeyBinding;
import net.minecraft.src.MBManager.MANAGER_RETURN;
import net.minecraft.src.MBSquare;
import net.minecraft.src.MathHelper;
import net.minecraft.src.Vec3;

public class MinecraftBot {

	public MBChat chat;
	public MinecraftBot mb;
	
	private static boolean firstInstance = true;
	
	public Minecraft mc;
	private MBManager manager;
	public MBTools tools;
	private MBScanner scanner;
    private long lastTick;
    private long tickCount;
    private BotPath tmpPath;
	private boolean btnForward;
	private boolean btnBackward;
	private boolean btnSneak;
	private boolean btnJump;
	
	/** Constants */
	private static final int PATH_SEARCH_TIMEOUT = 5000;
	
	public static int[] BLOCKS_LAVA_WATER_FIRE = {8, 9, 10, 11, 51};
	public static int[] BLOCKS_LAVA_WATER = {8, 9, 10, 11};
	public static int[] BLOCKS_ORE = {1, 4, 14, 15, 16, 21, 24, 48, 49, 56, 73, 74, 87, 88, 89};
	public static int[] BLOCKS_BUILD = {1, 3, 4};
	public static int[] BLOCKS_DIRT = {2, 3, 12, 13, 31, 78, 82, 88, 110};
	public static int[] BLOCKS_WOOD = {5, 17};
	public static int[] BLOCKS_EMPTY = {0, 31, 37, 50, 66, 68, 78, 106};
	public static int[] BLOCKS_EMPTY_WATER = {0, 9, 10, 31, 37, 50, 66, 68, 78, 106};
	public static int[] BLOCKS_EMPTY_LAVA_WATER_FIRE;
	
	private static final int[] ITEMS_PICKAXES = {257, 270, 274, 278, 285};
	private static final int[] ITEMS_SHOVELS = {256, 269, 273, 277, 284};
	private static final int[] ITEMS_AXES = {258, 271, 275, 279, 286};
	private static final int[] ITEMS_FOOD = {297, 354, 366, 349, 320, 357, 322, 362, 360, 335, 39, 40, 282, 361, 363, 365, 349, 319, 260, 367, 295, 364, 353, 338, 296};

	private static String CMD_PREFIX = "\\.";
	private static int LAG = 5;
	
	/** KeyBinding */
	public static KeyBinding keyBindBotPause = new KeyBinding("Bot v2 Pause", 23);
	public static KeyBinding keyBindBotMenu = new KeyBinding("Bot v2 Menu", 24);
	public static KeyBinding keyBindBotMacro = new KeyBinding("Bot v2 Macro", 25);
	/** Initialize everything. */
	public MinecraftBot(Minecraft parMc){
		
		mb = this;
		tickCount = 0;
		parMc.playerController = new MBPlayerControllerMP(parMc, parMc.getNetHandler());
		tools = new MBTools();
		chat = new MBChat();
		scanner = new MBScanner();
		this.mc = parMc;
		if(firstInstance){
			
			KeyBinding[] arr1and2 = new KeyBinding[parMc.gameSettings.keyBindings.length + keyList.length];
			System.arraycopy(parMc.gameSettings.keyBindings, 0, arr1and2, 0, parMc.gameSettings.keyBindings.length);
			System.arraycopy(keyList, 0, arr1and2, parMc.gameSettings.keyBindings.length, keyList.length);
			parMc.gameSettings.keyBindings = arr1and2;
		}
		lastTick = System.currentTimeMillis();
		firstInstance = false;
		arraySort();
        System.out.println("LAUNCHING NEW INSTANCE OF BOT!!!");
	}
	
	/** Entry point of the bot for each game tick */
	public void tick(){
		
		/* Key listener */
		if(this.keyBindBotMacro.isPressed()) manager = MBManager.chopStuff(this, BLOCKS_WOOD);
		if(this.keyBindBotMenu.isPressed()) tools.s("Brightness: " + mc.thePlayer.getBrightness(1));
		if(this.keyBindBotPause.isPressed()) manager = MBManager.chopSquare(this, new MBSquare(mb));
		
		/* If exist, run manager and delete it if return value is false */
		if(manager != null && manager.run() != MANAGER_RETURN.KEEP_RUNNING) manager = null;
		
		/* FPS counter */
        tickCount++;
        if(System.currentTimeMillis() - lastTick > 5000){
        	
        	System.out.println("TICK/sec: " + (System.currentTimeMillis() - lastTick) / tickCount);
        	lastTick = System.currentTimeMillis();
        	tickCount = 0;
        }
        	
	}
	
	public static KeyBinding keyList[] = {keyBindBotPause, keyBindBotMenu, keyBindBotMacro};

	private void arraySort(){
		  Arrays.sort(BLOCKS_LAVA_WATER_FIRE);
		  Arrays.sort(BLOCKS_ORE);
		  Arrays.sort(BLOCKS_DIRT);
		  Arrays.sort(BLOCKS_WOOD);
		  Arrays.sort(BLOCKS_BUILD);
		  Arrays.sort(BLOCKS_EMPTY);
		  Arrays.sort(BLOCKS_EMPTY_WATER);
		  Arrays.sort(BLOCKS_LAVA_WATER);
		  //Arrays.sort(ITEMS_PICKAXES);
		  //Arrays.sort(ITEMS_AXES);
		  //Arrays.sort(ITEMS_SHOVELS);
		  Arrays.sort(ITEMS_FOOD);
		  BLOCKS_EMPTY_LAVA_WATER_FIRE = new int[BLOCKS_EMPTY.length + BLOCKS_LAVA_WATER_FIRE.length];
		  System.arraycopy(BLOCKS_EMPTY, 0, BLOCKS_EMPTY_LAVA_WATER_FIRE, 0, BLOCKS_EMPTY.length);
		  System.arraycopy(BLOCKS_LAVA_WATER_FIRE, 0, BLOCKS_EMPTY_LAVA_WATER_FIRE, BLOCKS_EMPTY.length, BLOCKS_LAVA_WATER_FIRE.length);
		  Arrays.sort(BLOCKS_EMPTY_LAVA_WATER_FIRE);
		  }
	
	/** Interface for any bot used by MinecraftBot */
	interface Bot {
		
		/** Return positive value when task is finished successfully. 
		 *  Return negative value when task is finished with errors. 
		 *  Return zero when task is not finished. */
		void run(Stack<Bot> lst);
	}
	
	/** Bot for movement related functions */
	class BotMove implements Bot {
		
		/** Path */
		BotPath path;
		MBVec currStep;
		
		/**
		 *  Go to specified position.  
		 *  
		 * @param moveInside: False move beside the block. True move to the block.
		 */
		BotMove(MBVec vec, boolean moveInside){

			this(vec, moveInside, "Noname");
		}
		
		/**
		 *  Go to specified position.  
		 *  
		 * @param moveInside: False move beside the block. True move to the block.
		 */
		BotMove(MBVec vec, boolean moveInside, String parName){

			tools.p("New BotMove to vec " + vec.toString());
			if (moveInside) path = new BotPath(new MBVec(0, -1, 0), vec, parName);
			else  path = new BotPath(vec, parName);
			unpressAll();
		}
		
		/**
		 *  Follow precomputed path.  
		 *  
		 * @param moveInside: False move beside the block. True move to the block.
		 */
		BotMove(BotPath parPath){

			tools.p("New BotMove following path to " + parPath.path.peek().toString());
			path = parPath;
			unpressAll();
		}
		
		/** Find and Go to specified items */
		BotMove(int[] items, String parName){

			tools.p("New BotMove to items " + Arrays.toString(items));
			path = new BotPath(new MBVec(0, -1, 0), items, parName);
			unpressAll();
		}

		/** Return positive value when task is finished successfully. 
		 *  Return negative value when task is finished with errors. 
		 *  Return zero when task is not finished. */
		public void run(Stack<Bot> lst){ 
			
			if(path.path != null) {
				
				if (currStep != null && currStep.distanceToFeet() < 0.5){ path.path.remove(currStep); currStep = null;}
				if (currStep == null){ currStep = getNextStep(); tools.p("BotMove  - Distance left: " + (currStep != null ? currStep.distanceToFeet() : "Destination Reach"));}
				if (currStep != null){
					
					if (currStep.distanceToFeet() >= 2) {
						
						currStep = null;
						tools.p("BotMove (" + path.name + ") - Recalculating path. Old path.path.size(): " + path.path.size());
						path = new BotPath(new MBVec(0, -1, 0), path.path.firstElement(), path.name);
					} else {
						
						tools.lookAt(currStep, 0f);
						if (currStep.yCoord > mc.thePlayer.posY - 1 && !scanner.scanVec(new MBVec(0, -1, 1), BLOCKS_EMPTY_LAVA_WATER_FIRE)){ btnJump = true; btnSneak = true;}
							else{ btnJump = false; btnSneak = false;}
						btnForward = true;
					}
					
				} else {
					
					unpressAll();
					refreshKeys();
					lst.pop();
					return;
					
				}
			} else if (path.aStar == null) {

				unpressAll();
				refreshKeys();
				lst.pop();
				return;
			}
			
			refreshKeys();
			return;
		}
		
		/** Halt the bot */
		public void unpressAll(){
			
			btnForward = false;
			btnBackward = false;
			btnSneak = false;
			btnJump = false;
		}
		
		/** Refresh Keys */
		public void refreshKeys(){
			
	        if (mc.gameSettings.keyBindForward.pressed != btnForward) mc.gameSettings.keyBindForward.pressed = btnForward;
	        if (mc.gameSettings.keyBindBack.pressed != btnBackward) mc.gameSettings.keyBindBack.pressed = btnBackward;
	        if (mc.gameSettings.keyBindJump.pressed != btnJump) mc.gameSettings.keyBindJump.pressed = btnJump;
	        if (mc.gameSettings.keyBindSneak.pressed != btnSneak) mc.gameSettings.keyBindSneak.pressed = btnSneak;
		}
		
		private MBVec getNextStep() {
			
			if (path.path.size() < 1) {
				
				//path.path = null; 
				return null;
			}
			
			return path.path.peek();
		}
	}
	
	/** Bot for inventory management */
	class BotInventory implements Bot {
		
		int[] targetItems;

		/** Contructor */
		BotInventory(int item){ 
			
			this(new int[]{item});
			}

		/** Contructor */
		BotInventory(int[] items){ 
			
			targetItems = items;
			}

		/** Contructor */
		BotInventory(MBVec vec){ 
			
			if(scanner.scanVec(vec, BLOCKS_ORE)) targetItems = ITEMS_PICKAXES;
	   else if(scanner.scanVec(vec, BLOCKS_DIRT)) targetItems = ITEMS_SHOVELS;
	   else if(scanner.scanVec(vec, BLOCKS_WOOD)) targetItems = ITEMS_AXES;
			}

		/** Return positive value when task is finished successfully. 
		 *  Return negative value when task is finished with errors. 
		 *  Return zero when task is not finished. */
		public void run(Stack<Bot> lst){ 
			
			if(lst == null) lst = new Stack<Bot>();
			
			if(targetItems == null || targetItems.length < 1){
				
				if(lst.size() > 0) lst.pop();
				return;
			}
			
			if(!isHoldingTargetItems()){
				
				int tmpSlot = getTargetItemSlot();
				if(tmpSlot != -1){
					
					// Item not in hand and present in inventory
					tools.p("BotInventory  - Swapping item in slot " + tmpSlot + " for item in slot " + (36 + mc.thePlayer.inventory.currentItem));
					mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, tmpSlot, 0, 0, mc.thePlayer);
					mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, 36 + mc.thePlayer.inventory.currentItem, 0, 0, mc.thePlayer);
					mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, tmpSlot, 0, 0, mc.thePlayer);
				} else {

					if(lst.size() > 0) lst.pop();
					return;
				}
			} else {

				if(lst.size() > 0) lst.pop();
				return;
			}

			if(lst.size() > 0) lst.pop();
			return;
			}
		
		private boolean isHoldingTargetItems(){
			
			int itemId = mc.thePlayer.getCurrentEquippedItem() == null ? -1 : mc.thePlayer.getCurrentEquippedItem().itemID;
			return Arrays.binarySearch(targetItems, itemId) >= 0;
		}
		
		private int getTargetItemSlot(){
			
			int targetSlot = -1;
			int currTargetItem = targetItems.length;
			ItemStack currStack;
			
			for(int i = 0; i < mc.thePlayer.inventoryContainer.inventorySlots.size(); i++){

				currStack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
				for(int j = 0; j < targetItems.length; j++){
					
					// currTargetItem is used to determine tools priority
					if(currStack != null && currStack.getItem().itemID == targetItems[j] && currTargetItem > j){
						
						targetSlot = i;
						currTargetItem = j;
					}
				}
			}
			
			return targetSlot;
		}
	}
	
	/** Bot for mining */
	class BotMiner implements Bot {

		MBVec target;
		int[] items;
		int lag;
		boolean flag;
		
		BotMiner(MBVec vec){ 
			
			target = vec.cP();
			new BotInventory(vec).run(null);
			lag = LAG;
			flag = true;
		}

		public void run(Stack<Bot> lst){ 
			
			if(scanner.isTouchingLavaWater(target)){

				lst.pop();
				return;
			}
			
			if(flag) checkSide(target);
			
			tools.lookAt(target);
			
			if(target.side != 0) mc.playerController.onPlayerDamageBlock(target.xInt(), target.yInt(), target.zInt(), target.getSide());
			else mc.gameSettings.keyBindAttack.pressed = true;
			/*
            mc.getSendQueue().addToSendQueue(new Packet14BlockDig(0, target.xInt(), target.yInt(), target.zInt(), target.getSide()));
            mc.getSendQueue().addToSendQueue(new Packet14BlockDig(2, target.xInt(), target.yInt(), target.zInt(), target.getSide())); 
            */
			mc.thePlayer.swingItem();
			if (target.getId() == 0){

				if(lag == 0){
					mc.gameSettings.keyBindAttack.pressed = false;
					lst.pop();
					return;
				}else lag--;
			}
			
			return;
		}
		
		private void checkSide(MBVec parTarget){
			
			flag = false;
			Double sDist;
			int face = 5;
			
			if(target.yCoord > mc.thePlayer.posY){
				target.side = 0;
				target.yCoord -= 0.5;
				return;
			}
			
			parTarget.xCoord += 0.5;
			sDist = parTarget.distanceTo();
			parTarget.xCoord -= 1.0;
			if(parTarget.distanceTo() < sDist){
				sDist = parTarget.distanceTo();
				face = 4;
			}
			parTarget.xCoord += 0.5;
			parTarget.zCoord += 0.5;
			if(parTarget.distanceTo() < sDist){
				sDist = parTarget.distanceTo();
				face = 3;
			}
			parTarget.zCoord -= 1.0;
			if(parTarget.distanceTo() < sDist){
				sDist = parTarget.distanceTo();
				face = 2;
			}
			parTarget.zCoord += 0.5;

			parTarget.side = face;
			
			switch(face){
			case 5:{
				parTarget.xCoord += 0.4;
				break;
			}
			case 4:{
				parTarget.xCoord -=0.4;
				break;
			}
			case 3:{
				parTarget.zCoord += 0.4;
				break;
			}
			case 2:{
				parTarget.zCoord -= 0.4;
				break;
			}
			}
			
		}
	}
	
	/** Bot for building */
	class BotBuilder implements Bot {

		MBVec target;
		int[] items;
		
		BotBuilder(MBVec vec, int[] items, int parSide){ 
			
			target = vec.cP();
			target.side = parSide;
			new BotInventory(items).run(null);
		}

		/** Return positive value when task is finished successfully. 
		 *  Return negative value when task is finished with errors. 
		 *  Return zero when task is not finished. */
		public void run(Stack<Bot> lst){ 
			
			tools.lookAt(target);
			mc.thePlayer.sendQueue.addToSendQueue(new Packet15Place(target.xInt(), target.yInt(), target.zInt(), target.getSide(), mc.thePlayer.inventory.getCurrentItem(), (float)target.hitVec().xCoord, (float)target.hitVec().yCoord, (float)target.hitVec().zCoord));
			mc.thePlayer.swingItem();
			
			return;
		}
	}
	
	/** Path calculator */
	class BotPath implements Bot {

		private class Node implements Comparable<Node>{
			
			protected int x, y, z;
			protected int f, g, h;
			protected int x2, y2, z2;
			protected int dx, dy, dz;
			
			protected Node parent = null;
			
			protected Node(Node parParent, int parx, int pary, int parz, int parx2, int pary2, int parz2, int parg){this(parParent, parx, pary, parz, parx2, pary2, parz2, 0, 0, 0, parg);}
			protected Node(Node parParent, int parx, int pary, int parz, int parx2, int pary2, int parz2, int pardx, int pardy, int pardz, int parg){parent=parParent;
																																				    x=parx+pardx;
																																				    y=pary+pardy;
																																				    z=parz+pardz;
																																				    x2=parx2;
																																				    y2=pary2;
																																				    z2=parz2;
																																				    dx=pardx!=0?3:0;
																																				    dy=pardy!=0?3:0;
																																				    dz=pardz!=0?3:0;
																																				    g=parg;
																																				    h=h();
																																				    f=f();}
			
			protected int h(){ return 15 * (Math.abs(x2 - this.x) + Math.abs(y2 - this.y) + Math.abs(z2 - this.z));}
			protected int f(){ return g+h;}
			protected int getId(int x, int y, int z){ return mc.theWorld.getBlockId(x, y, z);}
			protected int[] getIds(){ 
				
				int[] intList = new int[21];

				intList[0] = getId(x+1,y-2,z);
				intList[1] = getId(x+1,y-1,z);
				intList[2] = getId(x+1,y,z);
				intList[3] = getId(x+1,y+1,z);
				intList[4] = getId(x+1,y+2,z);

				intList[5] = getId(x-1,y-2,z);
				intList[6] = getId(x-1,y-1,z);
				intList[7] = getId(x-1,y,z);
				intList[8] = getId(x-1,y+1,z);
				intList[9] = getId(x-1,y+2,z);
				
				intList[10] = getId(x,y-2,z+1);
				intList[11] = getId(x,y-1,z+1);
				intList[12] = getId(x,y,z+1);
				intList[13] = getId(x,y+1,z+1);
				intList[14] = getId(x,y+2,z+1);
				
				intList[15] = getId(x,y-2,z-1);
				intList[16] = getId(x,y-1,z-1);
				intList[17] = getId(x,y,z-1);
				intList[18] = getId(x,y+1,z-1);
				intList[19] = getId(x,y+2,z-1);
				
				intList[20] = Arrays.binarySearch(BLOCKS_EMPTY, getId(x,y+2,z));
				
				return intList;
				
				}
			protected boolean isLast(){ return x == x2 && y == y2 && z == z2;}
			
			protected List<Node> getBros(){
				
				List<Node> broList = new ArrayList<Node>();
				int[] intList = getIds();
				
				if(Arrays.binarySearch(BLOCKS_EMPTY, intList[3]) >= 0)
				{
					
					if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[2]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[1]) < 0) broList.add(new Node(this, x, y, z, x2, y2, z2, 1, 0, 0, g+dz+10));
					
					if (Arrays.binarySearch(BLOCKS_EMPTY, intList[2]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[1]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[0]) < 0) broList.add(new Node(this, x, y, z, x2, y2, z2, 1, -1, 0, g+dz+13));
					
					if (intList[20] >= 0){
						if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[4]) >= 0 && 
								Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[2]) < 0) broList.add(new Node(this, x, y, z, x2, y2, z2, 1, +1, 0, g+dz+16));}}

				if(Arrays.binarySearch(BLOCKS_EMPTY, intList[8]) >= 0)
				{
					
					if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[7]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[6]) < 0) broList.add(new Node(this, x, y, z, x2, y2, z2, -1, 0, 0, g+dz+10));
					
					if (Arrays.binarySearch(BLOCKS_EMPTY, intList[7]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[6]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[5]) < 0) broList.add(new Node(this, x, y, z, x2, y2, z2, -1, -1, 0, g+dz+13));
					
					if (intList[20] >= 0){
						if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[9]) >= 0 && 
								Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[7]) < 0) broList.add(new Node(this, x, y, z, x2, y2, z2, -1, +1, 0, g+dz+16));}}
				
				if(Arrays.binarySearch(BLOCKS_EMPTY, intList[13]) >= 0)
				{
					
					if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[12]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[11]) < 0) broList.add(new Node(this, x, y, z, x2, y2, z2, 0, 0, +1, g+dx+10));
					
					if (Arrays.binarySearch(BLOCKS_EMPTY, intList[12]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[11]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[10]) < 0) broList.add(new Node(this, x, y, z, x2, y2, z2, 0, -1, +1, g+dx+13));
					
					if (intList[20] >= 0){
						if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[14]) >= 0 && 
								Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[12]) < 0) broList.add(new Node(this, x, y, z, x2, y2, z2, 0, +1, +1, g+dx+16));}}
				
					if(Arrays.binarySearch(BLOCKS_EMPTY, intList[18]) >= 0)
					{
						
						if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[17]) >= 0 && 
								Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[16]) < 0) broList.add(new Node(this, x, y, z, x2, y2, z2, 0, 0, -1, g+dx+10));
						
						if (Arrays.binarySearch(BLOCKS_EMPTY, intList[17]) >= 0 && 
								Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[16]) >= 0 && 
								Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[15]) < 0) broList.add(new Node(this, x, y, z, x2, y2, z2, 0, -1, -1, g+dx+13));
						
						if (intList[20] >= 0){
							if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[19]) >= 0 && 
									Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[17]) < 0) broList.add(new Node(this, x, y, z, x2, y2, z2, 0, +1, -1, g+dx+16));}}
					
				return broList;
			}

			public int compareTo(Node o){return this.f - o.f;}
			@Override public boolean equals(Object o){if(o.getClass()!=this.getClass())return false;
													  return ((Node)o).x==this.x&&((Node)o).y==this.y&&((Node)o).z==this.z;}
		
		}
		private class NodeReach extends Node{
			
			protected NodeReach(Node parParent, int parx, int pary, int parz, int parx2, int pary2, int parz2, int parg){this(parParent, parx, pary, parz, parx2, pary2, parz2, 0, 0, 0, parg);}
			protected NodeReach(Node parParent, int parx, int pary, int parz, int parx2, int pary2, int parz2, int pardx, int pardy, int pardz, int parg){ super(parParent, parx, pary, parz, parx2, pary2, parz2, pardx, pardy, pardz, parg);}
			
			protected List<Node> getBros(){
				
				List<Node> broList = new ArrayList<Node>();
				int[] intList = getIds();
				
				if (y == y2 || y+1 == y2){
				
					if (x+1 == x2 || x-1 == x2){
						
						if (z == z2) {broList.add(new Node(this.parent, x, y, z, x, y, z, g)); return broList;}
					}
					else if (x == x2){
						
						if (z-1 == z2 || z+1 == z2) {broList.add(new Node(this.parent, x, y, z, x, y, z, g)); return broList;}
					}
				}
				
				if(Arrays.binarySearch(BLOCKS_EMPTY, intList[3]) >= 0)
				{
					
					if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[2]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[1]) < 0) broList.add(new NodeReach(this, x, y, z, x2, y2, z2, 1, 0, 0, g+dz+10));
					
					if (Arrays.binarySearch(BLOCKS_EMPTY, intList[2]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[1]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[0]) < 0) broList.add(new NodeReach(this, x, y, z, x2, y2, z2, 1, -1, 0, g+dz+13));
					
					if (intList[20] >= 0){
						if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[4]) >= 0 && 
								Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[2]) < 0) broList.add(new NodeReach(this, x, y, z, x2, y2, z2, 1, +1, 0, g+dz+16));}}

				if(Arrays.binarySearch(BLOCKS_EMPTY, intList[8]) >= 0)
				{
					
					if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[7]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[6]) < 0) broList.add(new NodeReach(this, x, y, z, x2, y2, z2, -1, 0, 0, g+dz+10));
					
					if (Arrays.binarySearch(BLOCKS_EMPTY, intList[7]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[6]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[5]) < 0) broList.add(new NodeReach(this, x, y, z, x2, y2, z2, -1, -1, 0, g+dz+13));
					
					if (intList[20] >= 0){
						if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[9]) >= 0 && 
								Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[7]) < 0) broList.add(new NodeReach(this, x, y, z, x2, y2, z2, -1, +1, 0, g+dz+16));}}
				
				if(Arrays.binarySearch(BLOCKS_EMPTY, intList[13]) >= 0)
				{
					
					if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[12]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[11]) < 0) broList.add(new NodeReach(this, x, y, z, x2, y2, z2, 0, 0, +1, g+dx+10));
					
					if (Arrays.binarySearch(BLOCKS_EMPTY, intList[12]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[11]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[10]) < 0) broList.add(new NodeReach(this, x, y, z, x2, y2, z2, 0, -1, +1, g+dx+13));
					
					if (intList[20] >= 0){
						if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[14]) >= 0 && 
								Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[12]) < 0) broList.add(new NodeReach(this, x, y, z, x2, y2, z2, 0, +1, +1, g+dx+16));}}
				
					if(Arrays.binarySearch(BLOCKS_EMPTY, intList[18]) >= 0)
					{
						
						if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[17]) >= 0 && 
								Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[16]) < 0) broList.add(new NodeReach(this, x, y, z, x2, y2, z2, 0, 0, -1, g+dx+10));
						
						if (Arrays.binarySearch(BLOCKS_EMPTY, intList[17]) >= 0 && 
								Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[16]) >= 0 && 
								Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[15]) < 0) broList.add(new NodeReach(this, x, y, z, x2, y2, z2, 0, -1, -1, g+dx+13));
						
						if (intList[20] >= 0){
							if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[19]) >= 0 && 
									Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[17]) < 0) broList.add(new NodeReach(this, x, y, z, x2, y2, z2, 0, +1, -1, g+dx+16));}}
					
				return broList;
			}
		
		}
		private class NodeSearch extends Node{
			
			private int items[] = null;
			protected boolean isFirst = false;
			protected boolean flag = false;
			
			NodeSearch(Node parParent, int parx, int pary, int parz, int parx2, int pary2, int parz2, int parg, int[] items, boolean parFlag){this(parParent, parx, pary, parz, parx2, pary2, parz2, 0, 0, 0, parg, items, parFlag);}
			NodeSearch(Node parParent, int parx, int pary, int parz, int parx2, int pary2, int parz2, int dx,  int dy,  int dz, int parg, int[] items, boolean parFlag){super(parParent, parx, pary, parz, parx2, pary2, parz2, dx, dy, dz, parg);
																																				       					this.items = items;
																																				       					this.flag = parFlag;
																																				       					if(parx==parx2&&pary==pary2&&parz==parz2) isFirst=true;}

			@Override protected boolean isLast(){return false;}
			
			@Override protected List<Node> getBros(){
				
				List<Node> broList = new ArrayList<Node>();
				int[] intList = getIds();

				if(!flag){
					
					for (int i = 0; i < 4; i++) if(Arrays.binarySearch(items, intList[i*5+2])>=0||Arrays.binarySearch(items, intList[i*5+3])>=0){broList.add(new Node(this.parent, x, y, z, x, y, z, g)); return broList;}
				}
				else {

					if(Arrays.binarySearch(items, intList[2]) >= 0){ broList.add(new Node(this.parent, x+1, y, z, x+1, y, z, g)); return broList;}
					else if(Arrays.binarySearch(items, intList[3]) >= 0){ broList.add(new Node(this.parent, x+1, y+1, z, x+1, y+1, z, g)); return broList;}
					else if(Arrays.binarySearch(items, intList[7]) >= 0){ broList.add(new Node(this.parent, x-1, y, z, x-1, y, z, g)); return broList;}
					else if(Arrays.binarySearch(items, intList[8]) >= 0){ broList.add(new Node(this.parent, x-1, y+1, z, x-1, y+1, z, g)); return broList;}
					else if(Arrays.binarySearch(items, intList[12]) >= 0){ broList.add(new Node(this.parent, x, y, z+1, x, y, z+1, g)); return broList;}
					else if(Arrays.binarySearch(items, intList[13]) >= 0){ broList.add(new Node(this.parent, x, y+1, z+1, x, y+1, z+1, g)); return broList;}
					else if(Arrays.binarySearch(items, intList[17]) >= 0){ broList.add(new Node(this.parent, x, y, z-1, x, y, z-1, g)); return broList;}
					else if(Arrays.binarySearch(items, intList[18]) >= 0){ broList.add(new Node(this.parent, x, y+1, z-1, x, y+1, z-1, g)); return broList;}
				}
				
				if(Arrays.binarySearch(BLOCKS_EMPTY, intList[3]) >= 0)
				{
					
					if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[2]) >= 0 && 
							/* Walkable, add to list. */
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[1]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, 1, 0, 0, g+dz+10, items, flag));
					
					if (Arrays.binarySearch(BLOCKS_EMPTY, intList[2]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[1]) >= 0 && 
							/* Walkable, add to list. */
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[0]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, 1, -1, 0, g+dz+13, items, flag));
					
					if (intList[20] >= 0){
						if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[4]) >= 0 && 
								/* Walkable, add to list. */
								Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[2]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, 1, +1, 0, g+dz+16, items, flag));}}

				if(Arrays.binarySearch(BLOCKS_EMPTY, intList[8]) >= 0)
				{
					
					if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[7]) >= 0 && 
							/* Walkable, add to list. */
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[6]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, -1, 0, 0, g+dz+10, items, flag));
					
					if (Arrays.binarySearch(BLOCKS_EMPTY, intList[7]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[6]) >= 0 && 
							/* Walkable, add to list. */
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[5]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, -1, -1, 0, g+dz+13, items, flag));
					
					if (intList[20] >= 0){
						if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[9]) >= 0 && 
								/* Walkable, add to list. */
								Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[7]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, -1, +1, 0, g+dz+16, items, flag));}}
				
				if(Arrays.binarySearch(BLOCKS_EMPTY, intList[13]) >= 0)
				{
					
					if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[12]) >= 0 && 
							/* Walkable, add to list. */
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[11]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, 0, 0, +1, g+dx+10, items, flag));
					
					if (Arrays.binarySearch(BLOCKS_EMPTY, intList[12]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[11]) >= 0 &&
							/* Walkable, add to list. */
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[10]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, 0, -1, +1, g+dx+13, items, flag));
					
					if (intList[20] >= 0){
						if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[14]) >= 0 &&
								/* Walkable, add to list. */
								Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[12]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, 0, +1, +1, g+dx+16, items, flag));}}
				
					if(Arrays.binarySearch(BLOCKS_EMPTY, intList[18]) >= 0)
					{
						
						if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[17]) >= 0 &&
								/* Walkable, add to list. */
								Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[16]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, 0, 0, -1, g+dx+10, items, flag));
						
						if (Arrays.binarySearch(BLOCKS_EMPTY, intList[17]) >= 0 && 
								Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[16]) >= 0 &&
								/* Walkable, add to list. */
								Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[15]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, 0, -1, -1, g+dx+13, items, flag));
						
						if (intList[20] >= 0){
							if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[19]) >= 0 &&
									/* Walkable, add to list. */
									Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[17]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, 0, +1, -1, g+dx+16, items, flag));}}
					
				return broList;
				
			}
		}		
		private class NodeComparateur implements Comparator<Node>{
			
			public int compare(Node o1, Node o2){return o1.x==o2.x&&o1.y==o2.y&&o1.z==o2.z?0:1;}
		}
		
		private Node start;
		Stack<MBVec> path = null;
		AStar aStar = null;
		public String name;
		
		private class AStar extends Thread{
			
			private List<Node> openNode;
			private List<Node> closeNode;
			private Node currNode;
			private BotPath parent;
			public boolean search = true;
			public int status;
			
			public AStar(Node start, BotPath parent){this.parent=parent;
													openNode = new ArrayList<Node>();
													closeNode = new ArrayList<Node>();
													openNode.add(start);}
			
			public void run(){

				List<Node> tmpList = new ArrayList<Node>();
				long tick = System.currentTimeMillis();
				
				status = 0; // NO PATH FOUND
				
				while (search){
					if(openNode.size()>0){currNode=Collections.min(openNode);
										  openNode.remove(currNode);}
					else {tools.p("AStar (" + name + ") - Bloque. Aucun chemin trouve");search=false;};
						closeNode.add(currNode);
						if(currNode.isLast()){status = 1;search=false;} // PATH FOUND
						if(System.currentTimeMillis()-tick > PATH_SEARCH_TIMEOUT){tools.p("AStar (" + name + ") - Timeout. Aucun chemin trouve");search=false;}
						tmpList = currNode.getBros();
						for (int i = 0; i < tmpList.size(); i++){
							Node tmpNode = tmpList.get(i);
							if(!closeNode.contains(tmpNode)){
								int j = openNode.indexOf(tmpNode);
								if( j > -1) {if ( tmpNode.g < openNode.get(j).g ) 
									openNode.set(j, tmpNode);
								}
								else openNode.add(tmpNode);}
						}
				}
				
				tools.p("AStar (" + name + ") - Paths scanned: " + closeNode.size());
				if(status == 1) parsePath();
				parent.aStar = null;
				tools.p("AStar (" + name + ") - TASK FINISHED");
			}
	
			private void parsePath(){

				if(parent.path == null) parent.path = new Stack<MBVec>();
				parent.path.clear();
				
				do{
					parent.path.add(new MBVec((double)currNode.x, (double)currNode.y, (double)currNode.z, 1, false));
					parent.path.peek().cP();
					//placeObject(parent.path.peekLast());
					currNode = currNode.parent;
				}while (currNode!=null);
				
				tools.p("AStar (" + name + ") - Path ready. (Size : " + parent.path.size() + " blocks.)");
				tools.p("AStar (" + name + ") - Last node is at : " + parent.path.firstElement().xCoord + ", " + parent.path.firstElement().yCoord + ", " + parent.path.firstElement().zCoord);
			}

		}

		public BotPath(MBVec parVec, String parName){ MBVec deleteMe = new MBVec(0, -1, 0); this.name = parName; start = new NodeReach(null, deleteMe.xInt(), deleteMe.yInt(), deleteMe.zInt(), parVec.xInt(), parVec.yInt(), parVec.zInt(), 0); tools.p("NodeStore (" + name + ") - Reach X: " + start.x2 + " Y: " + start.y2 + " Z: " + start.z2); search();}
		public BotPath(MBVec parStart, MBVec parEnd, String parName){ this.name = parName; start = new Node(null, parStart.xInt(), parStart.yInt(), parStart.zInt(), parEnd.xInt(), parEnd.yInt(), parEnd.zInt(), 0); tools.p("NodeStore (" + name + ") - Start set. X: " + start.x + " Y: " + start.y + " Z: " + start.z); search();}
		public BotPath(MBVec vec, int[] items, String parName){ this.name = parName; start = new NodeSearch(null, vec.xInt(), vec.yInt(), vec.zInt(), vec.xInt(), vec.yInt(), vec.zInt(), 0, items, false); search();}
		public BotPath(MBVec vec, int[] items, String parName, boolean parOutside){ this.name = parName; start = new NodeSearch(null, vec.xInt(), vec.yInt(), vec.zInt(), vec.xInt(), vec.yInt(), vec.zInt(), 0, items, parOutside); search();}
		public Stack<MBVec> getPath(){ return path;}
		public void search(){aStar = new AStar(start, this); tools.p("NodeStore (" + name + ") - Start search..."); aStar.start();}
		
		public void run(Stack<Bot> lst) {

			if(aStar != null && path == null) return;
			else if(path != null) return;
			else{
				
				lst.pop();
				return;
			}
		}
	
	}
	
	/** Handle new chat message. Return true if message is for bot. */
	public class MBChat {
		
		public boolean isForBot(String msg){String cmd = isForBot_do(msg); 
											if(cmd != "") cmd = parseCmd(cmd.split(":"));
											return cmd != "";}

		private String isForBot_do(String s){
			
			String[] str = s.split(CMD_PREFIX, 2);
			if(str[0].compareTo("") == 0 && str.length > 1) return str[1];
			
			return "";}
		
		private String parseCmd(String[] cmd){
			String[] args = null;
			if(cmd.length > 1) args = cmd[1].split(",");;
			
			try{
				if(cmd[0].toLowerCase().compareTo("mine") == 0)
				{
					int buffX = 1, buffY = 1, buffZ = 1;
					boolean left = false;
					if(args.length >= 1){
						buffX = Integer.parseInt(args[0]);
					}
					if(args.length >= 2){
						buffY = Integer.parseInt(args[1]);
					}
					if(args.length >= 3){
						buffZ = Integer.parseInt(args[2]);
					}
					if(args.length == 4){
						left = args[3].toLowerCase().compareTo("left") == 0;
					}
					if(args.length > 4) return "";
					manager = MBManager.chopSquare(mb, new MBSquare(mb, buffX, buffY, buffZ, left));
				}
				else if(cmd[0].toLowerCase().compareTo("chop") == 0)
				{
					int itemId = 1, qnt = 0;
					if(args.length >= 1){
						itemId = Integer.parseInt(args[0]);
					}
					if(args.length == 2){
						qnt = Integer.parseInt(args[1]);
					}
					if(args.length > 2) return "";
					manager = MBManager.chopStuff(mb, new int[]{itemId});
				}
				else if(cmd[0].toLowerCase().compareTo("search") == 0)
				{
					int itemId = 17;
					if(args.length == 1){
						itemId = Integer.parseInt(args[0]);
					}
					if(args.length > 1) return "";
					manager = MBManager.searchAndReach(mb, new int[]{itemId});
				}
				else if(cmd[0].toLowerCase().compareTo("lag") == 0)
				{
					if(args.length == 1){
						LAG = Integer.parseInt(args[0]);
					}
					if(args.length > 1) return "";
				}else if(cmd[0].toLowerCase().compareTo("stop") == 0)
				{
					if(manager != null) manager.stop();
				}
				else return "";
			}catch (NumberFormatException e){
				return "";
			}
			
			return "ANYTHING_GOES_HERE";
		}
	}
	
	/** Vec3 implementation with useful tools */
	class MBVec extends Vec3 {
		
		private int side;
		private float yaw;
		
		MBVec(){ this(true);}
		MBVec(float parYaw){ this(true); this.yaw=parYaw;}
		MBVec(Vec3 vec){ this(vec.xCoord, vec.yCoord, vec.zCoord, 0, false);}
		MBVec(boolean centered){ this(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, 0, centered);}
		MBVec(int x, int y, int z){ this(x, y, z, true);}
		MBVec(int x, int y, int z, boolean centered){ this(x, y, z, 0, centered);}
		MBVec(int x, int y, int z, int side, boolean centered){ this(mc.thePlayer.posX + tools.relX2Abs(x, z), mc.thePlayer.posY + tools.relY2Abs(y), mc.thePlayer.posZ + tools.relZ2Abs(x, z), side, centered);}
		MBVec(double x, double y, double z){this(x,y,z,0,false);}
		MBVec(double x, double y, double z, int side, boolean centered){super(null, x,y,z);
																		this.side = side;
																		if(centered) this.cP();}


		int xInt(){return rP(this.xCoord);}
		int yInt(){return (int)(this.yCoord);}
		int zInt(){return rP(this.zCoord);}
		
		private int xInt(double x){return rP(x);}
		private int yInt(double y){return (int)(y);}
		private int zInt(double z){return rP(z);}
		
		int getSide(){return side;}

		MBVec hitVec(){double x = 0.5, y = 0.5, z = 0.5; x += this.side==5? 0.5: this.side==4? -0.5: 0; y += this.side==0? -0.5: this.side==1? 0.5: 0; z += this.side==3? 0.5: this.side==2? -0.5: 0; return new MBVec(x, y, z);}

		/** Move vec position to the exact center of the block. */
		MBVec cP(){this.xCoord=this.xInt() + 0.5;
				  this.yCoord=this.yInt() + 0.5;
				  this.zCoord=this.zInt() + 0.5;
				  return this;}
		/** Level vec height one block above ground. */
		MBVec aH(){tools.p("MBVec  - aH " + toString()); 
					while(Arrays.binarySearch(BLOCKS_EMPTY_WATER, getId()) >= 0) yCoord-=1;
   				  	while(Arrays.binarySearch(BLOCKS_EMPTY_WATER, getId()) < 0) yCoord+=1;
   				  	return this;}
			
		int rP(double a){return (a>=0)?((int)a):a==(int)a?(int)a:((int)a)-1;} // Round to smaller int
	
		double distanceTo(MBVec vec){return this.distanceTo((Vec3)vec);}
		public double distanceTo(Vec3 vec){double var2 = vec.xCoord - this.xCoord;
							        	   double var4 = vec.yCoord - this.yCoord;
							        	   double var6 = vec.zCoord - this.zCoord;
							        	   return (double)MathHelper.sqrt_double(var2 * var2 + var4 * var4 + var6 * var6);}
		double distanceTo(){return this.distanceTo(mc.thePlayer.getPosition(1));}
		double distanceToFeet(){Vec3 vec = mc.thePlayer.getPosition(1);
								double var2 = vec.xCoord - this.xCoord;
						 	    double var4 = vec.yCoord - (this.yCoord + 1);
						 	    double var6 = vec.zCoord - this.zCoord;
						 	    return (double)MathHelper.sqrt_double(var2 * var2 + var4 * var4 + var6 * var6);}

		int getId(){ return getId(0, 0, 0);}
		int getId(int dx, int dy, int dz){ return mc.theWorld.getBlockId(xInt() + tools.relX2Abs(dx, dz), yInt() + tools.relY2Abs(dy), zInt() + tools.relZ2Abs(dx, dz));}
		int getId(double dx, double dy, double dz){ return mc.theWorld.getBlockId(xInt(dx), yInt(dy), zInt(dz));}

		/* Duplicate this Vec adding dx, dy, dz as offset */
		MBVec getVec(){ return new MBVec(xCoord, yCoord, zCoord, side, false);}
		/* Duplicate this Vec as is */
		MBVec getVec(int dx, int dy, int dz){ return new MBVec(xCoord + tools.relX2Abs(dx, dz), yCoord + tools.relY2Abs(dy), zCoord + tools.relZ2Abs(dx, dz), side, false);}
		
		public String toString(){ return "" + xCoord + ", " + yCoord + ", " + zCoord;}
	}
	
	/** Tools for environment scanning */
	class MBScanner {
		
		boolean isTouchingLavaWater(MBVec vec){
			
			return scanVec(vec, BLOCKS_LAVA_WATER, new int[]{0, 1, 0, 0, -1, 0, 1, 0, 0, -1, 0, 0, 0, 0, 1, 0, 0, -1});
		}
		boolean scanVec(MBVec vec, int[] ids, int[] delta){
			
			boolean result = false;
			
			for(int i = 0; i < delta.length / 3; i++){
				
				result = result || scanVec(vec.getVec(delta[i], delta[i+1], delta[i+2]), ids);
			}
			
			return result;
		}
		boolean scanVec(MBVec vec, int[] ids){
			
			return Arrays.binarySearch(ids, vec.getId()) >= 0;
		}
	}
	
	/** Random tools */
	class MBTools {

		void p(String msg){ System.out.println("MinecraftBot: " + msg);}
		void s(String msg){ mc.ingameGUI.getChatGUI().printChatMessage("§6Bot: " + msg);}
			
		int relX2Abs(int parX, int parZ){int buff; switch(rY(mc.thePlayer.rotationYaw)){ case 270:case -90:buff = parZ; break; case 180: case -180: buff = parX; break; case 90: case -270: buff = -parZ; break; default: buff = -parX;} return buff;}
		int relY2Abs(int parY){ return parY;}
		int relZ2Abs(int parX, int parZ){int buff; switch(rY(mc.thePlayer.rotationYaw)){ case 270:case -90:buff = parX; break; case 180: case -180: buff = -parZ; break; case 90: case -270: buff = -parX; break; default: buff = parZ;} return buff;}

		int rY(float b){int rounded = 0; // Round yaw
					    float a = b % 360;
					    if(a>45&&a<=135)rounded=90;
					    if(a>135&&a<=225)rounded=180;
					    if(a>225&&a<=315)rounded=270;
					    if(a>315||(a<=45&&a>0))rounded=0;
					    if(a<-45&&a>=-135)rounded=270;
					    if(a<-135&&a>=-225)rounded=180;
					    if(a<-225&&a>=-315)rounded=90;
					    if(a<-315||(a>=-45&&a<=0))rounded=0;
					    return rounded;}

		private void lookAt(Vec3 vec){ lookAt(yawFromVec(vec), pitchFromVec(vec));}
		private void lookAt(Vec3 vec, float pitch){ lookAt(yawFromVec(vec),  pitch);}
		private void lookAt(float yaw, float pitch){ mc.thePlayer.setRotation(yaw,  pitch);}
		
		private int getSide(){float tmpY = rY(mc.thePlayer.rotationYaw);return tmpY==0.0? 3: tmpY==90.0? 4: tmpY==180.0? 2: 5;} // Associate player yaw to block side
		
		private float yawFromVec(Vec3 target){ return yawFrom2Vec(mc.thePlayer.getPosition(1), target);}
		private float yawFrom2Vec(Vec3 start, Vec3 end){ return (float)(Math.atan2(start.xCoord - end.xCoord, end.zCoord - start.zCoord) * 180 / Math.PI);}
		private float pitchFromVec(Vec3 target){ return pitchFrom2Vec(mc.thePlayer.getPosition(1), target);}
		private float pitchFrom2Vec(Vec3 start, Vec3 end){ return (float)((Math.acos((end.yCoord - start.yCoord) / start.distanceTo(end)) * 180 / Math.PI) - 90);}
	
		public int countItems(int[] items){
			
			int count = 0;
			
			for(int i = 0; i < mc.thePlayer.inventoryContainer.inventorySlots.size(); i++){
				
				if(mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null && Arrays.binarySearch(items, mc.thePlayer.inventoryContainer.getSlot(i).getStack().itemID) >= 0)
					count += mc.thePlayer.inventoryContainer.getSlot(i).getStack().stackSize;
			}
			
			return count;
		}
	}
}