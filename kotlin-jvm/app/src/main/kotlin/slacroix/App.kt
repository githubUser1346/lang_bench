@file:Suppress("LiftReturnOrAssignment")

package slacroix

import com.jsoniter.JsonIterator
import com.jsoniter.annotation.JsonIgnore
import com.jsoniter.output.JsonStream
import jdk.incubator.vector.IntVector
import jdk.incubator.vector.VectorOperators
import java.util.*


var effort = parseEffort()
val effortSmall = 10_000 * effort
val effortMedium = 100_000 * effort
val effortBig = 1_000_000 * effort

var nondeterministicData: IntArray = IntArray(0)


fun main() {
    println(
        "# effort: $effort, java.vendor:${System.getProperty("java.vendor")}, java.version:${System.getProperty("java.version")}, VECTOR_ACCESS_OOB_CHECK=${
            System.getProperty(
                "jdk.incubator.vector.VECTOR_ACCESS_OOB_CHECK"
            )
        }"
    )

    val benches = listOf(
//        Bench("json-ser", "kotlin-jvm-jsoniter", effortSmall, ::benchJsoniterSer),
//        Bench("json-deser", "kotlin-jvm-jsoniter", effortSmall, ::benchJsoniterDeser),
//        Bench("itoa", "kotlin-jvm", effortBig, ::benchItoa),
//        Bench("itoa", "kotlin-jvm-heapless", effortBig, ::benchItoaHeapless),
//        Bench("nonVectoLoop", "kotlin-jvm", effortBig, ::benchNonVectoLoop),

        Bench("trivialVectoLoop", "kotlin-jvm-auto-vecto", effortMedium, ::benchTrivialAutoVectoLoop),
        Bench("trivialVectoLoop", "kotlin-jvm-explicit-vecto", effortMedium, ::benchTrivialExplicitVectoLoop),
        Bench("complexVectoLoop", "kotlin-jvm-auto-vecto", effortBig, ::benchComplexAutoVectoLoop),
        Bench("complexVectoLoop", "kotlin-jvm-explicit-vecto", effortBig, ::benchComplexExplicitVectoLoop),

//        Bench("branchingNonVectoLoop", "kotlin-jvm", effortMedium, ::benchBranchingNonVectoLoop),
//        Bench("branchingVectoLoop", "kotlin-jvm", effortMedium, ::benchBranchingAutoVectoLoop),

    )

    for (i in 1..4) {
        for (bench in benches) {
            System.gc()
            nondeterministicData = nondeterministicArray()
            val t0 = System.currentTimeMillis()
            val result = bench.runner(bench.iterCount)
            val t1 = System.currentTimeMillis()
            if (1 < i) {
                val json = JsonStream.serialize(
                    BenchResult(
                        bench.impl,
                        bench.name,
                        result.joinToString(),
                        t1 - t0,
                        getConsumedMem()
                    )
                )
                println(json)
            }
        }
    }
}

fun benchBranchingNonVectoLoop(iterCount: Int): List<String> {
    var result = 0
    for (i in 0 until iterCount) {
        for (value in nondeterministicData) {
            if (result > 100) {
                // This prevents this loop to be unrolled/auto-vectorized
                result = 0
            }
            // value will be in that range [1, 99]
            // It is important that this condition is not predicted correctly, otherwise it isnt real branching.
            if (value <= 50) {
                result += 10
            } else {
                result += 1
            }
        }
    }
    return listOf(result.toString())
}

// This method may be auto vectorized. That remains to be seen.
fun benchBranchingAutoVectoLoop(iterCount: Int): List<String> {
    var result = 0L
    for (i in 0 until iterCount) {
        for (value in nondeterministicData) {
            // value will be in that range [1, 99]
            // It is important that this condition is not predicted correctly, otherwise it isnt real branching.
            if (value <= 50) {
                result += 2
            } else {
                result += 1
            }
        }
    }
    return listOf(result.toString())
}


