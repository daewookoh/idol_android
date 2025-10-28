package net.ib.mn.core.model

import org.json.JSONObject

/**
 * Create Date: 2025/09/23
 *
 * Description: RemoteConfig data model
 *
 * @see
 */
data class RemoteConfigData(
    val game: GameConfig
) {
    companion object {
        fun fromJson(jsonObject: JSONObject): RemoteConfigData? {
            return try {
                val gameJson = jsonObject.getJSONObject("game")
                RemoteConfigData(
                    game = GameConfig.fromJson(gameJson)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}

data class GameConfig(
    val showMenu: Boolean,
    val portalUrl: String,
    val games: List<GameData>
) {
    companion object {
        fun fromJson(jsonObject: JSONObject): GameConfig {
            val showMenu = jsonObject.getBoolean("showMenu")
            val portalUrl = jsonObject.getString("portalUrl")
            val gamesArray = jsonObject.getJSONArray("games")
            val games = mutableListOf<GameData>()
            
            for (i in 0 until gamesArray.length()) {
                val gameJson = gamesArray.getJSONObject(i)
                games.add(GameData.fromJson(gameJson))
            }
            
            return GameConfig(
                showMenu = showMenu,
                portalUrl = portalUrl,
                games = games
            )
        }
    }
}

data class GameData(
    val gameId: String,
    val url: String,
    val splashImage: String,
    val splashBgColor: String,
    val fee: Int
) {
    companion object {
        fun fromJson(jsonObject: JSONObject): GameData {
            return GameData(
                gameId = jsonObject.getString("gameId"),
                url = jsonObject.getString("url"),
                splashImage = jsonObject.getString("splashImage"),
                splashBgColor = jsonObject.getString("splashBgColor"),
                fee = jsonObject.getInt("fee")
            )
        }
    }
}
