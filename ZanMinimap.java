/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.minecraft.src;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.src.mamiyaotaru.EntityWaypoint;
import net.minecraft.src.mamiyaotaru.EnumOptionsHelperMinimap;
import net.minecraft.src.mamiyaotaru.EnumOptionsMinimap;
import net.minecraft.src.mamiyaotaru.GuiMinimap;
import net.minecraft.src.mamiyaotaru.RenderWaypoint;
import net.minecraft.src.mamiyaotaru.Waypoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.input.Mouse;
import java.util.Random;
// extends BaseMod
//TODO: remodloaderize
public class ZanMinimap implements Runnable { // implements Runnable
	
	public Minecraft game; 
	
	private World world;

	/*motion tracker, may or may not exist*/
//	private mod_MotionTracker motionTracker = null;
	
	/*whether motion tracker exists*/
	public Boolean motionTrackerExists = false;
	
	/* mob overlay */
	public ZanRadar radar = null;
	
	/*whether radar exists*/
	public Boolean radarAllowed = true;
	
	/*Textures for each zoom level*/
	private BufferedImage[] map = new BufferedImage[4];

	/*Block colour array*/
	private int[] blockColors = new int[4096];

	private int q = 0;
	private Random generator = new Random();
	/*Current Menu Loaded*/
	public int iMenu = 1;
	
	/*Current Gui Screen*/
	private GuiScreen guiScreen = null;

	/*Display anything at all, menu, etc..*/
	private boolean enabled = true;

	/*Was mouse down last render?*/
	private boolean lfclick = false;

	/*Toggle full screen map*/
	public boolean full = false;

	/*Is map calc thread still executing?*/
	public boolean active = false;

	/*Current level of zoom*/
	private int zoom = 2;

	/*Current build version*/
	public String zmodver = "v2.0";

	/*Menu input string*/
	private String inStr = "";

	/*Waypoint name temporary input*/
	private String way = "";

	/*Waypoint X coord temp input*/
	private int wayX = 0;

	/*Waypoint Z coord temp input*/
	private int wayZ = 0;

	/*Colour or black and white minimap?*/
	private boolean rc = true;

	/*Holds error exceptions thrown*/
	private String error = "";

	/*Strings to show for menu*/
	private String[] sMenu = new String[5]; // bump up options here

	/*Time remaining to show error thrown for*/
	private int ztimer = 0;

	/*Minimap update interval*/
	private int timer = 0;

	/*Key entry interval (ie, can only zoom once every 20 ticks)*/
	private int fudge = 0;

	/*Last X coordinate rendered*/
	private int lastX = 0;

	/*Last Z coordinate rendered*/
	private int lastZ = 0;
	
	/*Last Y coordinate rendered*/
	private int lastY = 0;
	
	/*Last UI scale factor*/
	private int scScale = 0;
	
	/*Array of blockHeights*/
	private int[][] heightArray = new int[256][256];

	/*Last zoom level rendered at*/
	public int lZoom = 0;

	/*Menu level for next render*/
	private int next = 0;

	/*Cursor blink interval*/
	private int blink = 0;

	/*Last key down on previous render*/
	private int lastKey= 0;

	/*Direction you're facing*/
	private float direction = 0.0f;

	/*Setting file access*/
	private File settingsFile;

	/*Name of World currently loaded*/
	private String worldName = "";
	
	/*Current Texture Pack*/
	private TexturePackBase pack = null;

	/*Is the scrollbar being dragged?*/
	private boolean scrClick = false;

	/*Scrollbar drag start position*/
	private int scrStart = 0;

	/*Scrollbar offset*/
	private int sMin = 0;

	/*Scrollbar size*/
	private int sMax = 67;

	/*1st waypoint entry shown*/
	private int min = 0;

	/*Zoom key index*/
	private int zoomKey = Keyboard.KEY_Z;

	/*Menu key index*/
	private int menuKey = Keyboard.KEY_M;
	
	/*Hide just the minimap*/
	public boolean hide = false;
	
	/*Show coordinates toggle*/
	private boolean coords = true;

	/*Show the minimap when in the Nether*/
	private boolean showNether = true;

	/*Experimental cave mode (only applicable to overworld)*/
	private boolean showCaves = true;
	
	/*Dynamic lighting toggle*/
	private boolean lightmap = true;

	/*Terrain depth toggle*/
	private boolean heightmap = false;
	
	/*Terrain bump toggle*/
	private boolean slopemap = true;

	/*Square map toggle*/
	public boolean squareMap = false;

	/*Old north toggle*/
	public boolean oldNorth = false;

	public int northRotate = 0;
	
	/*Waypoint in world beacon toggle*/
	private boolean showBeacons = true;

	/*Show welcome message toggle*/
	private boolean welcome = true;

	/*Waypoint names and data*/
	public ArrayList<Waypoint> wayPts;

	/*Map calculation thread*/
	public Thread zCalc = new Thread(this);

	//should we be running the calc thread?
	public static boolean threading = true;

	/*Polygon creation class*/
	private Tessellator tesselator = Tessellator.instance;

	/*Font rendering class*/
	private FontRenderer fontRenderer;

	/*Render texture*/
	public RenderEngine renderEngine;
	
	/* reference to our framebuffer object */
	private int fboID = 0;
	
	/*are framebuffer objects even supported*/
	private boolean fboEnabled = GLContext.getCapabilities().GL_EXT_framebuffer_object;
	
	/* reference to the texture created by rendering to the fbo */
	private int fboTextureID = 0;


	public static File getAppDir(String app)
	{
		return Minecraft.getAppDir(app);
	}

	public void chatInfo(String s) {
		game.thePlayer.addChatMessage(s);
	}
	public String getMapName()
	{
		//return game.theWorld.worldInfo.getWorldName();
		return game.getIntegratedServer().getWorldName();
	}
	public String getServerName()
	{
		//return game.gameSettings.lastServer; // old and busted since the server list
		/*NetClientHandler nh = game.getSendQueue();
		TcpConnection tcp = (TcpConnection)nh.getNetManager();
		Socket sock = tcp.getSocket();
		java.net.InetAddress address = sock.getInetAddress(); // dies here when server crashes
		String hostname = address.getHostName();
		return hostname;*/
		// aka
		/*try {
			return ((TcpConnection)(game.getSendQueue().getNetManager())).getSocket().getInetAddress().getHostName();
		}
		catch (Exception e) {
			return null;
		}*/
		//System.out.println("IP: " + game.getServerData().serverIP + " name: " + game.getServerData().serverName + " ?string: " + game.getServerData().field_78846_c + " ?long: " + game.getServerData().field_78844_e + " motd: " + game.getServerData().serverMOTD);
		return game.getServerData().serverIP; // better

	}

	public void drawPre()
	{
		tesselator.startDrawingQuads();
	}
	public void drawPost()
	{
		tesselator.draw();
	}
	public void glah(int g)
	{
		renderEngine.deleteTexture(g);
	}
	public void ldrawone(int a, int b, double c, double d, double e)
	{
		tesselator.addVertexWithUV(a, b, c, d, e);
	}
	public void ldrawtwo(double a, double b, double c)
	{
		tesselator.addVertex(a, b, c);
	}
	public void ldrawthree(double a, double b, double c, double d, double e)
	{
		tesselator.addVertexWithUV(a, b, c, d, e);
	}
	public int getMouseX(int scWidth)
	{
		return Mouse.getX()*(scWidth+5)/game.displayWidth;
	}
	public int getMouseY(int scHeight)
	{
		return (scHeight+5) - Mouse.getY() * (scHeight+5) / this.game.displayHeight - 1;
	}
	public void setMenuNull()
	{
		game.currentScreen =null;
	}
	public Object getMenu()
	{
		return game.currentScreen;
	}
	//@Override
	public void onTickInGame(Minecraft mc)
	{

		northRotate = oldNorth ? 0 : 90;
		if(game==null) game = mc;
	
	/*	if (motionTrackerExists && motionTracker.activated) {
			motionTracker.OnTickInGame(mc);
			return;
		}*/

		if(fontRenderer==null) fontRenderer = this.game.fontRenderer;

		if(renderEngine==null) renderEngine = this.game.renderEngine;

		ScaledResolution scSize = new ScaledResolution(game.gameSettings, game.displayWidth, game.displayHeight);
		int scWidth = scSize.getScaledWidth();
		int scHeight = scSize.getScaledHeight();
		int scScale = scSize.getScaleFactor();

		if (Keyboard.isKeyDown(menuKey) && this.game.currentScreen ==null) {
			//this.iMenu = 2;
			//this.game.displayGuiScreen(new GuiScreen());
			this.iMenu = 0; // close welcome message
			this.game.displayGuiScreen(new GuiMinimap(this));
			//ModLoader.openGUI(this.game.thePlayer, new GuiMinimap(this));
			// TODO leverage internal gui code for more complex menu (and one I understand haha).  done, except for waypoints
		}

		if (Keyboard.isKeyDown(zoomKey) && this.game.currentScreen == null && (this.showNether || this.game.thePlayer.dimension!=-1)) {
			this.SetZoom();
		}

		checkForChanges();
		if(/*deathMarker &&*/ this.game.currentScreen instanceof GuiGameOver && !(this.guiScreen instanceof GuiGameOver)) {
			//tamis doid
			handleDeath();
		}

        this.guiScreen = this.game.currentScreen;

		if (threading)
		{

			if (!zCalc.isAlive() && threading) {
				zCalc = new Thread(this);
				zCalc.start();
			}
			if (!(this.game.currentScreen instanceof GuiGameOver) && !(this.game.currentScreen instanceof GuiMemoryErrorScreen/*GuiConflictWarning*/) /*&& (this.game.thePlayer.dimension!=-1)*/ && this.game.currentScreen!=null)
				try {this.zCalc.notify();} catch (Exception local) {}
		}
		else if (!threading)
		{
			if (this.enabled && !this.hide)
				mapCalc((timer>300)?true:false);
			timer=(timer>300)?0:timer+1;
		}

		if (this.iMenu==1) {
			if (!welcome) this.iMenu = 0;
		}

		if ((this.game.currentScreen instanceof GuiIngameMenu) || (Keyboard.isKeyDown(61)) /*|| (this.game.thePlayer.dimension==-1)*/)
			this.enabled=false;
		else this.enabled=true;

		if (this.game.currentScreen==null && this.iMenu > 1) 
			this.iMenu = 0;

		scWidth -= 5;
		scHeight -= 5;

		/* // wut why not just get it
				if (this.oldDir != this.radius()) {
					this.direction += this.oldDir - this.radius(); 
					this.oldDir = this.radius();
				}
		 */

		this.direction = -this.radius();

		if (this.direction >= 360.0f)
			while (this.direction >= 360.0f)
				this.direction -= 360.0f;

		if (this.direction < 0.0f) {
			while (this.direction < 0.0f)
				this.direction += 360.0f;
		}

		if ((!this.error.equals("")) && (this.ztimer == 0)) this.ztimer = 500;

		if (this.ztimer > 0) this.ztimer -= 1;

		if (this.fudge > 0) this.fudge -= 1;

		if ((this.ztimer == 0) && (!this.error.equals(""))) this.error = "";
		
		if (this.enabled) {

			GL11.glDisable(GL11.GL_DEPTH_TEST);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glDepthMask(false);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			if (this.showNether || this.game.thePlayer.dimension!=-1) {
				if(this.full) renderMapFull(scWidth,scHeight);
				else renderMap(scWidth, scScale);
			}					

			if (ztimer > 0)
				this.write(this.error, 20, 20, 0xffffff);

			if (this.iMenu>0) showMenu(scWidth, scHeight);

			GL11.glDepthMask(true);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

			if (this.showNether || this.game.thePlayer.dimension!=-1) {
				if(coords) showCoords(scWidth, scHeight);
				if (radar != null && this.radarAllowed)
					radar.OnTickInGame(mc);
			}
		}
	
		// draw menus after ontickingame.  fscking modloader
/*      
  		ScaledResolution var8 = new ScaledResolution(this.game.gameSettings, this.game.displayWidth, this.game.displayHeight);
        int var9 = var8.getScaledWidth();
        int var10 = var8.getScaledHeight();
        int var11 = Mouse.getX() * var9 / this.game.displayWidth;
        int var13 = var10 - Mouse.getY() * var10 / this.game.displayHeight - 1;
        this.game.entityRenderer.setupOverlayRendering();

        if (this.game.currentScreen != null)
        {
            GL11.glClear(256);
            this.game.currentScreen.drawScreen(var11, var13, 0.5F); //this.game.timer.renderPartialTicks

            if (this.game.currentScreen != null && this.game.currentScreen.guiParticles != null)
            {
                this.game.currentScreen.guiParticles.draw(0.5F); //this.game.timer.renderPartialTicks
            }
        }
*/
		//this.getWorld().addWeatherEffect(new EntityLightningBolt(this.getWorld(), -88, 64, 275));
		
	}

	private void checkForChanges() {
		String j;
		String mapName;
		if (game.isIntegratedServerRunning())
			mapName = this.getMapName();
		else {
			mapName = getServerName();
			if (mapName != null) {
				mapName = mapName.toLowerCase(); //.split(":"); we are fine with port.  deal with it in saving and loading
			}
		} 

		if(!worldName.equals(mapName) && (mapName != null)) {
			worldName = mapName;
			loadWaypoints();
			timer=500; // fullrender for new world
			if (!game.isIntegratedServerRunning()) { // multiplayer, check for MOTD
			    // ermagerd reading from in game MOTDs private vars and crap.  and it doesn't even work on first login for some reason.  read it from server list motd instead
				Object guiNewChat = this.game.ingameGUI.getChatGUI(); // NetClientHandler
				if (guiNewChat == null) {
					System.out.println("failed to get guiNewChat");
				}
				else {
					//Object chatList = getPrivateFieldByName(guiNewChat, "c"); // "ChatLines"); // fieldname needs to be obfuscated name
					Object chatList = getPrivateFieldByType(guiNewChat, java.util.List.class, 1); // "ChatLines"); // fieldname needs to be obfuscated name
					if (chatList == null) {
						System.out.println("could not get chatlist");
					}
					else {
						boolean killRadar = false;
						System.out.println("chatlist size: " + ((java.util.List)chatList).size());
						for (int t = 0; t < ((java.util.List)chatList).size(); t++) {
							String msg = ((ChatLine)((java.util.List)chatList).get(t)).getChatLineString();
							System.out.println("message: " + msg);
							if(msg.contains("�3 �6 �3 �6 �3 �6 �e")) { 
								killRadar = true;
							//	System.out.println("no radar");
							}
						}
						if (killRadar) this.radarAllowed = false;
						else this.radarAllowed = true; // allow radar if server doesn't kill it
					}
				}
			 	
			}
			else {
				radarAllowed = true; // allow for singleplayer worlds
			}
		}
		
		if (!(this.getWorld().equals(world))) {
			this.world = this.getWorld();
			injectWaypointsEntities();
		}
		
		if ((pack == null) || !(pack.equals(game.texturePackList.getSelectedTexturePack()))) {
			pack = game.texturePackList.getSelectedTexturePack();
			try {
			//	new Thread(new Runnable() { // load in a thread so we aren't blocking, particularly for giant texture packs
			//		public void run() {
						loadTexturePackColors();
						if (radar != null) {
							radar.setTexturePack(pack);
							radar.loadTexturePackIcons();
						}

			//		}
			//	}).start();
			}
			catch (Exception e) {
				//System.out.println("texture pack not ready yet");
			}
		}
	}
	
	private void handleDeath() { 
		boolean currentlyHiding = this.hide;
		this.hide = true;
		int toDel = -1;
		for(Waypoint pt:wayPts) {
			if (pt.name.equals("Latest Death"))
				toDel = wayPts.indexOf(pt);
			// don't remove here, while iterating.  comodification error!
		}
		if (toDel != -1)
			this.delWay(toDel); // remove previous

		Waypoint deathMarker = new Waypoint("Latest Death", this.xCoord(), this.zCoord(), true, 255, 255, 255, "skull");
		wayPts.add(deathMarker);
		deathMarker.setDisplayInWorld(this.showBeacons);
		EntityWaypoint ewpt = new EntityWaypoint(this.getWorld(), deathMarker, (this.game.thePlayer.dimension==-1));
		this.getWorld().addWeatherEffect(ewpt);
		
		this.saveWaypoints();
		this.hide = currentlyHiding;
	}

	private int chkLen(String paramStr) {
		return this.fontRenderer.getStringWidth(paramStr);
	}

	private void write(String paramStr, int paramInt1, int paramInt2, int paramInt3) {
		this.fontRenderer.drawStringWithShadow(paramStr, paramInt1, paramInt2, paramInt3);
	}

	private int xCoord() {
		return (int)(this.game.thePlayer.posX < 0.0D ? this.game.thePlayer.posX - 1 : this.game.thePlayer.posX);
	}

	private int zCoord() {
		return (int)(this.game.thePlayer.posZ < 0.0D ? this.game.thePlayer.posZ - 1 : this.game.thePlayer.posZ);
	}

	private int yCoord() {
		return (int)this.game.thePlayer.posY;
	}
	

	private float radius() {
		return this.game.thePlayer.rotationYaw;
	}

	private String dCoord(int paramInt1) {
		if(paramInt1 < 0)
			return "-" + Math.abs(paramInt1+1);
		else
			return "+" + paramInt1;
	}

	private int tex(BufferedImage paramImg) {
		return this.renderEngine.allocateAndSetupTexture(paramImg);
	}

	private int img(String paramStr) { // returns index of texturemap(name) aka glBoundTexture.  If there isn't one, it glBindTexture's it in setupTexture
		return this.renderEngine.getTexture(paramStr);
	}

	private void disp(int paramInt) { 
		this.renderEngine.bindTexture(paramInt); // this func glBindTexture's GL_TEXTURE_2D, int paramInt
	}
	
	public World getWorld()
	{
		return game.theWorld;
	}

