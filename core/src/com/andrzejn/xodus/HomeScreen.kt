package com.andrzejn.xodus

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFontCache
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.Align
import ktx.app.KtxScreen
import ktx.app.clearScreen
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
    private var fontSettings: BitmapFont = BitmapFont()
    private lateinit var fcNum: BitmapFontCache

    /**
     * Called by the GDX framework on screen change to this screen. When Home screen is shown,
     * we clear the saved game (going Home means the current game is abandoned)
     * and switch on the screen input processor.
     */
    override fun show() {
        super.show()
        Gdx.input.inputProcessor = ia
        Gdx.input.setCatchKey(Input.Keys.BACK, true) // Override the Android 'Back' button
        anySettingChanged = false
    }

    /**
     * Called by GDX runtime on screen hide.
     */
    override fun hide() {
        super.hide()
        Gdx.input.inputProcessor = null // Detach the input processor
        Gdx.input.setCatchKey(Input.Keys.BACK, false) // Override the Android 'Back' button
    }

    private val logo = Sprite(ctx.a.logo)
    private val play = Sprite(ctx.a.play)
    private val resume = Sprite(ctx.a.resume)
    private val exit = Sprite(ctx.a.exit)
    private val info = Sprite(ctx.a.info)
    private val gear = Sprite(ctx.a.gear)
    private val darktheme = Sprite(ctx.a.darktheme)
    private val lighttheme = Sprite(ctx.a.lighttheme)

    private val sizearrows = Sprite(ctx.a.sizearrows)
    private val chaos = Sprite(ctx.a.chaos)
    private val recycle = Sprite(ctx.a.recycle)
    private val death = Sprite(ctx.a.death)
    private val shredder = Sprite(ctx.a.shredder)

    private var gridX = 0f
    private var gridY = 0f
    private val lineWidth = 2f
    private var baseX = 0f
    private var baseWidth = 0f
    private var anySettingChanged = false

    /**
     * Called by the GDX framework on screen resize (window resize, device rotation). Triggers all subsequent
     * coordinates recalculations and layout changes.
     */
    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        if (width == 0 || height == 0) // Window minimize on desktop works that way
            return
        ctx.setScreenSize(width, height)
        val baseHeight = Gdx.graphics.height.toFloat()
        baseWidth = min(Gdx.graphics.width.toFloat(), baseHeight * 3 / 4)
        baseX = (Gdx.graphics.width - baseWidth) / 2
        gridX = baseWidth / 12
        gridY = baseHeight / 9

        ctx.cp.fitToRect(logo, baseWidth, 2 * gridY * 0.8f)
        logo.setPosition(
            (baseWidth - logo.width) / 2 + baseX,
            gridY * 8 - logo.height / 2
        )

        ctx.cp.fitToRect(sizearrows, 2 * gridX * 0.8f, gridY * 0.8f)
        sizearrows.setPosition(
            gridX * 1.1f + baseX,
            gridY * 6 + (gridY - sizearrows.height) / 2
        )
        ctx.cp.fitToRect(chaos, 2 * gridX * 0.8f, gridY * 0.8f)
        chaos.setPosition(
            gridX * 1.1f + baseX,
            gridY * 5 + (gridY - chaos.height) / 2
        )
        ctx.cp.fitToRect(shredder, 2 * gridX * 0.8f, gridY * 0.8f)
        shredder.setPosition(
            gridX * 1.1f + baseX,
            gridY * 4 + (gridY - shredder.height) / 2
        )
        ctx.cp.fitToRect(recycle, 3 * gridX * 0.8f, gridY * 0.8f)
        recycle.setPosition(
            4 * gridX - recycle.width / 2 + baseX,
            gridY * 3 + (gridY - recycle.height) / 2
        )
        ctx.cp.fitToRect(death, 3 * gridX * 0.8f, gridY * 0.8f)
        death.setPosition(
            8 * gridX - death.width / 2 + baseX,
            gridY * 3 + (gridY - death.height) / 2
        )

        ctx.cp.fitToRect(darktheme, 3 * gridX * 0.8f, gridY * 0.8f)
        darktheme.setPosition(
            4 * gridX - darktheme.width / 2 + baseX,
            gridY * 2 + (gridY - darktheme.height) / 2
        )
        ctx.cp.fitToRect(lighttheme, 3 * gridX * 0.8f, gridY * 0.8f)
        lighttheme.setPosition(
            8 * gridX - lighttheme.width / 2 + baseX,
            gridY * 2 + (gridY - lighttheme.height) / 2
        )

        fontSettings.dispose()
        fontSettings = ctx.a.createFont((min(3 * gridX, gridY) * 0.8f).toInt())
        fcNum = BitmapFontCache(fontSettings)
        fcNum.setText("7", gridX * 3.5f + baseX, gridY * 6.8f, gridX, Align.bottom, false)
        fcNum.addText("9", gridX * 6.5f + baseX, gridY * 6.8f, gridX, Align.bottom, false)
        fcNum.addText("11", gridX * 10f + baseX, gridY * 6.8f, gridX, Align.bottom, false)
        fcNum.addText("0", gridX * 3.5f + baseX, gridY * 5.8f, gridX, Align.bottom, false)
        fcNum.addText("1", gridX * 6.5f + baseX, gridY * 5.8f, gridX, Align.bottom, false)
        fcNum.addText("2", gridX * 10f + baseX, gridY * 5.8f, gridX, Align.bottom, false)
        fcNum.addText("1", gridX * 3.5f + baseX, gridY * 4.8f, gridX, Align.bottom, false)
        fcNum.addText("2", gridX * 6.5f + baseX, gridY * 4.8f, gridX, Align.bottom, false)
        fcNum.addText("3", gridX * 10f + baseX, gridY * 4.8f, gridX, Align.bottom, false)
        fcNum.setColors(ctx.theme.scorePoints)

        fontItems.dispose()
        fontItems = ctx.a.createFont((gridY * 0.3f).toInt())
        fcItems = BitmapFontCache(fontItems)
        fcItems.addText("1.", baseX * 0.2f, gridY * 6 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("2.", baseX * 0.2f, gridY * 5 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("3.", baseX * 0.2f, gridY * 4 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("4.", baseX * 0.2f, gridY * 3 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("5.", baseX * 0.2f, gridY * 2 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.setColors(ctx.theme.settingItem)

        ctx.cp.fitToRect(gear, 2 * gridX * 0.5f, gridY * 0.5f)
        ctx.cp.fitToRect(play, 4 * gridX * 0.8f, 2 * gridY * 0.8f)
        play.setPosition(
            6 * gridX - play.width / 2 + baseX,
            (2 * gridY - play.height) / 2
        )
        ctx.cp.fitToRect(resume, 4 * gridX * 0.8f, 2 * gridY * 0.8f)
        resume.setPosition(
            6 * gridX - resume.width / 2 + baseX,
            (2 * gridY - resume.height) / 2
        )
        ctx.cp.fitToRect(exit, 2 * gridX * 0.8f, gridY * 0.8f)
        exit.setPosition(
            11 * gridX - exit.width / 2 + baseX,
            (gridY - exit.height) / 2
        )
        ctx.cp.fitToRect(info, 2 * gridX * 0.8f, gridY * 0.8f)
        info.setPosition(
            gridX - info.width / 2 + baseX,
            (gridY - info.height) / 2
        )
    }

    /**
     * Called by the system each time when the screen needs to be redrawn. It is invoked very frequently,
     * especially when animations are running, so do not create any objects here and precalculate everything
     * as much as possible.
     */
    override fun render(delta: Float) {
        super.render(delta)
        with(ctx.theme.screenBackground) {
            clearScreen(r, g, b, a, true)
        }
        ctx.drawToScreen()
        ctx.batch.begin()
        ctx.sd.rectangle(baseX, 0f, baseWidth, Gdx.graphics.height.toFloat(), ctx.theme.settingSeparator)
        logo.draw(ctx.batch)
        renderGameSettings()
        fcNum.draw(ctx.batch)
        sizearrows.draw(ctx.batch)
        chaos.draw(ctx.batch)
        shredder.draw(ctx.batch)
        recycle.draw(ctx.batch)
        death.draw(ctx.batch)
        darktheme.draw(ctx.batch)
        lighttheme.draw(ctx.batch)
        if (baseX / fontItems.lineHeight > 15f / 22f)
            fcItems.draw(ctx.batch)

        ctx.sd.line(
            baseX,
            7 * gridY,
            12 * gridX + baseX,
            7 * gridY,
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
        gear.setPosition(-gear.width / 2 + baseX, 7 * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        gear.setPosition(12 * gridX - gear.width / 2 + baseX, 7 * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        gear.setPosition(-gear.width / 2 + baseX, 1.9f * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        gear.setPosition(12 * gridX - gear.width / 2 + baseX, 1.9f * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        if (isNewGame())
            play.draw(ctx.batch)
        else
            resume.draw(ctx.batch)
        exit.draw(ctx.batch)
        info.draw(ctx.batch)
        ctx.batch.end()
    }

    /**
     * Should we start new game or resume current one
     */
    private fun isNewGame() = anySettingChanged || !ctx.game.getScreen<GameScreen>().wasDisplayed

    /**
     * Render current game settings. When clicked/pressed, the settings changes are immediately saved and displayed.
     */
    private fun renderGameSettings() {
        var y = gridY * 6.05f
        var x = gridX * (when (ctx.gs.fieldSize) {
            7 -> 3f
            9 -> 6f
            else -> 9.2f
        }) + baseX
        ctx.sd.filledRectangle(
            x,
            y,
            2.2f * gridX,
            gridY * 0.9f,
            0f,
            ctx.theme.settingSelection, ctx.theme.settingSelection
        )

        y -= gridY
        x = gridX * (when (ctx.gs.chaosMoves) {
            0 -> 3f
            1 -> 6f
            else -> 9.2f
        }) + baseX
        ctx.sd.filledRectangle(
            x,
            y,
            2.2f * gridX,
            gridY * 0.9f,
            0f,
            ctx.theme.settingSelection, ctx.theme.settingSelection
        )

        y -= gridY
        x = gridX * (when (ctx.gs.shredderSpeed) {
            in 0f..0.55f -> 3f
            in 0.55f..0.7f -> 6f
            else -> 9.2f
        }) + baseX
        ctx.sd.filledRectangle(
            x,
            y,
            2.2f * gridX,
            gridY * 0.9f,
            0f,
            ctx.theme.settingSelection, ctx.theme.settingSelection
        )

        y -= gridY
        x = gridX * ((if (ctx.gs.reincarnation) 3f else 7f) - 0.2f) + baseX
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
            val v = ctx.cp.pointerPositionScreen(Gdx.input.x, Gdx.input.y)
            if (v.x < 0 || v.y < 0)
                return super.touchDown(screenX, screenY, pointer, button)
            v.x -= baseX

            if (v.y in 6 * gridY..7 * gridY) {
                when (v.x) {
                    in 2 * gridX..5 * gridX -> {
                        ctx.gs.fieldSize = 7
                        anySettingChanged = true
                    }
                    in 6 * gridX..8 * gridX -> {
                        ctx.gs.fieldSize = 9
                        anySettingChanged = true
                    }
                    in 9 * gridX..12 * gridX -> {
                        ctx.gs.fieldSize = 11
                        anySettingChanged = true
                    }
                }
            } else if (v.y in 5 * gridY..6 * gridY) {
                when (v.x) {
                    in 2 * gridX..5 * gridX -> {
                        ctx.gs.chaosMoves = 0
                        anySettingChanged = true
                    }
                    in 6 * gridX..8 * gridX -> {
                        ctx.gs.chaosMoves = 1
                        anySettingChanged = true
                    }
                    in 9 * gridX..12 * gridX -> {
                        ctx.gs.chaosMoves = 2
                        anySettingChanged = true
                    }
                }
            } else if (v.y in 4 * gridY..5 * gridY) {
                when (v.x) {
                    in 2 * gridX..5 * gridX -> {
                        ctx.gs.shredderSpeed = 1f / 2
                        anySettingChanged = true
                    }
                    in 6 * gridX..8 * gridX -> {
                        ctx.gs.shredderSpeed = 2f / 3
                        anySettingChanged = true
                    }
                    in 9 * gridX..12 * gridX -> {
                        ctx.gs.shredderSpeed = 3f / 4
                        anySettingChanged = true
                    }
                }
            } else if (v.y in 3 * gridY..4 * gridY) {
                if (v.x in 3 * gridX..5 * gridX) {
                    ctx.gs.reincarnation = true
                    anySettingChanged = true
                } else if (v.x in 7 * gridX..9 * gridX) {
                    ctx.gs.reincarnation = false
                    anySettingChanged = true
                }
            } else if (v.y in 2 * gridY..3 * gridY) {
                if (v.x in 3 * gridX..5 * gridX) {
                    ctx.gs.isDarkTheme = true
                    ctx.setTheme()
                } else if (v.x in 7 * gridX..9 * gridX) {
                    ctx.gs.isDarkTheme = false
                    ctx.setTheme()
                }
            } else if (v.y < 2 * gridY && v.x in 5 * gridX..7 * gridX) {
                if (isNewGame())
                    ctx.game.getScreen<GameScreen>().newGame(false)
                ctx.game.setScreen<GameScreen>()
            } else if (v.y < gridY && v.x > 10 * gridX)
                Gdx.app.exit()
            else if (v.y < gridY && v.x < 2 * gridX)
                ctx.game.setScreen<CreditsScreen>()
            return super.touchDown(screenX, screenY, pointer, button)
        }

        /**
         * On Android 'Back' button switch back to the Home/Settings screen instead of default action
         * (pausing the application)
         */
        override fun keyDown(keycode: Int): Boolean {
            if (keycode == Input.Keys.BACK)
                if (ctx.game.getScreen<GameScreen>().wasDisplayed)
                    ctx.game.setScreen<GameScreen>()
                else
                    Gdx.app.exit()
            return super.keyDown(keycode)
        }
    }
}