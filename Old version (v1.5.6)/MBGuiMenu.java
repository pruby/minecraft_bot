package net.minecraft.src;

import java.util.Collections;

import net.minecraft.client.*;

public class MBGuiMenu extends GuiScreen
{

    /** The title string that is displayed in the top-center of the screen. */
    protected String screenTitle = "Minecraft Digging Bot";
    private GuiTextField[] textField;
    
    private int nbTxtBox = 4;
    private int hasFocus = 0;
    private ItemStack item = null;
    
    private boolean flag = false;

    public MBGuiMenu()
    {
    }
    
    public MBGuiMenu(ItemStack parItems, Minecraft mc)
    {
    	this.item = parItems;
    }

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    public void initGui()
    {
    	if(mc.mBot.isActive())mc.mBot.pause();
    	
    	if(item!=null){
	    	mc.gameSettings.itemId = item.itemID;
	    	mc.mBot.mbChat.sendChatMessage(".bot search:" + item.itemID);
	        mc.displayGuiScreen((GuiScreen)null);}
    	
        //this.buttonList.add(new GuiSlider(var6.returnEnumOrdinal(), this.width / 2 - 155 + var2 % 2 * 160, this.height / 6 + 24 * (var2 >> 1), var6, this.options.getKeyBinding(var6), this.options.getOptionFloatValue(var6)));
        //GuiSmallButton var7 = new GuiSmallButton(var6.returnEnumOrdinal(), this.width / 2 - 155 + var2 % 2 * 160, this.height / 6 + 24 * (var2 >> 1), var6, this.options.getKeyBinding(var6));

    	this.buttonList.add(new GuiButton(101, this.width / 2 - 75 - 2, this.height / 6, 75, 20, mc.gameSettings.torch?"Auto":"Off"));
    	this.buttonList.add(new GuiSlider(102, this.width / 2 - 155 + 1 * 160, this.height / 6, EnumOptions.LIGHTNING, mc.gameSettings.getKeyBinding(EnumOptions.LIGHTNING), mc.gameSettings.lightning));
    	
    	this.buttonList.add(new GuiButton(97, this.width / 2 - 95 - 2, this.height / 6 + 25, 160, 20, "Number of blocks in front"));
    	this.buttonList.add(new GuiButton(98, this.width / 2 - 95 - 2, this.height / 6 + 25 + 20, 160, 20, "Number of blocks on the side"));
    	this.buttonList.add(new GuiButton(99, this.width / 2 - 95 - 2, this.height / 6 + 25 + 40, 160, 20, "Number of blocks in height"));
    	this.buttonList.add(new GuiButton(100, this.width / 2 - 95 - 2, this.height / 6 + 25 + 60, 160, 20, "Number of rows to skip"));
    	this.buttonList.add(new GuiButton(96, this.width / 2 - 40 - 2, this.height / 6 + 20 + 90, 70, 20, mc.gameSettings.left?"Dig left":"Dig right"));
    	this.textField = new GuiTextField[nbTxtBox];
    	
    	for(int i=0; i<nbTxtBox; i++)
    		this.textField[i] = new GuiTextField(this.fontRenderer, this.width / 2 + 75 - 2, this.height / 6 + 25 + i * 20, this.width / 3 - 110 - 2, 20);
    	
    	for(int i=0; i<nbTxtBox; i++){
    		this.textField[i].setText(" 0");
    		this.textField[i].setMaxStringLength(3);}
    	
    	this.textField[hasFocus].setFocused(true);

    	this.buttonList.add(new GuiButton(105, this.width / 2 - 150 - 2, this.height / 6 + 134, 150, 20, "Search for..."));
    	this.buttonList.add(new GuiButton(106, this.width / 2 + 2, this.height / 6 + 134, 150, 20, "Search for Food"));
        this.buttonList.add(new GuiButton(94, this.width / 2 - 150 - 2, this.height / 6 + 156, 150, 20, mc.mBot.timeLocked==0?"Default Daytime":mc.mBot.timeLocked==mc.mBot.LIGHT_ON?"Day":"Night"));
        this.buttonList.add(new GuiButton(95, this.width / 2 + 2, this.height / 6 + 156, 150, 20, "Set settings to key"));
        this.buttonList.add(new GuiButton(104, this.width / 2 + 2, this.height / 6 + 178, 150, 20, "Start with current settings"));
    }

