package bumblebee.extension

import bumblebee.util.Converter.Companion.byteToHex
import bumblebee.util.Converter.Companion.hexToInt
import bumblebee.core.ImgPix
import bumblebee.type.ColorType
import bumblebee.type.ImgFileType
import java.nio.ByteBuffer

class PIX(private var byteArray : ByteArray) : ImgPix() {

    /*
    * Signature 3 Byte
    * Width     4 Byte
    * Height    4 Byte
    * ColorType 1 Byte
    *
    * DATA      [Width * Height * ColorType] Byte
    *
    * */

    init {
        imgFileType = ImgFileType.PIX
        extract()
    }

    override fun extract() {
        metaData.width = hexToInt(byteToHex(byteArray.copyOfRange(3, 7)))
        metaData.height = hexToInt(byteToHex(byteArray.copyOfRange(7, 11)))
        metaData.colorType = ColorType.fromInt(hexToInt(byteToHex(byteArray[11])))
        pixelBufferArray = ByteBuffer.allocate(metaData.width * metaData.height * metaData.colorType.colorSpace)
        pixelBufferArray.put(byteArray.copyOfRange(12 , 12 + metaData.width * metaData.height * metaData.colorType.colorSpace))
    }
}