data class Bench(
    val name: String, val impl: String, val iterCount: Int, val runner: (iterCount: Int) -> List<String>
)


/*
@Serializable
data class DummyMessage(
    var id: Long, val vec: List<Long>, val map: Map<String, Long>
)
*/

class DummyMessage2(
    var id: Long? = null,
    val vec: List<Long>? = null,
    val map: Map<String, Long>? = null,
    @JsonIgnore val junk: String? = null
)

// 100x slower than jsoniter as of sept 2022
/*
fun benchKotlinxDeser(iterCount: Int): List<String> {
    var result: Long = 1
    val encoded = """{"id":1,"map":{"uno":12,"dos":123},"vec":[1234,12345]}""".encodeToByteArray()
    val bais = Bais(encoded)
    val zeroAscii = '0'.code.toByte()
    for (j in 0 until iterCount) {
        bais.offset = 0
        // The modulo is used only to avoid overflows.
        result = (result * 31) % 1_000_000
        encoded[6] = (zeroAscii + (result % 8)).toByte()
        val decoded = Json.decodeFromStream(DummyMessage.serializer(), bais)
        val x = decoded.id
        val y = decoded.vec.first()
        val z = decoded.map["uno"]!!
        result += (x + y + z)
    }
    return listOf(result.toString())
}
*/

/*
fun benchKotlinxJson(iterCount: Int): List<String> {
    var result: Long = 2
    var prevPrev: Long = 1
    var prev: Long = 1
    for (j in 0 until iterCount) {
        // The modulo is used only to avoid overflows.
        val next = (prevPrev + prev) % 1_000_000
        val nextStr = next.toString()
        prevPrev = prev
        prev = next

        val list = listOf(next * 2, next * 3)
        val map: Map<String, Long> = mapOf(nextStr to next * 2, (next * 3).toString() to next * 4)
        val encoded = Json.encodeToString(DummyMessage.serializer(), DummyMessage(next, list, map))
        val decoded = Json.decodeFromString(DummyMessage.serializer(), encoded)
        val x = decoded.id
        val y = decoded.vec.first()
        val z = decoded.map[nextStr]!!
        result += (x + y + z)
    }
    return listOf(result.toString())
}
*/


/*
fun benchKotlinxJsonSer(iterCount: Int): List<String> {
    var result: Long = 0
    val list = listOf(111L)
    val map: Map<String, Long> = mapOf("key" to 222)
    val message = DummyMessage(1, list, map)
    for (j in 0 until iterCount) {
        message.id = result
        val encoded = Json.encodeToString(DummyMessage.serializer(), message)
        val len = encoded.length
        result += len
    }
    return listOf(result.toString())
}
*/

/*
fun benchGson(iterCount: Int): List<String> {
    val gson = Gson()
    var result: Long = 2
    var prevPrev: Long = 1
    var prev: Long = 1
    for (j in 0 until iterCount) {
        // The modulo is used only to avoid overflows.
        val next = (prevPrev + prev) % 1_000_000
        val nextStr = next.toString()
        prevPrev = prev
        prev = next

        val list = listOf(next * 2, next * 3)
        val map: Map<String, Long> = mapOf(nextStr to next * 2, (next * 3).toString() to next * 4)
        val encoded = gson.toJson(DummyMessage(next, list, map))
        val decoded = gson.fromJson(encoded, DummyMessage::class.java)
        val x = decoded.id
        val y = decoded.vec.first()
        val z = decoded.map[nextStr]!!
        result += (x + y + z)
    }
    return listOf(result.toString())
}
*/


val list = listOf(111L)
val map: Map<String, Long> = mapOf("key" to 222)
val message = DummyMessage2(1, list, map, "")
fun benchJsoniterSer(iterCount: Int): List<String> {
    var result: Long = nondeterministicData.first().toLong()
    for (j in 0 until iterCount) {
        message.id = result
        val encoded = JsonStream.serialize(message)
        result += encoded.length
    }
    return listOf(result.toString())
}

