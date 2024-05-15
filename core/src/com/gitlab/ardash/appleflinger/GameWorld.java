/*******************************************************************************
 * Copyright (C) 2015-2023 Andreas Redmer <ar-appleflinger@abga.be>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.gitlab.ardash.appleflinger;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.math.Vector2;

import com.gitlab.ardash.appleflinger.actors.AppleActor;
import com.gitlab.ardash.appleflinger.actors.BackgroundActor;
import com.gitlab.ardash.appleflinger.actors.BackgroundActor.BackgroundConfiguration;
import com.gitlab.ardash.appleflinger.actors.Bird;
import com.gitlab.ardash.appleflinger.actors.BlockActor;
import com.gitlab.ardash.appleflinger.actors.GeneralTargetActor;
import com.gitlab.ardash.appleflinger.actors.Ground;
import com.gitlab.ardash.appleflinger.actors.IntervalSpawnActor;
import com.gitlab.ardash.appleflinger.actors.Jet;
import com.gitlab.ardash.appleflinger.actors.PhysicsActor;
import com.gitlab.ardash.appleflinger.actors.ProjectileActor;
import com.gitlab.ardash.appleflinger.actors.SlingShotActor;
import com.gitlab.ardash.appleflinger.ai.PlayerSimulator;
import com.gitlab.ardash.appleflinger.global.GameManager;
import com.gitlab.ardash.appleflinger.global.GameState;
import com.gitlab.ardash.appleflinger.global.MaterialConfig;
import com.gitlab.ardash.appleflinger.global.PhysicWorldObserver;
import com.gitlab.ardash.appleflinger.global.PlayerStatus.PlayerSide;
import com.gitlab.ardash.appleflinger.listeners.MyContactListener;
import com.gitlab.ardash.appleflinger.listeners.OnPhysicStoppedListener;
import com.gitlab.ardash.appleflinger.missions.Mission;
import com.gitlab.ardash.appleflinger.screens.GameScreen;
import com.gitlab.ardash.appleflinger.helpers.SoundPlayer;
import com.gitlab.ardash.appleflinger.i18n.I18N;

import com.gitlab.ardash.appleflinger.global.Assets;
import com.gitlab.ardash.appleflinger.global.Assets.SoundGroupAsset;

import com.gitlab.ardash.appleflinger.Ads1115;

public class GameWorld implements Disposable{

    // here we set up the actual viewport size of the game in meters.
    public static final float UNIT_TO_SCREEN = 160f;
    public static float UNIT_WIDTH = GameScreen.SCREEN_WIDTH/UNIT_TO_SCREEN; // 6.4 meters width  (12)
    public static float UNIT_HEIGHT = GameScreen.SCREEN_HEIGHT/UNIT_TO_SCREEN; // 3.75 meters height  (6.75)
//    public static float UNIT_WIDTH = GameScreen.SCREEN_WIDTH/341.333333333f; // 3 meters width
//    public static float UNIT_HEIGHT = GameScreen.SCREEN_HEIGHT/200f; // 3 meters height

//    public static final Vector2 GRAVITY = new Vector2(0, -9.8f);
    public static final Vector2 GRAVITY = new Vector2(0, -6.8f);  // REAL GAME
//    public static final Vector2 GRAVITY = new Vector2(0, 0f);

    public final AdvancedStage stage; // stage containing game actors (not GUI, but actual game elements)
    public World box2dWorld; // box2d world
    public PhysicWorldObserver physicWorldObserver;
	private ProjectileActor firstProjectile;

    private boolean isPhysicPaused;

	public final Mission mission;
	private Group birdGroup;
	private Group jetGroup;

	private Ads1115 ads1115;
	// AINP = AIN0 and AINN = GND, +/- 2.048V, Continuous conversion mode, 128 SPS
	private static final byte[] ch0Config = {(byte)0x42, (byte)0x83};
	private static final byte[] ch1Config = {(byte)0x62, (byte)0x83};
	private double[] zeroValue = {0, 0};
	private double lastValue;
	private double lastDistance;
	private float angle = 90;
	private float angleDirection = -1;
	private Vector2 shootDirection;
	private int count = 0;
	
	public GameWorld(Mission mission) {
    	this.mission = mission;
    	isPhysicPaused = false;
    	firstProjectile = null;

        World.setVelocityThreshold(0.2f);
        box2dWorld = new World(GRAVITY, true);
        physicWorldObserver = new PhysicWorldObserver(box2dWorld);
        box2dWorld.setContactListener(new MyContactListener());
        //stage = new Stage(); // create the game stage
        //stage.setViewport(UNIT_WIDTH, UNIT_HEIGHT, false); // set the game stage viewport to the meters size
        stage = new AdvancedStage(new FitViewport(UNIT_WIDTH, UNIT_HEIGHT));
        //stage = new Stage(new ExtendViewport(UNIT_WIDTH, UNIT_HEIGHT, UNIT_WIDTH, UNIT_HEIGHT));

        createWorld();

		if (GameManager.ANALOG_INPUT)
		{
			ads1115 = new Ads1115(Ads1115.ADDRESS.GND, Ads1115.GAIN.GAIN_4_096V, Ads1115.DataRate.SPS_128);
			// A2 is the left Wheel
			// A3 is the right Wheel
			System.out.println("Reading zeros...");
			zeroValue[0] = ads1115.readValue(Ads1115.Channel.A2);
			do
			{
				zeroValue[1] = ads1115.readValue(Ads1115.Channel.A3);
			}
			while (zeroValue[0] == zeroValue[1]);
			do
			{
				zeroValue[0] = ads1115.readValue(Ads1115.Channel.A2);
			}
			while (zeroValue[0] == zeroValue[1]);
			System.out.println((int)(zeroValue[0] * 100000) + ", " + (int)(zeroValue[1] * 100000));
		}
//        PlayerSimulator.INSTANCE.markAsUninitialised();
        PlayerSimulator.INSTANCE.startBackgroundInitForNextRound();
    }

    private void createWorld() {
    	final GameManager gm = GameManager.getInstance();
    	gm.setGameState(GameState.INIT_WORLD);

    	// choose background
    	switch (mission.getMajor()) {
		case 1:
	    	stage.addActor(new BackgroundActor(BackgroundConfiguration.ORIGINAL));
			break;
		case 2:
	    	stage.addActor(new BackgroundActor(BackgroundConfiguration.WINTER));
			break;
		default:
			throw new RuntimeException("no background image assigned for episode " + mission.getMajor());
		}

		jetGroup = new Group();
		stage.addActor(jetGroup);
    	birdGroup = new Group();
    	stage.addActor(birdGroup);

    	IntervalSpawnActor isa = new IntervalSpawnActor(jetGroup, Jet.class, 60f, 120f);
    	stage.addActor(isa);

    	// absorber box
    	float absorberDistance = 4f;
    	BlockActor absorber = new BlockActor(this,MaterialConfig.INVISIBLE,-absorberDistance,UNIT_HEIGHT/2, 1f,UNIT_HEIGHT*2.8f, BodyType.StaticBody);
    	absorber.setAbsorbing(true);
    	stage.addActor(absorber);
    	absorber = new BlockActor(this,MaterialConfig.INVISIBLE,UNIT_WIDTH+absorberDistance,UNIT_HEIGHT/2, 1f,UNIT_HEIGHT*2.8f, BodyType.StaticBody);
    	absorber.setAbsorbing(true);
    	stage.addActor(absorber);
    	absorber = new BlockActor(this,MaterialConfig.INVISIBLE,UNIT_WIDTH/2,-absorberDistance/2, UNIT_WIDTH*2,1f, BodyType.StaticBody);
    	absorber.setAbsorbing(true);
    	stage.addActor(absorber);
    	absorber = new BlockActor(this,MaterialConfig.INVISIBLE,UNIT_WIDTH/2,UNIT_HEIGHT+absorberDistance*1.5f, UNIT_WIDTH*2,1f, BodyType.StaticBody);
    	absorber.setAbsorbing(true);
    	stage.addActor(absorber);

    	// mirrored part
    	Group leftSideMirror = mission.getStageFiller().fillMirrorStage(this);
    	Group rightSideMirror = mission.getStageFiller().fillMirrorStage(this);

    	// apply a translation for all right side elements
    	// can't use a transformation matrix here, because the physics won't be transformed
    	for (Actor a : rightSideMirror.getChildren())
    	{
    		final float center = UNIT_WIDTH/2f;
    		// d is distance to center
    		final float d = center - a.getX();
    		final float newx = center + d;
    		//final float newx = 9;
    		if (a instanceof PhysicsActor) {
				PhysicsActor pa = (PhysicsActor) a;
				pa.setPhysicalPosition(new Vector2(newx,a.getY()));
				pa.setPhysicalRotation(-pa.getPhysicalRotation());
			}
    		else
    		{
        		if (a instanceof SlingShotActor) {
        			SlingShotActor pa = (SlingShotActor) a;
    				pa.setPosition(newx,a.getY());
    				pa.setMirrored(true);
    				//pa.setX(newx);
    			}
    		}
    	}

    	// apply player sides to the TargetActors
    	for (Actor a : leftSideMirror.getChildren())
    	{
    		if (a instanceof GeneralTargetActor) {
				GeneralTargetActor ta = (GeneralTargetActor) a;
				ta.setPlayerSide(PlayerSide.LEFT);
			}
    	}
    	for (Actor a : rightSideMirror.getChildren())
    	{
    		if (a instanceof GeneralTargetActor) {
				GeneralTargetActor ta = (GeneralTargetActor) a;
				ta.setPlayerSide(PlayerSide.RIGHT);
			}
    	}

    	stage.addActor(leftSideMirror);
    	stage.addActor(rightSideMirror);

    	// store references to the slingshots in the gamestate
    	for (Actor a : leftSideMirror.getChildren())
    		if (a instanceof SlingShotActor)
    			gm.PLAYER1.slingshot = (SlingShotActor) a;
    	for (Actor a : rightSideMirror.getChildren())
    		if (a instanceof SlingShotActor)
    			gm.PLAYER2.slingshot = (SlingShotActor) a;


    	final GameWorld thisForListener = this;
    	// projectile will now be added as soon as the initial physic get settled
    	physicWorldObserver.removeAllListeners();
    	physicWorldObserver.addListener(new OnPhysicStoppedListener() {

			@Override
			public void onPhysicStopped() {

				// if there is still a projectile, expire it and restart the physics
				if (firstProjectile!=null && !firstProjectile.isToBeDestroyed() && firstProjectile.getStage()!=null)
				{
					firstProjectile.endLifetimeNow();
					continuePhysics();
					return;
				}
				// if there is no projectile anymore, we can go on and create a new one
				gm.turnToNextPlayer();
				double rawValue;
				if (gm.currentPlayer == gm.PLAYER1)
					rawValue = ads1115.readValue(Ads1115.Channel.A2);
				else
					rawValue = ads1115.readValue(Ads1115.Channel.A3);
				if (gm.getGameState()==GameState.GAME_OVER_SCREEN)
					return;
			      firstProjectile = new AppleActor(thisForListener,MaterialConfig.PROJECTILE,0,0,0.4f, BodyType.DynamicBody);
			      gm.currentPlayer.slingshot.addProjectile(firstProjectile);
			      gm.setGameState(GameState.WAIT_FOR_DRAG);
			      // pause physical behaviour once it was stopping already
			      thisForListener.pausePhysics();

			      if (gm.isPlayer2CPU() && gm.currentPlayer == gm.PLAYER2)
			      {
			    	  PlayerSimulator.INSTANCE.playOneRound();
			    	  //PlayerSimulator.INSTANCE.tickThinking();
			      }
			      else
			      {
			    	  gm.getInputMultiplexer().removeProcessor(stage);
			    	  gm.getInputMultiplexer().addProcessor(stage);
			      }
			}
		});

        Ground ground = new Ground(this);
        stage.addActor(ground);

    	gm.setGameState(GameState.WAIT_FOR_PHYSICS);
    }

    public void update(float delta) {
      Array<Fixture> fixtures = new Array<>(0);
		// remove destroyed actors, before simulation step starts
    	box2dWorld.getFixtures(fixtures);
    	for (Fixture f : fixtures)
    	{
    		Object ud = f.getUserData();
    		if (ud instanceof PhysicsActor)
    		{
    			// check if it was absorbed / or expiration has ended the explosion animation
    			boolean remove = ((PhysicsActor)ud).isToBeRemoved();
    			if (remove)
    			{
    				box2dWorld.destroyBody(f.getBody());
    				Actor a = (Actor)ud;
    				a.remove();

    			}
    		}
    	}
		final GameManager gm = GameManager.getInstance();
		if (GameManager.ANALOG_INPUT
			&& !(gm.currentPlayer == gm.PLAYER2 && gm.isPlayer2CPU()))
		{
			int channel = 0;
			double rawValue = 0;
			if (gm.currentPlayer == gm.PLAYER1)
				rawValue = ads1115.readValue(Ads1115.Channel.A2);
			else
			{
				rawValue = ads1115.readValue(Ads1115.Channel.A3);
				channel = 1;
			}
			zeroValue[channel] = zeroValue[channel] * 0.998 + rawValue * 0.002;
			double value = rawValue - zeroValue[channel];
			count++;
			if (count == 9)
			{
				System.out.println((int)(rawValue * 100000) + " => " + (int)(value * 1000));
				count = 0;
			}

			if (firstProjectile != null)
			{
				if (gm.getGameState() == GameState.WAIT_FOR_DRAG)
				{
					if ((value >= 0.003) && (firstProjectile.slingShotActor != null))
					{
						firstProjectile.removePhysics();
						gm.setGameState(GameState.DRAGGING);
						SoundPlayer.playSound(Assets.getRandomSound(SoundGroupAsset.RUBBER),0.25f);
						angle = 90;
						if (gm.currentPlayer == gm.PLAYER1)
							angleDirection = 1;
						else
							angleDirection = -1;
						lastValue = 0;
						lastDistance = 0;
						shootDirection = new Vector2(0, 0);
					}
				}
				else if (gm.getGameState() == GameState.DRAGGING)
				{
					if (value < lastValue * 0.2 && lastValue > 0.003)
					{
						gm.getInputMultiplexer().removeProcessor(gm.currentGameScreen.getRenderer().world.stage);
						gm.getInputMultiplexer().addProcessor(gm.currentGameScreen.getGuiStage());
						gm.currentGameScreen.setAnnouncementText(I18N.getString("pleaseWait")+" ...", true);
						gm.onShotFired();
						SoundPlayer.playSound(Assets.getRandomSound(SoundGroupAsset.WHIZZ), 0.25f);
						firstProjectile.reAddPhysics();
						firstProjectile.body.setGravityScale(1.0f);

						GameManager.recordPullVector(shootDirection.cpy().scl(-1f));
						if (GameManager.DEBUG)
							System.out.println(shootDirection);

						firstProjectile.body.applyForceToCenter(shootDirection.scl(firstProjectile.shotForceMultiplyer), true);
						System.out.println("SHOOT "+ shootDirection
										   + ", len: " + shootDirection.len()
										   + ", angle: " + shootDirection.angle());

						gm.setGameState(GameState.WAIT_FOR_PHYSICS);
						firstProjectile.world.continuePhysics();
						final OrthographicCamera camera = gm.currentGameScreen.getRenderer().getCamera();
						camera.position.set(GameWorld.UNIT_WIDTH/2f, GameWorld.UNIT_HEIGHT/2f,0);
						firstProjectile.startLifetime();
						if (firstProjectile.slingShotActor != null)
							firstProjectile.slingShotActor.releaseProjectile();

					}
					else
					{
						final Vector2 rampCenterPoint = firstProjectile.slingShotActor.getSlingShotCenter();
						angle += angleDirection * delta * 33;
						if (gm.currentPlayer == gm.PLAYER1)
						{
							if (angle > 170 || angle < 60)
								angleDirection *= -1;
						}
						else
						{
							if (angle > 120 || angle < 10)
								angleDirection *= -1;
						}
						if (value > lastValue)
							lastValue = value;
						else
							lastValue = lastValue * 0.95 + value * 0.05;
						double distance = lastValue / 0.012 * firstProjectile.maxPullDistance;
						if (distance > firstProjectile.maxPullDistance)
						{
							distance = firstProjectile.maxPullDistance;
						}
						if (gm.currentPlayer == gm.PLAYER1)
						{
							distance *= -1;
							if (Math.abs(distance - lastDistance) > 0.05)
								System.out.println("Player1, distance "+distance);
						}
						else if (Math.abs(distance - lastDistance) > 0.05)
						{
							System.out.println("Player2, distance "+distance);
						}
						Vector2 move = new Vector2(0f, (float)distance).rotate(angle);
						firstProjectile.body.setTransform(rampCenterPoint.cpy().add(move.cpy().scl(-1)), 0);
						shootDirection.set(move);
						lastDistance = distance;
					}
				}
			}
		}
        // perform game logic here
    	if (!isPhysicPaused)
    	{
//	    	if (delta >0.02f)
//	    		delta = 0.02f;
	    	 if ( delta > 0.25f ) delta = 0.25f; // note: max frame time to avoid spiral of death
	    	 accumulator += delta;

	        while (accumulator >= step) {
		        box2dWorld.step(step, 25, 25); // update box2d world (high values stabilize stacks)
		        physicWorldObserver.step();
	            accumulator -= step;
	        }

//	        box2dWorld.step(delta, 25, 25); // update box2d world (high values stabilize stacks)
//	        physicWorldObserver.step();
	        //System.out.println(delta);
    	}
        stage.act(delta); // update game stage
        //System.out.println(delta);

        // if player 2 is a CPU it will act now
        PlayerSimulator.INSTANCE.act();

        Bird.pingBirdSpawn(birdGroup);
    }

    private double accumulator;
    private float step = 1.0f / 60.0f;


    public void pausePhysics()
    {
    	isPhysicPaused = true;
    }

    public void continuePhysics()
    {
    	//System.out.println("Continuing Physics. lastDelta: "+Gdx.graphics.getDeltaTime());
		//System.err.println("Applying "+shootDirection + " len: "+shootDirection.len()+ " ang: "+shootDirection.angle());

    	isPhysicPaused = false;
    }

    @Override
	public void dispose() {
		if (stage !=null)
			stage.dispose();
		if (box2dWorld !=null)
			box2dWorld.dispose();
	}

}