	private final int getBlockHeight(boolean nether, World world, int x, int z, int starty) 
	{
		if (!nether) {
			//int height = getBlockHeight(data, x, z); // newZan
			//int height = data.getChunkFromBlockCoords(x, z).getHeightValue(x & 0xf, z & 0xf); // replicate old way
			//int height = data.getHeightValue(x, z); // new method in world that easily replicates old way
			return world.getHeightValue(x, z);
		}
		else {
			int y = starty;
			//if (world.getBlockMaterial(x, y, z) == Material.air) {  // anything not air.  too much
			//if (!world.isBlockOpaqueCube(x, y, z)) { // anything not see through (no lava, water).  too little
			if (Block.lightOpacity[world.getBlockId(x, y, z)] == 0) { // material that blocks (at least partially) light - solids, liquids, not flowers or fences.  just right!
				while (y > 0) {
					y--;
					if (Block.lightOpacity[world.getBlockId(x, y, z)] > 0) 
						return y + 1;
				}
			}
			else {
				while ((y <= starty+10) && (y < 127)) {
					y++;
					if (Block.lightOpacity[world.getBlockId(x, y, z)] == 0)
						return y;
				}
			}
			return -1;
			//				return this.zCoord() + 1; // if it's solid all the way down we'll just take the block at the player's level for drawing
		}
	}
	
	private int getPixelColor(boolean full, boolean nether, World world, int skylightsubtract, int multi, int startX, int startZ, int imageX, int imageY) {
		int color24 = 0;
		int height = 0;
		int heightComp = 0;
		boolean solid = false;
		height = getBlockHeight(nether, world, startX + imageX, startZ + imageY, this.yCoord()); // x+y z-x west at top, x+x z+y north at top				if ((check) || (squareMap) || (this.full)) {
		if (full && slopemap) 
			heightArray[imageX][imageY]=height; // store heights so we don't get height twice for every pixel on full runs.  Only for slopemap though
		if (height == -1) {
			height = this.yCoord() + 1;
			solid = true;
		}
		if (this.rc) {
			if ((world.getBlockMaterial(startX + imageX, height, startZ + imageY) == Material.snow) || (world.getBlockMaterial(startX + imageX, height, startZ + imageY) == Material.craftedSnow)) 
				color24 = getBlockColor(80,0); // snow
			else {
				color24 = getBlockColor(world.getBlockId(startX + imageX, height - 1, startZ + imageY), world.getBlockMetadata(startX + imageX, height - 1, startZ + imageY));
			}
		} else color24 = 0xFFFFFF;

		if ((color24 != this.blockColors[0]) && (color24 != 0)) {
			if ((heightmap || slopemap) && !solid) {
				int diff=0;
				double sc = 0;
				if (heightmap) {
					diff = height-this.yCoord();
					//double sc = Math.log10(Math.abs(i2)/8.0D+1.0D)/1.3D;
					sc = Math.log10(Math.abs(diff)/8.0D+1.0D)/1.8D;
				}
				else if (slopemap) {
					if (full && ((oldNorth&&imageX<32 * multi-1) || (!oldNorth && imageX>0)) && imageY<32 * multi-1) // old north reverses X- to X+
						heightComp = heightArray[imageX-((oldNorth)?-1:1)][imageY+1]; // on full run, get stored height for neighboring pixels (if it exists)
					else
						heightComp = getBlockHeight(nether, world, startX + imageX -((oldNorth)?-1:1), startZ + imageY + 1, this.yCoord());
					if (heightComp == -1) // if compared area is solid, don't bump the stuff next to it
						heightComp = height;
					diff = heightComp-height;
					if (diff!=0){
						sc =(diff>0)?1:(diff<0)?-1:0;
						sc = sc/8;
					}
				}

				int r = color24 / 0x10000;
				int g = (color24 - r * 0x10000)/0x100;
				int b = (color24 - r * 0x10000-g*0x100);

				if (diff>=0) {
					r = (int)(sc * (0xff-r)) + r;
					g = (int)(sc * (0xff-g)) + g;
					b = (int)(sc * (0xff-b)) + b;
				} else {
					sc=Math.abs(sc);
					r = r -(int)(sc * r);
					g = g -(int)(sc * g);
					b = b -(int)(sc * b);
				}

				color24 = r * 0x10000 + g * 0x100 + b;
			}
			int i3 = 255;

			if (lightmap)
				//i3 = data.getBlockLightValue_do(startX + imageX, height, startZ + imageY, false) * 17; // SMP doesn't update skylightsubtract
				i3 = calcLightSMPtoo(world, startX + imageX, height, startZ + imageY, skylightsubtract) * 17;
			else if (solid) 
				i3 = 0;
			if(i3 > 255) i3 = 255;

			if (nether) {
				if (!solid)
					if(i3 < 76) i3 = 76; // nether/cave shows some light even in the dark so you can see caves/nether surface that isn't lit.  If it's solid though leave it black
					else
						if(i3<0) i3 = 0; // solid is black
			}
			else { // overworld
				if(i3 < 32) i3 = 32; // overworld lowest black is lower for some reason.  not as black as solid though
			}

			// store darkness in actual RGB.  Instead of mixing with black based on alpha later.  Can save alpha for stencilling this into a circle
			int r = color24 / 0x10000;
			int g = (color24 - r * 0x10000)/0x100;
			int b = (color24 - r * 0x10000-g*0x100);
			r=r*i3/255;
			g=g*i3/255;
			b=b*i3/255;
			color24 = r * 0x10000 + g * 0x100 + b;
			color24 = 255 * 0x1000000 + color24 ;

			// storing lighting in alpha channel.  doesn't work so well with stencilling
			//color24 = i3 * 0x1000000 + color24 ;
		}
		return color24;
	}
	
	private int calcLightSMPtoo(World world, int x, int y, int z, int skylightsubtract) {
		//				return getWorld().getBlockLightValue_do(x, z, y, false);
		Chunk chunk = world.getChunkFromChunkCoords(x >> 4, z >> 4);
		return chunk.getBlockLightValue(x &= 0xf, y, z &= 0xf, skylightsubtract); // call calculate since the var calc sets can't be counted on to be set in SMP.  ie it isn't
		// actually passed in.  called calculate once per tick in mapcalc instead of once per pixel.  same reason though
	}
	
	private void mapCalc(boolean full) {
		//final long startTime = System.nanoTime();
		int startX = this.xCoord(); // 1
		int startZ = this.zCoord(); // j
		int startY = this.yCoord();
		int offsetX = startX - lastX;
		int offsetZ = startZ - lastZ;
		int offsetY = startY - lastY;
		if (!full && offsetX == 0 && offsetZ == 0) 
			return;
		boolean nether = false;
		if (this.game.thePlayer.dimension==0)
			//if (showCaves && getWorld().getChunkFromBlockCoords(this.xCoord(), this.yCoord()).skylightMap.getNibble(this.xCoord() & 0xf, this.zCoord(), this.yCoord() & 0xf) <= 0) // ** pre 1.2
			//if (showCaves && getWorld().getChunkFromBlockCoords(this.xCoord(), this.yCoord()).func_48495_i()[this.zCoord() >> 4].func_48709_c(this.xCoord() & 0xf, this.zCoord() & 0xf, this.yCoord() & 0xf) <= 0) // ** post 1.2, naive: might not be a vertical chunk for the given chunk and height
			if (showCaves && getWorld().getChunkFromBlockCoords(this.xCoord(), this.zCoord()).getSavedLightValue(EnumSkyBlock.Sky, this.xCoord() & 0xf, this.yCoord(), this.zCoord() & 0xf) <= 0) // ** post 1.2, takes advantage of the func in chunk that does the same thing as the block below
				nether = true;
			else
				nether = false;
		else if (showNether)
			nether = true;
		else
			return; // if we are nether and nether mapping is not on, just exit;
		World world=this.getWorld();
		this.lastX = startX;
		this.lastZ = startZ;
		this.lastY = startY;
		int skylightsubtract = getWorld().calculateSkylightSubtracted(1.0F);
		this.lZoom = this.zoom;
		int multi = (int)Math.pow(2, this.lZoom);
		startX -= 16*multi;
		startZ -= 16*multi; // + west at top, - north at top
		int color24 = 0; // k

		// flat map or bump map.  or heightmap with no changed height.  No logarithmic height shading, so we can get away with only drawing the edges
		if (full || (heightmap && offsetY!=0)) { // still do a full render sometimes though (to catch changes, or on heightmap with changed height)
			for (int imageY = (32 * multi)-1; imageY >= 0; imageY--) { // on full go down here since we use height array on full, and to not look weird we need to compare with Y+1
				if (oldNorth) // old north reverses X-1 to X+1
					for (int imageX = (32 * multi)-1; imageX >= 0; imageX--) {
						color24 = getPixelColor(full, nether, world, skylightsubtract, multi, startX, startZ, imageX, imageY);
						this.map[this.lZoom].setRGB(imageX, imageY, color24);
					}
				else {
					for (int imageX = 0; imageX < 32 * multi; imageX++) {
						color24 = getPixelColor(full, nether, world, skylightsubtract, multi, startX, startZ, imageX, imageY);
						this.map[this.lZoom].setRGB(imageX, imageY, color24);
					}
				}
			}
		}
		else { // render edges, if moved.  This is the norm for flat or bump.  Also hit if heightmap and height hasn't changed
			BufferedImage comp = new BufferedImage(this.map[this.lZoom].getWidth(), this.map[this.lZoom].getHeight(), this.map[this.lZoom].getType());
			try {
				BufferedImage snip = this.map[this.lZoom].getSubimage(java.lang.Math.max(0,offsetX), java.lang.Math.max(0, offsetZ), this.map[this.lZoom].getWidth()-java.lang.Math.abs(offsetX) , this.map[this.lZoom].getHeight()-java.lang.Math.abs(offsetZ));
				java.awt.Graphics gfx = comp.getGraphics ();
				gfx.drawImage (snip, java.lang.Math.max(0, -offsetX), java.lang.Math.max(0, -offsetZ), null);
				gfx.dispose ();
			}
			catch (java.awt.image.RasterFormatException e) {
				return; // bail
			}
			for (int imageY = ((offsetZ>0)?32 * multi - offsetZ:0); imageY < ((offsetZ>0)?32 * multi:-offsetZ); imageY++) {			
				for (int imageX = 0; imageX < 32 * multi; imageX++) {
					color24 = getPixelColor(full, nether, world, skylightsubtract, multi, startX, startZ, imageX, imageY);
					comp.setRGB(imageX, imageY, color24);
					//this.map[this.lZoom].setRGB(imageX, imageY, color24);
				}
			}
			for (int imageY = 0; imageY < 32 * multi; imageY++) {			
				for (int imageX = ((offsetX>0)?32 * multi - offsetX:0); imageX < ((offsetX>0)?32 * multi:-offsetX); imageX++) {
					color24 = getPixelColor(full, nether, world, skylightsubtract, multi, startX, startZ, imageX, imageY);
					comp.setRGB(imageX, imageY, color24);
					//this.map[this.lZoom].setRGB(imageX, imageY, color24);
				}
			}
			this.map[this.lZoom] = comp;
		}
		//System.out.println("time: " + (System.nanoTime()-startTime));
	}

	public void run() {
		if (this.game == null)
			return;
		while(true){
			if(this.threading)
			{
				this.active = true;
				while(this.game.thePlayer!=null /*&& this.game.thePlayer.dimension!=-1*/ && active) {
					if (this.enabled && !this.hide)
						if(((this.lastX!=this.xCoord()) || (this.lastZ!=this.zCoord()) || (this.timer>300)))
							try {this.mapCalc((timer>300)?true:false);} catch (Exception local) {}
					this.timer=(this.timer>300)?0:this.timer+1;
					this.active = false;
				}
				try {this.zCalc.sleep(10);} catch (Exception exc) {}
				try {this.zCalc.wait(0);} catch (Exception exc) {}
			}
			else
			{
				try {this.zCalc.sleep(1000);} catch (Exception exc) {}
				try {this.zCalc.wait(0);} catch (Exception exc) {}
			}
		}
	}
	//END UPDATE SECTION


	public static ZanMinimap instance;

	public ZanMinimap() {
		//TODO: remodloaderize
		//ModLoader.setInGameHook(this, true, false);

		instance=this;
	/*	if (classExists("mod_MotionTracker")) {
			motionTracker = new mod_MotionTracker();		
			motionTrackerExists = true;
		}*/

//		if (classExists("ZanRadar")) { // change to mod_ZanRadar if this ever becomes independent and modloader enabled
			radar = new ZanRadar(this);		
//		}

		zCalc.start();
		this.map[0] = new BufferedImage(32,32,2);
		this.map[1] = new BufferedImage(64,64,2);
		this.map[2] = new BufferedImage(128,128,2);
		this.map[3] = new BufferedImage(256,256,2);

		this.sMenu[0] = "�4Zan's�F Mod! " + this.zmodver + " Maintaned by MamiyaOtaru";
		this.sMenu[1] = "Welcome to Zan's Minimap, there are a";
		this.sMenu[2] = "number of features and commands available to you.";
		this.sMenu[3] = "- Press �B" + Keyboard.getKeyName(zoomKey) + " �Fto zoom in/out, or �B"+ Keyboard.getKeyName(menuKey) + "�F for options.";
		this.sMenu[4] = "�7Press �F" + Keyboard.getKeyName(zoomKey) + "�7 to hide.";
		
		if (fboEnabled)
			setupFBO(); // setup our framebuffer object

		settingsFile = new File(getAppDir("minecraft"), "zan.settings");

		try {
			if(settingsFile.exists()) {
				BufferedReader in = new BufferedReader(new FileReader(settingsFile));
				String sCurrentLine;
				while ((sCurrentLine = in.readLine()) != null) {
					String[] curLine = sCurrentLine.split(":");

					if(curLine[0].equals("Show Coordinates"))
						coords = Boolean.parseBoolean(curLine[1]);
					else if(curLine[0].equals("Show Map in Nether"))
						showNether = Boolean.parseBoolean(curLine[1]);
					else if(curLine[0].equals("Enable Cave Mode"))
						showCaves = Boolean.parseBoolean(curLine[1]);
					else if(curLine[0].equals("Dynamic Lighting"))
						lightmap = Boolean.parseBoolean(curLine[1]);
					else if(curLine[0].equals("Height Map"))
						heightmap = Boolean.parseBoolean(curLine[1]);
					else if(curLine[0].equals("Slope Map"))
						slopemap = Boolean.parseBoolean(curLine[1]);
					else if(curLine[0].equals("Square Map"))
						squareMap = Boolean.parseBoolean(curLine[1]);
					else if(curLine[0].equals("Old North"))
						oldNorth = Boolean.parseBoolean(curLine[1]);
					else if(curLine[0].equals("Waypoint Beacons"))
						showBeacons = Boolean.parseBoolean(curLine[1]);
					else if(curLine[0].equals("Welcome Message"))
						welcome = Boolean.parseBoolean(curLine[1]);
					else if(curLine[0].equals("Zoom Key"))
						zoomKey = Keyboard.getKeyIndex(curLine[1]);
					else if(curLine[0].equals("Menu Key"))
						menuKey = Keyboard.getKeyIndex(curLine[1]);
					else if(curLine[0].equals("Threading"))
						threading=Boolean.parseBoolean(curLine[1]);
					// radar
					else if((radar != null) && curLine[0].equals("Hide Radar"))
						radar.hide = Boolean.parseBoolean(curLine[1]);
					else if((radar != null) && curLine[0].equals("Show Hostiles"))
						radar.showHostiles = Boolean.parseBoolean(curLine[1]);
					else if((radar != null) && curLine[0].equals("Show Players"))
						radar.showPlayers = Boolean.parseBoolean(curLine[1]);
					else if((radar != null) && curLine[0].equals("Show Neutrals"))
						radar.showNeutrals = Boolean.parseBoolean(curLine[1]);
				}
				in.close();
			}
			//else {
				saveAll(); // save, to catch welcome being turned off.  If that gets added back as an option, can forego this
			//}
		} catch (Exception e) {}

		for(int i = 0; i<blockColors.length; i++)
			blockColors[i] = 0xff01ff;
		getDefaultBlockColors();
		
		Object renderManager = RenderManager.instance; 
		if (renderManager == null) {
			System.out.println("failed to get render manager");
			return;
		}

		//Object entityRenderMap = getPrivateFieldByName(renderManager, "o" /*"entityRenderMap"*/); // Map - fieldname needs to be obfuscated name
		Object entityRenderMap = getPrivateFieldByType(renderManager, Map.class); // Map - fieldname needs to be obfuscated name
		if (entityRenderMap == null) {
			System.out.println("could not get entityRenderMap");
			return;
		}

		RenderWaypoint renderWaypoint = new RenderWaypoint();
		((java.util.HashMap)entityRenderMap).put(EntityWaypoint.class, renderWaypoint);
		renderWaypoint.setRenderManager(RenderManager.instance);
		
		//this does the same, clunkier than the above though
     /*   ((java.util.HashMap)entityRenderMap).put(EntityWaypoint.class, new RenderWaypoint());
        Iterator iterator = ((java.util.HashMap)entityRenderMap).values().iterator();
        
        Render render = null;
        while (iterator.hasNext())
        {
            render = (Render)iterator.next();
            if (render.getClass() == RenderWaypoint.class)
            	render.setRenderManager(RenderManager.instance); 
        }*/

	}
	
	public Object getPrivateFieldByName (Object o, String fieldName) {   

		// Go and find the private field... 
		final java.lang.reflect.Field fields[] = o.getClass().getDeclaredFields();
		for (int i = 0; i < fields.length; ++i) {
			if (fieldName.equals(fields[i].getName())) {
				try {
					fields[i].setAccessible(true);
					return fields[i].get(o);
				} 
				catch (IllegalAccessException ex) {
					//Assert.fail ("IllegalAccessException accessing " + fieldName);
				}
			}
		}
		//Assert.fail ("Field '" + fieldName +"' not found");
		return null;

		/*java.lang.reflect.Field privateField = null;
		  try {
			  privateField = o.getClass().getDeclaredField(fieldName);
		  }
		  catch (NoSuchFieldException e){}
		  privateField.setAccessible(true);
		  Object obj = null;
		  try {
			  obj = privateField.get(o);
		  }
		  catch (IllegalAccessException e){}
		  return obj;*/
	}
	
