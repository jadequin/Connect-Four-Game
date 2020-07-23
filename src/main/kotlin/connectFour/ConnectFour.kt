package connectFour

import kotlin.math.max
import kotlin.math.sign


//https://de.wikipedia.org/wiki/Vier_gewinnt

class ConnectFour(val p1: Long = 0, val p2: Long = 0, val height: List<Int> = listOf(0, 7, 14, 21, 28, 35, 42), val turn: Int = +1, val history: List<ConnectFour> = emptyList()) {

    companion object {
        private const val DEPTH = 3
        private const val MC_SIMULATIONS = 100
    }

    private fun board() = p1 or p2

    fun isP1Turn() = turn == 1

    fun isValidMove(col: Int): Boolean = ((board() shr (col + 1) * 7 - 1) and 1L) == 0L //returns true if the specified column within 0 to 6 is free

    fun listMoves(): List<ConnectFour> = (0..6).mapNotNull { if(isValidMove(it)) makeMove(it) else null}

    fun makeMove(col: Int): ConnectFour {
        val newPos =  1L shl height[col]
        return ConnectFour(
                p1 = p1 + if(turn == 1) newPos else 0,
                p2 = p2 + if(turn == -1) newPos else 0,
                height = height.take(col) + listOf(height[col] + 1) + height.drop(col + 1),
                turn = -turn,
                history = history.plus(this)
        )
    }

    fun undo(times: Int = 1) = if(history.isEmpty()) this else history.takeLast(times).first()

    fun result() = if(isWinPreviousPlayer()) -turn else if(isOutOfMoves()) 0 else turn

    fun isOutOfMoves() = listMoves().isEmpty()

    fun isGameOver() = isWinPreviousPlayer() || isOutOfMoves()

    //Quelle: https://github.com/denkspuren/playerC4/blob/master/playerDesign.md
    fun isWinPreviousPlayer(): Boolean {
        val player = if(turn == -1) p1 else p2
        return listOf(1,7,6,8).any { player and (player shr it) and (player shr (it * 2)) and (player shr (it * 3)) != 0L }
    }

    fun alphaBeta(depth: Int = DEPTH, alpha: Int = -Int.MAX_VALUE, beta: Int = Int.MAX_VALUE): Int {
//        if(results[this] != null)
//            return results[this]!! * -turn


        if(isGameOver())
            return result()

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
//        results[this] = bestScore * -turn
        return bestScore
    }

    fun randomEndGame(): ConnectFour = if(isGameOver()) this else listMoves().random().randomEndGame()

    fun simulatePlays() = (1..MC_SIMULATIONS).sumBy {
        val randomLateGame = randomEndGame()
        return@sumBy randomLateGame.result() * turn
    }

    fun monteCarloTreeSearch() = listMoves().map { it.simulatePlays() }.max()!!.sign


    override fun toString(): String {
        val positions = listOf(5,12,19,26,33,40,47,4,11,18,25,32,39,46,3,10,17,24,31,38,45,2,9,16,23,30,37,44,1,8,15,22,29,36,43,0,7,14,21,28,35,42)
        return positions.indices.joinToString(separator = "") {
            (if(it % 7 == 0 && it != 0) "\n" else "") + (if(((p1 shr positions[it]) and 1L) == 1L) "X" else if(((p2 shr positions[it]) and 1L) == 1L) "O" else ".")
        }
    }
}