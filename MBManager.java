package net.minecraft.src;

import java.util.*;

import net.minecraft.src.MinecraftBot.*;

public class MBManager {

	private int macroId;
	private int step;
	private int[] items;
	private int GRAVEL;
	private MBScanner scanner; 
	private static Stack<Bot> botList = new Stack<Bot>();
	private MinecraftBot mb;
	private MBSquare sq;
	private MBVec target;
	private MBVec buffVec;
	
	/** Configuration options */
	private int MAX_MINING_DISTANCE = 6;
	private boolean MINE_UNDER_FEET = false;
	
	MBManager(MinecraftBot parMb){
		
		mb = parMb;
		scanner = mb.new MBScanner();
		((MBPlayerControllerMP)(mb.mc.playerController)).flag = false;
	}
	
	public void stop(){

		botList.clear();
		step = 0;
		KeyBinding.unPressAllKeys();

	}
	
	public static MBManager searchAndReach(MinecraftBot mb, int[] items){
		
		MBManager manager = new MBManager(mb);
		manager.items = items;
		manager.botList.add(manager.mb.new BotPath(manager.mb.new MBVec(0, -1, 0), items, "MBManager Search and Reach Path", true));
		manager.macroId = 3;
		manager.step = 1;
		mb.tools.s("Searching - Don't move!");
		return manager;
	}
	
	public static MBManager chopSquare(MinecraftBot mb, MBSquare mbSq){
		
		MBManager manager = new MBManager(mb);
		manager.sq = mbSq;
		manager.macroId = 2;
		manager.step = 1;
		mb.tools.s("Mining - Don't move!");
		return manager;
	}
	
	public static MBManager chopStuff(MinecraftBot mb, int[] items){
		
		MBManager manager = new MBManager(mb);
		manager.items = items;
		manager.botList.add(manager.mb.new BotPath(manager.mb.new MBVec(0, -1, 0), items, "MBManager Chopwood Path", true));
		manager.macroId = 1;
		manager.step = 1;
		mb.tools.s("Chopper - Don't move!");
		return manager;
	}
	
	public enum MANAGER_RETURN{
		
		PLAYERCONTROLLER_ERROR, KEEP_RUNNING, HALT_OK
	}

	public MANAGER_RETURN run() {

		/* Block creation/destruction flag in MBPlayerControllerMP. Must be set to false when bot is running. */
		if(((MBPlayerControllerMP)(mb.mc.playerController)).flag) return MANAGER_RETURN.PLAYERCONTROLLER_ERROR;
		
		if(!isRunning()){
			mb.tools.s("§4Stopped.");
			stop();
			return MANAGER_RETURN.HALT_OK;
		}
		
		if(!runOptions()){
		
			if(macroId == 1){
				
				runChopStuff();
				
			}else if(macroId == 2){
				
				runChopSquare();
				
			}else if(macroId == 3){
				
				runSearchAndReach();
				
			}
		
		}
		
		runBots();

		return MANAGER_RETURN.KEEP_RUNNING;

	}
	
	private boolean runOptions(){
		
		if(mb.mc.thePlayer.getBrightness(1) < mb.TORCH_THRESHOLD && mb.tools.countItems(mb.ITEMS_TORCH) > 0 && mb.new MBVec(0, -1, 0).getId() == 0){
			
			botList.add(mb.new BotBuilder(mb.new MBVec(0, -1, 0), mb.ITEMS_TORCH));
			
			return true;
		}
		
		return false;
	}
	
	private void runSearchAndReach(){
		
		/* Second step: Destination reached */
		if(step == 2 && botList.size() == 0){
			
			step = 0;
		}
		
		/* First step: Finish search */
		if(step == 1 && botList.peek() != null && botList.peek() instanceof BotPath){
			
			if(((BotPath)botList.peek()).path != null){
				target = ((BotPath)botList.pop()).path.firstElement();
				botList.add(mb.new BotMove(target, false));
				step = 2;
			}else if(((BotPath)botList.peek()).aStar == null){
				botList.clear();
				step = 0;
			}

		}
	}
	
	private void runChopSquare(){
		
		if(botList.size() == 0){
		
			if(step == 1){
				
				target = sq.next();
				if(target == null){
					
					stop();
				}else{
					
					botList.add(mb.new BotMove(target, false));
					step = 2;
				}
			}
			
			if(step == 2 || GRAVEL > 0){
				
				if(target.distanceTo() > MAX_MINING_DISTANCE){
					
					stop();
				}else{
					
					if(mb.scanner.scanVec(target.getVec(0, 1, 0), new int[]{12, 13})){
						
						botList.add(mb.new BotMiner(target.getVec(0, 1, 0)));
						GRAVEL = 15;
					}
					else if(!mb.scanner.scanVec(target.getVec(0, 1, 0), mb.BLOCKS_EMPTY_LAVA_WATER_FIRE)) botList.add(mb.new BotMiner(target.getVec(0, 1, 0)));
					else{ 
						botList.add(mb.new BotMiner(target));
						if(GRAVEL == 0) step = 3;
						else GRAVEL--;
					}
				}
			}
			
			if(step == 3){
				
				if(mb.scanner.scanVec(target.getVec(0, -1, 0), mb.BLOCKS_EMPTY_LAVA_WATER_FIRE)){
					
					botList.add(mb.new BotBuilder(target.getVec(0, -1, 0), mb.BLOCKS_BUILD));
				}
				
				step = 1;
			}
		}
	}
	
	private void runChopStuff(){
		
		try{
		
			/* Fourth step: Target collected? Acquire new target. */
			if((step == 4 || step == 5) && botList.size() == 0){
				
				/* If one block over target is searched id, mine it (After coming back to original position)! */
				if(Arrays.binarySearch(items, target.getId(0, 1, 0)) >= 0 && target.distanceTo(mb.new MBVec(true)) <= MAX_MINING_DISTANCE - 2){
					
					target = target.getVec(0, 1, 0);
					step = 6;
				}
				else{
					
					botList.add(mb.new BotPath(mb.new MBVec(0, -1, 0), items, "MBManager Chopwood step 4", true));
					step = 1;
					System.out.println("Fourth Step");
				}
			}
	
			/* Third step: Target mined? Collecting. */
			if(step == 3 && botList.size() == 0){
				
				/* If target is two blocks high, move there. */
				if(target.getId(0, -1, 0) == 0 || target.getId(0, 1, 0) == 0){
					
					botList.add(mb.new BotMove(target.getVec().aH(), true, "MBManager Chopwood step 3"));
					step = 4;
				} else step = 5;
				System.out.println("Third Step");
			}
			
			/* Second step: Path reached? Mining target. */
			if((step == 2 && botList.size() == 0) || step == 6){
				
				botList.add(mb.new BotMiner(target));
				step = 3;
				System.out.println("Second Step " + target.toString());
			}
			
			/* First step: Path acquired? Moving to target. */
			if(step == 1 && botList.peek() instanceof BotPath && ((BotPath)botList.peek()).path != null){
				
				target = ((BotPath)botList.pop()).path.firstElement();
				botList.add(mb.new BotMove(target, false, "MBManager Chopwood step 1"));
				step = 2;
				System.out.println("First Step");
			}
		
		}catch(EmptyStackException e){
			
			step = 0;
		}
	}
	
	private boolean isRunning(){
		
		/* When to terminate bot */
		if(botList.size() == 0 && step == 0){
			
			((MBPlayerControllerMP)(mb.mc.playerController)).flag = true;
			return false;
		}
		
		return true;
	}
	
	private void runBots(){
		
		if(botList.size() > 0){
			
			botList.peek().run(botList);
		}
	}
}
