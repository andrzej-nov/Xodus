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

/**
 * The simplest screen. Just displays the credits and opens respective links.
 */
class CreditsScreen(
    /**
     * Reference to the main app context
     */
    val ctx: Context
) : KtxScreen {
    private val ia = IAdapter()
    private var font: BitmapFont = BitmapFont()
    private lateinit var fcText: BitmapFontCache
    private lateinit var fcTime: BitmapFontCache

    /**
     * Called by GDX runtime on screen show
     */
    override fun show() {
        super.show()
        Gdx.input.inputProcessor = ia // Attach the input processor
        Gdx.input.setCatchKey(Input.Keys.BACK, true) // Override the Android 'Back' button
    }

    /**
     * Called by GDX runtime on screen hide.
     * Uncatch the 'Back' button processing. The base screen will switch off the input processor
     */
    override fun hide() {
        super.hide()
        Gdx.input.inputProcessor = null // Detach the input processor
        Gdx.input.setCatchKey(Input.Keys.BACK, false)
    }

    private var gridX = 0f
    private var gridY = 0f
    private val logo = Sprite(ctx.logo)
    private val icongmail = Sprite(ctx.icongmail)
    private val icontelegram = Sprite(ctx.icontelegram)
    private val icongithub = Sprite(ctx.icongithub)
    private val exit = Sprite(ctx.exit)
    private val settings = Sprite(ctx.settings)

    /**
     * Invoked on each screen resize
     */
    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        gridX = Gdx.graphics.width.toFloat()
        gridY = Gdx.graphics.height.toFloat()

        ctx.cp.fitToRect(logo, Gdx.graphics.width.toFloat(), 2 * gridY * 0.8f)
        logo.setPosition(
            (Gdx.graphics.width - logo.width) / 2,
            gridY * 8 - logo.height / 2
        )
        ctx.cp.fitToRect(icongmail, gridX * 0.9f, gridY * 0.9f)
        icongmail.setPosition(
            gridX - icongmail.width / 2,
            gridY * 5 + (gridY - icongmail.height) / 2
        )
        ctx.cp.fitToRect(icontelegram, gridX * 0.9f, gridY * 0.9f)
        icontelegram.setPosition(
            gridX - icontelegram.width / 2,
            gridY * 4 + (gridY - icontelegram.height) / 2
        )
        ctx.cp.fitToRect(icongithub, gridX * 0.9f, gridY * 0.9f)
        icongithub.setPosition(
            gridX - icongithub.width / 2,
            gridY * 3 + (gridY - icongithub.height) / 2
        )
        ctx.cp.fitToRect(exit, gridX * 0.9f, gridY * 0.9f)
        exit.setPosition(
            7 * gridX - exit.width / 2,
            (gridY - exit.height) / 2
        )
        ctx.cp.fitToRect(settings, gridX * 0.9f, gridY * 0.9f)
        settings.setPosition(
            gridX - settings.width / 2,
            (gridY - settings.height) / 2
        )
        font.dispose()
        font = ctx.createFont((icongmail.height * 0.5).toInt())
        fcText = BitmapFontCache(font)
        fcText.setText("andrzej.novosiolov@gmail.com", gridX * 1.7f, gridY * 5.5f, gridX * 5f, Align.left, false)
        fcText.addText("t.me/Andrzejn", gridX * 1.7f, gridY * 4.5f, gridX * 5f, Align.left, false)
        fcText.addText("github.com/andrzej-nov", gridX * 1.7f, gridY * 3.5f, gridX * 5f, Align.left, false)
        fcText.setColors(ctx.theme.creditsText)
        fcTime = BitmapFontCache(font)
        fcTime.setText(millisToTimeString(ctx.gs.inGameDuration), gridX * 1.5f, gridY, gridX * 5f, Align.center, false)
        fcTime.setColors(ctx.theme.settingItem)
    }

    /***
     * Convert the in-game spent milliseconds into days-hours-mins-secongs format
     */
    private fun millisToTimeString(millis: Long): String {
        var s = millis / 1000
        var m = s / 60
        s -= m * 60
        var h = m / 60
        m -= h * 60
        val d = h / 24
        h -= d * 24
        return String.format(". . . %4d %02d:%02d:%02d   . . .", d, h, m, s)
    }

    /**
     * Called by GDX runtime when the screen is rendered. It is invoked frequently, so do not create objects here
     * and avoid extensive calculations
     */
    override fun render(delta: Float) {
        super.render(delta)
        with(ctx.theme.screenBackground) {
            clearScreen(r, g, b, a, true)
        }
        ctx.batch.begin()
        logo.draw(ctx.batch)
        icongmail.draw(ctx.batch)
        icontelegram.draw(ctx.batch)
        icongithub.draw(ctx.batch)
        settings.draw(ctx.batch)
        exit.draw(ctx.batch)
        fcText.draw(ctx.batch)
        fcTime.draw(ctx.batch)
        ctx.batch.end()
    }

    /**
     * The input adapter (processor)
     */
    inner class IAdapter : InputAdapter() {
        /**
         * Handle clicks/presses
         */
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val v = ctx.cp.pointerPositionScreen(Gdx.input.x, Gdx.input.y)

            if (v.y in 5f * gridY..6f * gridY)
                Gdx.net.openURI("mailto:andrzej.novosiolov@gmail.com?subject=The%20Xodus%20game")
            else if (v.y in 4f * gridY..5f * gridY)
                Gdx.net.openURI("https://t.me/AndrzejN")
            else if (v.y in 3f * gridY..4f * gridY)
                Gdx.net.openURI("https://github.com/andrzej-nov/Xodus")
            else if (v.y < gridY) {
                if (v.x < gridX * 2)
                    ctx.game.setScreen<HomeScreen>()
                else if (v.x > 6 * gridX)
                    Gdx.app.exit()
            }
            return super.touchDown(screenX, screenY, pointer, button)
        }

        /**
         * On Android 'Back' button switch back to the Home/Settings screen instead of default action
         * (pausing the application)
         */
        override fun keyDown(keycode: Int): Boolean {
            if (keycode == Input.Keys.BACK)
                ctx.game.setScreen<HomeScreen>()
            return super.keyDown(keycode)
        }
    }


}