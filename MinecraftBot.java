package net.minecraft.src;

import java.lang.Math;

import java.util.Arrays;
import java.util.Queue;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.lang.Thread;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import net.minecraft.src.*;
import net.minecraft.client.*;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Thaum the Bot
 * 
 * @author Pierre-Luc Bouchard
 * @email thaum@informaplux.com
 * @version 1.5.4
 *
 */
public class MinecraftBot{
	
	/*********************************************************/
	// Chat hook
	/*********************************************************/
	
	public class MBChat{
			
		public void sendChatMessage(String s){p("MBChat  - New chat string: " + s);if(isForBot(s.trim())){parseCmd(getCmd(s));}else mc.thePlayer.sendQueue.addToSendQueue(new Packet3Chat(s));}
		public void addChatMessage(String s){mc.thePlayer.addChatMessage(s);}
		
		private boolean isForBot(String s){String[] str = s.split(CMD_PREFIX);if(str[0].compareTo("")==0&&str.length>1)return true;return false;}
		private String getCmd(String s){String[] str = s.split(CMD_PREFIX);return str[1];}
		private void parseCmd(String s){String[] cmd = s.split(":");
										if(cmd.length>1){String[] arg = cmd[1].split(",");
														 if(cmd[0].compareTo("sq")==0||cmd[0].compareTo("rect")==0)cmdRect(arg);
														 else if(cmd[0].compareTo("light")==0){cmdLight(arg);}
														 else if(cmd[0].compareTo("lag")==0){cmdLag(arg);}
														 else if(cmd[0].compareTo("search")==0){cmdSearch(arg);}
														 else s("Unknown command");}
										else {s("Available commands:");
											  s(" ");
											  s("  rect - Dig");
											  s("  sq - Dig (Same as rect)");
											  s("  light - Switch light on/off/default");
											  s("  lag - Slow down mining speed");
											  s("  search - Search and move to specific block type");
											  s(" ");
											  s("e.g. .bot sq:3,3,2");}}
		private void cmdRect(String[] arg){int nb=arg.length;int l,w,h,sk;String left;
										 try{l=(nb>0)?Integer.parseInt(arg[0]):0;
										     w=(nb>1)?Integer.parseInt(arg[1]):1;
										     h=(nb>2)?Integer.parseInt(arg[2]):2;
										     sk=(nb>3)?Integer.parseInt(arg[3]):0;
										     left=(nb>4)?arg[4]:mc.gameSettings.left?"left":"right";
										     mbmn.setFloor((l>0)?l:0, (w>0)?w:0, (h>0)?h:0, (sk>0)?sk:0, left.compareTo("left")==0);}
										 catch(NumberFormatException e){s("rect cmd e.g. sq:3,3,2,0,left");}}
		private void cmdLag(String[] arg){  int nb=arg.length;int lag;
											 try{lag=(nb>0)?Integer.parseInt(arg[0]):0;
											     MinecraftBot.this.LAG = lag>0?lag:0;}
											 catch(NumberFormatException e){s("lag cmd e.g. lag:10");}}
		private void cmdSearch(String[] arg){  int nb=arg.length;int itemId;
											 try{if(nb>0)if(arg[0].compareTo("food")==0)mbMove.goToFood();
											 	 else{ itemId=Integer.parseInt(arg[0]);
											 	 	   mbMove.goToItems(new int[]{itemId>0?itemId:0});}}
											 catch(NumberFormatException e){s("search cmd e.g. search:54");}}
		private void cmdLight(String[] arg){if(arg[0].compareTo("on") == 0){timeLocked = LIGHT_ON; s("Light on");}else if(arg[0].compareTo("off") == 0){timeLocked=LIGHT_OFF; s("Light off");}else {timeLocked = 0; s("Light default");}}
	}
		
	/*********************************************************/
	// Mining Bot, Survival Bot (Not complete) && Moving Bot
	/*********************************************************/

	private class MBMining{
			
		private boolean isActive;
		
		private MBVec currBlock;
		private MBVec lineBegin;
		private int currId;
		private int currHeight;
		private int fixedHeight;
		private int currLength;
		private int fixedLength;
		private int currWidth;
		private int fixedWidth;
		private double lastDistance;
		private int currFloor;
		private int fixedFloor;
		private boolean side;
		private int currLine;
		private float currYaw;
		private float fixedYaw;
		private int digGoal;
		private boolean digFlag;
		private boolean gravel;
		private int debug_counter;
		private int skip;
		private int lightMin;
		private int nextCounter;
		private MBVec fixedPosition;
		private MBVec moveTo;
		private MBVec pausePosition;
		
		private MBMining(){reset();}
		private void tick(){if(isActive){mc.thePlayer.setRotation(currYaw, mc.thePlayer.rotationPitch);
								if(currId!=0)refreshDamageBlock();
						   else if(currHeight>-2)refreshHeight();
						   else if(moveTo!=null)refreshMoveTo();
						   else if(currLength>=1)refreshLenght();
						   else if(currWidth>0)refreshWidth();
						   else if(digGoal>-1)refreshDigging();
						   else if(currFloor>0)refreshFloor();
						   else stop();}}
		private void reset(){fixedLength=0;
		   					 currId=0;
		   					 digFlag=false;
							 currBlock=null;
							 moveTo=null;
							 pausePosition=null;
							 currHeight=-2;
							 currLength=0;
							 currWidth=0;
							 digGoal=-1;
							 currLine=0;
							 debug_counter=0;
							 lastDistance=0;
							 currFloor=0;}
		private void pause(){if(isActive){ pausePosition=new MBVec(); currYaw=rY(mc.thePlayer.rotationYaw); mbmn.isActive=false; KeyBinding.unPressAllKeys(); setFocus(true); s("Pause mining. (Current location saved)");}
							else if(pausePosition!=null){mbmn.isActive=true; setFocus(false); p("MBMining  - Resume mining: " + pausePosition.toString()); mbMove.goTo(pausePosition); pausePosition=null; s("Resume mining.");}}
		private void stop(){isActive = false;
							setFocus(true);
							p("MBMining  - TASK FINISHED");
							s("Stop mining.");}
		
