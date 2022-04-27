package com.andrzejn.xodus.helper

import com.andrzejn.xodus.Context
import com.andrzejn.xodus.GameScreen

/**
 * Handles game save/load
 */
class SaveGame(
    /**
     * Reference to the parent Context object
     */
    val ctx: Context
) {

    /**
     * Serialize the whole game
     */
    private fun serialize(gameScreen: GameScreen): String {
        val sb = com.badlogic.gdx.utils.StringBuilder()
        ctx.gs.serialize(sb)
        ctx.score.serialize(sb)
        gameScreen.serialize(sb)
        return sb.toString()
    }

    private fun deserializeSettingsAndScore(s: String): Int {
        if (!ctx.gs.deserialize(s.substring(0..4))) return -1
        if (!ctx.score.deserialize(s.substring(5..14))) return -1
        return 15
    }

    /**
     * Save current game to Preferences
     */
    fun saveGame(gameScreen: GameScreen) {
        ctx.gs.savedGame = serialize(gameScreen)
    }

    /**
     * Deletes saved game
     */
    fun clearSavedGame() {
        ctx.gs.savedGame = ""
    }

    /**
     * Serialized save game
     */
    fun savedGame(): String = ctx.gs.savedGame

    /**
     * Deserialize and set the game settings and score from the saved game
     */
    fun loadSettingsAndScore(s: String): Int {
        if (s.length < 120)
            return -1
        return deserializeSettingsAndScore(s)
    }
}