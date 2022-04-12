package com.andrzejn.xodus

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFontCache
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.Align
import ktx.app.KtxScreen
import kotlin.math.min

/**
 * The Home/Settings screen.
 * First screen of the application. Displayed by the Main class after the application is created.
 * (unless there is a saved game, then we go directly to the game screen with the resumed game).
 */
class HomeScreen(
    /**
     * Reference to the main scenn context
     */
    val ctx: Context
) : KtxScreen {
    private val ia = IAdapter()
    private var fontItems: BitmapFont = BitmapFont()
    private lateinit var fcItems: BitmapFontCache

    /**
     * Called by the GDX framework on screen change to this screen. When Home screen is shown,
     * we clear the saved game (going Home means the current game is abandoned)
     * and switch on the screen input processor.
     */
    override fun show() {
        super.show()
        ctx.sav.clearSavedGame()
        Gdx.input.inputProcessor = ia
    }

    /**
     * Called by GDX runtime on screen hide.
     */
    override fun hide() {
        super.hide()
        Gdx.input.inputProcessor = null // Detach the input processor
    }

    private val logo = Sprite(ctx.logo)
    private val play = Sprite(ctx.play)
    private val exit = Sprite(ctx.exit)
    private val options = Sprite(ctx.options)
    private val gear = Sprite(ctx.gear)
    private val darktheme = Sprite(ctx.darktheme)
    private val lighttheme = Sprite(ctx.lighttheme)

    private var gridX = 0f
    private var gridY = 0f
    private val lineWidth = 2f
    private var radius = 0f
    private var baseX = 0f
    private var baseWidth = 0f

    /**
     * Called by the GDX framework on screen resize (window resize, device rotation). Triggers all subsequent
     * coordinates recalculations and layout changes.
     */
    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        ctx.setCamera(width, height)
        val baseHeight = ctx.camera.viewportHeight
        baseWidth = min(ctx.camera.viewportWidth, baseHeight * 3 / 4)
        baseX = (ctx.camera.viewportWidth - baseWidth) / 2
        gridX = baseWidth / 12
        gridY = baseHeight / 8
        radius = min(2 * gridX, gridY) * 0.4f

        ctx.fitToRect(logo, baseWidth, 2 * gridY * 0.8f)
        logo.setPosition(
            (baseWidth - logo.width) / 2 + baseX,
            gridY * 7 - logo.height / 2
        )

        ctx.fitToRect(darktheme, 3 * gridX * 0.7f, gridY * 0.7f)
        darktheme.setPosition(
            4 * gridX - darktheme.width / 2 + baseX,
            gridY * 2 + (gridY - darktheme.height) / 2
        )
        ctx.fitToRect(lighttheme, 3 * gridX * 0.7f, gridY * 0.7f)
        lighttheme.setPosition(
            8 * gridX - lighttheme.width / 2 + baseX,
            gridY * 2 + (gridY - lighttheme.height) / 2
        )

        fontItems.dispose()
        fontItems = ctx.createFont((gridY * 0.3f).toInt())
        fcItems = BitmapFontCache(fontItems)
        fcItems.addText("1.", baseX * 0.2f, gridY * 5 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("2.", baseX * 0.2f, gridY * 4 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("3.", baseX * 0.2f, gridY * 3 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("4.", baseX * 0.2f, gridY * 2 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.setColors(ctx.theme.settingItem)

        ctx.fitToRect(gear, 2 * gridX * 0.5f, gridY * 0.5f)
        ctx.fitToRect(play, 4 * gridX * 0.8f, 2 * gridY * 0.8f)
        play.setPosition(
            6 * gridX - play.width / 2 + baseX,
            (2 * gridY - play.height) / 2
        )
        ctx.fitToRect(exit, 2 * gridX * 0.8f, gridY * 0.8f)
        exit.setPosition(
            11 * gridX - exit.width / 2 + baseX,
            (gridY - exit.height) / 2
        )
        ctx.fitToRect(options, 2 * gridX * 0.8f, gridY * 0.8f)
        options.setPosition(
            gridX - options.width / 2 + baseX,
            (gridY - options.height) / 2
        )
    }

    /**
     * Called by the system each time when the screen needs to be redrawn. It is invoked very frequently,
     * especially when animations are running, so do not create any objects here and precalculate everything
     * as much as possible.
     */
    override fun render(delta: Float) {
        super.render(delta)
        ctx.batch.begin()
        ctx.sd.filledRectangle(0f, 0f, ctx.camera.viewportWidth, ctx.camera.viewportHeight, ctx.theme.screenBackground)
        ctx.sd.rectangle(baseX, 0f, baseWidth, ctx.camera.viewportHeight, ctx.theme.settingSeparator)
        logo.draw(ctx.batch)
        renderGameSettings()

        darktheme.draw(ctx.batch)
        lighttheme.draw(ctx.batch)
        if (baseX / fontItems.lineHeight > 15f / 22f)
            fcItems.draw(ctx.batch)

        ctx.sd.line(
            baseX,
            6 * gridY,
            12 * gridX + baseX,
            6 * gridY,
            ctx.theme.settingSeparator,
            lineWidth
        )
        ctx.sd.line(
            baseX,
            1.9f * gridY,
            12 * gridX + baseX,
            1.9f * gridY,
            ctx.theme.settingSeparator,
            lineWidth
        )

        gear.setPosition(-gear.width / 2 + baseX, 6 * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        gear.setPosition(12 * gridX - gear.width / 2 + baseX, 6 * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        gear.setPosition(-gear.width / 2 + baseX, 1.9f * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        gear.setPosition(12 * gridX - gear.width / 2 + baseX, 1.9f * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        play.draw(ctx.batch)
        exit.draw(ctx.batch)
        options.draw(ctx.batch)
        ctx.batch.end()
    }

    /**
     * Render current game settings. When clicked/pressed, the settings changes are immediately saved and displayed.
     */
    private fun renderGameSettings() {
        var y = gridY * 3.05f
        var x = gridX * ((if (ctx.gs.colorsCount == 6) 3f else 7f) - 0.2f) + baseX
        ctx.sd.filledRectangle(
            x,
            y,
            2.4f * gridX,
            gridY * 0.9f,
            0f,
            ctx.theme.settingSelection, ctx.theme.settingSelection
        )

        y -= gridY
        x = gridX * ((if (ctx.gs.isDarkTheme) 3f else 7f) - 0.2f) + baseX
        ctx.sd.filledRectangle(
            x,
            y,
            2.4f * gridX,
            gridY * 0.9f,
            0f,
            ctx.theme.settingSelection, ctx.theme.settingSelection
        )
    }

    /**
     * The input adapter for this screen
     */
    inner class IAdapter : InputAdapter() {

        /**
         * Process clicks/presses. Change the settings as selected, or switch to another screen
         * (at the end of the method)
         */
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val v = ctx.pointerPosition(Gdx.input.x, Gdx.input.y)
            if (v.x < 0 || v.y < 0)
                return super.touchDown(screenX, screenY, pointer, button)
            v.x -= baseX

            if (v.y in 3 * gridY..4 * gridY) {
                if (v.x in 3 * gridX..5 * gridX)
                    ctx.gs.colorsCount = 6
                else if (v.x in 7 * gridX..9 * gridX)
                    ctx.gs.colorsCount = 7
            } else if (v.y in 2 * gridY..3 * gridY) {
                if (v.x in 3 * gridX..5 * gridX) {
                    ctx.gs.isDarkTheme = true
                    ctx.setTheme()
                } else if (v.x in 7 * gridX..9 * gridX) {
                    ctx.gs.isDarkTheme = false
                    ctx.setTheme()
                }
            } else if (v.y < 2 * gridY && v.x in 5 * gridX..7 * gridX) {
                ctx.game.getScreen<GameScreen>().newGame(false)
                ctx.game.setScreen<GameScreen>()
            } else if (v.y < gridY && v.x > 10 * gridX)
                Gdx.app.exit()
            else if (v.y < gridY && v.x < 2 * gridX)
                ctx.game.setScreen<CreditsScreen>()
            return super.touchDown(screenX, screenY, pointer, button)
        }

    }
}