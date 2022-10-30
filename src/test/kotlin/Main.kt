import bumblebee.FileManager
import bumblebee.core.ImgPix
import bumblebee.type.PadType
import bumblebee.util.ByteViewer
import bumblebee.util.Numeric

fun main(){
    var imgPix = FileManager.read("src/main/resources/lenna.png")
    imgPix.pad(PadType.AVERAGE, 10)
    imgPix.show()

//    var imgPix = FileManager.read("src/main/resources/balloons.jpg")

//    var answer = Numeric.softMax(doubleArrayOf(1.0, 2.0, 3.0))
//
//    ByteViewer(FileManager.readBytes("src/main/resources/balloons.jpg"))
}