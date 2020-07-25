package connectFour

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign


//https://de.wikipedia.org/wiki/Vier_gewinnt

class ConnectFour(val p1: Long = 0, val p2: Long = 0, val height: List<Int> = listOf(0, 7, 14, 21, 28, 35, 42), val turn: Int = +1, val previousBoard: ConnectFour? = null) {

    companion object {
        private const val DEPTH = 3
        private const val MC_SIMULATIONS = 1000

        val results = hashMapOf<Triple<Long, Long, Int>, Int>()

        fun parseDatabase(data: String) {
            if(data.isEmpty())
                return

            val entries = data.split(",")
            val p1Hashes = entries.map { it.split(";")[0].split(":")[0].toLong() }
            val p2Hashes = entries.map { it.split(";")[0].split(":")[1].toLong() }
            val turn = entries.map { it.split(";")[0].split(":")[2].toInt() }
            val values = entries.map { it.split(";")[1].toInt() }

            entries.indices.forEach {
                results[Triple(p1Hashes[it], p2Hashes[it], turn[it])] = values[it]
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

    fun isOutOfMoves() = (141845657554976 and board()) xor 141845657554976 == 0L //compare with top row

    fun isGameOver() = isWinPreviousPlayer() || isOutOfMoves()

    //Quelle: https://github.com/denkspuren/BitboardC4/blob/master/BitboardDesign.md
    fun isWinPreviousPlayer(): Boolean {
        val bitboard = if(turn == -1) p1 else p2
        return listOf(1,7,6,8).any { val bb = bitboard and (bitboard shr it); (bb and (bb shr (2 * it))) != 0L }
    }

    fun alphaBeta(depth: Int = DEPTH, alpha: Int = -Int.MAX_VALUE, beta: Int = Int.MAX_VALUE): Int {

        val hashKey = Triple(hashing(p1), hashing(p2), turn)

        if(results[hashKey] != null)
            return results[hashKey]!!

        if(isGameOver())
            return result() * (depth + 1) * 100_000

        if(depth == 0)
            return monteCarloTreeSearch() * (depth + 1)

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
        results[hashKey] = bestScore
        return bestScore
    }

    fun randomEndGame(): ConnectFour = if(isGameOver()) this else listMoves().random().randomEndGame()

    fun monteCarloTreeSearch() = (1..MC_SIMULATIONS).sumBy { val randGame = randomEndGame(); randGame.result() * if(randGame.turn == this.turn) 1 else -1 }.sign

    fun hashing(player: Long) = min(player, ((63L and player) shl 42) + ((8064L and player) shl 28) + ((1032192L and player) shl 14) + ((132120576L and player)) + ((16911433728L and player) shr 14) + ((2164663517184L and player) shr 28) + ((277076930199552L and player) shr 42))

    override fun toString(): String {
        val boardPositions = listOf(5,12,19,26,33,40,47,4,11,18,25,32,39,46,3,10,17,24,31,38,45,2,9,16,23,30,37,44,1,8,15,22,29,36,43,0,7,14,21,28,35,42)

        return boardPositions.indices.joinToString(separator = "") {
            (if(it % 7 == 0 && it != 0) "\n" else "") + (if(((p1 shr boardPositions[it]) and 1L) == 1L) "X" else if(((p2 shr boardPositions[it]) and 1L) == 1L) "O" else ".")
        }
    }
}