//private const val DESER_JSON = """{"id":1,"map":{"uno":11,"dos":222},"vec":[1234,12345],"junk":"hd83hd89"}"""
private const val DESER_JSON_SPACED =
    """{"id":1 , "map" : { "uno" : 11 , "dos" : 222 } , "vec" : [ 1234 , 12345 ],"junk":"hd83hd89" }"""

fun benchJsoniterDeser(iterCount: Int): List<String> {
    val zeroAscii = '0'.code.toByte()
    var result: Long = nondeterministicData.first().toLong()
    val encoded: ByteArray = DESER_JSON_SPACED.encodeToByteArray()
    for (j in 0 until iterCount) {
        encoded[6] = (zeroAscii + (result % 8)).toByte()
        val decoded = JsonIterator.deserialize(encoded, DummyMessage2::class.java)
        val w = decoded.junk!!.length
        val x = decoded.id!!
        val y = decoded.vec!!.first()
        val z = decoded.map!!["uno"]!!
        result += (x + y + z + w)
    }
    return listOf(result.toString())
}


fun benchItoa(iterCount: Int): List<String> {
    var result = nondeterministicData.first()
    for (j in 0 until iterCount) {
        result %= 1_000_000_000
        result += result.toString().length
    }
    return listOf(result.toString())
}

fun benchItoaHeapless(iterCount: Int): List<String> {
    var result = nondeterministicData.first()
    val array = ByteArray(32)
    val buffer = ByteBuf(array)
    for (j in 0 until iterCount) {
        result %= 1_000_000_000
        getChars(result, buffer)
        result += buffer.size
    }
    return listOf(result.toString())
}

class ByteBuf(val array: ByteArray, var size: Int = 0) {

    operator fun set(index: Int, byte: Byte) {
        if (size < index + 1) {
            size = index + 1
        }
        array[index] = byte
    }
}

fun getChars(i: Int, bytes: ByteBuf): Int {
    bytes.size = 0
    var value = i
    var q: Int
    var r: Int
    var charPos = stringSize(value)
    val negative = value < 0
    if (!negative) {
        value = -value
    }

    // Generate two digits per iteration
    while (value <= -100) {
        q = value / 100
        r = q * 100 - value
        value = q
        bytes[--charPos] = digitOnes[r]
        bytes[--charPos] = digitTens[r]
    }

    // We know there are at most two digits left at this point.
    bytes[--charPos] = digitOnes[-value]
    if (value < -9) {
        bytes[--charPos] = digitTens[-value]
    }
    if (negative) {
        bytes[--charPos] = '-'.code.toByte()
    }
    return charPos
}

fun stringSize(y: Int): Int {
    var x = y
    var d = 1
    if (x >= 0) {
        d = 0
        x = -x
    }
    var p = -10
    for (i in 1..9) {
        if (x > p) return i + d
        p = 10 * p
    }
    return 10 + d
}

