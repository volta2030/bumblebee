 package bumblebee.core

import bumblebee.FileManager
import bumblebee.util.Converter.Companion.byteToHex
import bumblebee.ImgExtractor
import bumblebee.color.Color
import bumblebee.type.*
import bumblebee.util.Histogram
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Image
import java.awt.image.*
import java.nio.ByteBuffer
import javax.swing.*


 open class ImgPix() : ImgExtractor, Cloneable {

    var metaData = MetaData(0, 0, ColorType.GRAY_SCALE)
    protected val OCTA = 8
    var bytesPerPixel = 0
    var bitDepth = 0
    val width : Int
        get() = metaData.width
    val height : Int
        get() = metaData.height
    val colorType : ColorType
        get() = metaData.colorType

    private var manipulatedInstance = false
    var pixelBufferArray: ByteBuffer = ByteBuffer.allocate(0)
    var imgFileType : ImgFileType = ImgFileType.PIX

    constructor(width: Int, height: Int, colorType: ColorType) : this() {
        metaData.width = width
        metaData.height = height
        metaData.colorType = colorType
        this.pixelBufferArray = ByteBuffer.allocate(width * height * colorType.colorSpace)
    }

    constructor(filePath : String) : this() {
       var imgPix  = FileManager.read(filePath)
        metaData.width = imgPix.width
        metaData.height = imgPix.height
        metaData.colorType = imgPix.colorType
        bytesPerPixel = imgPix.bytesPerPixel
        bitDepth = imgPix.bitDepth
        this.pixelBufferArray = imgPix.pixelBufferArray

    }

     public override fun clone(): ImgPix {
        return super.clone() as ImgPix
    }

    fun get(row : Int, col : Int) : String{
        val byteArray = ByteArray((colorType.colorSpace * (bitDepth/OCTA)))
        for (i : Int in 0 until bytesPerPixel){
            byteArray[i] = pixelBufferArray.get(i + bytesPerPixel * col + (width * bytesPerPixel) * row)
        }
        return byteToHex(byteArray)
    }

    fun get() : ByteArray {
        return pixelBufferArray.array()
    }

    fun show(){
        val buffer = DataBufferByte(pixelBufferArray.array(), pixelBufferArray.array().size)

        val bufferedImage : BufferedImage
        when(colorType){
            ColorType.GRAY_SCALE ->{
                bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
                bufferedImage.data = Raster.createInterleavedRaster(buffer, width, height, width * 1, 1, intArrayOf(0), null)
            }

            ColorType.TRUE_COLOR ->{
               bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
               bufferedImage.data = Raster.createInterleavedRaster(buffer, width, height, width * 3, 3, intArrayOf(0,1,2), null)
            }

            ColorType.TRUE_COLOR_ALPHA->{
                bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                bufferedImage.data = Raster.createInterleavedRaster(buffer, width, height, width * 4, 4, intArrayOf(0,1,2,3), null)
            }

            ColorType.INDEXED_COLOR->{
                bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
                bufferedImage.data = Raster.createInterleavedRaster(buffer, width, height, width * 1, 1, intArrayOf(0), null)
            }

            else -> {
                bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
                bufferedImage.data = Raster.createInterleavedRaster(buffer, width, height, width * 3, 3, intArrayOf(0,1,2), null)
            }
        }

        val frame = JFrame()
        val img: Image = frame.toolkit.getImage("bumblebee_icon.png")
        frame.iconImage = img
        frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        frame.title = "image"
        frame.isResizable = false
        frame.isVisible = true
        frame.setLocation(0,0)

        val pane: JPanel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                g.drawImage(bufferedImage, 0, 0, null)
                g.dispose()
            }
        }

        pane.preferredSize = Dimension(width, height)
        frame.add(pane)
        frame.pack()
    }

     override fun extract() {
         TODO("Not yet implemented")
     }

     fun set(row : Int, col : Int, color : Color) : ImgPix {
         return ImgProcess.set(this, row, col, color)
     }
     fun invert() : ImgPix {
         manipulatedInstance = true
         return ImgProcess.invert(this)
     }
     fun flip(orientation: OrientationType) : ImgPix {
         manipulatedInstance = true
         return ImgProcess.flip(this, orientation)
     }

     fun crop(row : Int, col : Int, width : Int, height : Int) : ImgPix {
         manipulatedInstance = true
         return ImgProcess.crop(this, row, col, width, height)
     }

     fun toGrayScale() : ImgPix {
         manipulatedInstance = true
         return ImgProcess.toGrayScale(this)
     }

     fun threshold(thresholdType: ThresholdType): ImgPix {
         manipulatedInstance = true
         return ImgProcess.threshold(this, thresholdType)
     }

     fun threshold(level : Int) : ImgPix {
         manipulatedInstance = true
         return ImgProcess.threshold(this, level)
     }

     fun pad(padType: PadType, padPixelSize : Int) : ImgPix{
         manipulatedInstance = true
         return ImgProcess.pad(this, padType, padPixelSize)
     }

     fun histogram() : Histogram{
        return Histogram(this)
     }

 }