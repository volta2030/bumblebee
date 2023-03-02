package bumblebee.extension

import bumblebee.core.ImgPix
import bumblebee.type.ColorType
import bumblebee.type.ImgFileType
import bumblebee.util.Converter
import bumblebee.util.Converter.Companion.byteToHex
import bumblebee.util.Converter.Companion.byteToInt
import bumblebee.util.Converter.Companion.hexToInt
import bumblebee.util.Converter.Companion.intToByteArray
import bumblebee.util.Converter.Companion.invert
import java.nio.ByteBuffer

//TIFF Revision 6.0 / Author : Aldus Corporation
class TIFF(private var byteArray: ByteArray) : ImgPix() {
    private var ifh = IFH()
    private var ifdArray = ArrayList<IFD>()
    private var compressionType = CompressionType.NONE
    private var rowsPerStrip = 0
    private var stripByteCounts = 0
    companion object {
        private var isLittle = false
        fun ByteArray.toEndian() : ByteArray {
            return if(isLittle){
                invert(this)
            }else{
                this
            }
        }
    }
    init {
        imgFileType = if (byteArray.sliceArray(0 until 2).contentEquals(ImgFileType.TIFF_LITTLE.signature)){
            isLittle = true
            ImgFileType.TIFF_LITTLE
        }else{
            isLittle = false
            ImgFileType.TIFF_BIG
        }
        extract()
    }

    override fun extract() {

        ifh.extract(imgFileType, ifdArray, byteArray)

        //don't need to make endianArray from here
        ifdArray.forEach {
            it.tagArray.forEach {tag->
                when(tag.tagId){
                    TagType.IMAGE_WIDTH -> metaData.width = byteToInt(tag.dataOffset.sliceArray(0 until 2))
                    TagType.IMAGE_LENGTH -> metaData.height = byteToInt(tag.dataOffset.sliceArray(0 until 2))
                    TagType.SAMPLES_PER_PIXEL -> {
                        bytesPerPixel = byteToInt(tag.dataOffset.sliceArray(0 until 2))
                        if(bytesPerPixel == 3){
                            metaData.colorType = ColorType.TRUE_COLOR
                        }
                    }
                    TagType.COMPRESSION -> {
                        compressionType = CompressionType.fromInt(byteToInt(tag.dataOffset.sliceArray(0 until 2)))
                    }
                    TagType.ROWS_PER_STRIP -> {
                        rowsPerStrip = byteToInt(tag.dataOffset.sliceArray(0 until 2))
                    }
                    TagType.STRIP_BYTE_COUNTS -> {
                        stripByteCounts = byteToInt(tag.dataOffset)
                    }
                    TagType.BITS_PER_SAMPLE -> {}
                    else -> {}
                }
            }

            it.tagArray.forEach {tag->
                if(tag.tagId == TagType.STRIP_OFFSETS) {
                        extractRasterImage(tag)
                }
            }
        }
    }

    private fun extractRasterImage(tag: Tag) {

        val stripCount = tag.dataCount
        val firstStripOffset = byteToInt(tag.dataOffset)
        val lastStripOffset = firstStripOffset + (4 * stripCount)
        var startIdx = byteToInt(byteArray.sliceArray(firstStripOffset until firstStripOffset + 4).toEndian())
        val endIdx = byteToInt(byteArray.sliceArray(lastStripOffset - 4 until lastStripOffset).toEndian()) + byteToInt(byteArray.sliceArray(stripByteCounts + (4 * stripCount) - 4 until stripByteCounts + 4 * stripCount).toEndian())

        this.pixelBufferArray = ByteBuffer.allocate(width * height * bytesPerPixel)
        when(compressionType){
            CompressionType.LZW->{
                pixelBufferArray.put(byteArray.sliceArray(startIdx until endIdx))
            }
            else->{
                pixelBufferArray.put(byteArray.sliceArray(startIdx until endIdx))
            }
        }
    }

//Image File Header
private class IFH {
    lateinit var byteOrder : ByteArray
    lateinit var fortyTwo : ByteArray
    lateinit var ifdOffset : ByteArray

