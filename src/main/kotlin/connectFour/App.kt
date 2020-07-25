

package connectFour

import kotlin.io.writeText
import kotlin.io.readText
import java.io.File


fun main() {
    val data = File("src/main/kotlin/connectFour/connect4DB.txt").readText()
    ConnectFour.parseDatabase(data)

    println(ConnectFour.results.values.max())

    var c4 = ConnectFour()
    println(c4)

    while(!c4.isGameOver()) {
        c4 = if(c4.isP1Turn()) c4.makeBestMove() else c4.listMoves().random()
        println(c4)
        println()
    }

    //database output with pattern of: P1Hash:P2Hash;Result
//    val dbText = ConnectFour.results.entries.joinToString(separator = ",") { "${it.key.first}:${it.key.second};${it.value}" }
//    File("src/main/kotlin/connectFour/connect4DB.txt").writeText(dbText)
}