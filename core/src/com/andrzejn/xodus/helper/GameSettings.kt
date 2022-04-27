package com.andrzejn.xodus.helper

import com.badlogic.gdx.Gdx

/**
 * Game settings and saved game. Stored in the GDX system-dependent Preferences
 */
class GameSettings {
    private val pref by lazy { Gdx.app.getPreferences("com.andrzejn.xodus") }
    private val sFIELDSIZE = "fieldSize"
    private val sCHAOSMOVES = "chaosMoves"
    private val sREINCARNATION = "reincarnation"
    private val sSHREDDERSPEED = "shredderSpeed"
    private val sSAVEDGAME = "savedGame"
    private val sDARKTHEME = "darkTheme"
    private val sINGAMEDURATION = "inGameDuration"
    private val sRECORDMOVES = "recordMoves"
    private val sRECORDPOINTS = "recordPoints"
    private var iFieldSize: Int = 7
    private var iChaosMoves: Int = 1
    private var iShredderSpeed: Float = 2f / 3
    private var iReincarnation: Boolean = true
    private var iDarkTheme: Boolean = true
    private var iInGameDuration: Long = 0

    /**
     * Reset game settings to default values
     */
    fun reset() {
        iFieldSize = pref.getInteger(sFIELDSIZE, 7)
        if (iFieldSize !in listOf(7, 9, 11))
            iFieldSize = 7
        fieldSize = iFieldSize
        iChaosMoves = pref.getInteger(sCHAOSMOVES, 1).coerceIn(0..2)
        chaosMoves = iChaosMoves
        iReincarnation = pref.getBoolean(sREINCARNATION, true)
        reincarnation = iReincarnation
        iShredderSpeed = shredderIntToSpeed(pref.getInteger(sSHREDDERSPEED, 2))
        shredderSpeed = iShredderSpeed
        iDarkTheme = pref.getBoolean(sDARKTHEME, true)
        iInGameDuration = pref.getLong(sINGAMEDURATION, 0)
    }

    /**
     *  Schredder line speed, 1/2, 2/3 or 3/4, encoded as 1..3
     */
    var shredderSpeed: Float
        get() = iShredderSpeed
        set(value) {
            iShredderSpeed = value
            pref.putInteger(sSHREDDERSPEED, shredderSpeedToInt(value))
        }

    /**
     * Encode shredder speed to integer code
     */
    private fun shredderSpeedToInt(speed: Float) = when (speed) {
        in 0f..0.6f -> 1
        in 0.7f..1f -> 3
        else -> 2
    }

    /**
     * Decode shredder speed from integer code
     */
    private fun shredderIntToSpeed(s: Int): Float = when (s) {
        1 -> 1f / 2
        3 -> 3f / 4
        else -> 2f / 3
    }

    /**
     * Field size, 7, 9, 11
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
     * Is ball reincarnation allowed
     */
    var reincarnation: Boolean
        get() = iReincarnation
        set(value) {
            iReincarnation = value
            pref.putBoolean(sREINCARNATION, value)
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
        return "$prefix$iChaosMoves$iFieldSize${shredderSpeedToInt(iShredderSpeed)}$iReincarnation}"
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
        sb.append(iFieldSize, 2).append(chaosMoves).append(if (reincarnation) 1 else 0)
            .append(shredderSpeedToInt(iShredderSpeed))
    }

    /**
     * Deserialize game settings from the saved game
     */
    fun deserialize(s: String): Boolean {
        if (s.length != 5) {
            reset()
            return false
        }
        val fs = s.substring(0..1).toIntOrNull()
        val cm = s[2].digitToIntOrNull()
        val r = s[3] != '0'
        val ss = s[4].digitToIntOrNull()
        if ((fs == null || fs !in listOf(5, 7, 9, 11))
            || cm == null || cm !in 0..2
            || ss == null || ss !in 1..3
        ) {
            reset()
            return false
        }
        fieldSize = fs
        chaosMoves = cm
        reincarnation = r
        shredderSpeed = shredderIntToSpeed(ss)
        return true
    }
}
