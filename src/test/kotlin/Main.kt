import bumblebee.core.ImgPix
import bumblebee.type.FilterType
import bumblebee.type.OrientationType
import bumblebee.type.PadType

fun main(){
    val imgPix = ImgPix("E:\\Dev\\bumblebee\\src\\main\\resources\\lena.png")
    imgPix.resize(256, 256)
    imgPix.invert()
    imgPix.flip(OrientationType.HORIZONTAL)
    imgPix.flip(OrientationType.VERTICAL)
    imgPix.toGrayScale()
    imgPix.threshold(120)
    imgPix.pad(PadType.ZERO, 3)
    imgPix.pad(PadType.AVERAGE, 5)
    imgPix.filter(FilterType.GAUSSIAN, 11, 3.0)
    imgPix.show()
}