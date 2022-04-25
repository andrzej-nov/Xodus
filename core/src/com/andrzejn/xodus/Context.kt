package com.andrzejn.xodus

import aurelienribon.tweenengine.Tween
import aurelienribon.tweenengine.TweenManager
import com.andrzejn.xodus.helper.*
import com.andrzejn.xodus.logic.Blot
import com.andrzejn.xodus.logic.Coord
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
    private val screen: ScreenViewport = ScreenViewport()

    /**
     * Viewport for the game field
     */
    private lateinit var field: ScalingViewport

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
     * Logical field coords to screen coords offset.
     * E.g. if the field has been visually scrolled by 1 cell to the right, then scrollOffset.x = 1
     */
    val scrollOffset: Coord = Coord(0, 0)
    private val bottomLeft = Coord(0, 0)
    private val topRight = Coord(gs.fieldSize - 1, gs.fieldSize - 1)

    /**
     * Reset scroll offset to zero
     */
    fun resetScrollOffset() {
        scrollOffset.set(0, 0)
        if (this::field.isInitialized) // Check if the lateinit property has been initialised already
            field.camera.position.set(wholeFieldSize / 2, wholeFieldSize / 2, 0f)
        bottomLeft.set(0, 0)
        topRight.set(gs.fieldSize - 1, gs.fieldSize - 1)
    }

    /**
     * Applies scrolling to the field
     */
    fun scrollFieldBy(change: Coord) {
        fieldCamPos.x += sideLen * change.x
        fieldCamPos.y += sideLen * change.y
        scrollOffset.add(change)
        bottomLeft.set(0, 0)
        bottomLeft.set(fieldIndexToTileIndex(bottomLeft))
        topRight.set(gs.fieldSize - 1, gs.fieldSize - 1)
        topRight.set(fieldIndexToTileIndex(topRight))
    }

    /**
     * The field cell side length
     */
    var sideLen: Float = 0f

    /**
     * Set the tile corner screen coordinates, considering scrolling
     */
    fun setTileBasePos(crd: Coord, basePos: Vector2) {
        c.set(tileIndexToFieldIndex(crd))
        basePos.set(c.x * sideLen, c.y * sideLen)
    }

    /**
     * Convert the provided coords from the cell pointed on the screen to the logical field tile indexes.
     * Returns the converted coord for chaining.
     */
    fun fieldIndexToTileIndex(crd: Coord): Coord =
        c.set(clipWrap(crd.x - scrollOffset.x), clipWrap(crd.y - scrollOffset.y))

    /**
     * Convert the provided coords from the logical field tile indexes to the cell pointed on the screen
     * Returns the converted coord for chaining.
     */
    fun tileIndexToFieldIndex(crd: Coord): Coord =
        c.set(clipWrap(crd.x + scrollOffset.x), clipWrap(crd.y + scrollOffset.y))

    /**
     * Converts screen cell indexes to the bottom-left screen coordinates to draw the rectangle
     * Sets private var v to the same value
     */
    fun toScreenCellCorner(c: Coord): Vector2 {
        val v = toFieldCellCorner(c)
        v3.set(v.x, v.y, 0f)
        field.project(v3)
        return v.set(v3.x, v3.y)
    }

    /**
     * Variables for internal calculations to reduce the GC load
     */
    private val v = Vector2()
    private val c = Coord()

    /**
     * Converts screen cell indexes to the bottom-left screen coordinates to draw the rectangle
     * Sets private var v to the same value
     */
    fun toFieldCellCorner(c: Coord): Vector2 = v.set(c.x.toFloat(), c.y.toFloat()).scl(sideLen)

    /**
     * Returns indexes of the screen cell pointed by touch/mouse. (-1, -1) of pointed otside of the field.
     * Sets private var c to the same return value
     */
    fun toScreenIndex(v: Vector2): Coord {
        if (v.x !in field.screenX.toFloat()..field.screenX.toFloat() + wholeFieldSize ||
            v.y !in field.screenY.toFloat()..field.screenY.toFloat() + wholeFieldSize
        ) return c.unSet()
        return c.set(((v.x - field.screenX) / sideLen).toInt(), ((v.y - field.screenY) / sideLen).toInt())
    }

    /**
     * Returns indexes of the field cell pointed by touch/mouse. (-1, -1) of pointed otside of the field.
     * Sets private var c to the same return value
     */
    fun toFieldIndex(v: Vector2): Coord {
        if (v.x !in -sideLen..wholeFieldSize + sideLen || v.y !in -sideLen..wholeFieldSize + sideLen) return c.unSet()
        return c.set((v.x / sideLen).toInt(), (v.y / sideLen).toInt())
    }

    /**
     * The whole field square size, in pixels
     */
    var wholeFieldSize: Float = 0f

    /**
     * Variable for internal calculations, to reduce GC load
     */
    private val vrend = Vector2()

    /**
     * Perform given render at given coordinates, and repeat it at respective duplicate positions
     * if it is at the field border tile
     */
    fun renderWithFieldBorders(v: Vector2, c: Coord, renderIt: (Vector2) -> Unit) {
        var xAtBorder = false
        renderIt(v)
        if (c.x == bottomLeft.x) {
            xAtBorder = true
            renderIt(vrend.set(v).add(wholeFieldSize, 0f))
        } else if (c.x == topRight.x) {
            xAtBorder = true
            renderIt(vrend.set(v).add(-wholeFieldSize, 0f))
        }
        if (xAtBorder) {
            if (c.y == bottomLeft.y)
                renderIt(vrend.add(0f, wholeFieldSize))
            else if (c.y == topRight.y)
                renderIt(vrend.add(0f, -wholeFieldSize))
        }
        if (c.y == bottomLeft.y)
            renderIt(vrend.set(v).add(0f, wholeFieldSize))
        else if (c.y == topRight.y)
            renderIt(vrend.set(v).add(0f, -wholeFieldSize))
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
     * Ensures the float index is within (0  until fieldSize), wrapping through another side as necessary.
     */
    fun clipWrap(x: Float): Float {
        val fieldSize = gs.fieldSize
        if (x < 0)
            return x + ((-(x + 1) / fieldSize).toInt() + 1) * fieldSize
        if (x >= fieldSize)
            return x - (x / fieldSize).toInt() * fieldSize
        return x
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
        field = ScalingViewport(Scaling.none, wholeFieldSize, wholeFieldSize)
        field.camera.position.set(Vector2(wholeFieldSize / 2, wholeFieldSize / 2), 0f)
        field.setScreenBounds(basePos.x.toInt(), basePos.y.toInt(), wholeFieldSize.toInt(), wholeFieldSize.toInt())
    }


    /**
     * A convenience shortcut
     */
    val fieldCamPos: Vector3 get() = this.field.camera.position

    /**
     * A variable for internal calculations, to reduce GC load
     */
    private val v3 = Vector3()
    private val vpp = Vector2()
    private val vppf = Vector2()

    /**
     * Convert the UI screen coordinates (mouse clicks or touches, for example) to the OpenGL scene coordinates
     * which are used for drawing
     */
    fun pointerPositionScreen(screenX: Int, screenY: Int): Vector2 {
        v3.set(screenX.toFloat(), screenY.toFloat(), 0f)
        screen.unproject(v3)
        return vpp.set(v3.x, v3.y)
    }

    /**
     * Convert the UI screen coordinates (mouse clicks or touches, for example) to the OpenGL field coordinates
     */
    fun pointerPositionField(screenX: Int, screenY: Int): Vector2 {
        v3.set(screenX.toFloat(), screenY.toFloat(), 0f)
        field.unproject(v3)
        return vppf.set(v3.x, v3.y)
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

    fun drawToScreen() {
        screen.apply()
        batch.projectionMatrix = screen.camera.combined
    }

    fun drawToField() {
        field.apply()
        batch.projectionMatrix = field.camera.combined
    }

}