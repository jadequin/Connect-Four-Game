package connectFour

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign


//https://de.wikipedia.org/wiki/Vier_gewinnt

class ConnectFour(val p1: Long = 0, val p2: Long = 0, val height: List<Int> = listOf(0, 7, 14, 21, 28, 35, 42), val turn: Int = +1, val previousBoard: ConnectFour? = null) {

    companion object {
        private const val DEPTH = 5
        private const val MC_SIMULATIONS = 100

        private val boardPositions = listOf(5,12,19,26,33,40,47,4,11,18,25,32,39,46,3,10,17,24,31,38,45,2,9,16,23,30,37,44,1,8,15,22,29,36,43,0,7,14,21,28,35,42)
        val results = hashMapOf<Pair<Long, Long>, Int>()

        fun parseDatabase(data: String) {
            val entries = data.split(",")
            val p1Hashes = entries.map { it.split(";")[0].split(":")[0] }
            val p2Hashes = entries.map { it.split(";")[0].split(":")[1] }
            val values = entries.map { it.split(";")[1] }

            entries.indices.forEach {
                results[Pair(p1Hashes[it].toLong(), p2Hashes[it].toLong())] = values[it].toInt()
            }
        }
    }

    private fun board() = p1 or p2

    fun isP1Turn() = turn == 1

    fun makeBestMove(): ConnectFour = listMoves().maxBy { it.alphaBeta() }!!

    fun isValidMove(col: Int): Boolean = ((board() shr (col + 1) * 7 - 1) and 1L) == 0L //returns true if the specified column within 0 to 6 is free

    fun listMoves(): List<ConnectFour> = (0..6).mapNotNull { if(isValidMove(it)) makeMove(it) else null}

    fun makeMove(col: Int): ConnectFour {
        val newPos =  1L shl height[col]
        return ConnectFour(
                p1 = p1 + if(turn == 1) newPos else 0,
                p2 = p2 + if(turn == -1) newPos else 0,
                height = height.take(col) + listOf(height[col] + 1) + height.drop(col + 1),
                turn = -turn,
                previousBoard = this
        )
    }

    fun undo(times: Int = 1): ConnectFour = if(previousBoard == null || times == 0) this else previousBoard.undo(times - 1)

    fun result() = if(isWinPreviousPlayer()) -turn else if(isOutOfMoves()) 0 else turn

    fun isOutOfMoves() = listMoves().isEmpty()

    fun isGameOver() = isWinPreviousPlayer() || isOutOfMoves()

    //Quelle: https://github.com/denkspuren/playerC4/blob/master/playerDesign.md
    fun isWinPreviousPlayer(): Boolean {
        val player = if(turn == -1) p1 else p2
        return listOf(1,7,6,8).any { player and (player shr it) and (player shr (it * 2)) and (player shr (it * 3)) != 0L }
    }

    fun alphaBeta(depth: Int = DEPTH, alpha: Int = -Int.MAX_VALUE, beta: Int = Int.MAX_VALUE): Int {
        if(results[Pair(mirrorHash(p1), mirrorHash(p2))] != null)
            return results[Pair(mirrorHash(p1), mirrorHash(p2))]!! * -turn

        if(isGameOver())
            return result() * (depth + 1)

        if(depth == 0)
            return monteCarloTreeSearch()

        //alpha-beta-implementation combined with negamax
        val bestScore = run {
            listMoves().fold(alpha) {
                bestScore, move ->
                val score = -move.alphaBeta(depth - 1, -beta, -bestScore)
                if(bestScore in beta until score)
                    return@run bestScore
                return@fold max(bestScore, score)
            }
        }
        results[Pair(mirrorHash(p1), mirrorHash(p2))] = bestScore * -turn
        return bestScore
    }

    fun randomEndGame(): ConnectFour = if(isGameOver()) this else listMoves().random().randomEndGame()

    fun simulatePlays() = (1..MC_SIMULATIONS).sumBy { randomEndGame().result() * turn }

    fun monteCarloTreeSearch() = listMoves().map { it.simulatePlays() }.max()!!.sign

    fun mirrorHash(player: Long) = min(
            player,
            ((63L shl 0 and player) shl 42) + ((63L shl 7 and player) shl 28) + ((63L shl 14 and player) shl 14) + ((63L shl 21 and player)) + ((63L shl 28 and player) shr 14) + ((63L shl 35 and player) shr 28) + ((63L shl 42 and player) shr 42)
    )


    override fun toString(): String {
        return boardPositions.indices.joinToString(separator = "") {
            (if(it % 7 == 0 && it != 0) "\n" else "") + (if(((p1 shr boardPositions[it]) and 1L) == 1L) "X" else if(((p2 shr boardPositions[it]) and 1L) == 1L) "O" else ".")
        }
    }
}