		private void refreshDamageBlock(){mbInventory.equipItem(currBlock);
										  mc.playerController.onPlayerDamageBlock(currBlock.xInt(), currBlock.yInt(), currBlock.zInt(), currBlock.getSide());
										  mc.thePlayer.swingItem();
										  mbMove.lookAt(currYaw, PITCH[(currBlock.yInt()-(int)mc.thePlayer.posY)+4]);
										  currYaw=rY(yawFrom2Vec(mc.thePlayer.getPosition(1), currBlock));
										  currId=currBlock.getId();
										  setFocus(false);}
		private void refreshHeight(){setBlock(0, currHeight, 1, 0);
									 if(gravel||mbScanner.scanFor(new int[]{0, currHeight + 1, 1}, new int[]{12, 13})){ wait = GRAVEL_TIMEOUT + LAG; gravel = !gravel;} // Gravel or Sand
									 else currHeight--;}
		private void refreshMoveTo(){ p("MBMining  - Moving to " + moveTo.toString()); mbMove.goTo(moveTo); moveTo=null;}
		private boolean refreshBridge(){int pathId = mbScanner.getId(0, -2, 1);
								        if(pathId==0||Arrays.binarySearch(BLOCKS_LAVA_WATER_FIRE, pathId)>=0){ mbInventory.setPlaceObject(new MBVec(0, -2, 0, getSide(), false), BLOCKS_BUILD); 
								        																	   mbMove.lookAt(mc.thePlayer.rotationYaw, PITCH[2]);
								        																	   if((new MBVec(0, -2, 1)).distanceToFeet() >= 1.5){ mc.gameSettings.keyBindForward.pressed = true;
								        																	   													  mc.gameSettings.keyBindSneak.pressed = true;
								        																	   													  p("--------------------------------START---------------------------");}}
								        else if(Arrays.binarySearch(BLOCKS_EMPTY, pathId)>=0){setBlock(0, -2, 1, getSide());}
								        else return true;
								        return false;}
		private void refreshLenght(){/*if(refreshBridge())*/{currLength=lineBegin.distanceTo()>=currLength-1.4?0:currLength;
														 setHeight(fixedHeight);
														 moveTo = new MBVec(0, 0, 1);
														 debug_counter = 0;}}
		private void refreshWidth(){boolean lng=(currLine)%2==0;
								    int yMod = SQ_YAW[currLine%4];
								    yMod = (side)?yMod:-yMod;
								    int len = lng?fixedLength-1:skip+1;
								    setLenght(len, fixedHeight, fixedYaw+yMod);
								    currLine++;
								    currWidth-=(lng)?1:skip;}
		private void refreshDigging(){if(digFlag){mc.thePlayer.setRotation(rY(mc.thePlayer.rotationYaw)+180, 0);digFlag=false;LAG = LAG - 5; currYaw=mc.thePlayer.rotationYaw;}
									  if(digGoal >= (int)mc.thePlayer.posY){digGoal = -1;
									  	 									if(new MBVec().xInt()==fixedPosition.xInt()||new MBVec().zInt()==fixedPosition.zInt()) side=!side;
									  										setRectangle(fixedLength, fixedWidth, 6, skip, currYaw);}							  
								 else if(!mbScanner.scanFor(0, 0, 1, 0))setBlock(0, 0, 1, 1);
								 else if(!mbScanner.scanFor(0, -1, 1, 0)){setBlock(0, -1, 1, 1);}
								 else if(!mbScanner.scanFor(0, -2, 1, 0)){setBlock(0, -2, 1, 1);}
								 else if(mbScanner.scanFor(0, -3, 1, 0)||mbScanner.scanFor(new int[]{0, -3, 1}, BLOCKS_LAVA_WATER_FIRE)){ mbInventory.setPlaceObject(new MBVec(0, -3, 0, getSide(), false), BLOCKS_BUILD);}
								 else if(!mbScanner.scanFor(new int[]{0, -2, 2, -1, -2, 1, 1, -2, 1}, BLOCKS_LAVA_WATER_FIRE)){moveTo = new MBVec(0, 0, 1);
								 																							   digFlag=true;
								 																							   LAG = LAG + 5;}}
		private void refreshFloor(){currFloor -= 1;
									//if((rY(mc.thePlayer.rotationYaw)+180)%360==fixedYaw)side=!side;
									//fixedYaw=rY(mc.thePlayer.rotationYaw)+180;
									digFlag=!digFlag;
									digGoal = (int)mc.thePlayer.posY - 6;}
		
		private void setBlock(){if(mc.objectMouseOver!=null)setBlock(mc.objectMouseOver.blockX,mc.objectMouseOver.blockY,mc.objectMouseOver.blockZ,mc.objectMouseOver.sideHit);}
		private void setBlock(int x, int y, int z, int side){setBlock(new MBVec(x,y,z,side,false));}
		private void setBlock(double x, double y, double z, int side){setBlock(new MBVec(x,y,z,side,false));}
		private void setBlock(MBVec block){if(mbScanner.isTouchingLavaWater(block)){p("MBMining  - Too close to lava.");
																			   s("Too close to lava/water.");
		 																	   stop();}
										   else if(block.getId()!=0){currBlock=block;
												    				 currId=currBlock.getId();
																     currYaw=rY(yawFrom2Vec(mc.thePlayer.getPosition(1), block));
												    				 p("MBMining  - Mining block " + block.toString());}}
	
		private void setHeight(){setHeight(2);}
		private void setHeight(int i){currHeight=i-2;
									  if(mbScanner.scanFor(new int[]{0, currHeight + 1, 1}, new int[]{12, 13})) gravel = true; // Gravel or Sand
									  placeTorch();}
		
		private void setLenght(){setLenght(-1, 6);}
		private void setLenght(int l, int h){setLenght(l, h, rY(mc.thePlayer.rotationYaw));}
		private void setLenght(int l, int h, float y){currLength=l;
									 			      fixedHeight=h;
									 			      currYaw=y;
									 			      lineBegin = new MBVec();}
	
		private void setRectangle(){setRectangle(5);}
		private void setRectangle(int l){setRectangle(l, 3);}
		private void setRectangle(int l, int w){setRectangle(l, w, 2);}
		private void setRectangle(int l, int w, int h){setRectangle(l, w, h, 1, rY(mc.thePlayer.rotationYaw));}
		private void setRectangle(int l, int w, int h, int sk, float parYaw){currWidth=w;
																		     fixedLength=l;
																		     fixedWidth=w;
																		     fixedHeight=(h>6)?6:h;
																		     currYaw=parYaw;
																		     currLine=0;
																		     skip=sk;
																		     p("MBMining  - Set rectangle. W=" + currWidth + " L=" + fixedLength + " H=" + fixedHeight);}
		
		private void setFloor(){setFloor(5);}
		private void setFloor(int l){setFloor(l, 3);}
		private void setFloor(int l, int w){setFloor(l, w, 2);}
		private void setFloor(int l, int w, int h){setFloor(l, w, h, 1);}
		private void setFloor(int l, int w, int h, int sk){setFloor(l, w, h, sk, true);}
		private void setFloor(int l, int w, int h, int sk, boolean side){reset();
																		 fixedLength=l;
																		 fixedWidth=w;
																		 fixedHeight=h%6;
																		 fixedHeight=fixedHeight==1?2:fixedHeight;
																		 fixedFloor=(int)h/6;
																		 skip=sk;
																		 this.side=fixedHeight==0?!side:side;
																	     fixedYaw=rY(mc.thePlayer.rotationYaw);
																		 fixedPosition=new MBVec();
																		 currFloor=fixedFloor;
																		 s("Start mining.");
																		 if(fixedHeight>0) setRectangle(l, w, fixedHeight, sk, fixedYaw);
																		 else digFlag=true;
																		 isActive=true;}
	
