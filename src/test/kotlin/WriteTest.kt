import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter

fun main(){
    val logFile = File("log.txt")
    val writer = FileWriter("log.txt", true)
    writer.close()
    val reader = BufferedReader(FileReader(logFile))
    var buf: String?
    while(true){
        buf = reader.readLine()
        if (buf == null)
            break
        println(buf)
    }
    reader.close()
}
