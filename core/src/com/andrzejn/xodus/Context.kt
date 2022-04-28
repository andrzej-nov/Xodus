package com.andrzejn.xodus

import aurelienribon.tweenengine.Tween
import aurelienribon.tweenengine.TweenManager
import com.andrzejn.xodus.helper.*
import com.andrzejn.xodus.logic.Blot
import com.andrzejn.xodus.logic.Field
import com.andrzejn.xodus.logic.Shredder
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.StringBuilder
import com.badlogic.gdx.utils.viewport.*
import space.earlygrey.shapedrawer.ShapeDrawer

/**
 * Holds all application-wide objects.
 * Singleton objects cause a lot of issues on Android because of its memory allocation/release strategy,
 * so everything should be passed in the Context object on each app object creation or method call
 * where it is needed.
 */
@Suppress("ArrayInDataClass", "KDocMissingDocumentation")
class Context(
    /**
     * Reference to the Main game object. Needed to switch game screens on different points of execution.
     */
    val game: Main
) {
    /**
     * The batch for drawing all screen contents.
     */
    lateinit var batch: PolygonSpriteBatch

    /**
     * The main screen viewport
     */
    val screen: ScreenViewport = ScreenViewport()

    /**
     * Viewport for the game field
     */
    lateinit var field: ScalingViewport

    /**
     * Graphics resources
     */
    var a: Atlas = Atlas()

    /**
     * Drawer for geometric shapes on the screens
     */
    lateinit var sd: ShapeDrawer

    /**
     * The main object that handles all animations
     */
    val tweenManager: TweenManager = TweenManager()

    /**
     * The game settings
     */
    val gs: GameSettings = GameSettings()

    /**
     * Various coordinate processing and translations methods
     */
    var cp: CoordProcessor = CoordProcessor(this)

    /**
     * The game score tracker
     */
    val score: Score = Score(this)

    /**
     * The game save/load handler
     */
    val sav: SaveGame = SaveGame(this)

    init { // Need to specify which objects' properties will be used for animations
        Tween.registerAccessor(FloatingTile::class.java, FloatingTileAccessor())
        Tween.registerAccessor(Field::class.java, FieldAccessor())
        Tween.registerAccessor(Blot::class.java, BlotAccessor())
        Tween.registerAccessor(Shredder::class.java, ShredderAccessor())
        Tween.registerAccessor(Vector3::class.java, Vector3Accessor())
        Tween.registerAccessor(Sprite::class.java, SpriteAccessor())
    }

    /**
     * Not clearly documented but working method to check whether some transition animations are in progress
     * (and ignore user input until animations complete, for example)
     */
    fun tweenAnimationRunning(): Boolean {
        return tweenManager.objects.isNotEmpty()
    }

    /**
     * Initialize the camera, batch and drawer that draw screens
     */
    fun initBatch() {
        if (this::batch.isInitialized) // Check if the lateinit property has been initialised already
            batch.dispose()
        batch = PolygonSpriteBatch()
        setScreenSize(Gdx.graphics.width, Gdx.graphics.height)
        sd = ShapeDrawer(batch, a.white) // A single-pixel texture provides the base color.
        // Then actual colors are specified on the drawing methon calls.
    }

    /**
     * A convenience shortcut
     */
    val fieldCamPos: Vector3 get() = this.field.camera.position

    /**
     * Deserialized value
     */
    private val savedCamPos = Vector2()

    private var deserializedScreenWidth: Int = 0

    /**
     * Update camera on screen resize
     */
    fun setScreenSize(width: Int, height: Int) {
        screen.update(width, height, true)
    }

    /**
     * Sets the game field viewport size and position
     */
    fun setFieldSize(basePos: Vector2) {
        val prevWorldsSize = if (this::field.isInitialized) field.worldWidth else -1f
        val basePosChanged =
            if (this::field.isInitialized) cp.wholeFieldSize != prevWorldsSize || field.screenX != basePos.x.toInt() || field.screenY != basePos.y.toInt()
            else true
        if (basePosChanged) {
            if (savedCamPos == Vector2.Zero && this::field.isInitialized && prevWorldsSize > 0) {
                savedCamPos.set(fieldCamPos.x, fieldCamPos.y).scl(cp.wholeFieldSize / prevWorldsSize)
            }
            field = ScalingViewport(Scaling.none, cp.wholeFieldSize, cp.wholeFieldSize)
            field.setScreenBounds(
                basePos.x.toInt(),
                basePos.y.toInt(),
                cp.wholeFieldSize.toInt(),
                cp.wholeFieldSize.toInt()
            )
        }
        if (basePosChanged || savedCamPos != Vector2.Zero) {
            if (savedCamPos != Vector2.Zero) {
                if (deserializedScreenWidth != 0) {
                    savedCamPos.scl(cp.wholeFieldSize / deserializedScreenWidth)
                    deserializedScreenWidth = 0
                }
                fieldCamPos.set(savedCamPos, 0f)
                savedCamPos.set(Vector2.Zero)
            } else
                centerFieldCamera()
        }
    }

    /**
     * Center camera on the field viewport
     */
    fun centerFieldCamera() {
        if (this::field.isInitialized) // Check if the lateinit property has been initialised already
            field.camera.position.set(cp.wholeFieldSize / 2, cp.wholeFieldSize / 2, 0f)
    }

    fun drawToScreen() {
        screen.apply()
        batch.projectionMatrix = screen.camera.combined
    }

    fun drawToField() {
        field.apply()
        batch.projectionMatrix = field.camera.combined
    }


    /**
     * The current UI colors theme
     */
    lateinit var theme: Atlas.Theme

    /**
     * Set color theme according to current game setting value
     */
    fun setTheme() {
        theme = if (gs.isDarkTheme) a.dk else a.lt
    }

    /**
     * Cleanup
     */
    fun dispose() {
        if (this::batch.isInitialized)
            batch.dispose()
        score.dispose()
    }

    /**
     * Serialize the field viewport/camera settings
     */
    fun serialize(sb: StringBuilder) {
        sb.append(fieldCamPos.x.toInt(), 4).append(fieldCamPos.y.toInt(), 4).append(field.screenWidth, 4)
    }


    /**
     * Deserialize the field viewport/camera settings
     */
    fun deserialize(s: String, i: Int): Int {
        savedCamPos.set(s.substring(i..i + 3).toFloat(), s.substring(i + 4..i + 7).toFloat())
        deserializedScreenWidth = s.substring(i + 8..i + 11).toInt()
        return i + 12
    }

}