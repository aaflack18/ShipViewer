package com.avanti.screens;

import com.avanti.shipviewer.ShipViewer;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class ShipViewScreen extends ButtonScreenAdapter implements InputProcessor{
	private Button startButton;
	TiledMap tiledMap; //our map, on which things will exist
	PerspectiveCamera camera; //the camera
	TiledMapRenderer tiledMapRenderer; //the thing that renders our map
	private Vector3 target, tileDragOriginLoc; //target isn't used right now, tileDragTarget is
	boolean wasDragged = false;
	private  ModelInstance instance;
	private Environment env;

	private boolean loading;
	private ModelBatch mBatch;
	private Matrix4 blockLocTarget;
	private Vector3 blockLoc;
	private Model shipModel;

	public ShipViewScreen(ShipViewer gameInstance) {
		super(gameInstance);
		camera = new PerspectiveCamera(65, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); //persepctive camera with a fieldOfView of 65 degrees I think
		camera.update(); //update it
		camera.position.set(0f, -200f, 100f); //sets it -100 units back, and 150 units above the bottom-left corner of the world
		camera.lookAt(0f, 0f, 0f); //from the camera's location, look at the bottom-left corner of the world
		camera.near = 0.05f;  //render things between .1f to 4000f stuff away
		camera.far = 4000.0f;

		target = camera.position.cpy();

		tileDragOriginLoc = camera.position.cpy(); //set tileDragOriginLoc location

		tiledMap = new TmxMapLoader().load("level.tmx"); //loads the level.tmx file. Needs level.jpg and level.tmx to work, kinda like a sprite sheet
		tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap); //funny considering we aren't using an orthogonal camera
		Gdx.input.setInputProcessor(this);

		ShipViewer.assetManager.load("ship.obj", Model.class);
		loading = true; //asset manager is currently loading asynchronously

		env = new Environment(); //an environment for the block
		env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f)); //adds an ambient light so yeah
		env.add(new DirectionalLight().set(ShipViewer.selectedColor, -1f, -0.8f, -0.2f)); //adds a directional  light 
		//instance.transform = (blockLocTarget); //adjusts the position

		mBatch = new ModelBatch();
	}
	
	public void doneLoading(){
		shipModel  = ShipViewer.assetManager.get("ship.obj", Model.class);
		instance = new ModelInstance(shipModel);
		blockLocTarget = instance.transform.cpy();
		blockLocTarget.setTranslation(new Vector3(16f, 16f, 16f));
		blockLocTarget.scale(10f, 10f, 10f);
		
		loading = false;
	}
	
	@Override
	public void show() {
		
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
		
		batch.begin();
		renderBackground(delta);
		batch.end();
		
		tiledMapRenderer.setView(camera.combined, 0f, 0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		tiledMapRenderer.render();
		if (loading){
			if (ShipViewer.assetManager.update())
				doneLoading();
		}
		else{
			
			mBatch.begin(camera);
			mBatch.render(instance, env);

			instance.transform.lerp(blockLocTarget, 6f*Gdx.graphics.getDeltaTime());
			mBatch.end();
		}
		

		//depending on what we do, we may use these but for dragging the map around, we don't need it.

		camera.position.lerp(target, 4f*Gdx.graphics.getDeltaTime());
		//instance.transform.lerp(instance.transform.setTranslation(blockLocTarget), Gdx.graphics.getDeltaTime());
		
		//camera.position.set(target);
		camera.update();
	}

	@Override
	public void resize(int width, int height) {
		if (buttonStage == null)
			buttonStage = new Stage();
		buttonStage.clear();
		initializeButtons();
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void hide() {

	}

	@Override
	protected void initializeButtons() {
		atlas = new TextureAtlas(Gdx.files.internal("playButton.atlas"));
		buttonSkin = new Skin(atlas);
		style = new ButtonStyle(buttonSkin.getDrawable("buttonUnpressed"), buttonSkin.getDrawable("buttonPressed"), buttonSkin.getDrawable("buttonPressed"));

		startButton = new Button(style);
		startButton.setWidth(MENU_BUTTON_WIDTH);
		startButton.setHeight(MENU_BUTTON_HEIGHT);
		startButton.setX(EDGE_TOLERANCE);
		startButton.setY(EDGE_TOLERANCE);
		startButton.addListener(new InputListener() {
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				return true;
			}

			public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
				gameInstance.setScreen(new MenuScreen(gameInstance));
				dispose();
			}
		});
		buttonStage.addActor(startButton);
		inputMultiplexer.addProcessor(buttonStage);
	}

	public void dispose() {
		super.dispose();
	}

	@Override
	public boolean keyDown(int keycode) {
		
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		if(keycode == Input.Keys.LEFT){
			target.add(-32f, 0f, 0f);
			blockLocTarget.translate(-32f, 0f, 0f);
		}
		if(keycode == Input.Keys.RIGHT){
			target.add(32f, 0f, 0f);
			blockLocTarget.translate(32f, 0f, 0f);
		}
		if(keycode == Input.Keys.UP){
			target.add(0f, 32f, 0f);
			blockLocTarget.translate(0f, 32f, 0f);
		}
		if(keycode == Input.Keys.DOWN){
			target.add(0f, -32f, 0f);
			blockLocTarget.translate(0f, -32f, 0f);
		}
		if(keycode == Input.Keys.SPACE){
			blockLocTarget.rotate(blockLocTarget.getTranslation(new Vector3()), 25f);
		}
		System.out.println("Camera: " + camera.position); //cam pos
		return false;
	}

	@Override
	public boolean keyTyped(char character) {

		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		//On touchdown, it picks a tileDragTarget as a starting reference point.

		wasDragged = false; //set wasDragged to false because we didn't start dragging across the screen yet
		//The next three lines converts screenCoords to worldMap coords
		Ray ray = camera.getPickRay(screenX, screenY); //origin and direction
		float scale = -camera.position.z/ray.direction.z; //scale it
		tileDragOriginLoc = ray.direction.scl(scale).cpy().add(camera.position); //make it so it touches the worldMap, and add the original camera position since it was pushed x:0 y:-100 z:250

		System.out.println("Touching down " + tileDragOriginLoc); //starting dragTileTarget print
		if (((TiledMapTileLayer)tiledMap.getLayers().get(0)).getCell((int)tileDragOriginLoc.x/32, (int) tileDragOriginLoc.y/32).getTile().getProperties().get("blocked") != null)
			System.out.println("BLOCKED TILE"); //just says that it's a blocked tile if it is.... BLOCKED TILES ARE THE MOSTLY PICK ONE WITH A HINT OF YELLOW AT THE BOTTOM RIGHT CORNER like at (0, 0)

		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		//This is to lerp to targets on the map

		if (!wasDragged){ //if dragging didn't happen then don't do this.
			Ray ray = camera.getPickRay(screenX, screenY); //get origin to direction
			float scale = -camera.position.z/ray.direction.z; //scale it
			Vector3 temp = ray.direction.scl(scale); //touch the map




			temp.add(camera.position);
			blockLoc = temp.cpy().add(0f, 0f, 16f); //16f for block heigh adjustment
			blockLoc.set(blockLoc.x - blockLoc.x%32, blockLoc.y - blockLoc.y%32, 16f);
			blockLoc.add(16f, 16f, 0f);

			blockLocTarget.setTranslation(blockLoc);
			//instance.transform.setTranslation(blockLocTarget.add(16f, 16f, 0f));

			temp.z = 150f; //back to normal camera height
			temp.y = temp.y - 100f; //adjust for camera's -100
			target = temp.cpy();


		}
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		wasDragged = true; //because, well... yeah.

		//Same deal as touchup
		Ray ray = camera.getPickRay(screenX, screenY);
		float scale = -camera.position.z/ray.direction.z;
		Vector3 temp3 = ray.direction.scl(scale);
		temp3.add(camera.position);


		Vector3 diff = new Vector3(-tileDragOriginLoc.x + temp3.x, -tileDragOriginLoc.y + temp3.y, 0f);// Get the difference between tileDragTarget and this new location, z is zero cuz we don't want the camera height  to change
		// NewLoc MINUS tileDragTarget location 
		target.set(camera.position.cpy().sub(diff)); //set the camera's position to the opposite direction of diff, I actually don't understand my own shit
		System.out.println(diff);//shows distance from tileDragTarget (the initial touchDown location)
		return true;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		/*Vector3 lookat = camera.position;
	    	camera.position.set(camera.position.cpy().add(0f, 0f, amount*20f));
	    	camera.lookAt(lookat);*/

		Vector3 lookat = camera.position; //whatever it was looking at earlier, save it
		target.set(camera.position.cpy().add(0f, 0f, amount*55f)); //change height of the camera
		camera.lookAt(lookat); //make it look at the same shit
		return false;
	}
}