	public Object getPrivateFieldByType (Object o, Class classtype) {   
		return getPrivateFieldByType(o, classtype, 0);
	}
	
	public Object getPrivateFieldByType (Object o, Class classtype, int index) {   
		// Go and find the private field... 
		int counter = 0;
		final java.lang.reflect.Field fields[] = o.getClass().getDeclaredFields();
		for (int i = 0; i < fields.length; ++i) {
			if (classtype.equals(fields[i].getType())) {
				if (counter == index) {
					try {
						fields[i].setAccessible(true);
						return fields[i].get(o);
					} 
					catch (IllegalAccessException ex) {
					}
				}
				counter++;
			}
		}
		return null;
	}
	
	private void getDefaultBlockColors() {
		blockColors[blockColorID(1, 0)] = 0x686868;
		blockColors[blockColorID(2, 0)] = 0x74b44a;
		blockColors[blockColorID(3, 0)] = 0x79553a;
		blockColors[blockColorID(4, 0)] = 0x959595;
		blockColors[blockColorID(5, 0)] = 0xbc9862; // oak wood planks
		blockColors[blockColorID(5, 1)] = 0x805e36; // spruce wood planks
		blockColors[blockColorID(5, 2)] = 0xd7c185; // birch planks
		blockColors[blockColorID(5, 3)] = 0x9f714a; // jungle planks
		blockColors[blockColorID(6, 0)] = 0x946428;
		blockColors[blockColorID(7, 0)] = 0x333333;
		blockColors[blockColorID(8, 0)] = 0x3256ff; // water
	/*	blockColors[blockColorID(8, 1)] = 0x3256ff;
		blockColors[blockColorID(8, 2)] = 0x3256ff;
		blockColors[blockColorID(8, 3)] = 0x3256ff;
		blockColors[blockColorID(8, 4)] = 0x3256ff;
		blockColors[blockColorID(8, 5)] = 0x3256ff;
		blockColors[blockColorID(8, 6)] = 0x3256ff;
		blockColors[blockColorID(8, 7)] = 0x3256ff; */
		blockColors[blockColorID(9, 0)] = 0x3256ff; // water still
		blockColors[blockColorID(10, 0)] = 0xd86514; //lava
	/*	blockColors[blockColorID(10, 1)] = 0xd76514;
		blockColors[blockColorID(10, 2)] = 0xd66414;
		blockColors[blockColorID(10, 3)] = 0xd56414;
		blockColors[blockColorID(10, 4)] = 0xd46314;
		blockColors[blockColorID(10, 5)] = 0xd36314;
		blockColors[blockColorID(10, 6)] = 0xd26214; */
		blockColors[blockColorID(11, 0)] = 0xd96514; // lava still
		blockColors[blockColorID(12, 0)] = 0xddd7a0;
		blockColors[blockColorID(13, 0)] = 0x747474;
		blockColors[blockColorID(14, 0)] = 0x747474;
		blockColors[blockColorID(15, 0)] = 0x747474;
		blockColors[blockColorID(16, 0)] = 0x747474;
		blockColors[blockColorID(17, 0)] = 0x342919; // logs right side up
		blockColors[blockColorID(17, 1)] = 0x342919;
		blockColors[blockColorID(17, 2)] = 0x342919;
		blockColors[blockColorID(17, 3)] = 0x584519;
		blockColors[blockColorID(17, 4)] = 0x342919; // logs on side
		blockColors[blockColorID(17, 5)] = 0x342919;
		blockColors[blockColorID(17, 6)] = 0x342919;
		blockColors[blockColorID(17, 7)] = 0x584519;
		blockColors[blockColorID(17, 8)] = 0x342919; 
		blockColors[blockColorID(17, 9)] = 0x342919;
		blockColors[blockColorID(17, 10)] = 0x342919;
		blockColors[blockColorID(17, 11)] = 0x584519;
		blockColors[blockColorID(17, 12)] = 0x342919; // logs all bark?
		blockColors[blockColorID(17, 13)] = 0x342919;
		blockColors[blockColorID(17, 14)] = 0x342919;
		blockColors[blockColorID(17, 15)] = 0x584519;
		blockColors[blockColorID(18, 0)] = 0x164d0c;
		blockColors[blockColorID(18, 1)] = 0x164d0c;
		blockColors[blockColorID(18, 2)] = 0x164d0c;
		blockColors[blockColorID(18, 3)] = 0x164d0c;
		blockColors[blockColorID(19, 0)] = 0xe5e54e;
		blockColors[blockColorID(20, 0)] = 0xffffff;
		blockColors[blockColorID(21, 0)] = 0x677087;
		blockColors[blockColorID(22, 0)] = 0xd2eb2;
		blockColors[blockColorID(23, 0)] = 0x747474;
		blockColors[blockColorID(24, 0)] = 0xc6bd6d; // sandstone
		blockColors[blockColorID(25, 0)] = 0x8f691d; // note block
		blockColors[blockColorID(30, 0)] = 0xf4f4f4; // cobweb
		blockColors[blockColorID(35, 0)] = 0xf4f4f4;
		blockColors[blockColorID(35, 1)] = 0xeb843e;
		blockColors[blockColorID(35, 2)] = 0xc55ccf;
		blockColors[blockColorID(35, 3)] = 0x7d9cda;
		blockColors[blockColorID(35, 4)] = 0xddd13a;
		blockColors[blockColorID(35, 5)] = 0x3ecb31;
		blockColors[blockColorID(35, 6)] = 0xe09aad;
		blockColors[blockColorID(35, 7)] = 0x434343;
		blockColors[blockColorID(35, 8)] = 0xafafaf;
		blockColors[blockColorID(35, 9)] = 0x2f8286;
		blockColors[blockColorID(35, 10)] = 0x9045d1;
		blockColors[blockColorID(35, 11)] = 0x2d3ba7;
		blockColors[blockColorID(35, 12)] = 0x573016;
		blockColors[blockColorID(35, 13)] = 0x41581f;
		blockColors[blockColorID(35, 14)] = 0xb22c27;
		blockColors[blockColorID(35, 15)] = 0x1b1717;
		blockColors[blockColorID(37, 0)] = 0xf1f902;
		blockColors[blockColorID(38, 0)] = 0xf7070f;
		blockColors[blockColorID(39, 0)] = 0x916d55;
		blockColors[blockColorID(40, 0)] = 0x9a171c;
		blockColors[blockColorID(41, 0)] = 0xfefb5d;
		blockColors[blockColorID(42, 0)] = 0xe9e9e9;
		blockColors[blockColorID(43, 0)] = 0xa8a8a8;
		blockColors[blockColorID(43, 1)] = 0xc6bd6d;
		blockColors[blockColorID(43, 2)] = 0xbc9862;
		blockColors[blockColorID(43, 3)] = 0x959595;
		blockColors[blockColorID(43, 4)] = 0xaa543b;
		blockColors[blockColorID(43, 5)] = 0x7a7a7a;
		blockColors[blockColorID(43, 6)] = 0xa8a8a8;
		blockColors[blockColorID(44, 0)] = 0xa8a8a8; // slabs
		blockColors[blockColorID(44, 1)] = 0xc6bd6d;
		blockColors[blockColorID(44, 2)] = 0xbc9862;
		blockColors[blockColorID(44, 3)] = 0x959595;
		blockColors[blockColorID(44, 4)] = 0xaa543b;
		blockColors[blockColorID(44, 5)] = 0x7a7a7a;
		blockColors[blockColorID(44, 6)] = 0xa8a8a8;
		blockColors[blockColorID(44, 8)] = 0xa8a8a8; // slabs upside down
		blockColors[blockColorID(44, 9)] = 0xc6bd6d;
		blockColors[blockColorID(44, 10)] = 0xbc9862;
		blockColors[blockColorID(44, 11)] = 0x959595;
		blockColors[blockColorID(44, 12)] = 0xaa543b;
		blockColors[blockColorID(44, 13)] = 0x7a7a7a;
		blockColors[blockColorID(45, 0)] = 0xaa543b;
		blockColors[blockColorID(46, 0)] = 0xdb441a;
		blockColors[blockColorID(47, 0)] = 0xb4905a;
		blockColors[blockColorID(48, 0)] = 0x1f471f;
		blockColors[blockColorID(49, 0)] = 0x101018;
		blockColors[blockColorID(50, 0)] = 0xffd800;
		blockColors[blockColorID(51, 0)] = 0xc05a01;
		blockColors[blockColorID(52, 0)] = 0x265f87;
		blockColors[blockColorID(53, 0)] = 0xbc9862;
		blockColors[blockColorID(53, 1)] = 0xbc9862;
		blockColors[blockColorID(53, 2)] = 0xbc9862;
		blockColors[blockColorID(53, 3)] = 0xbc9862;
		blockColors[blockColorID(54, 0)] = 0x8f691d; // chest
		blockColors[blockColorID(55, 0)] = 0x480000;
		blockColors[blockColorID(56, 0)] = 0x747474;
		blockColors[blockColorID(57, 0)] = 0x82e4e0;
		blockColors[blockColorID(58, 0)] = 0xa26b3e;
		blockColors[blockColorID(59, 0)] = 57872;
		blockColors[blockColorID(60, 0)] = 0x633f24;
		blockColors[blockColorID(61, 0)] = 0x747474;
		blockColors[blockColorID(62, 0)] = 0x747474;
		blockColors[blockColorID(63, 0)] = 0xb4905a;
		blockColors[blockColorID(64, 0)] = 0x7a5b2b;
		blockColors[blockColorID(65, 0)] = 0xac8852;
		blockColors[blockColorID(66, 0)] = 0xa4a4a4;
		blockColors[blockColorID(67, 0)] = 0x9e9e9e;
		blockColors[blockColorID(67, 1)] = 0x9e9e9e;
		blockColors[blockColorID(67, 2)] = 0x9e9e9e;
		blockColors[blockColorID(67, 3)] = 0x9e9e9e;
		blockColors[blockColorID(68, 0)] = 0x9f844d;
		blockColors[blockColorID(69, 0)] = 0x695433;
		blockColors[blockColorID(70, 0)] = 0x8f8f8f;
		blockColors[blockColorID(71, 0)] = 0xc1c1c1;
		blockColors[blockColorID(72, 0)] = 0xbc9862;
		blockColors[blockColorID(73, 0)] = 0x747474;
		blockColors[blockColorID(74, 0)] = 0x747474;
		blockColors[blockColorID(75, 0)] = 0x290000;
		blockColors[blockColorID(76, 0)] = 0xfd0000;
		blockColors[blockColorID(77, 0)] = 0x747474;
		blockColors[blockColorID(78, 0)] = 0xfbffff;
		blockColors[blockColorID(79, 0)] = 0x8ebfff;
		blockColors[blockColorID(80, 0)] = 0xffffff;
		blockColors[blockColorID(81, 0)] = 0x11801e;
		blockColors[blockColorID(82, 0)] = 0xffffff;
		blockColors[blockColorID(83, 0)] = 0xa1a7b2;
		blockColors[blockColorID(84, 0)] = 0x8f691d; // jukebox
		blockColors[blockColorID(85, 0)] = 0x9b664b;
		blockColors[blockColorID(86, 0)] = 0xbc9862;
		blockColors[blockColorID(87, 0)] = 0x582218;
		blockColors[blockColorID(88, 0)] = 0x996731;
		blockColors[blockColorID(89, 0)] = 0xcda838;
		blockColors[blockColorID(90, 0)] = 0x732486;
		blockColors[blockColorID(91, 0)] = 0xffc88d;
		blockColors[blockColorID(92, 0)] = 0xe3cccd;
		blockColors[blockColorID(93, 0)] = 0x979393;
		blockColors[blockColorID(94, 0)] = 0xc09393;
		blockColors[blockColorID(95, 0)] = 0x8f691d;
		blockColors[blockColorID(96, 0)] = 0x7e5d2d;
		blockColors[blockColorID(97, 0)] = 0x686868;
		blockColors[blockColorID(98, 0)] = 0x7a7a7a;
		blockColors[blockColorID(98, 1)] = 0x1f471f;
		blockColors[blockColorID(98, 2)] = 0x7a7a7a;
		blockColors[blockColorID(99, 0)] = 0xcaab78;
		blockColors[blockColorID(100, 0)] = 0xcaab78;
		blockColors[blockColorID(101, 0)] = 0x6d6c6a;
		blockColors[blockColorID(102, 0)] = 0xffffff;
		blockColors[blockColorID(103, 0)] = 0x979924;
		blockColors[blockColorID(104, 0)] = 39168;
		blockColors[blockColorID(105, 0)] = 39168;
		blockColors[blockColorID(106, 0)] = 0x1f4e0a;
		blockColors[blockColorID(107, 0)] = 0xbc9862;
		blockColors[blockColorID(108, 0)] = 0xaa543b;
		blockColors[blockColorID(108, 1)] = 0xaa543b;
		blockColors[blockColorID(108, 2)] = 0xaa543b;
		blockColors[blockColorID(108, 3)] = 0xaa543b;
		blockColors[blockColorID(109, 0)] = 0x7a7a7a;
		blockColors[blockColorID(109, 1)] = 0x7a7a7a;
		blockColors[blockColorID(109, 2)] = 0x7a7a7a;
		blockColors[blockColorID(109, 3)] = 0x7a7a7a;
		blockColors[blockColorID(110, 0)] = 0x6e646a; // mycelium
		blockColors[blockColorID(112, 0)] = 0x43262f; // netherbrick
		blockColors[blockColorID(114, 0)] = 0x43262f; // netherbrick stairs
		blockColors[blockColorID(114, 1)] = 0x43262f; // netherbrick stairs
		blockColors[blockColorID(114, 2)] = 0x43262f; // netherbrick stairs
		blockColors[blockColorID(114, 3)] = 0x43262f; // netherbrick stairs
		blockColors[blockColorID(121, 0)] = 0xd3dca4; // endstone
		blockColors[blockColorID(123, 0)] = 0x8f691d; // inactive glowstone lamp
		blockColors[blockColorID(124, 0)] = 0xcda838; // active glowstone lamp
		blockColors[blockColorID(125, 0)] = 0xbc9862; // wooden double slab
		blockColors[blockColorID(125, 1)] = 0x805e36;  
		blockColors[blockColorID(125, 2)] = 0xd7c185; 
		blockColors[blockColorID(125, 3)] = 0x9f714a; 
		blockColors[blockColorID(126, 0)] = 0xbc9862; // wooden slab
		blockColors[blockColorID(126, 1)] = 0x805e36;  
		blockColors[blockColorID(126, 2)] = 0xd7c185; 
		blockColors[blockColorID(126, 3)] = 0x9f714a;
		blockColors[blockColorID(126, 8)] = 0xbc9862; // wooden slab upside down
		blockColors[blockColorID(126, 9)] = 0x805e36;  
		blockColors[blockColorID(126, 10)] = 0xd7c185; 
		blockColors[blockColorID(126, 11)] = 0x9f714a;
		blockColors[blockColorID(127, 0)] = 0xae682a;  // cocoa plant
		blockColors[blockColorID(128, 0)] = 0xc6bd6d;  // sandstone stairs
		blockColors[blockColorID(128, 1)] = 0xc6bd6d;
		blockColors[blockColorID(128, 2)] = 0xc6bd6d;
		blockColors[blockColorID(128, 3)] = 0xc6bd6d;
		blockColors[blockColorID(129, 0)] = 0x747474; // emerald ore
		blockColors[blockColorID(130, 0)] = 0x2d0133; // ender chest (using side of enchanting table)
		blockColors[blockColorID(131, 0)] = 0x7a7a7a; // tripwire hook
		blockColors[blockColorID(132, 0)] = 0x767676; // tripwire
		blockColors[blockColorID(133, 0)] = 0x45d56a; // emerald block
		blockColors[blockColorID(134, 0)] = 0x805e36;  // spruce stairs
		blockColors[blockColorID(134, 1)] = 0x805e36;
		blockColors[blockColorID(134, 2)] = 0x805e36;
		blockColors[blockColorID(134, 3)] = 0x805e36;
		blockColors[blockColorID(135, 0)] = 0xd7c185;  // birch stairs
		blockColors[blockColorID(135, 1)] = 0xd7c185;
		blockColors[blockColorID(135, 2)] = 0xd7c185;
		blockColors[blockColorID(135, 3)] = 0xd7c185;
		blockColors[blockColorID(136, 0)] = 0x9f714a;  // jungle stairs
		blockColors[blockColorID(136, 1)] = 0x9f714a;
		blockColors[blockColorID(136, 2)] = 0x9f714a;
		blockColors[blockColorID(136, 3)] = 0x9f714a;
	}

	private final int blockColorID(int blockid, int meta) {
		return (blockid) | (meta << 8);
	}

	private final int getBlockColor(int blockid, int meta) {
		try {
			int col = blockColors[blockColorID(blockid, meta)];
			if (col != 0xff01ff) 
				return col;
			col = blockColors[blockColorID(blockid, 0)];
			if (col != 0xff01ff) 
				return col;
			col = blockColors[0];
			if (col != 0xff01ff) 
				return col;
		}
		catch (ArrayIndexOutOfBoundsException e) {
			//			System.err.println("BlockID: " + blockid + " - Meta: " + meta);
			throw e;
		}
		//		System.err.println("Unable to find a block color for blockid: " + blockid + " blockmeta: " + meta);
		return 0xff01ff;
	}

	private boolean classExists (String className) {
		try {
			Class.forName (className);
			return true;
		}
		catch (ClassNotFoundException exception) {
			return false;
		}
	}