		private void placeTorch(){if((mc.gameSettings.torch && mc.thePlayer.getBrightness(1) <= mc.gameSettings.lightning)){ mbInventory.setPlaceObject(new MBVec(0, -2, 0, 1, false), 50); /*mbMove.lookAt(currYaw, DOWN);*/};}
		
	}
	private class MBSurvival{
			
			boolean isActive; 
			boolean isEating;
			
			MBSurvival(){isActive=false;isEating=false;}
			
			void tick(){if(mc.thePlayer.getFoodStats().getFoodLevel()<15||isEating)refreshEat();}
			
			void refreshEat(){if(mbInventory.isCurrItem(ITEMS_FOOD)&&!mc.thePlayer.isEating()){mc.gameSettings.keyBindUseItem.pressed=true; isEating = true;}
			  			 else if(mc.thePlayer.getCurrentEquippedItem()==null&&isEating){mc.gameSettings.keyBindUseItem.pressed=false; isEating = false;}
			  			 else if(!isEating)System.out.println("" + (mbInventory.equipFood()?"-  MBSurvival  - Eating.":"-  MBSurvival  - No food available."));}
	
		}
	private class MBMove{

		boolean isActive;
		boolean issetForward;
		boolean issetBackward;
		boolean issetJump;
		boolean issetSneak;
		boolean bridge;

		float nextYaw;
		boolean waiting;
		boolean verbose;
		double tickDistance;
		MBVec lastStep = null;
		MBVec currStep = null;
		LinkedList<MBVec> currPath = null;
		
		MBMove(){isActive=false;waiting=false;}
		
		void tick(){if(currStep!=null)refreshOneStep();
	       	   else if(waiting)refreshWaiting();
		   	   else if(currPath!=null)refreshNextStep();
		   	   else { if(verbose) s("Destination reached."); stop();}
					refreshKeys();}
		void reset(){currStep = null; 
					 currPath = new LinkedList<MBVec>();
					 KeyBinding.unPressAllKeys(); 
					 this.isActive = true;
					 mbStore.clear();}
		void pause(){if(isActive)isActive=false;
					 else isActive = currStep != null || waiting || currPath != null;}
		void stop(){isActive = false;
					p("MBMove  - TASK FINISHED");
					dontMove();
					verbose = false;}
		void dontMove(){issetForward = false;
						issetSneak = false;
						issetBackward = false;
						issetJump = false;}
		
		void refreshKeys(){ if(mc.gameSettings.keyBindForward.pressed != issetForward) mc.gameSettings.keyBindForward.pressed = issetForward;
							if(mc.gameSettings.keyBindSneak.pressed != issetSneak) mc.gameSettings.keyBindSneak.pressed = issetSneak;
							if(mc.gameSettings.keyBindBack.pressed != issetBackward) mc.gameSettings.keyBindBack.pressed = issetBackward;
							if(mc.gameSettings.keyBindJump.pressed != issetJump) mc.gameSettings.keyBindJump.pressed = issetJump;}
		boolean refreshBridge(){int pathId = mbScanner.getId(0, -2, 1);
								if(Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, pathId)>=0){ mbInventory.setPlaceObject(new MBVec(0, -2, 0, getSide(), false), BLOCKS_BUILD); 
								        															   mbMove.lookAt(mc.thePlayer.rotationYaw, PITCH[2]);
								        															   /*if((new MBVec(0, -2, 1)).distanceToFeet() >= 1.5){ issetForward = true;
								        																	   											  issetSneak = true;
								        																	   											  p("--------------------------------START---------------------------");}*/}
								else if(Arrays.binarySearch(BLOCKS_EMPTY, pathId)>=0){mbmn.setBlock(0, -2, 1, getSide());}
								else return true;
								return false;}
		void refreshOneStep(){double left = currStep.distanceToFeet();
							  if(left<0.2) currStep = null;
				  		 else if(issetForward==false){ if(refreshBridge()){ issetForward=true; mc.thePlayer.setRotation(mc.thePlayer.rotationYaw, 20);}}
				  		 else if(left<0.5&&left>=0.2) issetSneak = true;
				  		 else if(!mbScanner.scanFor(new int[]{0, -1, 1}, BLOCKS_EMPTY_LAVA_WATER_FIRE)&&(left>1.5||currStep.yCoord>=mc.thePlayer.posY-0.3)){ issetJump=true;}
				  		 else if(mbScanner.scanFor(new int[]{0, -2, 0}, BLOCKS_EMPTY)){ issetSneak = false; issetJump = false;}
				  		 else if(mbScanner.scanFor(new int[]{0, -2, 1}, BLOCKS_EMPTY)) issetSneak=true;
				  		 else issetJump=false;
							  if(currStep != null){
								  if(left > tickDistance + 0.2) {
									  if(verbose) s("Recalculating path...");
									  p("MBMove  - Wrong route.");
									  dontMove();
									  this.goTo(lastStep);
								  	  wait = 20;}
								  else if(left < tickDistance - 0.2) tickDistance = left;
							  	}
							  p("MBMove  - Left : " + left);
							  }
		void refreshNextStep(){if(currPath.size()<1){currPath = null; return;}
							   if((new MBVec()).xInt()==currPath.peekLast().xInt()){
								   do{
										currStep = currPath.removeLast();
								   }while(currPath.size()>0&&(new MBVec()).xInt()==currPath.peekLast().xInt());}
							   else if((new MBVec()).zInt()==currPath.peekLast().zInt()){
								   do{
										currStep = currPath.removeLast();
								   }while(currPath.size()>0&&(new MBVec()).zInt()==currPath.peekLast().zInt());}
							   else currStep = currPath.removeLast();
							   dontMove();
							   this.setNextStep(currStep);}
		void refreshWaiting(){if (currPath.size()>0) {Collections.synchronizedCollection(currPath);
													  this.setPath(currPath);
													  this.waiting = false;
													  if(verbose) s("Path found. Let's go.");
													  p("MBMove  - Path found!");}
							  else if (currPath==null||mbStore.search==null||!mbStore.search.search){ p("MBMove  - TASK FINISHED");
							  																		  if(verbose) s("Destination unavailable.");
													    											  stop();}}
		
		void setNextStep(MBVec vec){this.currStep = vec;
									nextYaw = yawFrom2Vec(mc.thePlayer.getPosition(1), currStep);
									mc.thePlayer.setRotation(nextYaw,  0f);
									lookAt(nextYaw, 0f);
									this.isActive = true;
									tickDistance = currStep.distanceToFeet() + 0.2;
									p("MBMove  - Next Step. " + currStep.distanceToFeet());}
		void setPath(LinkedList<MBVec> path){if(path==null||path.size()<1){p("MBMove  - Invalid path. (Too soon?)"); return;}
											 currPath=path;
											 this.lastStep = path.peekFirst();
											 currStep = null;}
		
