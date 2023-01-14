package slacroix.demo

import jdk.incubator.vector.FloatVector
import jdk.incubator.vector.VectorOperators
import jdk.incubator.vector.VectorSpecies
import java.util.regex.Pattern

private val SPECIES: VectorSpecies<Float> = FloatVector.SPECIES_PREFERRED

fun main() {
    val (firstName, lastName) = parseName("Joe Smith")
    println("$firstName; $lastName;")
}

fun parseName(string: String): Pair<String, String> {
    val tokens: List<String> = string.trim().split(Pattern.compile("\\s+"))
    return Pair(tokens[0], tokens[1])
}

private fun vectoSum(arr: FloatArray): Float {
    var sum = FloatVector.zero(SPECIES)
    var i = 0
    while (i < arr.size) {
        val chunck = FloatVector.fromArray(SPECIES, arr, i)
        sum = sum.add(chunck)
        i += SPECIES.length()
    }
    val result = sum.reduceLanes(VectorOperators.ADD)
    return result;
}

fun vectorComputation(a: FloatArray, b: FloatArray, c: FloatArray) {
    var i = 0
    val upperBound: Int = SPECIES.loopBound(a.size)
    while (i < upperBound) {

        // FloatVector va, vb, vc;
        val va = FloatVector.fromArray(SPECIES, a, i)
        val vb = FloatVector.fromArray(SPECIES, b, i)
        val vc = va.mul(va)
            .add(vb.mul(vb))
            .neg()
        vc.intoArray(c, i)
        i += SPECIES.length()
    }
    while (i < a.size) {
        c[i] = (a[i] * a[i] + b[i] * b[i]) * -1.0f
        i++
    }
}