    fun extract(imgFileType: ImgFileType, ifdArray: ArrayList<IFD>, byteArray: ByteArray){
        byteOrder = byteArray.sliceArray(0 until 2)
        fortyTwo = byteArray.sliceArray(2 until 4)
        ifdOffset = byteArray.sliceArray(4 until 8)

        val startIdx = if(imgFileType.signature.contentEquals(ImgFileType.TIFF_LITTLE.signature)){
            byteToInt(invert(ifdOffset))
        }else{
            byteToInt(ifdOffset)
        }

        do{
            val ifd = IFD(byteArray.sliceArray(startIdx until byteArray.size))
            ifdArray.add(ifd)
        }while(!ifd.nextIFDOffset.contentEquals(byteArrayOf(0, 0, 0, 0)))
    }
}

//Image File Directory
private class IFD(byteArray: ByteArray){
    var numOfTags : ByteArray = byteArray.sliceArray(0 until 2).toEndian()
    var tagArray = ArrayList<Tag>() //12 Byte * numOfTags
    var nextIFDOffset : ByteArray //4 Byte

    init {
        val value = hexToInt(byteToHex(numOfTags))
        for(i : Int in 0 until  value){
            var tag = Tag(byteArray.sliceArray(2 + i*12 until 2 + (i+1) * 12))
            tagArray.add(tag)
        }
        nextIFDOffset = byteArray.sliceArray(2 + 12 * value until 2 + 12 * value + 4).toEndian()
    }
}

private class Tag(byteArray: ByteArray) {
    var tagId : TagType = TagType.fromByteArray(byteArray.sliceArray(0 until 2).toEndian())
    var dataType : DataType = DataType.fromByteArray(byteArray.sliceArray(2 until 4).toEndian()) //2 Byte
    var dataCount : Int = byteToInt(byteArray.sliceArray(4 until 8).toEndian()) //4 Byte
    var dataOffset : ByteArray = byteArray.sliceArray(8 until 12).toEndian()// 4Byte
}
private enum class TagType (val byteArray : ByteArray) {