	public void saveAll() {
		settingsFile = new File(getAppDir("minecraft"), "zan.settings");

		try {
			PrintWriter out = new PrintWriter(new FileWriter(settingsFile));
			out.println("Show Coordinates:" + Boolean.toString(coords));
			out.println("Show Map in Nether:" + Boolean.toString(showNether));
			out.println("Enable Cave Mode:" + Boolean.toString(showCaves));
			out.println("Dynamic Lighting:" + Boolean.toString(lightmap));
			out.println("Height Map:" + Boolean.toString(heightmap));
			out.println("Slope Map:" + Boolean.toString(slopemap));
			out.println("Square Map:" + Boolean.toString(squareMap));
			out.println("Old North:" + Boolean.toString(oldNorth));
			out.println("Waypoint Beacons:" + Boolean.toString(showBeacons));
			out.println("Welcome Message:" + Boolean.toString(welcome));
			out.println("Threading:" + Boolean.toString(threading));
			out.println("Zoom Key:" + Keyboard.getKeyName(zoomKey));
			out.println("Menu Key:" + Keyboard.getKeyName(menuKey));
			if (radar != null)
				radar.saveAll(out);
			out.close();
		} catch (Exception local) {
			chatInfo("�EError Saving Settings");
		}
	}

	private void saveWaypoints() {
		String[] worldNameParts = worldName.split(":");
		String worldNameSave = worldNameParts[0];
		if (worldNameParts.length != 1)
			worldNameSave = worldNameSave + "~colon~" + worldNameParts[1];

		settingsFile = new File(getAppDir("minecraft/mods/zan"), worldName + ".points");

		try {
			PrintWriter out = new PrintWriter(new FileWriter(settingsFile));

			for(Waypoint pt:wayPts) {
				if(!pt.name.startsWith("^"))
					out.println(pt.name + ":" + pt.x + ":" + pt.z + ":" + Boolean.toString(pt.enabled) + ":" + pt.red + ":" + pt.green + ":" + pt.blue + ":" + pt.imageSuffix);
			}

			out.close();
		} catch (Exception local) {
			chatInfo("�EError Saving Waypoints");
		}
	}

	private void loadWaypoints() {
		String[] worldNameParts = worldName.toLowerCase().split(":");
		String worldNameWithoutPort = worldNameParts[0];
		String worldNameWithPort = worldNameParts[0];
		if (worldNameParts.length != 1)
			worldNameWithPort = worldNameWithPort + "~colon~" + worldNameParts[1];
		
		wayPts = new ArrayList<Waypoint>();
		settingsFile = new File(getAppDir("minecraft/mods/zan"), worldNameWithPort + ".points");
		if(!settingsFile.exists()) { // try to get it without .port and from the old location
			settingsFile = new File(getAppDir("minecraft"), worldNameWithoutPort + ".points");
		}
		if(!settingsFile.exists()) { // try to get it without .port from the new location, in case users copied it over
			settingsFile = new File(getAppDir("minecraft/mods/zan"), worldNameWithoutPort + ".points");
		}
		if(!settingsFile.exists()) { // try to get it from Rei's
			settingsFile = new File(getAppDir("minecraft/mods/rei_minimap"), worldNameWithoutPort + ".points");
		}

		try {
			if(settingsFile.exists()) {
				BufferedReader in = new BufferedReader(new FileReader(settingsFile));
				String sCurrentLine;

				while ((sCurrentLine = in.readLine()) != null) {
					String[] curLine = sCurrentLine.split(":");
					
					Waypoint wpt = null;
					if(curLine.length==4) { //super old zan,s pre color I guess
						wpt = new Waypoint(curLine[0],Integer.parseInt(curLine[1]),Integer.parseInt(curLine[2]),Boolean.parseBoolean(curLine[3]));
					}
					else if (curLine.length==7) { // zan's when I started using it
						wpt = new Waypoint(curLine[0],Integer.parseInt(curLine[1]),Integer.parseInt(curLine[2]),Boolean.parseBoolean(curLine[3]),
								Float.parseFloat(curLine[4]), Float.parseFloat(curLine[5]), Float.parseFloat(curLine[6]));
					}
					else if (curLine.length==8) { // zan's with additional suffix (for "skull" etc)
						wpt = new Waypoint(curLine[0],Integer.parseInt(curLine[1]),Integer.parseInt(curLine[2]),Boolean.parseBoolean(curLine[3]),
								Float.parseFloat(curLine[4]), Float.parseFloat(curLine[5]), Float.parseFloat(curLine[6]), curLine[7]);
					}
					else if (curLine.length==6) { // rei's
						int color = Integer.parseInt(curLine[5], 16); // ,16 is the radix for hex, which rei stores his color as
				        float red = (float)(color >> 16 & 255)/255; // split out to RGB, then get as a fraction
				        float green = (float)(color >> 8 & 255)/255; // like we store it (and OpenGL uses)
				        float blue = (float)(color >> 0 & 255)/255;
				        // alternate way to do it bleh though this works don't experiment
						//int r = color24 / 0x10000;
						//int g = (color24 - r * 0x10000)/0x100;
						//int b = (color24 - r * 0x10000-g*0x100);
						wpt = new Waypoint(curLine[0],Integer.parseInt(curLine[1]),Integer.parseInt(curLine[3]),Boolean.parseBoolean(curLine[4]),
								red, green, blue, "");
					}
					
					if (wpt != null) {
						wayPts.add(wpt);
						wpt.setDisplayInWorld(this.showBeacons);
					}
					// do in checkChanges instead.  load them any time we are in a new world (bring them back after return from nether.  Initial load is also a world change; would double up with this
					//EntityWaypoint ewpt = new EntityWaypoint(this.getWorld(), wpt);
					//this.getWorld().addWeatherEffect(ewpt);
				}
								
				in.close();
				chatInfo("�EWaypoints loaded for " + worldNameWithoutPort);
			} else chatInfo("�EError: No waypoints exist for this world/server.");
		} catch (Exception local) {
			chatInfo("�EError Loading Waypoints");
			System.out.println(local.getLocalizedMessage());
		}
	}
	
	private void injectWaypointsEntities() {
		if (!(this.game.thePlayer.dimension==-1) || this.showNether) { // check if nether
			for(Waypoint wpt:wayPts) {
				EntityWaypoint ewpt = new EntityWaypoint(world, wpt, (this.game.thePlayer.dimension==-1));
				this.world.addWeatherEffect(ewpt);
			}
		}
	}
	
	private void displayWaypointEntities(boolean show) {
		for(Waypoint wpt:wayPts) {
			wpt.setDisplayInWorld(show);
		}
	}
	
