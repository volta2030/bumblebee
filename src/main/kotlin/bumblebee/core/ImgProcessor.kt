package bumblebee.core

import bumblebee.color.Color
import bumblebee.type.*
import bumblebee.util.Converter.Companion.byteToInt
import bumblebee.util.Converter.Companion.colorToByte
import bumblebee.util.Histogram
import java.nio.ByteBuffer
import kotlin.experimental.inv
import kotlin.math.floor
import kotlin.math.pow

class ImgProcessor {
    companion object {
        fun set(imgPix: ImgPix, row: Int, col: Int, color: Color): ImgPix {
            if (imgPix.colorType != color.colorType) {
                System.err.println("ERROR : ColorType does not match")
            } else {
                val byteArray: ByteArray = colorToByte(color)
                for (i: Int in 0 until imgPix.bytesPerPixel) {
                    imgPix.pixelByteArray[i + imgPix.bytesPerPixel * col + (imgPix.width * imgPix.bytesPerPixel) * row] =
                        byteArray[i]
                }
            }
            return imgPix
        }

        fun crop(imgPix: ImgPix, row: Int, col: Int, width: Int, height: Int): ImgPix {

            val bytesPerPixel = imgPix.bytesPerPixel
            val pixelByteArray = ByteArray(width * height * bytesPerPixel)
            val startIdx = row * (imgPix.width * bytesPerPixel) + col * bytesPerPixel

            for (i: Int in 0 until height) {
                for (j: Int in 0 until width) {
                    for (k: Int in 0 until bytesPerPixel) {
                        pixelByteArray[j * bytesPerPixel + k + (i * bytesPerPixel * width)] =
                            (imgPix.pixelByteArray.get(startIdx + j * bytesPerPixel + k + (i * bytesPerPixel * imgPix.width)))
                    }
                }
            }

            imgPix.metaData.width = width
            imgPix.metaData.height = height
            imgPix.pixelByteArray = pixelByteArray

            return imgPix
        }

        fun resize(imgPix: ImgPix, width: Int, height: Int): ImgPix {
            val pixelByteArray = ByteArray(width * height * imgPix.bytesPerPixel)

            val oriW = imgPix.width
            val oriH = imgPix.height

            val widthRatioPixel = imgPix.width.toDouble() / width
            val heightRatioPixel = imgPix.height.toDouble() / height


            val processedImgPix = imgPix.pad(PadType.AVERAGE, 1).crop(1, 1, oriW + 1, oriH + 1)

            //Actual pixels that have been truncated
            val processedpixelByteArray = processedImgPix.pixelByteArray

            for (i: Int in 0 until height) {
                for (j: Int in 0 until width) {
                    for (k: Int in 0 until imgPix.bytesPerPixel) {

                        //Ideal pixels (x, y)
                        val x = j * widthRatioPixel
                        val y = i * heightRatioPixel

                        val x0 = floor(x).toInt()
                        val x1 = x0 + 1
                        val y0 = floor(y).toInt()
                        val y1 = y0 + 1


                        val p00 = processedpixelByteArray.get(k + imgPix.bytesPerPixel * (x0 + processedImgPix.width * y0))
                        val p10 = processedpixelByteArray.get(k + imgPix.bytesPerPixel * (x1 + processedImgPix.width * y0))
                        val p01 = processedpixelByteArray.get(k + imgPix.bytesPerPixel * (x0 + processedImgPix.width * y1))
                        val p11 = processedpixelByteArray.get(k + imgPix.bytesPerPixel * (x1 + processedImgPix.width * y1))

                        //Weight
                        val fx = x - x0
                        val fy = y - y0

                        //Interpolation for x
                        val fa = p00 * (1 - fx) + p10 * fx
                        val fb = p01 * (1 - fx) + p11 * fx

                        //Interpolation for y
                        val value = (fa * (1 - fy) + fb * fy).toInt().toByte()

                        pixelByteArray[k + imgPix.bytesPerPixel * j + (width * imgPix.bytesPerPixel * i)] = value
                    }
                }
            }
            imgPix.metaData.width = width
            imgPix.metaData.height = height
            imgPix.pixelByteArray = pixelByteArray

            // bilinear interpolation 완료
            return imgPix
        }

        /**
        * This function inverts the color of pixel
        * @return[ImgPix]
        * */
        fun invert(imgPix: ImgPix) : ImgPix {

            val width = imgPix.width
            val height = imgPix.height
            val bytesPerPixel = imgPix.bytesPerPixel

            for(i : Int in 0 until width * height * bytesPerPixel){
                imgPix.pixelByteArray[i] = imgPix.pixelByteArray[i].inv()
               }
            return imgPix
        }

        fun flip(imgPix : ImgPix, orientation: OrientationType) : ImgPix{

            val width = imgPix.width
            val height = imgPix.height
            val bytesPerPixel = imgPix.bytesPerPixel

            val pixelByteArray = ByteArray(width * height * bytesPerPixel)

            when(orientation){
                OrientationType.HORIZONTAL -> {
                    for(i : Int in 0 until height){
                        for(j : Int in 0 until width){
                            for(k : Int in 0 until bytesPerPixel){
                                pixelByteArray[j * bytesPerPixel + k + (i * bytesPerPixel * width)] = (imgPix.pixelByteArray.get(((i + 1) * bytesPerPixel * width - 1) - (j * bytesPerPixel + (bytesPerPixel - 1) - k)))
                            }
                        }
                    }
                }

                OrientationType.VERTICAL ->{
                    for(i : Int in 0 until height){
                        for(j : Int in 0 until width){
                            for(k : Int in 0 until bytesPerPixel){
                                pixelByteArray[j * bytesPerPixel + k + (i * bytesPerPixel * width)] = (imgPix.pixelByteArray.get(width * (height - (i + 1)) * bytesPerPixel + j * bytesPerPixel + k))
                            }
                        }
                    }
                }
            }

            imgPix.pixelByteArray = pixelByteArray

            return imgPix
        }

        fun rotate(imgPix: ImgPix, degree: Int) : ImgPix{
            return imgPix
        }
        fun crop(imgPix: ImgPix, degree : Int) : ImgPix{
            return imgPix
        }

        fun toGrayScale(imgPix : ImgPix) : ImgPix{
            val oldBytesPerPixel = imgPix.bytesPerPixel

            val width = imgPix.width
            val height = imgPix.height

            imgPix.metaData.colorType = ColorType.GRAY_SCALE

            val pixelByteArray = ByteArray(width * height * imgPix.bytesPerPixel)

            for(i : Int in 0 until height){
                for(j : Int in 0 until width){
                    var integer = 0
                    for(k : Int in 0 until oldBytesPerPixel){
                         integer += imgPix.pixelByteArray.get((i * oldBytesPerPixel* width) + (j * oldBytesPerPixel) + k).toUByte().toInt()
                    }
                    pixelByteArray[i * width + j] = (integer / oldBytesPerPixel).toByte()
                }
            }

            imgPix.pixelByteArray = pixelByteArray

            return imgPix
        }

        fun threshold(imgPix: ImgPix, level : Int) : ImgPix{

            val width = imgPix.width
            val height = imgPix.height
            val bytesPerPixel = imgPix.bytesPerPixel

            if(imgPix.colorType != ColorType.GRAY_SCALE){
               toGrayScale(imgPix)
            }

            for(i : Int in 0 until width * height * bytesPerPixel){
                val byte = if (imgPix.pixelByteArray.get(i).byteToInt() > level) (255).toByte() else (0).toByte()
                imgPix.pixelByteArray[i] = byte
            }

            return imgPix
        }

        fun threshold(imgPix : ImgPix, thresholdType : ThresholdType) : ImgPix{
            if(imgPix.colorType != ColorType.GRAY_SCALE){
                toGrayScale(imgPix)
            }

            when(thresholdType){
                ThresholdType.OTSU -> {
                    return threshold(imgPix, Histogram(imgPix).getOtsuLevel())
                }
                else -> {}
            }

            return imgPix
        }

        fun pad(imgPix: ImgPix, padType: PadType, padSize: Int) : ImgPix{

            val width = padSize + imgPix.width + padSize
            val height = padSize + imgPix.height + padSize
            val bytesPerPixel = imgPix.bytesPerPixel

            val pixelByteArray = ByteArray(width * height * bytesPerPixel)

            when(padType){
                PadType.ZERO -> {
                    for(i : Int in 0 until height){
                        for(j : Int in 0 until width){
                            for(k : Int in 0 until bytesPerPixel){
                                if((i >= padSize && i < height - padSize) && (j >= padSize && j < width - padSize)){
                                    pixelByteArray[j * bytesPerPixel + k + (i * bytesPerPixel * width)] = (imgPix.pixelByteArray.get(k + (j-padSize) * bytesPerPixel + (i-padSize) * bytesPerPixel * (width - 2 * padSize)))
                                }else{
                                    pixelByteArray[j * bytesPerPixel + k + (i * bytesPerPixel * width)] = 0
                                }
                            }
                        }
                    }
                }

                PadType.AVERAGE -> {
                    var averagePixel = Histogram(imgPix).getAverage(imgPix.colorType)
                    for(i : Int in 0 until height){
                        for(j : Int in 0 until width){
                            for(k : Int in 0 until bytesPerPixel){
                                if((i >= padSize && i < height - padSize) && (j >= padSize && j < width - padSize)){
                                    pixelByteArray[j * bytesPerPixel + k + (i * bytesPerPixel * width)] = (imgPix.pixelByteArray.get(k + (j-padSize) * bytesPerPixel + (i-padSize) * bytesPerPixel * (width - 2 * padSize)))
                                }else{
                                    pixelByteArray[j * bytesPerPixel + k + (i * bytesPerPixel * width)] = averagePixel[k]
                                }
                            }
                        }
                    }
                }
            }

            imgPix.metaData.width = width
            imgPix.metaData.height = height
            imgPix.pixelByteArray = pixelByteArray

            return imgPix
        }

        /**
         *
         * This function process image filtering : Average, Median
         *
         * @param[imgPix] The ImgPix
         * @param[filterType] The FilterType
         * @param[filterSize] The filterSize it will be odd number
         * @return[imgPix]
         */
        fun filter(imgPix: ImgPix, filterType : FilterType, filterSize : Int) : ImgPix{
            return filter(imgPix, filterType, filterSize, 1.0)
        }

        fun filter(imgPix: ImgPix, filterType : FilterType, filterSize : Int, stdev : Double) : ImgPix{

            val width = imgPix.width
            val height = imgPix.height
            val bytesPerPixel = imgPix.bytesPerPixel
            val pixelByteArray = ByteArray(width * height * bytesPerPixel)

            when(filterType){
                FilterType.AVERAGE->{

                    val windowSize = filterSize * filterSize
                    val halfFilterSize = filterSize / 2

                    val tempImgPix = imgPix.pad(PadType.AVERAGE, halfFilterSize)
                    val padImgPixWidth = tempImgPix.width
                    val temppixelByteArray = tempImgPix.pixelByteArray

                    for(i : Int in halfFilterSize until height + halfFilterSize){
                        for(j : Int in halfFilterSize until width + halfFilterSize){
                            for(k : Int in 0 until bytesPerPixel){
                                var intValue = 0
                                for(l : Int in 0 until windowSize){
                                    intValue += temppixelByteArray.get(((i - halfFilterSize + (l % filterSize)) * bytesPerPixel * padImgPixWidth) + ((j - halfFilterSize + (l / filterSize)) * bytesPerPixel) + k).toUByte().toInt()
                                }
                                pixelByteArray[(j - halfFilterSize) * bytesPerPixel + k + ((i - halfFilterSize) * bytesPerPixel * width)] =((intValue/windowSize).toByte())
                            }
                        }
                    }
                }

                FilterType.MEDIAN->{

                    val windowSize = filterSize * filterSize
                    val middleSize = windowSize/2
                    val halfFilterSize = filterSize/2

                    val tempImgPix = imgPix.pad(PadType.AVERAGE, halfFilterSize)
                    val padImgPixWidth = tempImgPix.width
                    val temppixelByteArray = tempImgPix.pixelByteArray

                    for(i : Int in halfFilterSize until height + halfFilterSize){
                        for(j : Int in halfFilterSize until width + halfFilterSize){
                            repeat(bytesPerPixel){
                                    k->
                                var uByteArray = UByteArray(windowSize)
                                repeat(windowSize){
                                        l-> uByteArray[l] = (temppixelByteArray.get(((i - halfFilterSize + (l % filterSize)) * bytesPerPixel * padImgPixWidth) + ((j - halfFilterSize + (l / filterSize)) * bytesPerPixel) + k)).toUByte()
                                }
                                uByteArray.sort()
                                pixelByteArray[(j - halfFilterSize) * bytesPerPixel + k + ((i - halfFilterSize) * bytesPerPixel * width)] = ((uByteArray[middleSize]).toByte())
                            }
                        }
                    }

                }

                FilterType.SHARPEN -> {
                    val padSize = 1
                    val windowSize = 9
                    val filterSize = 3
                    val strength = 9
                    val exceptStrength = -1
                    val tempImgPix = imgPix.pad(PadType.AVERAGE, padSize)
                    val padImgPixWidth = tempImgPix.width
                    val temppixelByteArray = tempImgPix.pixelByteArray

                    for(i : Int in padSize until height + padSize){
                        for(j : Int in padSize until width + padSize){
                            for(k : Int in 0 until bytesPerPixel){
                                var intValue = 0
                                for(l : Int in 0 until windowSize){
                                    var temp = (
                                            temppixelByteArray.get(((i - padSize + (l / filterSize)) * bytesPerPixel * padImgPixWidth) + ((j - padSize + (l % filterSize)) * bytesPerPixel) + k).toUByte().toInt())
                                    intValue += if(l == 4){
                                        strength * temp
                                    }else{
                                        exceptStrength * temp
                                    }
                                }
                                pixelByteArray[(j - padSize) * bytesPerPixel + k + ((i - padSize) * bytesPerPixel * width)] = intValue.toByte()
                            }
                        }
                    }
                }

                FilterType.GAUSSIAN -> {
                    val padSize = ((filterSize - 1) / 2)
                    val tempImgPix = imgPix.pad(PadType.AVERAGE, padSize)
                    val padImgPixWidth = tempImgPix.width
                    val temppixelByteArray = tempImgPix.pixelByteArray
                    val mask = getGaussianMask(filterSize, padSize, stdev)
                    for(i : Int in padSize until height + padSize){
                        for(j : Int in padSize until width + padSize){
                            for(k : Int in 0 until bytesPerPixel){
                                var sum = 0.0

                                for(l : Int in mask.indices){
                                    for(m : Int in mask.indices){
                                       val value =  temppixelByteArray.get(((i - padSize + l) * bytesPerPixel * padImgPixWidth) + ((j - padSize + m) * bytesPerPixel) + k).toUByte().toInt()
                                       sum+= value * mask[m][l]
                                    }
                                }
                                pixelByteArray[(j - padSize) * bytesPerPixel + k + ((i - padSize) * bytesPerPixel * width)] = (sum.toInt().toByte())
                            }
                        }
                    }
                }

            }

            imgPix.metaData.width = width
            imgPix.metaData.height = height
            imgPix.pixelByteArray = pixelByteArray

            return imgPix
        }

        fun getChannel(imgPix: ImgPix, chanelIndex : Int) : ImgPix{
            val width = imgPix.width
            val height = imgPix.height
            val pixelByteArray = ByteArray(width * height * 1)
            val originpixelByteArray = imgPix.pixelByteArray

            for(i : Int in 0 until height){
                for(j : Int in 0 until width){
                    pixelByteArray[i * width + j] = (originpixelByteArray.get(i * imgPix.bytesPerPixel * width + j * imgPix.bytesPerPixel + chanelIndex))
                }
            }

            imgPix.metaData.colorType = ColorType.GRAY_SCALE
            imgPix.pixelByteArray = pixelByteArray

            return imgPix
        }

        private fun getGaussianMask(filterSize: Int, padSize: Int, stdev: Double) : Array<Array<Double>>{
            val mask = Array(filterSize) { Array(filterSize) { 0.0 } }
            var sum = 0.0
            for(row : Int in -padSize until  padSize + 1){
                for(col : Int in -padSize until  padSize + 1){
                    val value = (1.0 / (2* Math.PI * stdev * stdev)) * Math.E.pow(-(row * row + col * col) / (2 * stdev * stdev))
                    mask[row + padSize][col + padSize] = value
                    sum += value
                }
            }

            //must do norm because pixel distance is discrete...
            for (row in 0 until filterSize) {
                for (col in 0 until filterSize) {
                    mask[row][col] /= sum
                }
            }
            return mask
        }

   }
}