    NEW_SUBFILE_TYPE(intToByteArray(254, 2)),
    SUBFILE_TYPE(intToByteArray(255, 2)),
    IMAGE_WIDTH(intToByteArray(256, 2)),
    IMAGE_LENGTH(intToByteArray(257, 2)),
    BITS_PER_SAMPLE(intToByteArray(258, 2)),
    COMPRESSION(intToByteArray(259, 2)),
    PHOTOMETRIC_INTERPRETATION(intToByteArray(262, 2)),
    THRESHOLDING(intToByteArray(263, 2)),
    CELL_WIDTH(intToByteArray(264, 2)),
    CELL_LENGTH(intToByteArray(265, 2)),
    FILL_ORDER(intToByteArray(266, 2)),
    DOCUMENT_NAME(intToByteArray(269, 2)),
    IMAGE_DESCRIPTION(intToByteArray(270, 2)),
    MAKE(intToByteArray(271, 2)),
    MODEL(intToByteArray(272, 2)),
    STRIP_OFFSETS(intToByteArray(273, 2)),
    ORIENTATION(intToByteArray(274, 2)),
    SAMPLES_PER_PIXEL(intToByteArray(277, 2)),
    ROWS_PER_STRIP(intToByteArray(278, 2)),
    STRIP_BYTE_COUNTS(intToByteArray(279, 2)),
    MIN_SAMPLE_VALUE(intToByteArray(280, 2)),
    MAX_SAMPLE_VALUE(intToByteArray(281, 2)),
    X_RESOLUTION(intToByteArray(282, 2)),
    Y_RESOLUTION(intToByteArray(283, 2)),
    PLANAR_CONFIGURATION(intToByteArray(284, 2)),
    PAGE_NAME(intToByteArray(285, 2)),
    X_POSITION(intToByteArray(286, 2)),
    Y_POSITION(intToByteArray(287, 2)),
    FREE_OFFSETS(intToByteArray(288, 2)),
    FREE_BYTE_COUNTS(intToByteArray(289, 2)),
    GRAY_RESPONSE_UNIT(intToByteArray(290, 2)),
    GRAY_RESPONSE_CURVE(intToByteArray(291, 2)),
    T4_OPTIONS(intToByteArray(292, 2)),
    T6_OPTIONS(intToByteArray(293, 2)),
    RESOLUTION_UNIT(intToByteArray(296, 2)),
    PAGE_NUMBER(intToByteArray(297, 2)),
    TRANSFER_FUNCTION(intToByteArray(301, 2)),
    SOFTWARE(intToByteArray(305, 2)),
    DATE_TIME(intToByteArray(306, 2)),
    ARTIST(intToByteArray(315, 2)),
    HOST_COMPUTER(intToByteArray(316, 2)),
    PREDICTOR(intToByteArray(317, 2)),
    WHITE_POINT(intToByteArray(318, 2)),
    PRIMARY_CHROMATICITIES(intToByteArray(319, 2)),
    COLOR_MAP(intToByteArray(320, 2)),
    HALF_ONE_HINTS(intToByteArray(321, 2)),
    TILE_WIDTH(intToByteArray(322, 2)),
    TILE_LENGTH(intToByteArray(323, 2)),
    TILE_OFFSETS(intToByteArray(324, 2)),
    TILE_BYTE_COUNTS(intToByteArray(325, 2)),
    INK_SET(intToByteArray(332, 2)),
    INK_NAMES(intToByteArray(333, 2)),
    NUMBER_OF_INKS(intToByteArray(334, 2)),
    DOT_RANGE(intToByteArray(336, 2)),
    TARGET_PRINTER(intToByteArray(337, 2)),
    EXTRA_SAMPLES(intToByteArray(338, 2)),
    SAMPLE_FORMAT(intToByteArray(339, 2)),
    S_MIN_SAMPLE_VALUE(intToByteArray(340, 2)),
    S_MAX_SAMPLE_VALUE(intToByteArray(341, 2)),
    TRANSFER_RANGE(intToByteArray(342, 2)),
    JPEG_PROC(intToByteArray(512, 2)),
    JPEG_INTERCHANGE_FORMAT(intToByteArray(513, 2)),
    JPEG_INTERCHANGE_FORMAT_LENGTH(intToByteArray(514, 2)),
    JPEG_RESTART_INTERVAL(intToByteArray(515, 2)),
    JPEG_LOSSLESS_PREDICTORS(intToByteArray(517, 2)),
    JPEG_POINT_TRANSFORMS(intToByteArray(518, 2)),
    JPEG_Q_TABLES(intToByteArray(519, 2)),
    JPEG_DC_TABLES(intToByteArray(520, 2)),
    JPEG_AC_TABLES(intToByteArray(521, 2)),
    Y_CB_CR_COEFFICIENTS(intToByteArray(529, 2)),
    Y_CB_CR_SUB_SAMPLING(intToByteArray(530, 2)),
    REFERENCE_BLACK_WHITE(intToByteArray(532, 2));
    companion object {
        fun fromByteArray(byteArray : ByteArray) = TagType.values().first { it.byteArray.contentEquals(byteArray) }
    }
}

private enum class DataType (val byteArray: ByteArray){

    BYTE(byteArrayOf(0,1)),
    ASCII(byteArrayOf(0,2)),
    SHORT(byteArrayOf(0,3)),
    LONG(byteArrayOf(0,4)),
    RATIONAL(byteArrayOf(0,5)),
    SBYTE(byteArrayOf(0,6)),
    UNDEFINED(byteArrayOf(0,7)),
    SSHORT(byteArrayOf(0,8)),
    SLONG(byteArrayOf(0,9)),
    SRATIONAL(byteArrayOf(0,10)),
    FLOAD(byteArrayOf(0,11)),
    DOUBLE(byteArrayOf(0,12));
    companion object {
        fun fromByteArray(byteArray : ByteArray) = DataType.values().first { it.byteArray.contentEquals(byteArray) }
    }
}

private enum class CompressionType (val integer : Int){
    NONE(1),
    CCITTRLE(2),
    CCITTFAX3(3),
    CCITTFAX4(4),
    LZW(5),
    OJPEG(6),
    JPEG(7),
    NEXT(32766),
    CCITTRLEW(32771),
    PACKBITS(32773),
    THUNDERSCAN(32809),
    IT8CTPAD(32895),
    IT8LW(32896),
    IT8MP(32897),
    IT8BL(32898),
    PIXARFILM(32908),
    PIXARLOG(32909),
    DEFLATE(32946),
    ADOBE_DEFLATE(8),
    DCS(32947),
    JBIG(34661),
    SGILOG(34676),
    SGILOG24(34677),
    JP2000(34712);

    companion object {
        fun fromInt(integer : Int) = CompressionType.values().first { it.integer == integer }
    }
}

}