val digitTens = byteArrayOf(
    '0'.code.toByte(),
    '0'.code.toByte(),
    '0'.code.toByte(),
    '0'.code.toByte(),
    '0'.code.toByte(),
    '0'.code.toByte(),
    '0'.code.toByte(),
    '0'.code.toByte(),
    '0'.code.toByte(),
    '0'.code.toByte(),
    '1'.code.toByte(),
    '1'.code.toByte(),
    '1'.code.toByte(),
    '1'.code.toByte(),
    '1'.code.toByte(),
    '1'.code.toByte(),
    '1'.code.toByte(),
    '1'.code.toByte(),
    '1'.code.toByte(),
    '1'.code.toByte(),
    '2'.code.toByte(),
    '2'.code.toByte(),
    '2'.code.toByte(),
    '2'.code.toByte(),
    '2'.code.toByte(),
    '2'.code.toByte(),
    '2'.code.toByte(),
    '2'.code.toByte(),
    '2'.code.toByte(),
    '2'.code.toByte(),
    '3'.code.toByte(),
    '3'.code.toByte(),
    '3'.code.toByte(),
    '3'.code.toByte(),
    '3'.code.toByte(),
    '3'.code.toByte(),
    '3'.code.toByte(),
    '3'.code.toByte(),
    '3'.code.toByte(),
    '3'.code.toByte(),
    '4'.code.toByte(),
    '4'.code.toByte(),
    '4'.code.toByte(),
    '4'.code.toByte(),
    '4'.code.toByte(),
    '4'.code.toByte(),
    '4'.code.toByte(),
    '4'.code.toByte(),
    '4'.code.toByte(),
    '4'.code.toByte(),
    '5'.code.toByte(),
    '5'.code.toByte(),
    '5'.code.toByte(),
    '5'.code.toByte(),
    '5'.code.toByte(),
    '5'.code.toByte(),
    '5'.code.toByte(),
    '5'.code.toByte(),
    '5'.code.toByte(),
    '5'.code.toByte(),
    '6'.code.toByte(),
    '6'.code.toByte(),
    '6'.code.toByte(),
    '6'.code.toByte(),
    '6'.code.toByte(),
    '6'.code.toByte(),
    '6'.code.toByte(),
    '6'.code.toByte(),
    '6'.code.toByte(),
    '6'.code.toByte(),
    '7'.code.toByte(),
    '7'.code.toByte(),
    '7'.code.toByte(),
    '7'.code.toByte(),
    '7'.code.toByte(),
    '7'.code.toByte(),
    '7'.code.toByte(),
    '7'.code.toByte(),
    '7'.code.toByte(),
    '7'.code.toByte(),
    '8'.code.toByte(),
    '8'.code.toByte(),
    '8'.code.toByte(),
    '8'.code.toByte(),
    '8'.code.toByte(),
    '8'.code.toByte(),
    '8'.code.toByte(),
    '8'.code.toByte(),
    '8'.code.toByte(),
    '8'.code.toByte(),
    '9'.code.toByte(),
    '9'.code.toByte(),
    '9'.code.toByte(),
    '9'.code.toByte(),
    '9'.code.toByte(),
    '9'.code.toByte(),
    '9'.code.toByte(),
    '9'.code.toByte(),
    '9'.code.toByte(),
    '9'.code.toByte()
)

