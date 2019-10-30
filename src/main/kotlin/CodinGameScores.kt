package de.schmidtsoftware.bottersofthegalaxy

import com.mashape.unirest.http.Unirest
import org.apache.http.cookie.SM.COOKIE
import org.json.JSONObject
import kotlin.math.min
import kotlin.math.pow


// Clash of Code: 5000 for best Code Clasher
// Code Golf: max 200 Points per language for a maximum of up to 5 languages => 1000 max for Code Golf
// Multiplayer: Capped at 5000
// Optimization: Capped at 1000

private const val REMEMBER_ME_COOKIE_NAME = "rememberMe="
private const val REMEMBER_ME_COOKIE_VALUE = "placeTheValueOfYourCodinGameRememberMeCookieHere!!!"

fun main() {
    val response = Unirest.post("https://www.codingame.com/services/PuzzleRemoteService/findGamesPuzzleProgress")
        .body("[null]")
        .asJson().body.`object`.getJSONArray("success")
    val multiPlayerGames = response.filter { g ->
        val level = (g as JSONObject).getString("level")
        level == "multi" || level == "optim"
    }

    multiPlayerGames.map { mgraw ->
        val prettyId = (mgraw as JSONObject).getString("prettyId")
        // position for optims?
        val gameDetail = Unirest.post("https://www.codingame.com/services/PuzzleRemoteService/findProgressByPrettyId")
            .headers(mapOf(COOKIE to REMEMBER_ME_COOKIE_NAME + REMEMBER_ME_COOKIE_VALUE))
            .body("""["$prettyId",2155687]""")
            .asJson().body.`object`.getJSONObject("success")
        Game(
            gameDetail.getString("title"),
            gameDetail.getInt("globalTotal"),
            gameDetail.getString("level"),
            if (gameDetail.has("position")) gameDetail.getInt("position") else gameDetail.getInt("total")
        )
    }.filter { g ->
        g.maximumPoints > 0
    }.sortedByDescending { g ->
        g.pointsPerHour
    }.forEach { g ->
        println("\n===> [${g.level}] ${g.title} (${g.numberOfPlayers} players) <===")
        printGamePoints(g)
    }
}

data class Game(val title: String, val numberOfPlayers: Int, val level: String, val myPosition: Int) {
    val maximumCap = when (level) {
        "multi" -> 5000
        "optim" -> 1000
        else -> throw IllegalArgumentException("Dont know level type '$level'")
    }
    val maximumPoints = points(numberOfPlayers, 1, maximumCap)
    val myPoints = points(numberOfPlayers, myPosition, maximumCap)
    val pointsForMovingUp50Percent = points(numberOfPlayers, myPosition / 2, maximumCap) - myPoints
    val timeNecessaryToMoveUp50Percent =
        timeNecessaryToMoveToPercentage((100.0 / numberOfPlayers * (myPosition / 2)).toInt())
    val pointsPerHour = pointsForMovingUp50Percent / timeNecessaryToMoveUp50Percent
}

fun printGamePoints(game: Game) {
    println("          I've earned ${game.myPoints.formatted()} points")
    println("  Moving up 50% earns ${game.pointsForMovingUp50Percent.formatted()} points")
    println("  Time to move up 50% ${game.timeNecessaryToMoveUp50Percent.formatted()} hours")
    println("      Points per hour ${game.pointsPerHour.formatted(2)} points/hour")
    println("     1st player earns ${game.maximumPoints.formatted()} points")
    println(
        " Top 10% player earns ${points(
            game.numberOfPlayers,
            game.numberOfPlayers / 10,
            game.maximumCap
        ).formatted()} points"
    )
    println(
        " Top 50% player earns ${points(
            game.numberOfPlayers,
            game.numberOfPlayers / 2,
            game.maximumCap
        ).formatted()} points"
    )
}

private fun timeNecessaryToMoveToPercentage(i: Int) = points(100, i, 100)

private fun points(numberOfPlayers: Int, myRank: Int, maximumCap: Int) =
    if (numberOfPlayers == myRank)
        0.0
    else
        min(maximumCap, numberOfPlayers).toDouble()
            .pow((numberOfPlayers - myRank + 1) / numberOfPlayers.toDouble())

fun Double.formatted(i: Int = 0): String = java.lang.String.format("%4.${i}f", this)