	private void loadTexturePackColors() {
		try {
		    // Read from a file
		    //File file = new File("image.gif");
		    //image = ImageIO.read(file);

		    // Read from an input stream
		    //InputStream is = new BufferedInputStream(
		    //    new FileInputStream("image.gif"));
		    //image = ImageIO.read(is);

		    // Read from a URL
		    //URL url = new URL("http://hostname.com/image.gif");
		    //image = ImageIO.read(url);
		
			//File file = new File("c:/terrain.png");
			//java.awt.Image terrain = ImageIO.read(file);
			InputStream is = pack.getResourceAsStream("/terrain.png");
			java.awt.Image terrain = ImageIO.read(is);
			is.close();
			//System.out.println("WIDTH: " + terrain.getWidth(null));
			terrain = terrain.getScaledInstance(16,16, java.awt.Image.SCALE_SMOOTH);
			
			BufferedImage terrainBuff = new BufferedImage(terrain.getWidth(null), terrain.getHeight(null), BufferedImage.TYPE_INT_RGB);
			java.awt.Graphics gfx = terrainBuff.createGraphics();
		    //Paint the image onto the buffered image
		    gfx.drawImage(terrain, 0, 0, null);
		    gfx.dispose();
			
		    /**taking into account transparency in the bufferedimage makes stuff like the cobwebs look right.
		     * downside is it gets the actual RGB of water, which can be very bright.
		     * we sort of expect it to be darker (ocean deep, seeing the bottom through it etc)
		     * having non transparent bufferedimage bakes the transparency in, darkening the RGB  
		     * baking it in on cobweb makes it way too dark.  We want the actual pixel color there.
		     * experiment with leaves and ice - I think both look better with transparency baked in there too..
		     * shadows in leaves, stuff under the ice, etc.  So basically on the transparent blocks only cobwebs need real RGB 
		     */
		    BufferedImage terrainBuffTrans = new BufferedImage(terrain.getWidth(null), terrain.getHeight(null), BufferedImage.TYPE_4BYTE_ABGR);
			gfx = terrainBuffTrans.createGraphics();
		    //Paint the image onto the buffered image
		    gfx.drawImage(terrain, 0, 0, null);
		    gfx.dispose();


		 		    
			blockColors[blockColorID(1, 0)] = getColor(terrainBuff, 1);
//			blockColors[blockColorID(2, 0)] = getColor(terrainBuff, 0); // grass
			blockColors[blockColorID(2, 0)] = colorMultiplier(getColor(terrainBuff, 0), ColorizerGrass.getGrassColor(0.7,  0.7)) & 0x00FFFFFF;
			blockColors[blockColorID(3, 0)] = getColor(terrainBuff, 2); 
			blockColors[blockColorID(4, 0)] = getColor(terrainBuff, 16);
			blockColors[blockColorID(5, 0)] = getColor(terrainBuff, 4); // planks
			blockColors[blockColorID(5, 1)] = getColor(terrainBuff, 198);
			blockColors[blockColorID(5, 2)] = getColor(terrainBuff, 214);
			blockColors[blockColorID(5, 3)] = getColor(terrainBuff, 199);
			blockColors[blockColorID(6, 0)] = getColor(terrainBuff, 15);
			blockColors[blockColorID(7, 0)] = getColor(terrainBuff, 17);
			getWaterColor(terrainBuff);
			getLavaColor(terrainBuff);
	/*		blockColors[blockColorID(8, 0)] = getColor(terrainBuff, 205); // water
			blockColors[blockColorID(8, 1)] = getColor(terrainBuff, 205);
			blockColors[blockColorID(8, 2)] = getColor(terrainBuff, 205);
			blockColors[blockColorID(8, 3)] = getColor(terrainBuff, 205);
			blockColors[blockColorID(8, 4)] = getColor(terrainBuff, 205);
			blockColors[blockColorID(8, 5)] = getColor(terrainBuff, 205);
			blockColors[blockColorID(8, 6)] = getColor(terrainBuff, 205);
			blockColors[blockColorID(8, 7)] = getColor(terrainBuff, 205);
			blockColors[blockColorID(9, 0)] = getColor(terrainBuff, 205); // stationary water
			blockColors[blockColorID(10, 0)] = getColor(terrainBuff, 237); // lava
			blockColors[blockColorID(10, 1)] = getColor(terrainBuff, 237);
			blockColors[blockColorID(10, 2)] = getColor(terrainBuff, 237);
			blockColors[blockColorID(10, 3)] = getColor(terrainBuff, 237);
			blockColors[blockColorID(10, 4)] = getColor(terrainBuff, 237);
			blockColors[blockColorID(10, 5)] = getColor(terrainBuff, 237);
			blockColors[blockColorID(10, 6)] = getColor(terrainBuff, 237);
			blockColors[blockColorID(11, 0)] = getColor(terrainBuff, 237); */// flowing lava
			blockColors[blockColorID(12, 0)] = getColor(terrainBuff, 18);
			blockColors[blockColorID(13, 0)] = getColor(terrainBuff, 19);
			blockColors[blockColorID(14, 0)] = getColor(terrainBuff, 32);
			blockColors[blockColorID(15, 0)] = getColor(terrainBuff, 33);
			blockColors[blockColorID(16, 0)] = getColor(terrainBuff, 34);
			blockColors[blockColorID(17, 0)] = getColor(terrainBuff, 21); // logs right side up
			blockColors[blockColorID(17, 1)] = getColor(terrainBuff, 21);
			blockColors[blockColorID(17, 2)] = getColor(terrainBuff, 21);
			blockColors[blockColorID(17, 3)] = getColor(terrainBuff, 21);
			blockColors[blockColorID(17, 4)] = getColor(terrainBuff, 20); // logs on side
			blockColors[blockColorID(17, 5)] = getColor(terrainBuff, 116);
			blockColors[blockColorID(17, 6)] = getColor(terrainBuff, 117);
			blockColors[blockColorID(17, 7)] = getColor(terrainBuff, 153);
			blockColors[blockColorID(17, 8)] = getColor(terrainBuff, 20); 
			blockColors[blockColorID(17, 9)] = getColor(terrainBuff, 116);
			blockColors[blockColorID(17, 10)] = getColor(terrainBuff, 117);
			blockColors[blockColorID(17, 11)] = getColor(terrainBuff, 153);
			blockColors[blockColorID(17, 12)] = getColor(terrainBuff, 20); // logs all bark?
			blockColors[blockColorID(17, 13)] = getColor(terrainBuff, 116);
			blockColors[blockColorID(17, 14)] = getColor(terrainBuff, 117);
			blockColors[blockColorID(17, 15)] = getColor(terrainBuff, 153);
			//blockColors[blockColorID(18, 0)] = getColor(terrainBuff, 53); // leaves
			//blockColors[blockColorID(18, 1)] = getColor(terrainBuff, 133);
			//blockColors[blockColorID(18, 2)] = getColor(terrainBuff, 53);
			//blockColors[blockColorID(18, 3)] = getColor(terrainBuff, 196);
			blockColors[blockColorID(18, 0)] = colorMultiplier(getColor(terrainBuff, 53), ColorizerFoliage.getFoliageColor(0.7,  0.7)) & 0x00FFFFFF;
			blockColors[blockColorID(18, 1)] = colorMultiplier(getColor(terrainBuff, 133), ColorizerFoliage.getFoliageColorPine()) & 0x00FFFFFF;
			blockColors[blockColorID(18, 2)] = colorMultiplier(getColor(terrainBuff, 53), ColorizerFoliage.getFoliageColorBirch()) & 0x00FFFFFF;
			blockColors[blockColorID(18, 3)] = colorMultiplier(getColor(terrainBuff, 196), ColorizerFoliage.getFoliageColorBasic()) & 0x00FFFFFF;
			blockColors[blockColorID(19, 0)] = getColor(terrainBuff, 48);
			blockColors[blockColorID(20, 0)] = getColor(terrainBuff, 49);
			blockColors[blockColorID(21, 0)] = getColor(terrainBuff, 160);
			blockColors[blockColorID(22, 0)] = getColor(terrainBuff, 144);
			blockColors[blockColorID(23, 0)] = getColor(terrainBuff, 62);
			blockColors[blockColorID(24, 0)] = getColor(terrainBuff, 176); // sandstone.  could differentiate
			blockColors[blockColorID(25, 0)] = getColor(terrainBuff, 74); // note block
			blockColors[blockColorID(30, 0)] = getColor(terrainBuffTrans, 11); // cobweb
			blockColors[blockColorID(35, 0)] = getColor(terrainBuff, 64);
			blockColors[blockColorID(35, 1)] = getColor(terrainBuff, 210);
			blockColors[blockColorID(35, 2)] = getColor(terrainBuff, 194);
			blockColors[blockColorID(35, 3)] = getColor(terrainBuff, 178);
			blockColors[blockColorID(35, 4)] = getColor(terrainBuff, 162);
			blockColors[blockColorID(35, 5)] = getColor(terrainBuff, 146);
			blockColors[blockColorID(35, 6)] = getColor(terrainBuff, 130);
			blockColors[blockColorID(35, 7)] = getColor(terrainBuff, 114);
			blockColors[blockColorID(35, 8)] = getColor(terrainBuff, 225);
			blockColors[blockColorID(35, 9)] = getColor(terrainBuff, 209);
			blockColors[blockColorID(35, 10)] = getColor(terrainBuff, 193);
			blockColors[blockColorID(35, 11)] = getColor(terrainBuff, 177);
			blockColors[blockColorID(35, 12)] = getColor(terrainBuff, 161);
			blockColors[blockColorID(35, 13)] = getColor(terrainBuff, 145);
			blockColors[blockColorID(35, 14)] = getColor(terrainBuff, 129);
			blockColors[blockColorID(35, 15)] = getColor(terrainBuff, 113);
			blockColors[blockColorID(37, 0)] = getColor(terrainBuff, 13); // dandelion
			blockColors[blockColorID(38, 0)] = getColor(terrainBuff, 12); // rose
			blockColors[blockColorID(39, 0)] = getColor(terrainBuff, 29); // brown shroom
			blockColors[blockColorID(40, 0)] = getColor(terrainBuff, 28); // red shroom
			blockColors[blockColorID(41, 0)] = getColor(terrainBuff, 23);
			blockColors[blockColorID(42, 0)] = getColor(terrainBuff, 22);
			blockColors[blockColorID(43, 0)] = getColor(terrainBuff, 6); // double slabs
			blockColors[blockColorID(43, 1)] = getColor(terrainBuff, 176); 
			blockColors[blockColorID(43, 2)] = getColor(terrainBuff, 4);
			blockColors[blockColorID(43, 3)] = getColor(terrainBuff, 16);
			blockColors[blockColorID(43, 4)] = getColor(terrainBuff, 7);
			blockColors[blockColorID(43, 5)] = getColor(terrainBuff, 54);
			blockColors[blockColorID(44, 0)] = getColor(terrainBuff, 6); // slabs
			blockColors[blockColorID(44, 1)] = getColor(terrainBuff, 176);
			blockColors[blockColorID(44, 2)] = getColor(terrainBuff, 4);
			blockColors[blockColorID(44, 3)] = getColor(terrainBuff, 16);
			blockColors[blockColorID(44, 4)] = getColor(terrainBuff, 7);
			blockColors[blockColorID(44, 5)] = getColor(terrainBuff, 54);
			blockColors[blockColorID(44, 8)] = getColor(terrainBuff, 6); // slabs upside down
			blockColors[blockColorID(44, 9)] = getColor(terrainBuff, 176);
			blockColors[blockColorID(44, 10)] = getColor(terrainBuff, 4);
			blockColors[blockColorID(44, 11)] = getColor(terrainBuff, 16);
			blockColors[blockColorID(44, 12)] = getColor(terrainBuff, 7);
			blockColors[blockColorID(44, 13)] = getColor(terrainBuff, 54);
			blockColors[blockColorID(45, 0)] = getColor(terrainBuff, 7);
			blockColors[blockColorID(46, 0)] = getColor(terrainBuff, 9); // tnt
			blockColors[blockColorID(47, 0)] = getColor(terrainBuff, 4); // bookshelf
			blockColors[blockColorID(48, 0)] = getColor(terrainBuff, 36);
			blockColors[blockColorID(49, 0)] = getColor(terrainBuff, 37);
			blockColors[blockColorID(50, 0)] = getColor(terrainBuff, 80); // torch
			blockColors[blockColorID(51, 0)] = getColor(terrainBuff, 237); // fire (using lava)
			blockColors[blockColorID(52, 0)] = getColor(terrainBuff, 65); // spawner
			blockColors[blockColorID(53, 0)] = getColor(terrainBuff, 4); // oak stairs
			blockColors[blockColorID(53, 1)] = getColor(terrainBuff, 4);
			blockColors[blockColorID(53, 2)] = getColor(terrainBuff, 4);
			blockColors[blockColorID(53, 3)] = getColor(terrainBuff, 4);
			blockColors[blockColorID(54, 0)] = getColor(terrainBuff, 58); // chest (as long as some of it is still in terrain.png)
			blockColors[blockColorID(55, 0)] = getColor(terrainBuff, 164); // redstone wire
			blockColors[blockColorID(56, 0)] = getColor(terrainBuff, 50);
			blockColors[blockColorID(57, 0)] = getColor(terrainBuff, 24);
			blockColors[blockColorID(58, 0)] = getColor(terrainBuff, 43); // crafting table
			blockColors[blockColorID(59, 0)] = getColor(terrainBuff, 95); // wheat seeds
			blockColors[blockColorID(60, 0)] = getColor(terrainBuff, 87); // farmland
			blockColors[blockColorID(60, 1)] = getColor(terrainBuff, 86); 
			blockColors[blockColorID(60, 2)] = getColor(terrainBuff, 86); 
			blockColors[blockColorID(60, 3)] = getColor(terrainBuff, 86); 
			blockColors[blockColorID(60, 4)] = getColor(terrainBuff, 86); 
			blockColors[blockColorID(60, 5)] = getColor(terrainBuff, 86); 
			blockColors[blockColorID(60, 6)] = getColor(terrainBuff, 86); 
			blockColors[blockColorID(60, 7)] = getColor(terrainBuff, 86); 
			blockColors[blockColorID(60, 8)] = getColor(terrainBuff, 86); 
			blockColors[blockColorID(61, 0)] = getColor(terrainBuff, 62); // furnace
			blockColors[blockColorID(62, 0)] = getColor(terrainBuff, 62); // burning furnace
			blockColors[blockColorID(63, 0)] = getColor(terrainBuff, 4); // sign
			blockColors[blockColorID(64, 0)] = getColor(terrainBuff, 97); // wood door
			blockColors[blockColorID(65, 0)] = getColor(terrainBuff, 83); // ladder
			blockColors[blockColorID(66, 0)] = getColor(terrainBuff, 28); // rails
			blockColors[blockColorID(67, 0)] = getColor(terrainBuff, 16); // cobble stairs
			blockColors[blockColorID(67, 1)] = getColor(terrainBuff, 16);
			blockColors[blockColorID(67, 2)] = getColor(terrainBuff, 16);
			blockColors[blockColorID(67, 3)] = getColor(terrainBuff, 16);
			blockColors[blockColorID(68, 0)] = getColor(terrainBuff, 4); // wall sign
			blockColors[blockColorID(69, 0)] = getColor(terrainBuff, 96); // lever
			blockColors[blockColorID(70, 0)] = getColor(terrainBuff, 1); // stone plate
			blockColors[blockColorID(71, 0)] = getColor(terrainBuff, 98); // iron door
			blockColors[blockColorID(72, 0)] = getColor(terrainBuff, 4); // wood plate
			blockColors[blockColorID(73, 0)] = getColor(terrainBuff, 51); // redstone ore
			blockColors[blockColorID(74, 0)] = getColor(terrainBuff, 51); // glowing redstone ore
			blockColors[blockColorID(75, 0)] = getColor(terrainBuff, 115); // redstone torch off
			blockColors[blockColorID(76, 0)] = getColor(terrainBuff, 99); // redstone torch on
			blockColors[blockColorID(77, 0)] = getColor(terrainBuff, 1); // button
			blockColors[blockColorID(78, 0)] = getColor(terrainBuff, 66); // snow
			blockColors[blockColorID(79, 0)] = getColor(terrainBuff, 67); // ice
			blockColors[blockColorID(80, 0)] = getColor(terrainBuff, 66); // snow block
			blockColors[blockColorID(81, 0)] = getColor(terrainBuff, 69); // cactus
			blockColors[blockColorID(82, 0)] = getColor(terrainBuff, 72); // clay
			blockColors[blockColorID(83, 0)] = getColor(terrainBuff, 73); // sugar cane
			blockColors[blockColorID(84, 0)] = getColor(terrainBuff, 75); // jukebox
			blockColors[blockColorID(85, 0)] = getColor(terrainBuff, 4); // fence
			blockColors[blockColorID(86, 0)] = getColor(terrainBuff, 102); // pumpkin
			blockColors[blockColorID(87, 0)] = getColor(terrainBuff, 103); // netherrack
			blockColors[blockColorID(88, 0)] = getColor(terrainBuff, 104); // soulsand
			blockColors[blockColorID(89, 0)] = getColor(terrainBuff, 105); // glowstone
			blockColors[blockColorID(90, 0)] = getColor(terrainBuff, 14); // portal
			blockColors[blockColorID(91, 0)] = getColor(terrainBuff, 102); // lit pumpkin
			blockColors[blockColorID(92, 0)] = getColor(terrainBuff, 121); // cake
			blockColors[blockColorID(93, 0)] = getColor(terrainBuff, 131); // repeater off
			blockColors[blockColorID(94, 0)] = getColor(terrainBuff, 147); // repeater on
			blockColors[blockColorID(95, 0)] = getColor(terrainBuff, 58); // locked chest
			blockColors[blockColorID(96, 0)] = getColor(terrainBuff, 84); // trapdoor
			blockColors[blockColorID(97, 0)] = getColor(terrainBuff, 1); // monster egg (stone, cobble, or stone brick)
			blockColors[blockColorID(98, 0)] = getColor(terrainBuff, 54); // stone brick
			blockColors[blockColorID(98, 1)] = getColor(terrainBuff, 100);
			blockColors[blockColorID(98, 2)] = getColor(terrainBuff, 101);
			blockColors[blockColorID(98, 3)] = getColor(terrainBuff, 213);
			blockColors[blockColorID(99, 0)] = getColor(terrainBuff, 142); // huge brown shroom pores
			blockColors[blockColorID(99, 1)] = getColor(terrainBuff, 126); // huge brown shroom cap
			blockColors[blockColorID(99, 2)] = getColor(terrainBuff, 126); // huge brown shroom cap
			blockColors[blockColorID(99, 3)] = getColor(terrainBuff, 126); // huge brown shroom cap
			blockColors[blockColorID(99, 4)] = getColor(terrainBuff, 126); // huge brown shroom cap
			blockColors[blockColorID(99, 5)] = getColor(terrainBuff, 126); // huge brown shroom cap
			blockColors[blockColorID(99, 6)] = getColor(terrainBuff, 126); // huge brown shroom cap
			blockColors[blockColorID(99, 7)] = getColor(terrainBuff, 126); // huge brown shroom cap
			blockColors[blockColorID(99, 8)] = getColor(terrainBuff, 126); // huge brown shroom cap
			blockColors[blockColorID(99, 9)] = getColor(terrainBuff, 126); // huge brown shroom cap
			blockColors[blockColorID(99, 10)] = getColor(terrainBuff, 142); // huge brown shroom stem, pores on top
			blockColors[blockColorID(99, 14)] = getColor(terrainBuff, 126); // huge brown shroom all cap
			blockColors[blockColorID(99, 15)] = getColor(terrainBuff, 141); // huge brown shroom all stem
			blockColors[blockColorID(100, 0)] = getColor(terrainBuff, 142); // huge red shroom pores
			blockColors[blockColorID(100, 1)] = getColor(terrainBuff, 125); // huge red shroom cap
			blockColors[blockColorID(100, 2)] = getColor(terrainBuff, 125); // huge red shroom cap
			blockColors[blockColorID(100, 3)] = getColor(terrainBuff, 125); // huge red shroom cap
			blockColors[blockColorID(100, 4)] = getColor(terrainBuff, 125); // huge red shroom cap
			blockColors[blockColorID(100, 5)] = getColor(terrainBuff, 125); // huge red shroom cap
			blockColors[blockColorID(100, 6)] = getColor(terrainBuff, 125); // huge red shroom cap
			blockColors[blockColorID(100, 7)] = getColor(terrainBuff, 125); // huge red shroom cap
			blockColors[blockColorID(100, 8)] = getColor(terrainBuff, 125); // huge red shroom cap
			blockColors[blockColorID(100, 9)] = getColor(terrainBuff, 125); // huge red shroom cap
			blockColors[blockColorID(100, 10)] = getColor(terrainBuff, 142); // huge red shroom stem, pores on top
			blockColors[blockColorID(100, 14)] = getColor(terrainBuff, 125); // huge brown shroom all cap
			blockColors[blockColorID(100, 15)] = getColor(terrainBuff, 141); // huge brown shroom all stem
			blockColors[blockColorID(101, 0)] = getColor(terrainBuff, 85); // iron bars
			blockColors[blockColorID(102, 0)] = getColor(terrainBuff, 49); // glass pane
			blockColors[blockColorID(103, 0)] = getColor(terrainBuff, 137); // melon
			blockColors[blockColorID(104, 0)] = getColor(terrainBuff, 127); // pumpkin stem
			blockColors[blockColorID(105, 0)] = getColor(terrainBuff, 127); // melon stem
			blockColors[blockColorID(106, 0)] = getColor(terrainBuff, 143); // vines
			blockColors[blockColorID(107, 0)] = getColor(terrainBuff, 4); // fence gate
			blockColors[blockColorID(108, 0)] = getColor(terrainBuff, 7); // brick stairs
			blockColors[blockColorID(108, 1)] = getColor(terrainBuff, 7); 
			blockColors[blockColorID(108, 2)] = getColor(terrainBuff, 7);
			blockColors[blockColorID(108, 3)] = getColor(terrainBuff, 7);
			blockColors[blockColorID(109, 0)] = getColor(terrainBuff, 54); // stone brick stairs
			blockColors[blockColorID(109, 1)] = getColor(terrainBuff, 54);
			blockColors[blockColorID(109, 2)] = getColor(terrainBuff, 54);
			blockColors[blockColorID(109, 3)] = getColor(terrainBuff, 54);
			blockColors[blockColorID(110, 0)] = getColor(terrainBuff, 78); // mycelium
			blockColors[blockColorID(112, 0)] = getColor(terrainBuff, 224); // netherbrick
			blockColors[blockColorID(114, 0)] = getColor(terrainBuff, 224); // netherbrick stairs
			blockColors[blockColorID(114, 1)] = getColor(terrainBuff, 224); // netherbrick stairs
			blockColors[blockColorID(114, 2)] = getColor(terrainBuff, 224); // netherbrick stairs
			blockColors[blockColorID(114, 3)] = getColor(terrainBuff, 224); // netherbrick stairs
			blockColors[blockColorID(121, 0)] = getColor(terrainBuff, 175); // endstone
			blockColors[blockColorID(123, 0)] = getColor(terrainBuff, 211); // inactive glowstone lamp
			blockColors[blockColorID(124, 0)] = getColor(terrainBuff, 212); // active glowstone lamp
			blockColors[blockColorID(125, 0)] = getColor(terrainBuff, 4); // wooden double slab
			blockColors[blockColorID(125, 1)] = getColor(terrainBuff, 198);  
			blockColors[blockColorID(125, 2)] = getColor(terrainBuff, 214); 
			blockColors[blockColorID(125, 3)] = getColor(terrainBuff, 199); 
			blockColors[blockColorID(126, 0)] = getColor(terrainBuff, 4); // wooden slab
			blockColors[blockColorID(126, 1)] = getColor(terrainBuff, 198);  
			blockColors[blockColorID(126, 2)] = getColor(terrainBuff, 214); 
			blockColors[blockColorID(126, 3)] = getColor(terrainBuff, 199);
			blockColors[blockColorID(126, 8)] = getColor(terrainBuff, 4); // wooden slab upside down
			blockColors[blockColorID(126, 9)] = getColor(terrainBuff, 198);  
			blockColors[blockColorID(126, 10)] = getColor(terrainBuff, 214); 
			blockColors[blockColorID(126, 11)] = getColor(terrainBuff, 199);
			blockColors[blockColorID(127, 0)] = getColor(terrainBuff, 168);  // cocoa plant
			blockColors[blockColorID(128, 0)] = getColor(terrainBuff, 176);  // sandstone stairs
			blockColors[blockColorID(128, 1)] = getColor(terrainBuff, 176);
			blockColors[blockColorID(128, 2)] = getColor(terrainBuff, 176);
			blockColors[blockColorID(128, 3)] = getColor(terrainBuff, 176);
			blockColors[blockColorID(129, 0)] = getColor(terrainBuff, 171); // emerald ore
			blockColors[blockColorID(130, 0)] = getColor(terrainBuff, 182); // ender chest (using side of enchanting table)
			blockColors[blockColorID(131, 0)] = getColor(terrainBuff, 172); // tripwire hook
			blockColors[blockColorID(132, 0)] = getColor(terrainBuff, 173); // tripwire
			blockColors[blockColorID(133, 0)] = getColor(terrainBuff, 25); // emerald block
			blockColors[blockColorID(134, 0)] = getColor(terrainBuff, 198);  // spruce stairs
			blockColors[blockColorID(134, 1)] = getColor(terrainBuff, 198);
			blockColors[blockColorID(134, 2)] = getColor(terrainBuff, 198);
			blockColors[blockColorID(134, 3)] = getColor(terrainBuff, 198);
			blockColors[blockColorID(135, 0)] = getColor(terrainBuff, 214);  // birch stairs
			blockColors[blockColorID(135, 1)] = getColor(terrainBuff, 214);
			blockColors[blockColorID(135, 2)] = getColor(terrainBuff, 214);
			blockColors[blockColorID(135, 3)] = getColor(terrainBuff, 214);
			blockColors[blockColorID(136, 0)] = getColor(terrainBuff, 199);  // jungle stairs
			blockColors[blockColorID(136, 1)] = getColor(terrainBuff, 199);
			blockColors[blockColorID(136, 2)] = getColor(terrainBuff, 199);
			blockColors[blockColorID(136, 3)] = getColor(terrainBuff, 199);

		    this.timer = 300; // force rerender with texture pack colors now that they are loaded
		}
		catch (Exception e) {
			System.out.println("ERRRORRR " + e.getLocalizedMessage());
		}
		
	}
	
	private int getColor(BufferedImage image, int textureID) {
//		int texX = (textureID & 15) << 4; // 0 based horizontal offset in pixels from left of terrain.png (assumes each block is 16px)
//		int texY = textureID & 240; // 0 based vertical offset in pixels from top of terrain.png (assumes each block is 16px)
		int texX = textureID & 15; // 0 based column in terrain.png 
		int texY = (textureID & 240) >> 4; // 0 based row in terrain.png
//		System.out.println("int: " + image.getRGB(texX, texY));
//		System.out.println("as hex: " +  java.lang.Integer.toHexString(image.getRGB(texX, texY)));
//		System.out.println("22 int: " + (image.getRGB(texX, texY) & 0x00FFFFFF));
//		System.out.println("22 hex: " +  java.lang.Integer.toHexString(image.getRGB(texX, texY) & 0x00FFFFFF));
		
		return (image.getRGB(texX, texY) & 0x00FFFFFF); // the and dumps the alpha, in hex the ff at the beginning
	}
	
    private int colorMultiplier(int color1, int color2)
    {
        int red1 = (color1 >> 16 & 255);
        int green1 = (color1 >> 8 & 255);
        int blue1 = (color1 >> 0 & 255);
       
        int red2 = (color2 >> 16 & 255);
        int green2 = (color2 >> 8 & 255);
        int blue2 = (color2 >> 0 & 255);
        
        int red = red1 * red2 / 256;
        int green = green1 * green2 / 256;
        int blue = blue1 * blue2 / 256;
        
        
        return (red & 255) << 16 | (green & 255) << 8 | blue & 255;
       // this.red = (float)(var1 >> 16 & 255) * 0.003921569F * var2;
       // this.green = (float)(var1 >> 8 & 255) * 0.003921569F * var2;
       // this.blue = (float)(var1 >> 0 & 255) * 0.003921569F * var2;

    }
    
    private void getWaterColor(BufferedImage terrainBuff) {
    	try {
    		int waterRGB = -1;
    		int waterBase = -1;
    		InputStream is = pack.getResourceAsStream("/anim/custom_water_still.png");
    		if (is == null) { 
    			is = pack.getResourceAsStream("/custom_water_still.png");
    		}
    		if (is == null) {
    			waterBase = getColor(terrainBuff, 205);
    		}
    		else {
    			java.awt.Image water = ImageIO.read(is);
    			is.close();
    			water = water.getScaledInstance(1,1, java.awt.Image.SCALE_SMOOTH);
    			BufferedImage waterBuff = new BufferedImage(water.getWidth(null), water.getHeight(null), BufferedImage.TYPE_INT_RGB);
    			java.awt.Graphics gfx = waterBuff.createGraphics();
    			// Paint the image onto the buffered image
    			gfx.drawImage(water, 0, 0, null);
    			gfx.dispose();
    			waterBase = waterBuff.getRGB(0, 0) & 0x00FFFFFF;
    		}
    		int waterMult = -1;
    		is = pack.getResourceAsStream("/misc/watercolorX.png");
    		if (is != null) {
    			java.awt.Image waterColor = ImageIO.read(is);
    			is.close();
    			BufferedImage waterColorBuff = new BufferedImage(waterColor.getWidth(null), waterColor.getHeight(null), BufferedImage.TYPE_INT_RGB);
    			java.awt.Graphics gfx = waterColorBuff.createGraphics();
    			// Paint the image onto the buffered image
    			gfx.drawImage(waterColor, 0, 0, null);
    			gfx.dispose();
    			waterMult = waterColorBuff.getRGB(waterColorBuff.getWidth()*76/256, waterColorBuff.getHeight()*112/256) & 0x00FFFFFF;
    		}
    		if (waterMult != -1) 
    			waterRGB = this.colorMultiplier(waterBase, waterMult);
    		else
    			waterRGB = waterBase;
    		blockColors[blockColorID(8, 0)] = waterRGB;
    		blockColors[blockColorID(9, 0)] = waterRGB;
    	} 
    	catch (Exception e) {
			chatInfo("�EError Loading Water Color, using defaults");
			blockColors[blockColorID(8, 0)] = 0x2f51ff;
			blockColors[blockColorID(9, 0)] = 0x2f51ff;
    	}
    }
    
