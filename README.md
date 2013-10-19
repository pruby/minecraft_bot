minecraft_bot
=============
Everything needed to update the bot is to update the sections listed below:


-------------------------------
NetClientHandler.java
-------------------------------
    
    import net.minecraft.src.MinecraftBot;
    
    /** MinecraftBot instance */
    public MinecraftBot mBot;

    public void handleLogin(Packet1Login par1Packet1Login)
    {
	(...)
	mBot = new MinecraftBot(mc);
    }
	
	
-------------------------------
EntityClientPlayerMP.java
-------------------------------
    
    public void onUpdate()
    {
	(...)
	this.sendQueue.mBot.tick();
    }

    public void sendChatMessage(String par1Str)
    {
    	if(!this.sendQueue.mBot.chat.isForBot(par1Str)) this.sendQueue.addToSendQueue(new Packet3Chat(par1Str));
    }
