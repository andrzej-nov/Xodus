package com.andrzejn.xodus

import aurelienribon.tweenengine.Tween
import aurelienribon.tweenengine.TweenManager
import com.andrzejn.xodus.helper.*
import com.andrzejn.xodus.logic.Blot
import com.andrzejn.xodus.logic.Field
import com.andrzejn.xodus.logic.Shredder
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.setMaxTextureSize
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.*
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
     * The main screen viewport
     */
    val screen: ScreenViewport = ScreenViewport()

    /**
     * Viewport for the game field
     */
    lateinit var field: ScalingViewport

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
        sd = ShapeDrawer(batch, white) // A single-pixel texture provides the base color.
        // Then actual colors are specified on the drawing methon calls.
    }

    /**
     * Update camera on screen resize
     */
    fun setScreenSize(width: Int, height: Int) {
        screen.update(width, height, true)
//        batch.projectionMatrix = screen.camera.combined
    }

    /**
     * Sets the game field viewport size and position
     */
    fun setFieldSize(basePos: Vector2) {
        field = ScalingViewport(Scaling.none, cp.wholeFieldSize, cp.wholeFieldSize)
        field.setScreenBounds(
            basePos.x.toInt(),
            basePos.y.toInt(),
            cp.wholeFieldSize.toInt(),
            cp.wholeFieldSize.toInt()
        )
        centerFieldCamera()
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
     * A convenience shortcut
     */
    val fieldCamPos: Vector3 get() = this.field.camera.position


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
    val ok: TextureRegion get() = texture("ok")
    val gear: TextureRegion get() = texture("gear")
    val darktheme: TextureRegion get() = texture("darktheme")
    val lighttheme: TextureRegion get() = texture("lighttheme")
    val icongmail: TextureRegion get() = texture("icongmail")
    val icontelegram: TextureRegion get() = texture("icontelegram")
    val icongithub: TextureRegion get() = texture("icongithub")
    val chaos: TextureRegion get() = texture("chaos")
    val logo: TextureRegion get() = texture("logo")
    val sizearrows: TextureRegion get() = texture("sizearrows")
    val playblue: TextureRegion get() = texture("playblue")
    val resume: TextureRegion get() = texture("resume")

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
        val cellHilight: Color,
        val shredderYellow: Color,
        val shredderRed: Color
    )

    private val lt: Theme = Theme(
        screenBackground = Color.GRAY,
        settingSelection = Color.LIGHT_GRAY,
        settingItem = Color.DARK_GRAY,
        settingSeparator = Color.DARK_GRAY,
        gameboardBackground = Color.LIGHT_GRAY,
        creditsText = Color.NAVY,
        scorePoints = Color(Color.GOLD).apply { a = 0.7f },
        scoreMoves = Color(Color.CHARTREUSE).apply { a = 0.7f },
        light = this.dark,
        dark = this.light,
        eyeColor = Color.BLACK,
        gameBorders = Color.GRAY,
        cellHilight = Color.DARK_GRAY,
        shredderYellow = Color.GOLDENROD,
        shredderRed = Color.FIREBRICK
    )

    private val dk: Theme = Theme(
        screenBackground = Color.DARK_GRAY,
        settingSelection = Color.GRAY,
        settingItem = Color.LIGHT_GRAY,
        settingSeparator = Color.LIGHT_GRAY,
        gameboardBackground = Color.BLACK,
        creditsText = Color.WHITE,
        scorePoints = Color(Color.GOLD).apply { a = 0.7f },
        scoreMoves = Color(Color.CHARTREUSE).apply { a = 0.7f },
        light = this.light,
        dark = this.dark,
        eyeColor = Color.DARK_GRAY,
        gameBorders = Color.DARK_GRAY,
        cellHilight = Color.LIGHT_GRAY,
        shredderYellow = Color.GOLD,
        shredderRed = Color.RED
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