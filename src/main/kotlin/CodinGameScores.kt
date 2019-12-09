package de.schmidtsoftware.bottersofthegalaxy

import kotlinx.coroutines.async
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.math.min
import kotlin.math.pow


// Clash of Code: 5000 for best Code Clasher
// Code Golf: max 200 Points per language for a maximum of up to 5 languages => 1000 max for Code Golf
// Multiplayer: Capped at 5000
// Optimization: Capped at 1000

private const val REMEMBER_ME_COOKIE_NAME = "rememberMe="
private const val REMEMBER_ME_COOKIE_VALUE = "placeTheValueOfYourCodinGameRememberMeCookieHere!!!"
private const val USERID = "placeYourCodinGameUserIdHere!!!"

private val client = HttpClient.newBuilder().build()

fun main() {
    val multiplayerGameIds = retrieveMultiplayerGameIds()
    runBlocking {
        multiplayerGameIds.map { id ->
            async { retrieveGameDetails(id) }
        }.map { g ->
            g.await()
        }.filter { g ->
            g.maximumPoints > 0
        }.sortedByDescending { g ->
            g.pointsPerHour
        }.forEach { g ->
            println("\n===> [${g.level}] ${g.title} (${g.numberOfPlayers} players) <===")
            printGamePoints(g)
        }
    }
}

private fun retrieveMultiplayerGameIds(): List<String> {
    val request = HttpRequest.newBuilder()
        .uri(URI.create("https://www.codingame.com/services/PuzzleRemoteService/findGamesPuzzleProgress"))
        .POST(HttpRequest.BodyPublishers.ofString("[null]"))
        .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString()).body()
    val games = JSONObject(response).getJSONArray("success")

    return games.filter { game ->
        val level = (game as JSONObject).getString("level")
        level == "multi" || level == "optim"
    }.map { mgraw ->
        (mgraw as JSONObject).getString("prettyId")
    }
}

suspend fun retrieveGameDetails(prettyId: String): Game {
    val request = HttpRequest.newBuilder()
        .uri(URI.create("https://www.codingame.com/services/PuzzleRemoteService/findProgressByPrettyId"))
        .POST(HttpRequest.BodyPublishers.ofString("""["$prettyId",$USERID]"""))
        .header("Cookie", REMEMBER_ME_COOKIE_NAME + REMEMBER_ME_COOKIE_VALUE)
        .build()

    println("About to request Game Details for '$prettyId'")
    val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
    val body = response.await().body()
    val gameDetail = JSONObject(body).getJSONObject("success")

    return Game(
        gameDetail.getString("title"),
        gameDetail.getInt("globalTotal"),
        gameDetail.getString("level"),
        if (gameDetail.has("position")) gameDetail.getInt("position") else gameDetail.getInt("total")
    )
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