    /**
     * Fired when a control is clicked. This is the equivalent of ActionListener.actionPerformed(ActionEvent e).
     */
    protected void actionPerformed(GuiButton par1GuiButton)
    {
        if (par1GuiButton.enabled)
        {
        	if (par1GuiButton.id == 94)
        	{
        		mc.mBot.timeLocked = mc.mBot.timeLocked == 0? mc.mBot.LIGHT_ON: mc.mBot.timeLocked == mc.mBot.LIGHT_ON? mc.mBot.LIGHT_OFF: 0;
        	}
        	
        	if (par1GuiButton.id == 95)
        	{
        		if(((GuiButton)this.buttonList.get(buttonList.size()-2)).displayString != ">Press key<")((GuiButton)this.buttonList.get(buttonList.size()-2)).displayString = ">Press key<";
        		else {mc.gameSettings.macro = getString();}
        	}
        	
        	if (par1GuiButton.id == 96)
        	{
        		mc.gameSettings.left = !mc.gameSettings.left;
        	}
        	
        	if (par1GuiButton.id == 97)
            {
        		setFocus(0);
        		this.textField[0].setText("");
            }
        	
        	if (par1GuiButton.id == 98)
            {
        		setFocus(1);    
        		this.textField[1].setText("");         
            }
        	
        	if (par1GuiButton.id == 99)
            {
        		setFocus(2);  
        		this.textField[2].setText("");         
            }

            if (par1GuiButton.id == 100)
            {
            	setFocus(3);  
        		this.textField[3].setText("");         
            }

            if (par1GuiButton.id == 101)
            {
            	mc.gameSettings.torch = !mc.gameSettings.torch;   
            }

            if (par1GuiButton.id == 102)
            {
            	
            }

            if (par1GuiButton.id == 104)
            {
            	mc.mBot.mbChat.sendChatMessage(getString());
                this.mc.displayGuiScreen((GuiScreen)null);
            }
            
            if (par1GuiButton.id == 105)
            {
            	this.mc.displayGuiScreen(new MBGuiContainerCreative(mc.thePlayer));
            }

            if (par1GuiButton.id == 106)
            {
            	mc.mBot.mbChat.sendChatMessage(".bot search:food");
    	        mc.displayGuiScreen((GuiScreen)null);
            }
        
	        if (par1GuiButton.id == 200)
	        {
	        	
	        }
        }
    }
    
    private String getString(){return ".bot sq:" + this.textField[0].getText().trim() + "," + this.textField[1].getText().trim() + "," + this.textField[2].getText().trim() + "," + this.textField[3].getText().trim() + "," + (mc.gameSettings.left?"left":"right");} 

    private void setFocus(int i){
    	
    	for(int j=0; j<nbTxtBox; j++)
			if(this.textField[j].isFocused()) this.textField[j].setFocused(false);
    	this.textField[i].setFocused(true);
    	
    	flag = true;
    }
    
    protected void keyTyped(char par1, int par2)
    {
    	super.keyTyped(par1, par2);
    	if(((GuiButton)this.buttonList.get(buttonList.size()-2)).displayString == ">Press key<"){mc.gameSettings.keyBindBotMacro.keyCode=par2;
    																							   mc.gameSettings.macro=getString();
																						    	   this.mc.displayGuiScreen((GuiScreen)null);}
    	if(par2==mc.gameSettings.keyBindBotMenu.keyCode) this.mc.displayGuiScreen((GuiScreen)null);
    	if(par2==15)for(int i=0; i<nbTxtBox; i++)
    					if(this.textField[i].isFocused()){this.textField[i].setFocused(false);
    													  this.textField[(i+1)%nbTxtBox].setFocused(true);
    													  i=nbTxtBox;}

    	for(int i=0; i<nbTxtBox; i++)
			if(this.textField[i].isFocused()) this.textField[i].textboxKeyTyped(par1, par2);
    }
    
    protected void mouseClicked(int par1, int par2, int par3)
    {
    	super.mouseClicked(par1, par2, par3);
    	
        if (!flag){ if (par3 == 0) for (int var4 = 0; var4 < textField.length; ++var4) textField[var4].mouseClicked(par1, par2, par3);}
        else flag = false;
    }
    
    public void updateScreen()
    {
    	for(int i=0; i<nbTxtBox; i++)
    		this.textField[i].updateCursorCounter();
    }
    
    public void drawScreen(int par1, int par2, float par3)
    {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, this.screenTitle, this.width / 2, 20, 16777215);
       
        ((GuiButton)this.buttonList.get(0)).displayString = mc.gameSettings.torch?"Auto":"Off";
        ((GuiButton)this.buttonList.get(1)).enabled = mc.gameSettings.torch;
        ((GuiButton)this.buttonList.get(6)).displayString = mc.gameSettings.left?"Dig left":"Dig right";
        ((GuiButton)this.buttonList.get(buttonList.size()-3)).displayString = mc.mBot.timeLocked==0?"Default Daytime":mc.mBot.timeLocked==mc.mBot.LIGHT_ON?"Day":"Night";
        
        for(int i=0; i<nbTxtBox; i++)
        	this.textField[i].drawTextBox();

    	this.drawString(this.fontRenderer, "Place torch", this.width / 2 - 150, this.height / 6 + 6, -1);
        
        super.drawScreen(par1, par2, par3);
    }
}
