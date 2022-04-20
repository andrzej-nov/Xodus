package com.andrzejn.xodus.helper

import com.badlogic.gdx.Gdx

/**
 * Game settings and saved game. Stored in the GDX system-dependent Preferences
 */
class GameSettings {
    private val pref by lazy { Gdx.app.getPreferences("com.andrzejn.xodus") }
    private val sFIELDSIZE = "fieldSize"
    private val sCHAOSMOVES = "chaosMoves"
    private val sCOLORSCOUNT = "colorsCount"
    private val sSAVEDGAME = "savedGame"
    private val sDARKTHEME = "darkTheme"
    private val sINGAMEDURATION = "inGameDuration"
    private val sRECORDMOVES = "recordMoves"
    private val sRECORDPOINTS = "recordPoints"
    private var iFieldSize: Int = 7
    private var iChaosMoves: Int = 1
    private var iColorsCount: Int = 6
    private var iDarkTheme: Boolean = true
    private var iInGameDuration: Long = 0

    /**
     * Reset game settings to default values
     */
    fun reset() {
        iFieldSize = pref.getInteger(sFIELDSIZE, 7)
        if (iFieldSize !in listOf(7, 9, 11, 13))
            iFieldSize = 9
        fieldSize = iFieldSize
        iChaosMoves = pref.getInteger(sCHAOSMOVES, 1).coerceIn(0..2)
        chaosMoves = iChaosMoves
        iColorsCount = pref.getInteger(sCOLORSCOUNT, 6)
        iColorsCount = iColorsCount.coerceIn(6, 7)
        colorsCount = iColorsCount
        iDarkTheme = pref.getBoolean(sDARKTHEME, true)
        iInGameDuration = pref.getLong(sINGAMEDURATION, 0)
    }

    /**
     * Maximum connector radius, 3..6
     */
    var fieldSize: Int
        get() = iFieldSize
        set(value) {
            iFieldSize = value
            pref.putInteger(sFIELDSIZE, value)
            pref.flush()
        }

    /**
     * Number of Chaos moves after the player move, 0..2
     */
    var chaosMoves: Int
        get() = iChaosMoves
        set(value) {
            iChaosMoves = value
            pref.putInteger(sCHAOSMOVES, value)
            pref.flush()
        }

    /**
     * Number of different colors used for ball sockets. 6..7
     */
    var colorsCount: Int
        get() = iColorsCount
        set(value) {
            iColorsCount = value
            pref.putInteger(sCOLORSCOUNT, value)
            pref.flush()
        }

    /**
     * Dark/Light color theme selector
     */
    var isDarkTheme: Boolean
        get() = iDarkTheme
        set(value) {
            iDarkTheme = value
            pref.putBoolean(sDARKTHEME, value)
            pref.flush()
        }

    /**
     * Total time spent in game
     */
    var inGameDuration: Long
        get() = iInGameDuration
        set(value) {
            iInGameDuration = value
            pref.putLong(sINGAMEDURATION, value)
            pref.flush()
        }

    /**
     * Serialized save game
     */
    var savedGame: String
        get() = pref.getString(sSAVEDGAME, "")
        set(value) {
            pref.putString(sSAVEDGAME, value)
            pref.flush()
        }

    /**
     * Key name for storing the records for the current tile type - game size - colors
     */
    private fun keyName(prefix: String): String {
        return "$prefix$iChaosMoves$iColorsCount$iFieldSize}"
    }

    /**
     * Record moves value for the current balls count - max radius - colors
     */
    var recordMoves: Int
        get() = pref.getInteger(keyName(sRECORDMOVES), 0)
        set(value) {
            pref.putInteger(keyName(sRECORDMOVES), value)
            pref.flush()
        }

    /**
     * Record moves value for the current balls count - max radius - colors
     */
    var recordPoints: Int
        get() = pref.getInteger(keyName(sRECORDPOINTS), 0)
        set(value) {
            pref.putInteger(keyName(sRECORDPOINTS), value)
            pref.flush()
        }

    /**
     * Serialize game settings, to include into the saved game. Always 6 characters.
     */
    fun serialize(sb: com.badlogic.gdx.utils.StringBuilder) {
        sb.append(iFieldSize, 2).append(chaosMoves).append(colorsCount)
    }

    /**
     * Deserialize game settings from the saved game
     */
    fun deserialize(s: String): Boolean {
        if (s.length != 6) { // TODO Fix the count when I know the settings size
            reset()
            return false
        }
        val fv = s.substring(0..1).toIntOrNull()

        val bc = s.substring(3..4).toIntOrNull()
        val cc = s[5].digitToIntOrNull()
        if ((fv == null || fv !in listOf(5, 7, 9, 11))
            || bc == null || bc !in 20..60
            || cc == null || cc !in 6..7
        ) {
            reset()
            return false
        }
        fieldSize = fv
        chaosMoves = bc
        colorsCount = cc
        return true
    }
}
