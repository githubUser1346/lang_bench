import kotlin.random.Random

fun main() {

    val random = Random(123)
    print("{")
    for (i in 0 until 512) {
        print(random.nextInt(99) + 1)
        if (i < 512 - 1) {
            print(", ")
        }
    }
    print("}")

}