val digitOnes = byteArrayOf(
    '0'.code.toByte(),
    '1'.code.toByte(),
    '2'.code.toByte(),
    '3'.code.toByte(),
    '4'.code.toByte(),
    '5'.code.toByte(),
    '6'.code.toByte(),
    '7'.code.toByte(),
    '8'.code.toByte(),
    '9'.code.toByte(),
    '0'.code.toByte(),
    '1'.code.toByte(),
    '2'.code.toByte(),
    '3'.code.toByte(),
    '4'.code.toByte(),
    '5'.code.toByte(),
    '6'.code.toByte(),
    '7'.code.toByte(),
    '8'.code.toByte(),
    '9'.code.toByte(),
    '0'.code.toByte(),
    '1'.code.toByte(),
    '2'.code.toByte(),
    '3'.code.toByte(),
    '4'.code.toByte(),
    '5'.code.toByte(),
    '6'.code.toByte(),
    '7'.code.toByte(),
    '8'.code.toByte(),
    '9'.code.toByte(),
    '0'.code.toByte(),
    '1'.code.toByte(),
    '2'.code.toByte(),
    '3'.code.toByte(),
    '4'.code.toByte(),
    '5'.code.toByte(),
    '6'.code.toByte(),
    '7'.code.toByte(),
    '8'.code.toByte(),
    '9'.code.toByte(),
    '0'.code.toByte(),
    '1'.code.toByte(),
    '2'.code.toByte(),
    '3'.code.toByte(),
    '4'.code.toByte(),
    '5'.code.toByte(),
    '6'.code.toByte(),
    '7'.code.toByte(),
    '8'.code.toByte(),
    '9'.code.toByte(),
    '0'.code.toByte(),
    '1'.code.toByte(),
    '2'.code.toByte(),
    '3'.code.toByte(),
    '4'.code.toByte(),
    '5'.code.toByte(),
    '6'.code.toByte(),
    '7'.code.toByte(),
    '8'.code.toByte(),
    '9'.code.toByte(),
    '0'.code.toByte(),
    '1'.code.toByte(),
    '2'.code.toByte(),
    '3'.code.toByte(),
    '4'.code.toByte(),
    '5'.code.toByte(),
    '6'.code.toByte(),
    '7'.code.toByte(),
    '8'.code.toByte(),
    '9'.code.toByte(),
    '0'.code.toByte(),
    '1'.code.toByte(),
    '2'.code.toByte(),
    '3'.code.toByte(),
    '4'.code.toByte(),
    '5'.code.toByte(),
    '6'.code.toByte(),
    '7'.code.toByte(),
    '8'.code.toByte(),
    '9'.code.toByte(),
    '0'.code.toByte(),
    '1'.code.toByte(),
    '2'.code.toByte(),
    '3'.code.toByte(),
    '4'.code.toByte(),
    '5'.code.toByte(),
    '6'.code.toByte(),
    '7'.code.toByte(),
    '8'.code.toByte(),
    '9'.code.toByte(),
    '0'.code.toByte(),
    '1'.code.toByte(),
    '2'.code.toByte(),
    '3'.code.toByte(),
    '4'.code.toByte(),
    '5'.code.toByte(),
    '6'.code.toByte(),
    '7'.code.toByte(),
    '8'.code.toByte(),
    '9'.code.toByte()
)

private fun getConsumedMem() = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000_000

fun benchNonVectoLoop(iterCount: Int): List<String> {
    var counter = 0L
    var result: Long = 0

    // Because each iteration depends on the previous one(via the result variable),
    // this loop is not vectorized by the compiler.
    // It is observable by the fact that its vectorizable cousin, benchComplexAutoVectoLoop, has a different
    // performance signature across languages.
    for (i in nondeterministicData) {
        for (j in nondeterministicData) {
            for (k in nondeterministicData) {
                for (l in nondeterministicData) {
                    result += (i * j + k * l + result) and 1023
                }
                counter += nondeterministicData.size
                if (counter >= iterCount) {
                    return listOf(result.toString())
                }
            }
        }
    }
    throw IllegalStateException()
}

fun benchComplexAutoVectoLoop(iterCount: Int): List<String> {
    var counter = 0L
    var result: Long = 0

    for (i in nondeterministicData) {
        for (j in nondeterministicData) {
            for (k in nondeterministicData) {
                for (l in nondeterministicData) {
                    result += (i * j + k * l + 7) and 31
                }
                counter += nondeterministicData.size
                if (counter >= iterCount) {
                    return listOf(result.toString())
                }
            }
        }
    }
    throw IllegalStateException()
}


val SPECIES = IntVector.SPECIES_256
val v7 = IntVector.broadcast(SPECIES, 7)
val v31 = IntVector.broadcast(SPECIES, 31)

fun benchComplexExplicitVectoLoop(iterCount: Int): List<String> {
    var counter = 0L
    var sumVec = IntVector.broadcast(SPECIES, 0)

    for (i in nondeterministicData) {
        val i_vec = IntVector.broadcast(SPECIES, i)
        for (j in nondeterministicData) {
            val j_vec = IntVector.broadcast(SPECIES, j)
            for (k in nondeterministicData) {
                val k_vec = IntVector.broadcast(SPECIES, k)
                for (l in nondeterministicData.indices step SPECIES.length()) {
                    val l_vec = IntVector.fromArray(SPECIES, nondeterministicData, l)
                    val mul1 = i_vec.mul(j_vec)
                    val mul2 = k_vec.mul(l_vec)
                    sumVec = sumVec.add(mul1.add(mul2).add(v7).and(v31))
                    val horArr = sumVec.reduceLanes(VectorOperators.ADD)
//                    println(horArr)
                }

                counter += nondeterministicData.size
                if (counter >= iterCount) {
                    val result = sumVec.reduceLanes(VectorOperators.ADD)
                    return listOf(result.toString())
                }
            }
        }
    }
    throw IllegalStateException()
}

