package com.andrzejn.xodus.helper

import com.andrzejn.xodus.Context
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.setMaxTextureSize
import ktx.assets.Asset
import ktx.assets.loadOnDemand

open class Atlas {
    private lateinit var atlas: Asset<TextureAtlas>
    val white: TextureRegion get() = texture("white")
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
    val hand: TextureRegion get() = texture("hand")

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
    val lt: Theme = Theme(
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

    val dk: Theme = Theme(
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
}