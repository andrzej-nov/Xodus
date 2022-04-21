package com.andrzejn.xodus

import aurelienribon.tweenengine.Tween
import aurelienribon.tweenengine.TweenManager
import com.andrzejn.xodus.helper.*
import com.andrzejn.xodus.logic.Field
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.setMaxTextureSize
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import ktx.assets.Asset
import ktx.assets.loadOnDemand
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
     * Camera for drawing all screens. Simple unmoving orthographic camera for static 2D view.
     */
    lateinit var camera: OrthographicCamera

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
    }

    /**
     * Not clearly documented but working method to check whether some transition animations are in progress
     * (and ignore user input until animations complete, for example)
     */
    fun tweenAnimationRunning(): Boolean {
        return tweenManager.objects.isNotEmpty()
    }

    /**
     * Fit a sprite into given rectangle, retaining proportions
     */
    fun fitToRect(s: Sprite, wBound: Float, hBound: Float) {
        var width = wBound
        var height = wBound * s.regionHeight / s.regionWidth
        if (height > hBound) {
            height = hBound
            width = hBound * s.regionWidth / s.regionHeight
        }
        s.setSize(width, height)
    }

    /**
     * A variable for internal calculations, to reduce GC load
     */
    private val v3 = Vector3()

    /**
     * Convert the UI screen coordinates (mouse clicks or touches, for example) to the OpenGL scene coordinates
     * which are used for drawing
     */
    fun pointerPosition(screenX: Int, screenY: Int): Vector2 {
        v3.set(screenX.toFloat(), screenY.toFloat(), 0f)
        camera.unproject(v3)
        return Vector2(v3.x, v3.y)
    }

    /**
     * Ensures the index is within (0  until fieldSize), wrapping through another side as necessary.
     */
    fun clipWrap(c: Int): Int {
        val fieldSize = gs.fieldSize
        if (c < 0)
            return c + (-(c + 1) / fieldSize + 1) * fieldSize
        if (c >= fieldSize)
            return c - (c / fieldSize) * fieldSize
        return c
    }

    /**
     * Initialize the camera, batch and drawer that draw screens
     */
    fun initBatch() {
        if (this::batch.isInitialized) // Check if the lateinit property has been initialised already
            batch.dispose()
        batch = PolygonSpriteBatch()
        camera = OrthographicCamera()
        setCamera(Gdx.graphics.width, Gdx.graphics.height)
        sd = ShapeDrawer(batch, white) // A single-pixel texture provides the base color.
        // Then actual colors are specified on the drawing methon calls.
    }

    /**
     * Update camera on screen resize
     */
    fun setCamera(width: Int, height: Int) {
        camera.setToOrtho(false, width.toFloat(), height.toFloat())
        camera.update()
        batch.projectionMatrix = camera.combined
    }

    private lateinit var atlas: Asset<TextureAtlas>

    /**
     * (Re)load the texture resources definition. In this application we have all textures in the single small PNG
     * picture, so there is just one asset loaded, and loaded synchronously (it is simpler, and does not slow down
     * app startup noticeably)
     */
    fun reloadAtlas() {
        atlas = AssetManager().loadOnDemand("Main.atlas", TextureAtlasLoader.TextureAtlasParameter(false))
    }

    /**
     * Returns reference to particular texture region (sprite image) from the PNG image
     */
    private fun texture(regionName: String): TextureRegion = atlas.asset.findRegion(regionName)

    private val white: TextureRegion get() = texture("white")
    val settings: TextureRegion get() = texture("settings")
    val play: TextureRegion get() = texture("play")
    val exit: TextureRegion get() = texture("exit")
    val info: TextureRegion get() = texture("info")
    val help: TextureRegion get() = texture("help")
    val gear: TextureRegion get() = texture("gear")
    val darktheme: TextureRegion get() = texture("darktheme")
    val lighttheme: TextureRegion get() = texture("lighttheme")
    val icongmail: TextureRegion get() = texture("icongmail")
    val icontelegram: TextureRegion get() = texture("icontelegram")
    val icongithub: TextureRegion get() = texture("icongithub")
    val chaos: TextureRegion get() = texture("chaos")
    val logo: TextureRegion get() = texture("logo")

    /**
     * Create a bitmap font with given size, base color etc. from the provided TrueType font.
     * It is more convenient than keep a lot of fixed font bitmaps for different resolutions.
     */
    fun createFont(height: Int): BitmapFont {
        with(FreeTypeFontGenerator(Gdx.files.internal("ADYS-Bold_V5.ttf"))) {
            // Required for same devices like Xiaomi, where the default 1024 causes garbled fonts
            setMaxTextureSize(2048)
            val font = generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().apply {
                size = height
                color = Color.WHITE
                minFilter = Texture.TextureFilter.Linear
                magFilter = Texture.TextureFilter.Linear
                characters =
                    "\u0000ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890\"!`?'.,;:()[]{}<>|/@\\^$€-%+=#_&~*"
            })
            dispose()
            return font
        } // don't forget to dispose the font later to avoid memory leaks!
    }

    /**
     * Light (bright) colors palette for the tile lines
     */
    private val light: Array<Color> = arrayOf(
        Color.GRAY,
        Color(0xd1b153ff.toInt()),
        Color(0x6d9edbff),
        Color(0x8ab775ff.toInt()),
        Color(0xc260ffff.toInt()),
        Color(0xd68a00ff.toInt()),
        Color(0xdc4125ff.toInt()),
        Color(0x20bfbfff),
    )

    /**
     * Darker colors palette for the tile lines' edges
     */
    private val dark: Array<Color> = arrayOf(
        Color.DARK_GRAY,
        Color(0xaf8000ff.toInt()),
        Color(0x2265bcff),
        Color(0x38761dff),
        Color(0x8600afff.toInt()),
        Color(0xc86006ff.toInt()),
        Color(0x95200cff.toInt()),
        Color(0x286d6dff)
    )

    /**
     * The UI colors theme
     */
    data class Theme(
        val screenBackground: Color,
        val settingSelection: Color,
        val settingItem: Color,
        val settingSeparator: Color,
        val gameboardBackground: Color,
        val creditsText: Color,
        val scorePoints: Color,
        val scoreMoves: Color,
        val light: Array<Color>,
        val dark: Array<Color>,
        val eyeColor: Color,
        val gameBorders: Color,
        val ballColor: Color,
        val cellHilight: Color
    )

    private val lt: Theme = Theme(
        screenBackground = Color.GRAY,
        settingSelection = Color.LIGHT_GRAY,
        settingItem = Color.DARK_GRAY,
        settingSeparator = Color.DARK_GRAY,
        gameboardBackground = Color.LIGHT_GRAY,
        creditsText = Color.NAVY,
        scorePoints = Color(Color.CHARTREUSE).apply { a = 0.7f },
        scoreMoves = Color(Color.GOLD).apply { a = 0.7f },
        light = this.dark,
        dark = this.light,
        eyeColor = Color.GRAY,
        gameBorders = Color.GRAY,
        ballColor = Color.WHITE,
        cellHilight = Color.DARK_GRAY
    )

    private val dk: Theme = Theme(
        screenBackground = Color.DARK_GRAY,
        settingSelection = Color.GRAY,
        settingItem = Color.LIGHT_GRAY,
        settingSeparator = Color.LIGHT_GRAY,
        gameboardBackground = Color.BLACK,
        creditsText = Color.WHITE,
        scorePoints = Color(Color.CHARTREUSE).apply { a = 0.7f },
        scoreMoves = Color(Color.GOLD).apply { a = 0.7f },
        light = this.light,
        dark = this.dark,
        eyeColor = Color.GRAY,
        gameBorders = Color.DARK_GRAY,
        ballColor = Color.GRAY,
        cellHilight = Color.LIGHT_GRAY
    )

    /**
     * The current UI colors theme
     */
    lateinit var theme: Theme

    /**
     * Set color theme according to current game setting value
     */
    fun setTheme() {
        theme = if (gs.isDarkTheme) dk else lt
    }

    /**
     * Cleanup
     */
    fun dispose() {
        if (this::batch.isInitialized)
            batch.dispose()
        score.dispose()
    }

}