fun benchTrivialAutoVectoLoop(iterCount: Int): List<String> {
    var result = 0
    for (i in 0 until iterCount) {
        val mask = i + 31
        for (element in nondeterministicData) {
            result += (element and mask)
        }
    }
    return listOf(result.toString())
}

fun benchTrivialExplicitVectoLoop(iterCount: Int): List<String> {
    var sumVec = IntVector.broadcast(SPECIES, 0)
    for (i in 0 until iterCount) {
        val maskVec = IntVector.broadcast(SPECIES, i + 31)
        for (j in nondeterministicData.indices step SPECIES.length()) {
            val dataVec = IntVector.fromArray(SPECIES, nondeterministicData, j)
            val maskedVec = dataVec.and(maskVec)
            sumVec = sumVec.add(maskedVec)
//            println("----")
        }
    }
    val result = sumVec.reduceLanes(VectorOperators.ADD)
    return listOf(result.toString())
}


// TODO
interface Bench2 {
    val iterCount: Int
    fun name(): String = javaClass.simpleName
    fun setup(seed: Int)
    fun run(): String
}

// TODO
class NonVectoLoop(override val iterCount: Int) : Bench2 {

    var array = IntArray(0)

    override fun setup(seed: Int) {
        TODO()
    }

    override fun run(): String {
        TODO()
    }
}