		public void goTo(MBVec vec){if(vec.distanceTo()>1.5){this.reset();
															  mbStore.set(vec);
															  mbStore.set(new MBVec());
															  mbStore.setContainer(currPath);
															  mbStore.set();
															  this.waiting = true;
															  this.lastStep = vec;
															  p("MBMove  - Waiting for a valid path to destination...");}
									 else{this.reset();
									 	  vec.aH();
										  this.lastStep = vec;
										  p("MBMove  - Moving straight to destination.");
										  this.setNextStep(vec);}}
		public void goToItems(int[] items){MBVec here = new MBVec();
											this.reset();
											mbStore.setContainer(currPath);
											mbStore.set(here, items);
											this.waiting = true;
											verbose = true;
											s("Calculating path to destination...");
											p("MBMove  - Waiting for a valid path to destination...");}
		public void goToFood(){goToItems(ITEMS_FOOD);}
		public void goToChest(){goToItems(new int[]{54});}
		public void goToWood(){goToItems(new int[]{17});}
		public void moveOnSecondHalfOf(MBVec vec){}
		
		void lookAt(float yaw, float pitch){mc.getNetHandler().addToSendQueue(new Packet12PlayerLook(yaw, pitch, true));}
	}
	
	/*********************************************************/
	// Surrounding block scanner class, inventory manager class
	// and custom Vec3 class containing usefull tools used by 
	// the bot.
	/*********************************************************/
	
	private class MBScanner{
		
		private boolean scanFor(MBVec[] xyz, int[] ids){for(int i=0;i<xyz.length;i++)if(Arrays.binarySearch(ids, xyz[i].getId())>=0)return true; return false;}
		private boolean scanFor(MBVec xyz, int[] ids){return Arrays.binarySearch(ids, xyz.getId()) >= 0;}
		private boolean scanFor(int[] xyz, int id[]){MBVec[] mbVec = new MBVec[xyz.length/3];for(int i = 0; i < xyz.length/3; i++)mbVec[i] = new MBVec(xyz[i*3], xyz[i*3+1], xyz[i*3+2], 0, false);return scanFor(mbVec, id);}
		private boolean scanFor(double[] xyz, int id[]){MBVec[] mbVec = new MBVec[xyz.length/3];for(int i = 0; i < xyz.length/3; i++)mbVec[i] = new MBVec(xyz[i*3], xyz[i*3+1], xyz[i*3+2], 0, false);return scanFor(mbVec, id);}
		private boolean scanFor(int x, int y, int z, int id){return (new MBVec(x, y, z, 0, false)).getId()==id;}
		
		private boolean isTouchingLavaWater(MBVec block){double x = block.xCoord, y = block.yCoord, z = block.zCoord; return mbScanner.scanFor(new double[]{x+1, y, z, x, y, z+1, x-1, y, z, x, y, z-1, x, y+1, z, x, y-1, z}, BLOCKS_LAVA_WATER);}	
		
		private int getId(int x, int y, int z){return (new MBVec(x, y, z, false)).getId();}
		private int getId(double x, double y, double z){return (new MBVec(x, y, z, 0, false)).getId();}
		
	}
	private class MBInventory{
		
		int slotA;
		int slotB;
		int step = 0;
		boolean isActive = false;
		MBVec placeObject = null;
		
		void tick(){if(step == 1)refreshStepA();
	  	       else if(step == 2)refreshStepB();
		  	   else if(placeObject != null)refreshPlaceObject();
		  	   else isActive = false;}
		
		void refreshStepA(){mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slotA, 0, 0, mc.thePlayer); this.step=2;} // Swap Items
		void refreshStepB(){mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slotB, 0, 0, mc.thePlayer); mc.thePlayer.closeScreen(); this.step=0; wait = LAG;} // Replace items
		void refreshPlaceObject(){mc.thePlayer.sendQueue.addToSendQueue(new Packet15Place(placeObject.xInt(), placeObject.yInt(), placeObject.zInt(), placeObject.getSide(), mc.thePlayer.inventory.getCurrentItem(), (float)placeObject.hitVec().xCoord, (float)placeObject.hitVec().yCoord, (float)placeObject.hitVec().zCoord));
								  placeObject = null;
								  mc.thePlayer.swingItem();
								  p("MBInventory  - refreshPlaceObject: " + mc.thePlayer.inventory.getCurrentItem().getItemName());
								  wait = LAG;}

		public void setPlaceObject(MBVec vec, int parItemId){ setPlaceObject(vec, new int[]{parItemId});}
		public void setPlaceObject(MBVec vec, int[] parItemId){if(equipItem(parItemId)){ placeObject = vec;
																			 	  		 this.isActive = true;
																			 	  		 p("MBInventory  - setPlaceObject: " + vec.toString());
																			 	  		 wait = LAG;}}
		
		boolean isCurrItem(int[] items){boolean success=false;ItemStack currItem = mc.thePlayer.getCurrentEquippedItem();if(currItem!=null){success=Arrays.binarySearch(items, currItem.itemID)>=0;}return success;}
		int searchItemSlot(int[] arrIds){int itemId = -1;int slot= -1;int tmpId = -1;for(int i=0;i<mc.thePlayer.inventoryContainer.inventorySlots.size();i++){if(mc.thePlayer.inventoryContainer.getSlot(i).getStack()!=null){tmpId=Arrays.binarySearch(arrIds, (mc.thePlayer.inventoryContainer.getSlot(i).getStack().itemID));slot=(tmpId>itemId)?i:slot;itemId=(tmpId>itemId)?tmpId:	itemId;}}return slot;}
		void swapItemsSlots(int slotA, int slotB){ mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slotB, 0, 0, mc.thePlayer); this.step = 1; this.isActive=true; this.slotA = slotA; this.slotB = slotB;}
		boolean equipItem(MBVec block){int[] itemIds = getToolType(block.getId()); return equipItem(itemIds);}
		boolean equipItem(int[] itemsIds){if(itemsIds==null) return false; boolean isHoldingItem = isCurrItem(itemsIds); if(!isHoldingItem){ int itemSlot=searchItemSlot(itemsIds); if(itemSlot>=0){ p("MBInventory  - swapItemsSlots for item(s) id(s): " + Arrays.toString(itemsIds)); swapItemsSlots(36 + mc.thePlayer.inventory.currentItem, itemSlot); isHoldingItem = true;}} return isHoldingItem;}
		boolean equipItem(int itemId){return equipItem(new int[]{itemId});}
		boolean equipFood(){return equipItem(ITEMS_FOOD);}
		int[] getToolType(int blockId){if(Arrays.binarySearch(BLOCKS_ORE, blockId)>=0)return ITEMS_PICKAXES;
	 	   									   if(Arrays.binarySearch(BLOCKS_DIRT, blockId)>=0)return ITEMS_SHOVELS;
	 	   									   if(Arrays.binarySearch(BLOCKS_WOOD, blockId)>=0)return ITEMS_AXES;
	 	   									   return null;}
	}
	private class MBVec extends Vec3{
		
		private int side;
		private float yaw;
		
		MBVec(){this(true);}
		MBVec(float parYaw){this(true); this.yaw=parYaw;}
		MBVec(Vec3 vec){this(vec.xCoord, vec.yCoord, vec.zCoord, 0, false);}
		MBVec(boolean centered){this(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, 0, centered);}
		MBVec(int x, int y, int z){this(x, y, z, true);}
		MBVec(int x, int y, int z, boolean centered){this(x, y, z, 0, centered);}
		MBVec(int x, int y, int z, int side, boolean centered){this(xabs((double)x, (double)z), yabs((double)y), zabs((double)x, (double)z), side, centered);}
		MBVec(double x, double y, double z){this(x,y,z,0,false);}
		MBVec(double x, double y, double z, int side, boolean centered){super(null, x,y,z);
																		this.side = side;
																		if(centered)this.cP();}

		
		int xInt(){return rP(this.xCoord);}
		int yInt(){return (int)(this.yCoord);}
		int zInt(){return rP(this.zCoord);}
		
		int getSide(){return side;}

		Vec3 hitVec(){double x = 0.5, y = 0.5, z = 0.5; x += this.side==5? 0.5: this.side==4? -0.5: 0; y += this.side==0? -0.5: this.side==1? 0.5: 0; z += this.side==3? 0.5: this.side==2? -0.5: 0; return new Vec3(null, x, y, z);}
		
		void cP(){this.xCoord=this.xInt() + 0.5;
				  this.yCoord=this.yInt() + 0.5;
				  this.zCoord=this.zInt() + 0.5;}
		void aH(){p("MBVec  - aH " + toString());while(Arrays.binarySearch(BLOCKS_EMPTY_WATER, getId()) >= 0) yCoord-=1;
   				  while(Arrays.binarySearch(BLOCKS_EMPTY_WATER, getId()) < 0) yCoord+=1;}
			
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
			
		int getId(){return mc.theWorld.getBlockId(xInt(), yInt(), zInt());}
		public String toString(){return "" + xCoord + ", " + yCoord + ", " + zCoord;}
	}

	/*********************************************************/
	// Everything needed for pathfinding.
	/*********************************************************/
	
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
		
		protected int h(){return 15 * (Math.abs(x2 - this.x) + Math.abs(y2 - this.y) + Math.abs(z2 - this.z));}
		protected int f(){return g+h;}
		protected int getId(int x, int y, int z){return mc.theWorld.getBlockId(x, y, z);}
		protected boolean isLast(){return x==x2&&y==y2&&z==z2;}
		
		protected List<Node> getBros(){
			
			List<Node> broList = new ArrayList<Node>();
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
	private class NodeSearch extends Node{
		
		private int items[] = null;
		protected boolean isFirst = false;
		
		NodeSearch(Node parParent, int parx, int pary, int parz, int parx2, int pary2, int parz2, int parg, int[] items){this(parParent, parx, pary, parz, parx2, pary2, parz2, 0, 0, 0, parg, items);}
		NodeSearch(Node parParent, int parx, int pary, int parz, int parx2, int pary2, int parz2, int dx,  int dy,  int dz, int parg, int[] items){super(parParent, parx, pary, parz, parx2, pary2, parz2, dx, dy, dz, parg);
																																			       this.items = items;
																																			       if(parx==parx2&&pary==pary2&&parz==parz2)isFirst=true;}

		@Override protected boolean isLast(){return false;}
		
		@Override protected List<Node> getBros(){
			
			List<Node> broList = new ArrayList<Node>();
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

			for (int i = 0; i < 4; i++) if(Arrays.binarySearch(items, intList[i*5+2])>=0||Arrays.binarySearch(items, intList[i*5+3])>=0){broList.add(new Node(this.parent, x, y, z, x, y, z, g)); return broList;}
			
			if(Arrays.binarySearch(BLOCKS_EMPTY, intList[3]) >= 0)
			{
				
				if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[2]) >= 0 && 
						Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[1]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, 1, 0, 0, g+dz+10, items));
				
				if (Arrays.binarySearch(BLOCKS_EMPTY, intList[2]) >= 0 && 
						Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[1]) >= 0 && 
						Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[0]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, 1, -1, 0, g+dz+13, items));
				
				if (intList[20] >= 0){
					if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[4]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[2]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, 1, +1, 0, g+dz+16, items));}}

			if(Arrays.binarySearch(BLOCKS_EMPTY, intList[8]) >= 0)
			{
				
				if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[7]) >= 0 && 
						Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[6]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, -1, 0, 0, g+dz+10, items));
				
				if (Arrays.binarySearch(BLOCKS_EMPTY, intList[7]) >= 0 && 
						Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[6]) >= 0 && 
						Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[5]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, -1, -1, 0, g+dz+13, items));
				
				if (intList[20] >= 0){
					if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[9]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[7]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, -1, +1, 0, g+dz+16, items));}}
			
			if(Arrays.binarySearch(BLOCKS_EMPTY, intList[13]) >= 0)
			{
				
				if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[12]) >= 0 && 
						Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[11]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, 0, 0, +1, g+dx+10, items));
				
				if (Arrays.binarySearch(BLOCKS_EMPTY, intList[12]) >= 0 && 
						Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[11]) >= 0 && 
						Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[10]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, 0, -1, +1, g+dx+13, items));
				
				if (intList[20] >= 0){
					if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[14]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[12]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, 0, +1, +1, g+dx+16, items));}}
			
				if(Arrays.binarySearch(BLOCKS_EMPTY, intList[18]) >= 0)
				{
					
					if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[17]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[16]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, 0, 0, -1, g+dx+10, items));
					
					if (Arrays.binarySearch(BLOCKS_EMPTY, intList[17]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[16]) >= 0 && 
							Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[15]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, 0, -1, -1, g+dx+13, items));
					
					if (intList[20] >= 0){
						if (Arrays.binarySearch(BLOCKS_EMPTY_WATER, intList[19]) >= 0 && 
								Arrays.binarySearch(BLOCKS_EMPTY_LAVA_WATER_FIRE, intList[17]) < 0) broList.add(new NodeSearch(this, x, y, z, x2, y2, z2, 0, +1, -1, g+dx+16, items));}}
				
			return broList;
			
		}
		
		@Override public boolean equals(Object o){if(o.getClass()!=this.getClass())return false;
		  										  return ((Node)o).x==this.x&&((Node)o).y==this.y&&((Node)o).z==this.z;}
	}
	private class NodeStore{
		
		LinkedList<MBVec> path = null;
		AStar search = null;
		
		private class AStar extends Thread{
			
			private List<Node> openNode;
			private List<Node> closeNode;
			private NodeStore parent;
			public boolean search = true;
			public int status;
			
			public AStar(Node start, NodeStore parent){this.parent=parent;
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
					else {p("AStar  - Bloque. Aucun chemin trouve");search=false;};
						closeNode.add(currNode);
						if(currNode.isLast()){status = 1;search=false;} // PATH FOUND
						if(System.currentTimeMillis()-tick > PATH_SEARCH_TIMEOUT){p("AStar  - Timeout. Aucun chemin trouve");search=false;}
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
				
				p("AStar  - Paths scanned: " + closeNode.size());
				if(status==1)parsePath();
				else parent.path=null;
				clear();
				System.gc();
				p("AStar  - TASK FINISHED");
			}
	
			private void parsePath(){

				if(parent.path==null) parent.path = new LinkedList<MBVec>();
				parent.path.clear();
				
				do{
					parent.path.add(new MBVec((double)currNode.x, (double)currNode.y, (double)currNode.z, 1, false));
					parent.path.peekLast().cP();
					//placeObject(parent.path.peekLast());
					currNode = currNode.parent;
				}while (currNode!=null);
				
				p("AStar  - Path ready. (Size : " + parent.path.size() + " blocks.)");
				p("AStar  - Last node is at : " + parent.path.getFirst().xCoord + ", " + parent.path.getFirst().yCoord + ", " + parent.path.getFirst().zCoord);
			}
			
			private void clear(){
				
				closeNode.clear();
				openNode.clear();
			}

		}
		private Node currNode,
					 start = null;
		private MBVec end = null;
		
		public NodeStore(){}
		
		public LinkedList<MBVec> getPath(){clear(); return path;}
		public void clear(){start=null;end=null;if(path!=null)path.clear(); p("NodeStore  - Start and End point cleared."); if(search!=null) search.search=false;}
		public void search(){search = new AStar(start, this); p("NodeStore  - Start search..."); search.start();}
		
		public void set(){if(end==null)if(mc.objectMouseOver!=null)set(new MBVec((double)mc.objectMouseOver.blockX, (double)mc.objectMouseOver.blockY, (double)mc.objectMouseOver.blockZ));
									   else set(new MBVec());
					 else if(start==null)if(mc.objectMouseOver!=null)set(new MBVec((double)mc.objectMouseOver.blockX, (double)mc.objectMouseOver.blockY, (double)mc.objectMouseOver.blockZ));
					   					else set(new MBVec());
					 else if(path!=null&&path.size()>0) clear();
					 else search();}
		public void set(MBVec vec, int[] items){setSearch(vec, items);
												if(end!=null&&start!=null) search();}
		public void set(double x, double y, double z){set(new MBVec(x, y, z, 0, false));}
		public void set(MBVec vec){if(end==null) setEnd(vec);
							  else if(start==null) setStart(vec);
							  else search();}
		public void setContainer(LinkedList<MBVec> vec){path = vec;}
		public void setEnd(MBVec vec){vec.aH(); end = vec; p("NodeStore  - End set. " + end.toString());}
		public void setStart(MBVec vec){if(end!=null){vec.aH(); start = new Node(null, vec.xInt(), vec.yInt(), vec.zInt(), end.xInt(), end.yInt(), end.zInt(), 0); p("NodeStore  - Start set. X: " + start.x + " Y: " + start.y + " Z: " + start.z);}}
		public void setSearch(MBVec vec, int[] items){vec.aH(); end = vec;
										   			  start = new NodeSearch(null, vec.xInt(), vec.yInt(), vec.zInt(), vec.xInt(), vec.yInt(), vec.zInt(), 0, items);}
	}
	private class NodeComparateur implements Comparator<Node>{
		
		public int compare(Node o1, Node o2){return o1.x==o2.x&&o1.y==o2.y&&o1.z==o2.z?0:1;}
	}

	/*********************************************************/
	// A bunch of objects
	/*********************************************************/
	
	public MBChat mbChat;
	
	private Minecraft mc;
	private MBMining mbmn;
	private MBInventory mbInventory;
	private MBScanner mbScanner;
	private MBSurvival mbSurvive;
	private NodeStore mbStore;
	private MBMove mbMove;
	
	public boolean inGameHasFocus;
	public long timeLocked;
	private int wait;
	private long tick,
				 PATH_SEARCH_TIMEOUT = 20000;
	
	protected boolean[] status = new boolean[]{false, false, false};

	/*********************************************************/
	// Ids for everything. From blocks to tools.
	/*********************************************************/
	
	private static int[] ITEMS_PICKAXES = {257, 270, 274, 278, 285};
	private static int[] ITEMS_SHOVELS = {256, 269, 273, 277, 284};
	private static int[] ITEMS_AXES = {258, 271, 275, 279, 286};
	private static int[] ITEMS_FOOD = {297, 354, 366, 349, 320, 357, 322, 362, 360, 335, 39, 40, 282, 361, 363, 365, 349, 319, 260, 367, 295, 364, 353, 338, 296};

	private static int[] BLOCKS_LAVA_WATER_FIRE = {8, 9, 10, 11, 51};
	private static int[] BLOCKS_LAVA_WATER = {8, 9, 10, 11};
	private static int[] BLOCKS_ORE = {1, 4, 14, 15, 16, 21, 24, 48, 49, 56, 73, 74, 87, 88, 89};
	private static int[] BLOCKS_BUILD = {1, 3, 4};
	private static int[] BLOCKS_DIRT = {2, 3, 12, 13, 31, 78, 82, 88, 110};
	private static int[] BLOCKS_WOOD = {5, 17};
	private static int[] BLOCKS_EMPTY = {0, 31, 37, 50, 66, 68, 78, 106};
	private static int[] BLOCKS_EMPTY_LAVA_WATER_FIRE;
	private static int[] BLOCKS_EMPTY_WATER = {0, 9, 10, 31, 37, 50, 66, 68, 78, 106};
	
	private static int TYPE_ORE = 0;
	private static int TYPE_DIRT = 1;
	private static int TYPE_WOOD = 2;
	private static int TYPE_OTHER = 3;

	/*********************************************************/
	// Custom player pitch and rotation values
	/*********************************************************/
	
	private static int[] SQ_YAW = {0, -90, -180, -90};
	private static int[] PITCH = {85, 84, 80, 70, 0, -60, -75, -84, -85};
	
	private static int UP = -90;
	private static int HALF_UP = -45;
	private static int DOWN = 90;
	private static int HALF_DOWN = 45;

	private static int NORTH = 180;
	private static int SOUTH = 0;
	private static int EAST = 270;
	private static int WEST = 90;
	
	private static int FRONT = 1;
	private static int LEFT = 2;
	private static int BACK = 3;
	private static int RIGHT = 4;
	private static int CENTER = 0;

	/*********************************************************/
	// Fixed values for daytime.
	/*********************************************************/
	
	public static long LIGHT_ON = 6000;
	public static long LIGHT_OFF = 18000;
	private static long DAY = 6000;
	private static long NIGHT = 18000;
	private static long SUNRISE = 23000;
	private static long SUNSET = 12500;
	
	private static int GRAVEL_TIMEOUT = 5;
	private static int LAG = 3;
	
	private static String CMD_PREFIX = ".bot ";
	private static String CONFIG_FILE_PATH = "MinecraftBot.ini";
	
	/*********************************************************/
	// A bunch of method for one time initialisation and stuff
	/*********************************************************/
	
	public MinecraftBot(Minecraft mc){this.mc = mc;
									  mbChat=new MBChat();
									  mbInventory=new MBInventory();
									  mbScanner=new MBScanner();
									  mbmn=new MBMining();
									  mbMove=new MBMove();
									  mbSurvive=new MBSurvival();
									  mbStore=new NodeStore();
									  inGameHasFocus=true;
									  timeLocked=0;}
	public boolean loadSettings(){
		
		boolean success = false;
		int count = 0;
		File file = new File(CONFIG_FILE_PATH);
		StringBuffer contents = new StringBuffer();
		BufferedReader reader = null;
		
		try {
            reader = new BufferedReader(new FileReader(file));
            String text = null;
 
            // repeat until all lines is read
            while ((text = reader.readLine()) != null) {
                contents.append(text).append(System.getProperty("line.separator"));
                if(parseConfigLine(text)) count++;
                success = true;
            }
        } catch (FileNotFoundException e) {
            
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
		
		arraySort();
		
		return success;
	}
	private void arraySort(){
		  Arrays.sort(BLOCKS_LAVA_WATER_FIRE);
		  Arrays.sort(BLOCKS_ORE);
		  Arrays.sort(BLOCKS_DIRT);
		  Arrays.sort(BLOCKS_WOOD);
		  Arrays.sort(BLOCKS_BUILD);
		  Arrays.sort(BLOCKS_EMPTY);
		  Arrays.sort(BLOCKS_EMPTY_WATER);
		  Arrays.sort(ITEMS_PICKAXES);
		  Arrays.sort(ITEMS_AXES);
		  Arrays.sort(ITEMS_SHOVELS);
		  Arrays.sort(ITEMS_FOOD);
		  BLOCKS_EMPTY_LAVA_WATER_FIRE = new int[BLOCKS_EMPTY.length + BLOCKS_LAVA_WATER_FIRE.length];
		  System.arraycopy(BLOCKS_EMPTY, 0, BLOCKS_EMPTY_LAVA_WATER_FIRE, 0, BLOCKS_EMPTY.length);
		  System.arraycopy(BLOCKS_LAVA_WATER_FIRE, 0, BLOCKS_EMPTY_LAVA_WATER_FIRE, BLOCKS_EMPTY.length, BLOCKS_LAVA_WATER_FIRE.length);
		  Arrays.sort(BLOCKS_EMPTY_LAVA_WATER_FIRE);
		  }
	private boolean parseConfigLine(String configLine){
		
		boolean success = true;
		String[] result = configLine.split("=");
		if(result.length==2 && !configLine.startsWith("#")) {
			String 	name = result[0],
					value = result[1].trim();
			
			switch (Config.toName(name.trim().toLowerCase())){
			case place_torch:{
				p("Torch set to " + (value.toLowerCase().compareTo("true")==0?"true.":"false."));
				mc.gameSettings.torch = value.toLowerCase().compareTo("true")==0;
				break;
			}
			case light_min:{
				float buff;
				try{ buff = Float.parseFloat(value);}
				catch(NumberFormatException e){buff = 0; p("Invalid light level for torchs.");}
				buff = buff < 0 ? 0f : buff > 1 ? 1f : buff;
				p("Minimum light set to " + (buff));
				mc.gameSettings.lightning = buff;
				break;
			}
			case default_side:{
				p("Side to dig set to " + (value.toLowerCase().compareTo("left")==0?"left.":"right."));
				mc.gameSettings.left = value.toLowerCase().compareTo("left") == 0;
				break;
			}
			case default_macro:{
				String buff = value.substring(0, value.length() > 255 ? 255 : value.length());
				p("Default macro will be " + buff);
				mc.gameSettings.macro = buff;
				break;
			}
			case building_blocks:{
				String[] buff = value.split(",");
				value = "";
				for (int i = 0; i < buff.length; i++){
					try{ value += (value != ""? ", ": "") + Integer.parseInt(buff[i].trim());}
					catch(NumberFormatException e){ p("Invalid block id.");}
				}
				buff = value.split(",");
				BLOCKS_BUILD = new int[buff.length];
				for (int i = 0; i < buff.length; i++){
					try{ BLOCKS_BUILD[i] = Integer.parseInt(buff[i].trim());}
					catch(NumberFormatException e){ p("Invalid block id.");}
				}
				p("List of block ids to build is " + Arrays.toString(BLOCKS_BUILD));
				break;
			}
			case pickaxes_ids:{
				String[] buff = value.split(",");
				value = "";
				for (int i = 0; i < buff.length; i++){
					try{ value += (value != ""? ", ": "") + Integer.parseInt(buff[i].trim());}
					catch(NumberFormatException e){ p("Invalid pickaxe id.");}
				}
				buff = value.split(",");
				ITEMS_PICKAXES = new int[buff.length];
				for (int i = 0; i < buff.length; i++){
					try{ ITEMS_PICKAXES[i] = Integer.parseInt(buff[i].trim());}
					catch(NumberFormatException e){ p("Invalid pickaxe id.");}
				}
				p("List of pickaxe ids " + Arrays.toString(ITEMS_PICKAXES));
				break;
			}
			case shovels_ids:{
				String[] buff = value.split(",");
				value = "";
				for (int i = 0; i < buff.length; i++){
					try{ value += (value != ""? ", ": "") + Integer.parseInt(buff[i].trim());}
					catch(NumberFormatException e){ p("Invalid shovel id.");}
				}
				buff = value.split(",");
				ITEMS_SHOVELS = new int[buff.length];
				for (int i = 0; i < buff.length; i++){
					try{ ITEMS_SHOVELS[i] = Integer.parseInt(buff[i].trim());}
					catch(NumberFormatException e){ p("Invalid shovel id.");}
				}
				p("List of shovel ids " + Arrays.toString(ITEMS_SHOVELS));
				break;
			}
			case axes_ids:{
				String[] buff = value.split(",");
				value = "";
				for (int i = 0; i < buff.length; i++){
					try{ value += (value != ""? ", ": "") + Integer.parseInt(buff[i].trim());}
					catch(NumberFormatException e){ p("Invalid axes id.");}
				}
				buff = value.split(",");
				ITEMS_AXES = new int[buff.length];
				for (int i = 0; i < buff.length; i++){
					try{ ITEMS_AXES[i] = Integer.parseInt(buff[i].trim());}
					catch(NumberFormatException e){ p("Invalid axes id.");}
				}
				p("List of axes ids " + Arrays.toString(ITEMS_AXES));
				break;
			}
			case food_ids:{
				String[] buff = value.split(",");
				value = "";
				for (int i = 0; i < buff.length; i++){
					try{ value += (value != ""? ", ": "") + Integer.parseInt(buff[i].trim());}
					catch(NumberFormatException e){ p("Invalid food id.");}
				}
				buff = value.split(",");
				ITEMS_FOOD = new int[buff.length];
				for (int i = 0; i < buff.length; i++){
					try{ ITEMS_FOOD[i] = Integer.parseInt(buff[i].trim());}
					catch(NumberFormatException e){ p("Invalid food id.");}
				}
				p("List of food ids " + Arrays.toString(ITEMS_FOOD));
				break;
			}
			case light_on_lvl:{
				long buff;
				try{ buff = Long.parseLong(value);}
				catch(NumberFormatException e){buff = DAY; p("Invalid light on level.");}
				buff = buff < 0 ? 0 : buff > 23999 ? 23999 : buff;
				p("Light on level will be " + buff);
				LIGHT_ON = buff;
				break;
			}
			case light_off_lvl:{
				long buff;
				try{ buff = Long.parseLong(value);}
				catch(NumberFormatException e){buff = NIGHT; p("Invalid light off level.");}
				buff = buff < 0 ? 0 : buff > 23999 ? 23999 : buff;
				p("Light off level will be " + buff);
				LIGHT_OFF = buff;
				break;
			}
			case cmd_prefix:{
				String buff = value.substring(0, value.length() > 255 ? 255 : value.length());
				if(buff.split("\"").length == 2 && buff.split("\"")[0].compareTo("")==0) CMD_PREFIX = buff.split("\"")[1];
				p("Prefix for all commands will be \"" + CMD_PREFIX + "\"");
				break;
			}
			case gravel_timeout:{
				int buff;
				try{ buff = Integer.parseInt(value);}
				catch(NumberFormatException e){buff = 0; p("Invalid gravel timeout.");}
				buff = buff < 0 ? 0 : buff > 65535 ? 65535 : buff;
				p("Waiting time for falling gravel is " + buff);
				GRAVEL_TIMEOUT = buff;
				break;
			}
			case path_search_timeout:{
				long buff;
				try{ buff = Long.parseLong(value);}
				catch(NumberFormatException e){buff = 0; p("Invalid search path timeout.");}
				buff = buff < 0 ? 0 : buff;
				p("Max timeout to search for a path is " + buff);
				PATH_SEARCH_TIMEOUT = buff;
				break;
			}
			case default_lag:{
				int buff;
				try{ buff = Integer.parseInt(value);}
				catch(NumberFormatException e){buff = 0; p("Invalid lag time.");}
				buff = buff < 0 ? 0 : buff > 65535 ? 65535 : buff;
				p("Default lag set to " + buff);
				LAG = buff;
				break;
			}
			default:{
				success = false;
				p("Setting not found for " + name.trim() + ".");
			}
			}
		}
		else success = false;
		
		return success;
	}
	private enum Config{
		place_torch,
		light_min,
		default_side,
		default_macro,
		building_blocks,
		pickaxes_ids,
		shovels_ids,
		axes_ids,
		food_ids,
		light_on_lvl,
		light_off_lvl,
		cmd_prefix,
		gravel_timeout,
		default_lag,
		path_search_timeout,
		novalue;

	    public static Config toName(String str)
	    {
	        try {
	            return valueOf(str);
	        } 
	        catch (Exception ex) {
	            return novalue;
	        }
	    }   
	}

	/*********************************************************/
	// 'run()' is called each game frame by minecraft.java and
	// is calling 'refreshKeys()' and 'refreshBots()' respectively
	/*********************************************************/
	
	public void run(){if(mc.theWorld != null){refreshKeys();
						   	   			      if(wait<=0) refreshBots();
						   	   			      else wait--;}
					  else if(isActive()) stop();}
	private void refreshKeys(){if(mc.gameSettings.keyBindBotMenu.isPressed()) mc.displayGuiScreen(new MBGuiMenu());
	   						   if(mc.gameSettings.keyBindBotPause.isPressed()) pause();
	   						   if(mc.gameSettings.keyBindBotMacro.isPressed()){if (this.isActive()) this.stop(); else mbChat.parseCmd(mc.gameSettings.macro);}
							   /*if(mc.gameSettings.keyBindBotMenu.isPressed()) mc.displayGuiScreen(new MBGuiMenu());
							   if(mc.gameSettings.keyBindBotPause.isPressed()) p("" + h());
							   if(mc.gameSettings.keyBindBotMacro.isPressed()) mbChat.sendChatMessage("hi");*/}
	private void refreshBots(){if(mbInventory.isActive)mbInventory.tick();
						  else if(mbMove.isActive) mbMove.tick();
						  else if(mbmn.isActive)mbmn.tick();
							   if(mbSurvive.isActive)mbSurvive.tick();}

	/*********************************************************/
	// Random tools used here and there by the bot
	/*********************************************************/
	
	public void setWorldTime(long time){if(timeLocked==0)mc.theWorld.setWorldTime(time);else mc.theWorld.setWorldTime(timeLocked);}
	private int getSide(){float tmpY = rY(mc.thePlayer.rotationYaw);return tmpY==0.0? 3: tmpY==90.0? 4: tmpY==180.0? 2: 5;} // Associate player yaw to block side
	private void setFocus(boolean focusOn){if(inGameHasFocus!=focusOn){ inGameHasFocus=focusOn; if(focusOn) mc.setIngameFocus(); else mc.setIngameNotInFocus(); mc.gameSettings.thirdPersonView=(focusOn)?0:1;}}
	private void p(String s){System.out.println("MinecraftBot: " + s);} // Centralized output to debug
	private void s(String s){if(mc.theWorld != null) mbChat.addChatMessage("#Bot : " + s);} // Centralized output to chat
	private int rY(float b){int rounded = 0; // Round yaw
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
	private float yawFrom2Vec(Vec3 start, Vec3 end){return (float)(Math.atan2(start.xCoord - end.xCoord, end.zCoord - start.zCoord) * 180 / Math.PI);}
	private void placeObject(MBVec placeObject){mc.thePlayer.sendQueue.addToSendQueue(new Packet15Place(placeObject.xInt(), placeObject.yInt(), placeObject.zInt(), placeObject.getSide(), mc.thePlayer.inventory.getCurrentItem(), (float)placeObject.hitVec().xCoord, (float)placeObject.hitVec().yCoord, (float)placeObject.hitVec().zCoord));}

	private double xabs(double parX, double parZ){double buff;switch(rY(mc.thePlayer.rotationYaw)){case 270:case -90:buff = parZ;break;case 180:case -180:buff = parX;break;case 90:case -270:buff = -parZ;break;default:buff = -parX;}return mc.thePlayer.posX+buff;}
	private double yabs(double parY){return mc.thePlayer.posY + parY;}
	private double zabs(double parX, double parZ){double buff;switch(rY(mc.thePlayer.rotationYaw)){case 270:case -90:buff = parX;break;case 180:case -180:buff = -parZ;break;case 90:case -270:buff = -parX;break;default:buff = parZ;}return mc.thePlayer.posZ+buff;}

	/*********************************************************/
	// Random tools used here and there by other minecraft
	// classes.
	/*********************************************************/
	
	public boolean isActive(){return mbmn.isActive||mbMove.isActive||mbInventory.isActive;}
	public void pause(){p("Pause/Resume"); mbMove.pause(); mbmn.pause();}
	public void stop(){p("Stop"); mbmn.stop(); mbMove.stop();}
	public void sendMacro(String macro){ mbChat.parseCmd(macro);}
}