    private void getLavaColor(BufferedImage terrainBuff) {
    	try {
    		int lavaRGB = -1;
    		InputStream is = pack.getResourceAsStream("/anim/custom_lava_still.png");
    		if (is == null) { 
    			is = pack.getResourceAsStream("/custom_lava_still.png");
    		}
    		if (is == null) {
    			lavaRGB = getColor(terrainBuff, 237);
    		}
    		else {
    			java.awt.Image lava = ImageIO.read(is);
    			is.close();
    			lava = lava.getScaledInstance(1,1, java.awt.Image.SCALE_SMOOTH);
    			BufferedImage lavaBuff = new BufferedImage(lava.getWidth(null), lava.getHeight(null), BufferedImage.TYPE_INT_RGB);
    			java.awt.Graphics gfx = lavaBuff.createGraphics();
    			// Paint the image onto the buffered image
    			gfx.drawImage(lava, 0, 0, null);
    			gfx.dispose();
    			lavaRGB = lavaBuff.getRGB(0, 0) & 0x00FFFFFF;
    		}
    		blockColors[blockColorID(10, 0)] = lavaRGB;
    		blockColors[blockColorID(11, 0)] = lavaRGB;
    	} 
    	catch (Exception e) {
			chatInfo("�EError Loading Water Color, using defaults");
			blockColors[blockColorID(8, 0)] = 0xecad41;
			blockColors[blockColorID(9, 0)] = 0xecad41;
    	}
    }
	
	
	private void renderMap (int scWidth, int scScale) {
		if (this.game.thePlayer.username.equals("jacoboom100")) {
			this.error = "no map for you";
			return;
		}
		if (!this.hide && !this.full) {
			if (this.q != 0) glah(this.q);
			boolean scaleChanged = (this.scScale != scScale);
			this.scScale = scScale;

			if (squareMap) { // square map
				if (this.zoom == 3) {
					GL11.glPushMatrix();
					GL11.glScalef(0.5f, 0.5f, 1.0f);
					this.q = this.tex(this.map[this.zoom]);
					GL11.glPopMatrix();
				} else this.q = this.tex(this.map[this.zoom]);
				// from here
				GL11.glPushMatrix();
				GL11.glTranslatef(scWidth - 32.0F, 37.0F, 0.0F);
				GL11.glRotatef(90.0F - northRotate, 0.0F, 0.0F, 1.0F); // +90 west at top.  +0 north at top
				GL11.glTranslatef(-(scWidth - 32.0F), -37.0F, 0.0F);
				// to here + the popmatrix below only necessary with variable north, and no if/else statements in mapcalc
				drawPre();
				this.setMap(scWidth);
				drawPost();

				GL11.glPopMatrix();

				try {
					this.disp(this.img("/mamiyaotaru/minimap.png"));
					drawPre();
					this.setMap(scWidth);
					drawPost();
				} catch (Exception localException) {
					this.error = "error: minimap overlay not found!";
				}
				try {
					GL11.glPushMatrix();
					this.disp(scScale>=3?this.img("/mamiyaotaru/mmarrow2x.png"):this.img("/mamiyaotaru/mmarrow.png"));
					GL11.glTranslatef(scWidth - 32.0F, 37.0F, 0.0F); 
					GL11.glRotatef(-this.direction -90.0F - northRotate, 0.0F, 0.0F, 1.0F); // -dir-90 W top, -dir-180 N top
					GL11.glTranslatef(-(scWidth - 32.0F), -37.0F, 0.0F);
					drawPre();
					this.setMap(scWidth, 16);
					drawPost();
				} catch (Exception localException) {
					this.error = "Error: minimap arrow not found!";
				} finally {
					GL11.glPopMatrix();
				}

				for(Waypoint pt:wayPts) {
					if(pt.enabled) {
						double wayX = 0;
						double wayY = 0;
						if (this.game.thePlayer.dimension!=-1) {
							wayX = this.xCoord() - pt.x;
							wayY = this.zCoord() - pt.z;
						}
						else {
							wayX = this.xCoord() - (pt.x / 8);
							wayY = this.zCoord() - (pt.z / 8);
						}
						if (Math.abs(wayX)/(Math.pow(2,this.zoom)/2) > 31 || Math.abs(wayY)/(Math.pow(2,this.zoom)/2) > 31) {
							float locate = (float)Math.toDegrees(Math.atan2(wayX, wayY));
							double hypot = Math.sqrt((wayX*wayX)+(wayY*wayY));
							hypot = hypot / Math.max(Math.abs(wayX), Math.abs(wayY)) * 34;
							try {
								GL11.glPushMatrix();
								GL11.glColor3f(pt.red, pt.green, pt.blue);
								//this.disp(this.img("/marker.png"));
								this.disp(scScale>=3?this.img("/mamiyaotaru/marker" + pt.imageSuffix + "2x.png"):this.img("/mamiyaotaru/marker" + pt.imageSuffix + ".png"));
								GL11.glTranslatef(scWidth - 32.0F, 37.0F, 0.0F);
								GL11.glRotatef(-locate + 90 - northRotate, 0.0F, 0.0F, 1.0F); // +90 w top, 0 N top
								GL11.glTranslatef(-(scWidth - 32.0F), -37.0F, 0.0F);
								GL11.glTranslated(0.0D,/*-34.0D*/-hypot,0.0D); // hypotenuse is variable.  34 incorporated hypot's calculation above
								drawPre();
								this.setMap(scWidth, 16);
								drawPost();
							} catch (Exception localException) {
								this.error = "Error: marker overlay not found!";
							} finally {
								GL11.glPopMatrix();
							}
						} // end if waypoint is far away and drawn as an arrow on the edge
						else { // else waypoint is close enough to be on the map
							float locate = (float)Math.toDegrees(Math.atan2(wayX, wayY));
							double hypot = Math.sqrt((wayX*wayX)+(wayY*wayY))/(Math.pow(2,this.zoom)/2);
							try 
							{
								GL11.glPushMatrix();
								GL11.glColor3f(pt.red, pt.green, pt.blue);
								//this.disp(this.img("/waypoint.png"));
								this.disp(scScale>=3?this.img("/mamiyaotaru/waypoint" + pt.imageSuffix + "2x.png"):this.img("/mamiyaotaru/waypoint" + pt.imageSuffix + ".png"));
							//	GL11.glTranslated(-wayX/(Math.pow(2,this.zoom)/2),-wayY/(Math.pow(2,this.zoom)/2),0.0D); //y -x W at top, -x -y N at top
								// from here
								GL11.glTranslatef(scWidth - 32.0F, 37.0F, 0.0F);
								GL11.glRotatef(-locate + 90.0F - northRotate, 0.0F, 0.0F, 1.0F); // + 90 w top, 0 n top
								GL11.glTranslated(0.0D,-hypot,0.0D);
								GL11.glRotatef(-(-locate + 90.0F - northRotate), 0.0F, 0.0F, 1.0F); // + 90 w top, 0 n top
								GL11.glTranslated(0.0D,hypot,0.0D);
								GL11.glTranslatef(-(scWidth - 32.0F), -37.0F, 0.0F);
								GL11.glTranslated(0.0D,-hypot,0.0D);
								// to here only necessary with variable north, and no if/else statements in mapcalc.  otherwise uncomment the translated above this block
								drawPre();
								this.setMap(scWidth, 16);
								drawPost();
							} catch (Exception localException) 
							{
								this.error = "Error: waypoint overlay not found!";
							} finally 
							{
								GL11.glPopMatrix();
							}
						} // end waypoint is on current map
					} // end if pt enabled
				} // end for waypoints
			} // end if squaremap
			else { // else roundmap
			//	final long startTime = System.nanoTime();

/*				
				// do with opengl.  Faster.  Uses alpha channel, have to set lighting in actual RGB instead of alpha.
				// note, this fails on nVidia unless display is created with alpha bits  Display.create((new PixelFormat()).withDepthBits(24).withAlphaBits(8));
				// have to do that in minecraft.java, don't want to alter that.  So no destination alpha exists or can be used on nVidia cards :(
				GL11.glColorMask(false,false,false,true); // draw to alpha (from circle.png) - used to make square map round with GL
				if (this.game.gameSettings.showDebugInfo) { // only do f3 fix (that makes map invisible) if f3 text is up.  Still issues if f3 AND chat, oh well
					// clear alpha before drawing circle to it to get rid of the alpha the f3 text put in
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
					GL11.glBlendFunc(GL11.GL_ZERO, GL11.GL_ZERO);
					GL11.glColor3f(0, 0, 255);
					//Begin drawing the square with the assigned coordinates and size
					GL11.glBegin(GL11.GL_QUADS); 
					GL11.glVertex2f(scWidth-80, 80);//bottom left of the square
					GL11.glVertex2f(scWidth+5, 840);//bottom right of the square
					GL11.glVertex2f(scWidth+5, 0);//top right of the square
					GL11.glVertex2f(scWidth-80, 0);//top left of the square
					GL11.glEnd();
					GL11.glColor4f(1, 1, 1, 1);
					
					//basically the same, but slower?  have to check
					//GL11.glClearColor(0, 0, 0, 0);
					//GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
				}
							
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				this.disp(scScale>=3?this.img("/mamiyaotaru/circle2x.png"):this.img("/mamiyaotaru/circle.png")); // does weird things to f3 text.  deal with it!  Also fux dynamic lighting.  Deal with it :(  (can do if we don't usee alpha channel for lighting info, just darken actual RGB
				drawPre();
				this.setMap(scWidth);
				drawPost();
				
				
				GL11.glColorMask(true,true,true,true);
				GL11.glBlendFunc(GL11.GL_DST_ALPHA, GL11.GL_ONE_MINUS_DST_ALPHA); // pasted on image uses alpha of BG - can stencil it out, but images own alpha goes away
*/				
				
				// this chunk used for 2d and stenciling, not FBO
/*			// 	GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ZERO); // used for light info stored in alpha channel          
				if (this.zoom == 3) {
					GL11.glPushMatrix();
					GL11.glScalef(0.5f, 0.5f, 1.0f);
					this.q = this.tex(this.map[this.lZoom]);
					GL11.glPopMatrix();
				} else this.q = this.tex(this.map[this.lZoom]);
				
				// This is for rotating the whole map image.  Was fine when transparency stencil was drawn, then map and we rotated the map
				// now we are drawing FBO, that has the transparency baked in.  Can't rotate that.  Instead rotate the map in the FBO.  Note the differences (in rotation, translation, and normal locations) due to Minecraft doing it backwards
				GL11.glPushMatrix();
		    	GL11.glTranslatef(scWidth - 32.0F, 37.0F, 0.0F); 
				GL11.glRotatef(this.direction + 180.0F, 0.0F, 0.0F, 1.0F); 
				GL11.glTranslatef(-(scWidth - 32.0F), -37.0F, 0.0F);

				if(this.zoom==0) 
					GL11.glTranslatef(-1.1f, -0.8f, 0.0f);
				else 
					GL11.glTranslatef(-0.5f, -0.5f, 0.0f);
*/
				
				if (fboEnabled) {
					// FBO render pass
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0); // unlink textures because if we dont it all is gonna fail

					GL11.glPushAttrib(GL11.GL_VIEWPORT_BIT | GL11.GL_TRANSFORM_BIT | GL11.GL_COLOR_BUFFER_BIT);
					GL11.glViewport (0, 0, 256, 256); // set The Current Viewport to the fbo size
					GL11.glMatrixMode(GL11.GL_PROJECTION);
					GL11.glPushMatrix();
					GL11.glLoadIdentity();
					GL11.glOrtho(0.0, 256.0, 256.0, 0.0, 1000.0, 3000.0);
					GL11.glMatrixMode(GL11.GL_MODELVIEW);
					GL11.glPushMatrix();
					// Reset The Modelview Matrix
					GL11.glLoadIdentity ();  
					GL11.glTranslatef(0.0F, 0.0F, -2000.0F);

					EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fboID); // draw to the FBP

					// Clear Screen And Depth Buffer on the fbo to black.  We draw same circle each time, so only need to do it on scale change (when we start drawing a new circle)
					if (scaleChanged) {
						GL11.glClearColor (0.0f, 0.0f, 0.0f, 0.0f);
						GL11.glClear (GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);            
					}
					GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);  
					this.disp(scScale>=3?this.img("/mamiyaotaru/circle2x.png"):this.img("/mamiyaotaru/circle.png")); 
					drawPre();
					ldrawthree(0, 256, 1.0D, 0.0D, 0.0D);
					ldrawthree(256, 256, 1.0D, 1.0D, 0.0D);
					ldrawthree(256, 0, 1.0D, 1.0D, 1.0D);
					ldrawthree(0, 0, 1.0D, 0.0D, 1.0D);
					drawPost();

					GL14.glBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_DST_COLOR, GL11.GL_ZERO); // source image's alpha is based on the color of the destination.  Don't need DST_ALPHA (thanks nvidia)         
					if (this.zoom == 3) {
						GL11.glPushMatrix();
						GL11.glScalef(0.5f, 0.5f, 1.0f);
						this.q = this.tex(this.map[this.lZoom]);
						GL11.glPopMatrix();
					} else this.q = this.tex(this.map[this.lZoom]);
					GL11.glTranslatef(128, 128, 0.0F); 
					GL11.glRotatef(-this.direction + 180.0F, 0.0F, 0.0F, 1.0F); 
					GL11.glTranslatef(-(128), -128F, 0.0F);
					if(this.zoom==0) 
						GL11.glTranslatef(-2.2f, 1.6f, 0.0f);
					else 
						GL11.glTranslatef(-1.0f, 1.0f, 0.0f);
					drawPre();
					// position of the texture to put on the vertexes is flipped in Y (ie last number is 1 instead of 0 and vice versa) because minecraft has things upside down in the ortho.  Upside down FBO makes it upside down twice, akaa out of whack with minecraft 
					ldrawthree(0, 256, 1.0D, 0.0D, 0.0D);
					ldrawthree(256, 256, 1.0D, 1.0D, 0.0D);
					ldrawthree(256, 0, 1.0D, 1.0D, 1.0D);  
					ldrawthree(0, 0, 1.0D, 0.0D, 1.0D);
					drawPost();


					EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0); // stop drawing to FBO

					// restore viewport settings
					GL11.glMatrixMode(GL11.GL_PROJECTION);
					GL11.glPopMatrix();
					GL11.glMatrixMode(GL11.GL_MODELVIEW);
					GL11.glPopMatrix();
					GL11.glPopAttrib();		        


					GL11.glPushMatrix();
					GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
					this.disp(fboTextureID);
				}
				else { // do in 2d

					// have to convert square image into a circle here now that we don't redraw the image from scratch every frame.  Can't just draw it round from the start, makes it impossible to know which ones are new
					// make into a circle with java2d.  Must do if we store lighting in alpha channel.  slightly slower
					int diameter = this.map[this.lZoom].getWidth();
					BufferedImage roundImage = new BufferedImage(diameter, diameter,this.map[this.lZoom].getType());
					java.awt.geom.Ellipse2D.Double ellipse = new java.awt.geom.Ellipse2D.Double((this.lZoom*10/6),(this.lZoom*10/6),diameter-(this.lZoom*2),diameter-(this.lZoom*2));
					//java.awt.geom.Ellipse2D.Double ellipse = new java.awt.geom.Ellipse2D.Double(0,0,diameter,diameter);
					java.awt.Graphics2D gfx = roundImage.createGraphics();
					gfx.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
					gfx.setClip(ellipse);
					//gfx.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
					//gfx.drawImage(this.map[this.zoom],0,0,diameter,diameter,null); // just draw it enlarged instead of creating a new scaled instance every time
					// why enlarge it here at all?  opengl should do it faster.. but it will enlarge (in the case of the smaller ones) the massively aliased clipped one, with huge multi pixel jaggies around the edge.  Enlarge here into a larger (less aliased) clip for smoother edges even while the "pixels" are large
					// deal with it, it is faster
					//gfx.drawImage((this.map[this.zoom]).getScaledInstance(diameter, diameter, BufferedImage.SCALE_REPLICATE),0,0,null);
					gfx.drawImage(this.map[this.zoom],0,0,null); //(let opengl do it)
					gfx.dispose();
					
								// 	GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ZERO); // used for light info stored in alpha channel
					GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); // pasted on image uses alpha of BG - can stencil it out, but images own alpha goes away
					if (this.zoom == 3) {
						GL11.glPushMatrix();
						GL11.glScalef(0.5f, 0.5f, 1.0f);
						this.q = this.tex(roundImage);
						GL11.glPopMatrix();
					} else this.q = this.tex(roundImage);
					
					// This is for rotating the whole map image.  Was fine when transparency stencil was drawn, then map and we rotated the map
					// now we are drawing FBO, that has the transparency baked in.  Can't rotate that.  Instead rotate the map in the FBO.  Note the differences (in rotation, translation, and normal locations) due to Minecraft doing it backwards
					GL11.glPushMatrix();
			    	GL11.glTranslatef(scWidth - 32.0F, 37.0F, 0.0F); 
					GL11.glRotatef(this.direction + 180.0F, 0.0F, 0.0F, 1.0F); 
					GL11.glTranslatef(-(scWidth - 32.0F), -37.0F, 0.0F);

					if(this.zoom==0) 
						GL11.glTranslatef(-1.1f, -0.8f, 0.0f);
					else 
						GL11.glTranslatef(-0.5f, -0.5f, 0.0f);
				}
				

		//		System.out.println("time: " + (System.nanoTime()-startTime));
				drawPre();
				this.setMap(scWidth);
				drawPost();
				GL11.glPopMatrix();
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				//GL11.glDisable(GL11.GL_BLEND);                         // Disable Blending
				//GL11.glEnable(GL11.GL_DEPTH_TEST); 

				GL11.glColor3f(1.0F, 1.0F, 1.0F);
				this.drawRound(scWidth, scScale);
				this.drawDirections(scWidth);
				
				for(Waypoint pt:wayPts) {
					if(pt.enabled) {
						int wayX = 0;
						int wayY = 0;
						if (this.game.thePlayer.dimension!=-1) {
							wayX = this.xCoord() - pt.x;
							wayY = this.zCoord() - pt.z;
						}
						else {
							wayX = this.xCoord() - (pt.x / 8);
							wayY = this.zCoord() - (pt.z / 8);
						}
						float locate = (float)Math.toDegrees(Math.atan2(wayX, wayY));
						double hypot = Math.sqrt((wayX*wayX)+(wayY*wayY))/(Math.pow(2,this.zoom)/2);

						if (hypot >= 31.0D) {
							try {
								GL11.glPushMatrix();
								GL11.glColor3f(pt.red, pt.green, pt.blue);
								//this.disp(this.img("/marker.png"));
								this.disp(scScale>=3?this.img("/mamiyaotaru/marker" + pt.imageSuffix + "2x.png"):this.img("/mamiyaotaru/marker" + pt.imageSuffix + ".png"));
								GL11.glTranslatef(scWidth - 32.0F, 37.0F, 0.0F);
								GL11.glRotatef(-locate + this.direction + 180.0F, 0.0F, 0.0F, 1.0F);
								GL11.glTranslatef(-(scWidth - 32.0F), -37.0F, 0.0F);
								GL11.glTranslated(0.0D,-34.0D,0.0D);
								drawPre();
								this.setMap(scWidth, 16);
								drawPost();
							} catch (Exception localException) {
								this.error = "Error: marker overlay not found!";
							} finally {
								GL11.glPopMatrix();
							}
						}
						else {
							try 
							{
								GL11.glPushMatrix();
								GL11.glColor3f(pt.red, pt.green, pt.blue);
								//this.disp(this.img("/waypoint.png"));
								this.disp(scScale>=3?this.img("/mamiyaotaru/waypoint" + pt.imageSuffix + "2x.png"):this.img("/mamiyaotaru/waypoint" + pt.imageSuffix + ".png"));
								GL11.glTranslatef(scWidth - 32.0F, 37.0F, 0.0F);
								GL11.glRotatef(-locate + this.direction + 180.0F, 0.0F, 0.0F, 1.0F);
								GL11.glTranslated(0.0D,-hypot,0.0D);
								GL11.glRotatef(-(-locate + this.direction + 180.0F), 0.0F, 0.0F, 1.0F);
								GL11.glTranslated(0.0D,hypot,0.0D);
								GL11.glTranslatef(-(scWidth - 32.0F), -37.0F, 0.0F);
								GL11.glTranslated(0.0D,-hypot,0.0D);
								drawPre();
								this.setMap(scWidth, 16);
								drawPost();
							} catch (Exception localException) 
							{
								this.error = "Error: waypoint overlay not found!";
							} finally 
							{
								GL11.glPopMatrix();
							}
						}
					}
				}
			} // end roundmap
		}
	}

	private void renderMapFull (int scWidth, int scHeight) {
		if (this.game.thePlayer.username.equals("jacoboom100")) {
			this.error = "no map for you";
			return;
		}
		if (this.q != 0) glah(this.q);
		this.q = this.tex(this.map[this.zoom]);

		// from here

		GL11.glPushMatrix();
		//GL11.glTranslatef(0f, 0f, -2f);
		GL11.glTranslatef((scWidth + 5) / 2.0F, ((scHeight + 5) / 2.0F), 0.0F);
		GL11.glRotatef(90.0F - northRotate, 0.0F, 0.0F, 1.0F); // +90 west at top.  +0 north at top
		GL11.glTranslatef(-((scWidth + 5) / 2.0F), -((scHeight + 5) / 2.0F), 0.0F);
		// to here + the popmatrix below only necessary with variable north, and no if/else statements in mapcalc
		drawPre();
		ldrawone((scWidth+5)/2-128, (scHeight+5)/2+128, 1.0D, 0.0D, 1.0D);
		ldrawone((scWidth+5)/2+128, (scHeight+5)/2+128, 1.0D, 1.0D, 1.0D);
		ldrawone((scWidth+5)/2+128, (scHeight+5)/2-128, 1.0D, 1.0D, 0.0D);
		ldrawone((scWidth+5)/2-128, (scHeight+5)/2-128, 1.0D, 0.0D, 0.0D);
		drawPost();
		GL11.glPopMatrix();

		try {
			GL11.glPushMatrix();
			this.disp(this.img("/mamiyaotaru/mmarrow2x.png"));
		//	GL11.glTranslatef(0f, 0f, -2f);
			GL11.glTranslatef((scWidth+5)/2, (scHeight+5)/2, 0.0F);
			GL11.glRotatef(-this.direction - 90.0F - northRotate, 0.0F, 0.0F, 1.0F); // -dir-90 W top, -dir-180 N top
			GL11.glTranslatef(-((scWidth+5)/2), -((scHeight+5)/2), 0.0F);
			drawPre();
			//ldrawone((scWidth+5)/2-32, (scHeight+5)/2+32, 1.0D, 0.0D, 1.0D); // the old, 128-ified arrow
			ldrawone((scWidth+5)/2-4, (scHeight+5)/2+4, 1.0D, 0.0D, 1.0D);
			ldrawone((scWidth+5)/2+4, (scHeight+5)/2+4, 1.0D, 1.0D, 1.0D);
			ldrawone((scWidth+5)/2+4, (scHeight+5)/2-4, 1.0D, 1.0D, 0.0D);
			ldrawone((scWidth+5)/2-4, (scHeight+5)/2-4, 1.0D, 0.0D, 0.0D);
			drawPost();
		} catch (Exception localException) {
			this.error = "Error: minimap arrow not found!";
		} finally {
			GL11.glPopMatrix();
		}
	}
	
	private void setupFBO () {
		 fboID = EXTFramebufferObject.glGenFramebuffersEXT(); // create a new framebuffer
	     fboTextureID = GL11.glGenTextures(); // and a new texture used as a color buffer
	     int width = 256;
	     int height = 256;
	     EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fboID); // switch to the new framebuffer
	     ByteBuffer byteBuffer = BufferUtils.createByteBuffer(4 * width * height);

	     GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboTextureID); // Bind the colorbuffer texture
	    // GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
	    // GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
	     GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
	     GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
	     GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_BYTE, byteBuffer); // Create the texture data

	     EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, GL11.GL_TEXTURE_2D, fboTextureID, 0);  // attach it to the framebuffer
	     
	     EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0); // Swithch back to normal framebuffer rendering
	}

	private void showMenu (int scWidth, int scHeight) { 
		//System.out.println("menu: " + this.iMenu);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		int height;
		int maxSize = 0;
		int border = 2;
		boolean set = false;
		boolean click = false;
		int MouseX = getMouseX(scWidth);
		int MouseY = getMouseY(scHeight);

		if (Mouse.getEventButtonState() && Mouse.getEventButton() == 0)
			if (!this.lfclick) {
				set = true;
				this.lfclick = true;
			} else click = true;
		else if (this.lfclick) this.lfclick = false;

		String head = "Waypoints";
		String opt1 = "Exit Menu";
		String opt2 = "Waypoints";
		String opt3 = "Remove";

		if(this.iMenu==1) { // intro message
			head = this.sMenu[0];

			for(height=1; height < sMenu.length - 1; height++)
				if (this.chkLen(sMenu[height])>maxSize) maxSize = this.chkLen(sMenu[height]);
		} else { // waypoints
			opt1 = /*"Back"*/ StringTranslate.getInstance().translateKey("gui.done");

			if (this.iMenu==4) opt2 = "Cancel";
			else opt2 = "Add";

			maxSize = 80;

			for(int i = 0; i<wayPts.size(); i++)
				if(chkLen((i+1) + ") " + wayPts.get(i).name)>maxSize)
					maxSize = chkLen((i+1) + ") " + wayPts.get(i).name) + 32;

			height = 10; 
		}

		int title = this.chkLen(head);
		int centerX = (int)((scWidth+5)/2.0D);
		int centerY = (int)((scHeight+5)/2.0D);
		String hide = sMenu[sMenu.length-1];
		int footer = this.chkLen(hide);
		GL11.glDisable(3553); //GL_TEXTURE_2D
		GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.7f);
		double leftX = centerX - title/2.0D - border;
		double rightX = centerX + title/2.0D + border;
		double topY = centerY - (height-1)/2.0D*10.0D - border - 20.0D;
		double botY = centerY - (height-1)/2.0D*10.0D + border - 10.0D;
		this.drawBox(leftX, rightX, topY, botY);

		if(this.iMenu==1) { // intro menu
			leftX = centerX - maxSize/2.0D - border;
			rightX = centerX + maxSize/2.0D + border;
			topY = centerY - (height-1)/2.0D*10.0D - border;
			botY = centerY + (height-1)/2.0D*10.0D + border;
			this.drawBox(leftX, rightX, topY, botY);
			leftX = centerX - footer/2.0D - border;
			rightX = centerX + footer/2.0D + border;
			topY = centerY + (height-1)/2.0D*10.0D - border + 10.0D;
			botY = centerY + (height-1)/2.0D*10.0D + border + 20.0D;
			this.drawBox(leftX, rightX, topY, botY);
		}  else { // waypoints
			leftX = centerX - maxSize/2.0D - 25 - border;
			rightX = centerX + maxSize/2.0D + 25 + border;
			topY = centerY - (height-1)/2.0D*10.0D - border;
			botY = centerY + (height-1)/2.0D*10.0D + border;
			this.drawBox(leftX, rightX, topY, botY);
			this.drawOptions(rightX-border, topY+border, MouseX, MouseY, set, click);
			footer = this.drawFooter(centerX, centerY, height, opt1, opt2, opt3, border, MouseX, MouseY, set, click);
		}

		GL11.glEnable(3553); //GL_TEXTURE_2D
		this.write(head, centerX - title/2, (centerY - (height-1)*10/2) - 19, 0xffffff);

		if(this.iMenu==1) {
			for(int n=1; n<height; n++)
				this.write(this.sMenu[n], centerX - maxSize/2, ((centerY - (height-1)*10/2) + (n * 10))-9, 0xffffff);

			this.write(hide, centerX - footer/2, ((scHeight+5)/2 + (height-1)*10/2 + 11), 0xffffff);
		} else { // waypoints

			int max = min+9;

			if(max>wayPts.size()) {
				max = wayPts.size();

				if(min>=0) {
					if(max-9>0)
						min = max-9;
					else
						min = 0;
				}
			}

			for(int n=min; n<max; n++) {
				int yTop = ((centerY - (height-1)*10/2) + ((n+1-min) * 10));
				int leftTxt = (int)leftX + border + 1;
				this.write((n+1) + ") " + wayPts.get(n).name, leftTxt, yTop-9, 0xffffff);

				if(this.iMenu==4) {
					hide = "X";
				} else {
					if(wayPts.get(n).enabled) hide = "On";
					else hide = "Off";
				}

				this.write(hide, (int)rightX - border - 29 - this.chkLen(hide)/2, yTop-8, 0xffffff);

				if (MouseX>leftTxt && MouseX<(rightX-border-77) && MouseY>yTop-10 && MouseY<yTop-1) {
					String out = wayPts.get(n).x + ", " + wayPts.get(n).z;
					int len = chkLen(out)/2;
					GL11.glDisable(3553);
					GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.8f);
					this.drawBox(MouseX-len-1, MouseX+len+1, MouseY-11, MouseY-1);
					GL11.glEnable(3553);
					this.write(out, MouseX-len, MouseY-10, 0xffffff);
				}
			}


			int footpos = ((scHeight+5)/2 + (height-1)*10/2 + 11);

			if (this.iMenu==2) {
				this.write(opt1, centerX - 5 - border - footer - this.chkLen(opt1)/2, footpos , 16777215);
				this.write(opt2, centerX + border +5 + footer - this.chkLen(opt2)/2, footpos, 16777215);
			} else {
				if (this.iMenu!=4)this.write(opt1, centerX - 5 - border*2 - footer*2 - this.chkLen(opt1)/2, footpos, 16777215);

				this.write(opt2, centerX - this.chkLen(opt2)/2, footpos, 16777215);

				if (this.iMenu!=4)this.write(opt3, centerX + 5 + border*2 + footer*2 - this.chkLen(opt3)/2, footpos, 16777215);
			}
		}

		if (this.iMenu>4) {
			String verify = " !\"#$%&'()*+,-./0123456789;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_'abcdefghijklmnopqrstuvwxyz{|}~⌂ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»";

			if(this.iMenu>5 && this.inStr.equals("")) verify = "-0123456789";
			else if (this.iMenu>5) verify = "0123456789";

			if(Keyboard.getEventKeyState()) {
				do {
					if(Keyboard.getEventKey() == Keyboard.KEY_RETURN && this.lastKey!= Keyboard.KEY_RETURN)
						if (this.inStr.equals(""))
							this.next = 3;
						else if(this.iMenu == 5) {
							this.next = 6;
							this.way = this.inStr;
							if (this.game.thePlayer.dimension!=-1)
								this.inStr = Integer.toString(this.xCoord());
							else
								this.inStr = Integer.toString(this.xCoord()*8);
						} else if (this.iMenu==6) {
							this.next = 7;

							try {
								this.wayX = Integer.parseInt(this.inStr);
							} catch (Exception localException) {
								this.next=3;
							}
							if (this.game.thePlayer.dimension!=-1)
								this.inStr = Integer.toString(this.zCoord());
							else
								this.inStr = Integer.toString(this.zCoord()*8);
						} else {
							this.next = 3;

							try {
								this.wayZ = Integer.parseInt(this.inStr);
							} catch (Exception localException) {
								this.inStr="";
							}

							if(!this.inStr.equals("")) {
								Waypoint wpt = new Waypoint(this.way, wayX, wayZ, true);
								wayPts.add(wpt);
								wpt.setDisplayInWorld(this.showBeacons);
								EntityWaypoint ewpt = new EntityWaypoint(this.getWorld(), wpt, (this.game.thePlayer.dimension==-1));
								this.getWorld().addWeatherEffect(ewpt);
								this.saveWaypoints();

								if(wayPts.size()>9) min = wayPts.size()-9;
							}
						}
					else if (Keyboard.getEventKey() == Keyboard.KEY_BACK && this.lastKey!= Keyboard.KEY_BACK)
						if (this.inStr.length() > 0)
							this.inStr = this.inStr.substring(0, this.inStr.length()-1);

					if(verify.indexOf(Keyboard.getEventCharacter()) >= 0 && Keyboard.getEventKey()!= this.lastKey)
						if(this.chkLen(this.inStr + Keyboard.getEventCharacter()) < 148)
							this.inStr = this.inStr + Keyboard.getEventCharacter();

					this.lastKey = Keyboard.getEventKey();
				} while (Keyboard.next());
			} else this.lastKey = 0;

			GL11.glDisable(3553);
			GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.7f);
			leftX = centerX - 75 - border;
			rightX = centerX + 75 + border;
			topY = centerY - 10 - border;
			botY = centerY + 10 + border;
			this.drawBox(leftX, rightX, topY, botY);
			leftX = leftX+border;
			rightX = rightX-border;
			topY = topY + 11;
			botY = botY - border;
			GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
			this.drawBox(leftX, rightX, topY, botY);
			GL11.glEnable(3553);
			String out = "Please enter a name:";

			if(this.iMenu==6) out = "Enter X coordinate:";
			else if(this.iMenu == 7) out = "Enter Z coordinate:";

			this.write(out, (int)leftX + border, (int)topY-11 + border, 0xffffff);

			if(this.blink>60)this.blink=0;

			if(this.blink<30)this.write(this.inStr + "|", (int)leftX + border, (int)topY + border, 0xffffff);
			else this.write(this.inStr, (int)leftX + border, (int)topY + border, 0xffffff);

			if(this.iMenu==6)
				try {
					if(Integer.parseInt(this.inStr)==this.xCoord()) this.write("(Current)", (int)leftX + border + this.chkLen(this.inStr) + 5, (int)topY + border, 0xa0a0a0);
				} catch(Exception localException) {}
			else if (this.iMenu==7)
				try {
					if(Integer.parseInt(this.inStr)==this.zCoord()) this.write("(Current)", (int)leftX + border + this.chkLen(this.inStr) + 5, (int)topY + border, 0xa0a0a0);
				} catch(Exception localException) {}

			this.blink++;
		}

		if(this.next!=0) {
			this.iMenu = this.next;
			this.next = 0;
		}
	}

	private void showCoords (int scWidth, int scHeight) {
		if(!this.hide) {
			GL11.glPushMatrix();
			GL11.glScalef(0.5f, 0.5f, 1.0f);
			String xy ="";
			if (this.game.thePlayer.dimension!=-1)
				xy = this.dCoord(xCoord()) + ", " + this.dCoord(zCoord());
			else
				xy = this.dCoord(xCoord()*8) + ", " + this.dCoord(zCoord()*8);
			int m = this.chkLen(xy)/2;
			this.write(xy, scWidth*2-32*2-m, 146, 0xffffff);
			xy = Integer.toString(this.yCoord());
			m = this.chkLen(xy)/2;
			//	xy="" + this.getWorld().skylightSubtracted + " " + this.getWorld().calculateSkylightSubtracted(1.0F) + " " + this.getWorld().func_35464_b(1.0F); // always 0 in SMP. method works, not value.  it's never updated, no world tick in SMP.  Fscks lightmap functionality
			this.write(xy, scWidth*2-32*2-m, 156, 0xffffff);
			GL11.glPopMatrix();
		} else {
			if (this.game.thePlayer.dimension!=-1) this.write("(" + this.dCoord(xCoord()) + ", " + this.yCoord() + ", " + this.dCoord(zCoord()) + ") " + (int) this.direction + "'", 2, 10, 0xffffff);
			else this.write("(" + this.dCoord(xCoord()*8) + ", " + this.yCoord() + ", " + this.dCoord(zCoord()*8) + ") " + (int) this.direction + "'", 2, 10, 0xffffff);
		}
	}

	private void drawRound(int paramInt1, int scScale) {
		try {
			this.disp(scScale>=3?this.img("/mamiyaotaru/roundmap2x.png"):this.img("/mamiyaotaru/roundmap.png"));
			drawPre();
			this.setMap(paramInt1);
			drawPost();
		} catch (Exception localException) {
			this.error = "Error: minimap overlay not found!";
		}
	}

	private void drawBox(double leftX, double rightX, double topY, double botY) {
		drawPre();
		ldrawtwo(leftX, botY, 0.0D);
		ldrawtwo(rightX, botY, 0.0D);
		ldrawtwo(rightX, topY, 0.0D);
		ldrawtwo(leftX, topY, 0.0D);
		drawPost();
	}

	private void drawOptions(double rightX,double topY,int MouseX,int MouseY,boolean set,boolean click) {
		if(this.iMenu>2) { // waypoint stuff
			if(min<0) min = 0;

			if(!Mouse.isButtonDown(0) && scrClick) scrClick = false;

			if (MouseX>(rightX-10) && MouseX<(rightX-2) && MouseY>(topY+1) && MouseY<(topY+10)) {
				if(set || click) {
					if(set&&min>0) min--;

					GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.7f);
				} else GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
			} else
				GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.3f);

			drawPre();
			ldrawtwo(rightX-10, topY+10, 0.0D);
			ldrawtwo(rightX-2, topY+10, 0.0D);
			ldrawtwo(rightX-6, topY+1, 0.0D);
			ldrawtwo(rightX-6, topY+1, 0.0D);
			drawPost();

			if(wayPts.size()>9) {
				sMax = (int)(9.0D/wayPts.size()*67.0D);
			} else {
				sMin = 0;
				sMax = 67;
			}

			if (MouseX>rightX-10 && MouseX<rightX-2 && MouseY>topY+12+sMin && MouseY<topY+12+sMin+sMax || scrClick) {
				if(Mouse.isButtonDown(0)&&!scrClick) {
					scrClick = true;
					scrStart = MouseY;
				} else if (scrClick && wayPts.size()>9) {
					int offset = MouseY-scrStart;

					if(sMin+offset<0) sMin = 0;
					else if (sMin+offset+sMax>67) sMin = 67-sMax;
					else {
						sMin = sMin+offset;
						scrStart = MouseY;
					}

					min = (int)((sMin/(67.0D-sMax))*(wayPts.size()-9));
					GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.7f);
				} else GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
			} else {
				if(wayPts.size()>9)
					sMin = (int)((double)min/(double)(wayPts.size()-9)*(67.0D-sMax));

				GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.3f);
			}

			this.drawBox(rightX-10, rightX-2, topY+12+sMin, topY+12+sMin+sMax);

			if (MouseX>rightX-10 && MouseX<rightX-2 && MouseY>topY+81 && MouseY<topY+90) {
				if(set || click) {
					if(set&&min<wayPts.size()-9) min++;

					GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.7f);
				} else GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
			} else
				GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.3f);

			drawPre();
			ldrawtwo(rightX-6, topY+90, 0.0D);
			ldrawtwo(rightX-6, topY+90, 0.0D);
			ldrawtwo(rightX-2, topY+81, 0.0D);
			ldrawtwo(rightX-10, topY+81, 0.0D);
			drawPost();
		}

		double leftX = rightX - 30;
		double botY = 0;
		topY+=1;
		int max = min+9;

		if(max>wayPts.size()) {
			max = wayPts.size();

			if(min>0) {
				if(max-9>0)
					min = max-9;
				else
					min = 0;
			}
		}

		double leftCl = 0;
		double rightCl = 0;

			leftX = leftX - 14;
			rightX = rightX - 14;
			rightCl = rightX - 32;
			leftCl = rightCl - 9;

		for(int i = min; i<max; i++) { // draw options.  location varies based on which menu it is :-/
			if(i>min) topY += 10;

			botY = topY + 9; 

			if (MouseX>leftX && MouseX<rightX && MouseY>topY && MouseY<botY && this.iMenu < 5)
				if (set || click) {
					if(set) {
						if (this.iMenu==3) {
							wayPts.get(i).enabled = !wayPts.get(i).enabled;
							this.saveWaypoints();
						} else {
							this.delWay(i);
							this.next=3;
						}
					}

					GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
				} else GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.6f);
			else {
				if (this.iMenu==4) {
					GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.4f);
				} else {
					if (wayPts.get(i).enabled) {
						GL11.glColor4f(0.0f, 1.0f, 0.0f, 0.6f);
					} else GL11.glColor4f(1.0f, 0.0f, 0.0f, 0.6f);
				}
			}

			this.drawBox(leftX, rightX, topY, botY);

			if(!(iMenu==4 && this.next==3)) { // if not waypoint delete menu
				if (MouseX>leftCl && MouseX<rightCl && MouseY>topY && MouseY<botY && this.iMenu==3)
					if (set) {
						wayPts.get(i).red = generator.nextFloat();
						wayPts.get(i).green = generator.nextFloat();
						wayPts.get(i).blue = generator.nextFloat();
						saveWaypoints();
					}

				GL11.glColor3f(wayPts.get(i).red, wayPts.get(i).green, wayPts.get(i).blue);
				this.drawBox(leftCl, rightCl, topY, botY);
			}
		}
	}

	private void delWay(int i) { 
		wayPts.get(i).kill();
		wayPts.remove(i);
		this.saveWaypoints();
	}
	
	private void delWay(Waypoint point) { // TODO perhaps let entity know it is dead by making the entity a listener
		point.kill();
		wayPts.remove(point);
		this.saveWaypoints();
	}

	private int drawFooter(int centerX,int centerY,int m, String opt1, String opt2, String opt3, int border,int MouseX,int MouseY,boolean set,boolean click) {
		int footer = this.chkLen(opt1);

		if (this.chkLen(opt2) > footer) footer = this.chkLen(opt2);

		double leftX = centerX - footer - border*2 - 5;
		double rightX = centerX - 5;
		double topY = centerY + (m-1)/2.0D*10.0D - border + 10.0D;
		double botY = centerY + (m-1)/2.0D*10.0D + border + 20.0D;

		if (this.chkLen(opt3) > footer) footer = this.chkLen(opt3);

		leftX = centerX - border*3 - footer*1.5 - 5;
		rightX = centerX - footer/2 - border - 5;

		if (MouseX>leftX && MouseX<rightX && MouseY>topY && MouseY<botY && this.iMenu < 4) // if on main menu or waypoint menu
			if (set || click) {
				if(set) {
					// new GuiMinimap since we are only using this legacy for waypoints
					this.iMenu = 0; // disable built in menu
					this.game.displayGuiScreen(new GuiMinimap(this)); // display new minecraft style main menu
					//if (this.iMenu==2) 
					//	setMenuNull(); // if on main menu, exit menu system when clicked
					//else 
					//	this.next=2; // else go back to main menu
				}

				GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			} else GL11.glColor4f(0.5f, 0.5f, 0.5f, 0.7f);
		else GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.7f);

		if (this.iMenu!=4)this.drawBox(leftX, rightX, topY, botY);

		leftX = centerX - footer/2 - border;
		rightX = centerX + footer/2 + border;

		if (MouseX>leftX && MouseX<rightX && MouseY>topY && MouseY<botY && this.iMenu < 5)
			if (set || click) {
				if(set) {
					if (this.iMenu==2 || this.iMenu==4) this.next=3;
					else {
						this.next = 5;
						this.inStr = "";
					}
				}

				GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			} else GL11.glColor4f(0.5f, 0.5f, 0.5f, 0.7f);
		else GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.7f);

		this.drawBox(leftX, rightX, topY, botY);

		rightX = centerX + border*3 + footer*1.5 + 5;
		leftX = centerX + footer/2 + border + 5;

		if (MouseX>leftX && MouseX<rightX && MouseY>topY && MouseY<botY && this.iMenu < 4)
			if (set || click) {
				if(set) this.next = 4;

				GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			} else GL11.glColor4f(0.5f, 0.5f, 0.5f, 0.7f);
		else GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.7f);

		if (this.iMenu!=4) this.drawBox(leftX, rightX, topY, botY);

		return footer/2;
	}

	private void setMap(int paramInt1) {
	//	ldrawthree(paramInt1 - 64.0D, 64.0D + 5.0D, 1.0D, 0.0D, 1.0D);
	//	ldrawthree(paramInt1, 64.0D + 5.0D, 1.0D, 1.0D, 1.0D);
	//	ldrawthree(paramInt1, 5.0D, 1.0D, 1.0D, 0.0D);
	//	ldrawthree(paramInt1 - 64.0D, 5.0D, 1.0D, 0.0D, 0.0D);
		setMap(paramInt1, 128); // do this with default image size of 128 (that everything was before I decided to stop padding 16px and 8px images out to 128)
	}
	
	private void setMap(int paramInt1, int imageSize) {
		int scale = imageSize/4;
		ldrawthree(paramInt1-32.0D-scale, 37.0D+scale, 1.0D, 0.0D, 1.0D);
		ldrawthree(paramInt1-32.0D+scale, 37.0D+scale, 1.0D, 1.0D, 1.0D);
		ldrawthree(paramInt1-32.0D+scale, 37.0D-scale, 1.0D, 1.0D, 0.0D);
		ldrawthree(paramInt1-32.0D-scale, 37.0D-scale, 1.0D, 0.0D, 0.0D);
	}

	private void drawDirections(int scWidth) {

		/*// this looks to be a way to display an image with NSEW on it, overlaid over the top.  obsoleted, use text
		int wayX = this.xCoord();
		int wayY = this.yCoord();
		float locate = (float)Math.toDegrees(Math.atan2(wayX, wayY));
		double hypot = Math.sqrt((wayX*wayX)+(wayY*wayY))/(Math.pow(2,this.zoom)/2);


			try 
			{
				GL11.glPushMatrix();
				GL11.glColor3f(1.0f, 1.0f, 1.0f);
				this.disp(this.img("/compass.png"));
				GL11.glTranslatef(scWidth - 32.0F, 37.0F, 0.0F);
				GL11.glRotatef(-locate + this.direction + 180.0F, 0.0F, 0.0F, 1.0F);
				GL11.glTranslated(0.0D,-hypot,0.0D);
				GL11.glRotatef(-(-locate + this.direction + 180.0F), 0.0F, 0.0F, 1.0F);
				GL11.glTranslated(0.0D,hypot,0.0D);
				GL11.glTranslatef(-(scWidth - 32.0F), -37.0F, 0.0F);
				GL11.glTranslated(0.0D,-hypot,0.0D);
				drawPre();
				this.setMap(scWidth);
				drawPost();
			} catch (Exception localException) 
			{
				this.error = "Error: compass overlay not found!";
			} finally 
			{
				GL11.glPopMatrix();
			}*/

		GL11.glPushMatrix();
		GL11.glScalef(0.5f, 0.5f, 1.0f);
		GL11.glTranslated((64.0D * Math.sin(Math.toRadians(-(this.direction - 90.0D + northRotate)))),(64.0D * Math.cos(Math.toRadians(-(this.direction - 90.0D + northRotate)))),0.0D); // direction -90 w top.  0 n top.  in all cases n top means 90 more (or w top means 90 less)
		this.write("N", scWidth*2-66, 70, 0xffffff);
		GL11.glPopMatrix();
		GL11.glPushMatrix();
		GL11.glScalef(0.5f, 0.5f, 1.0f);
		GL11.glTranslated((64.0D * Math.sin(Math.toRadians(-(this.direction + northRotate)))),(64.0D * Math.cos(Math.toRadians(-(this.direction + northRotate)))),0.0D);
		this.write("E", scWidth*2-66, 70, 0xffffff);
		GL11.glPopMatrix();
		GL11.glPushMatrix();
		GL11.glScalef(0.5f, 0.5f, 1.0f);
		GL11.glTranslated((64.0D * Math.sin(Math.toRadians(-(this.direction + 90.0D + northRotate)))),(64.0D * Math.cos(Math.toRadians(-(this.direction + 90.0D + northRotate)))),0.0D);
		this.write("S", scWidth*2-66, 70, 0xffffff);
		GL11.glPopMatrix();
		GL11.glPushMatrix();
		GL11.glScalef(0.5f, 0.5f, 1.0f);
		GL11.glTranslated((64.0D * Math.sin(Math.toRadians(-(this.direction + 180.0D + northRotate)))),(64.0D * Math.cos(Math.toRadians(-(this.direction + 180.0D + northRotate)))),0.0D);
		this.write("W", scWidth*2-66, 70, 0xffffff);
		GL11.glPopMatrix();
	}

	private void SetZoom() {
		if (this.fudge > 0) return;

		if (this.iMenu != 0) {
			this.iMenu = 0;

			if(getMenu()!=null) setMenuNull();
		} else {
			if (this.zoom == 3) {
				if(!this.full) this.full = true;
				else {
					this.zoom = 2;
					this.full = false;
					this.error = "Zoom Level: (1.0x)";
				}
			} else if (this.zoom == 0) {
				this.zoom = 3;
				this.error = "Zoom Level: (0.5x)";
			} else if (this.zoom==2) {
				this.zoom = 1;
				this.error = "Zoom Level: (2.0x)";
			} else {
				this.zoom = 0;
				this.error = "Zoom Level: (4.0x)";
			}
			this.timer = 500;
		}

		this.fudge = 20;
	}

	//@Override
	public String Version() {
		return "1.3_01 - "+zmodver;
	}
	
	// menu from here
	
    /**
     * Gets a key binding. // aka the text on the button?
     */
    public String getKeyText(EnumOptionsMinimap par1EnumOptions)
    {
        StringTranslate stringtranslate = StringTranslate.getInstance();
//      String s = (new StringBuilder()).append(stringtranslate.translateKey(par1EnumOptions.getEnumString())).append(": ").toString(); // use if I ever do translations
        String s = (new StringBuilder()).append(par1EnumOptions.getEnumString()).append(": ").toString();

        if (par1EnumOptions.getEnumFloat())
        {
            float f = getOptionFloatValue(par1EnumOptions);

            if (par1EnumOptions == EnumOptionsMinimap.ZOOM)
            {
                return (new StringBuilder()).append(s).append((int)(f)).toString();
            }

            if (f == 0.0F)
            {
                return (new StringBuilder()).append(s).append(stringtranslate.translateKey("options.off")).toString();
            }
            else
            {
                return (new StringBuilder()).append(s).append((int)(f * 100F)).append("%").toString();
            }
        }

        if (par1EnumOptions.getEnumBoolean())
        {
            boolean flag = getOptionBooleanValue(par1EnumOptions);

            if (flag)
            {
                return (new StringBuilder()).append(s).append(stringtranslate.translateKey("options.on")).toString();
            }
            else
            {
                return (new StringBuilder()).append(s).append(stringtranslate.translateKey("options.off")).toString();
            }
        }
        
        if (par1EnumOptions.getEnumList())
        {
            String state = getOptionListValue(par1EnumOptions);

            return (new StringBuilder()).append(s).append(state).toString();
        }

        else
        {
            return s;
        }
    }
    
    public float getOptionFloatValue(EnumOptionsMinimap par1EnumOptions)
    {
        if (par1EnumOptions == EnumOptionsMinimap.ZOOM)
        {
            return this.lZoom;
        }
        else
        {
            return 0.0F;
        }
    }
    
    public boolean getOptionBooleanValue(EnumOptionsMinimap par1EnumOptions)
    {
        switch (EnumOptionsHelperMinimap.enumOptionsMappingHelperArray[par1EnumOptions.ordinal()])
        {
            case 0:
                return this.coords;
                
            case 1:
            	return this.hide;

            case 2:
                return this.showNether;

            case 3:
                return this.showCaves;

            case 4:
                return this.lightmap;

            case 5:
                return this.heightmap;

            case 6:
                return this.squareMap;
                
            case 7:
                return this.oldNorth;
                
            case 8:
                return this.showBeacons; 
                
            case 9:
                return this.welcome;
                
            case 10:
                return this.threading;
        }

        return false;
    }
    
    public String getOptionListValue(EnumOptionsMinimap par1EnumOptions)
    {
        if (par1EnumOptions == EnumOptionsMinimap.TERRAIN)
        {
            if (this.heightmap) return "Height";
            else if (this.slopemap) return "Slope";
            else return "Off";
        }
        else
        {
            return "";
        }
    }

	public void setOptionValue(EnumOptionsMinimap par1EnumOptions, int i) {
        switch (par1EnumOptions.ordinal())
        {
            case 0:
                this.coords = !coords;
                break;
                
            case 1:
                this.hide = !hide;
                break;

            case 2:
                this.showNether = !showNether;
                break;

            case 3:
                this.showCaves = !showCaves;
                break;

            case 4:
                this.lightmap = !lightmap;
                break;

            case 5:
    			if (this.slopemap) {
    				this.slopemap = false;
    				this.heightmap = true;
    			}
    			else if (this.heightmap) {
    				this.slopemap = false;
    				this.heightmap = false;
    			}
    			else {
    				this.slopemap = true;
    				this.heightmap = false;
    			}
                break;
                
            case 6:
                this.squareMap = !squareMap;
                break;
                
            case 7:
                this.oldNorth = !oldNorth;
                break;
                
            case 8:
            	showBeacons = !showBeacons;
            	this.displayWaypointEntities(showBeacons); // calls method to iterate through them and tell them to be off so the renderers (not referenced here) know to turn off 
                break;
                
            case 9:
                this.welcome = !welcome;
                break;
                
            case 10:
                this.threading = !threading;
                break;
                
  /*          case 12:
    			if (motionTrackerExists) {
    				motionTracker.activated = true;
    				this.game.displayGuiScreen((GuiScreen)null);
    			}
            	break;*/
        }
		this.timer=500; // re-render immediately for new options
	}

    
    
	
}