fun nondeterministicArray(): IntArray {
    // pseudo random data
    val result = intArrayOf(
        36,
        31,
        68,
        73,
        91,
        33,
        4,
        30,
        94,
        28,
        82,
        46,
        87,
        89,
        47,
        63,
        46,
        97,
        74,
        60,
        61,
        24,
        11,
        72,
        24,
        17,
        22,
        9,
        65,
        81,
        71,
        17,
        88,
        1,
        20,
        70,
        94,
        52,
        94,
        22,
        95,
        84,
        93,
        65,
        10,
        50,
        99,
        73,
        35,
        53,
        93,
        46,
        33,
        85,
        81,
        52,
        22,
        91,
        87,
        70,
        60,
        94,
        80,
        59,
        12,
        44,
        43,
        68,
        49,
        33,
        21,
        40,
        51,
        95,
        81,
        84,
        18,
        2,
        94,
        34,
        98,
        95,
        44,
        10,
        4,
        35,
        97,
        57,
        2,
        98,
        58,
        4,
        52,
        15,
        40,
        20,
        84,
        48,
        60,
        21,
        71,
        47,
        53,
        38,
        95,
        69,
        58,
        32,
        66,
        37,
        72,
        29,
        32,
        63,
        1,
        94,
        8,
        67,
        88,
        77,
        51,
        77,
        71,
        83,
        62,
        33,
        32,
        89,
        5,
        17,
        81,
        37,
        9,
        32,
        9,
        7,
        83,
        28,
        60,
        14,
        9,
        86,
        43,
        5,
        82,
        68,
        82,
        99,
        98,
        96,
        38,
        54,
        97,
        51,
        34,
        90,
        47,
        89,
        12,
        9,
        16,
        39,
        41,
        66,
        99,
        27,
        78,
        38,
        50,
        50,
        67,
        39,
        70,
        78,
        58,
        21,
        47,
        11,
        46,
        41,
        21,
        96,
        24,
        44,
        80,
        95,
        56,
        13,
        94,
        79,
        43,
        48,
        86,
        64,
        9,
        24,
        31,
        49,
        30,
        58,
        43,
        38,
        31,
        39,
        65,
        84,
        30,
        3,
        3,
        20,
        73,
        63,
        84,
        76,
        14,
        24,
        77,
        51,
        50,
        22,
        7,
        11,
        58,
        40,
        72,
        35,
        76,
        20,
        44,
        94,
        6,
        3,
        95,
        50,
        32,
        4,
        81,
        71,
        67,
        45,
        69,
        12,
        21,
        78,
        49,
        96,
        50,
        3,
        47,
        53,
        35,
        47,
        75,
        40,
        24,
        34,
        68,
        38,
        48,
        2,
        69,
        22,
        19,
        24,
        27,
        27,
        70,
        41,
        62,
        55,
        91,
        3,
        72,
        16,
        52,
        40,
        55,
        68,
        79,
        13,
        71,
        27,
        64,
        24,
        63,
        70,
        60,
        48,
        36,
        83,
        16,
        86,
        99,
        69,
        15,
        5,
        24,
        30,
        63,
        98,
        89,
        51,
        9,
        40,
        57,
        67,
        96,
        32,
        42,
        60,
        79,
        86,
        57,
        48,
        65,
        56,
        69,
        98,
        43,
        21,
        92,
        20,
        38,
        40,
        24,
        62,
        62,
        5,
        54,
        50,
        58,
        53,
        32,
        31,
        17,
        11,
        20,
        30,
        95,
        53,
        21,
        47,
        86,
        51,
        11,
        69,
        95,
        48,
        56,
        91,
        49,
        29,
        12,
        95,
        19,
        55,
        79,
        42,
        50,
        12,
        49,
        20,
        57,
        20,
        11,
        29,
        71,
        64,
        68,
        86,
        50,
        65,
        60,
        90,
        93,
        75,
        65,
        4,
        6,
        67,
        95,
        34,
        42,
        54,
        13,
        58,
        52,
        5,
        11,
        79,
        37,
        41,
        83,
        9,
        96,
        43,
        15,
        32,
        30,
        35,
        29,
        98,
        53,
        80,
        74,
        89,
        65,
        30,
        14,
        69,
        73,
        82,
        87,
        61,
        50,
        44,
        53,
        12,
        39,
        37,
        47,
        21,
        82,
        66,
        8,
        49,
        99,
        52,
        97,
        66,
        51,
        33,
        35,
        19,
        45,
        28,
        86,
        90,
        20,
        43,
        24,
        37,
        39,
        83,
        27,
        2,
        12,
        4,
        77,
        87,
        90,
        62,
        46,
        22,
        50,
        90,
        55,
        85,
        84,
        57,
        72,
        65,
        3,
        7,
        90,
        31,
        50,
        80,
        11,
        60,
        27,
        78,
        36,
        3,
        43,
        81,
        10,
        93,
        28,
        98,
        86,
        99,
        9,
        67,
        18,
        31,
        81,
        14,
        53,
        11,
        90,
        17,
        60,
        66,
        88,
        73,
        26,
        13,
        29,
        3,
        69,
        48,
        24,
        25,
        16,
        36,
        73,
        79,
        91,
        16,
        76,
        8
    )
    require(result.size == 512)
    val random = Random()
    for (i in result.indices) {
        val nondeterministicLong = random.nextLong()
        if (nondeterministicLong == 1L) {
            // Statistically, that will branch will never be taken,
            // If ever this is invoked... the benchmark will fail during the result verification in BenchRunner.
            result[i] = nondeterministicLong.toInt()
        }
    }
    return result
}

data class BenchResult(
    val impl: String, val bench: String, val result: String, val ms: Long, val consumedMemoryMb: Long
)

private fun parseEffort(): Int {
    val effortString: String = System.getenv("LANG_BENCH_EFFORT") ?: "1"
    return try {
        effortString.toInt()
    } catch (ex: NumberFormatException) {
        1
    }
}