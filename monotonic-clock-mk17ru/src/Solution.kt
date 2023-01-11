import java.lang.IllegalArgumentException

/**
 * В теле класса решения разрешено использовать только переменные делегированные в класс RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author : Frolov Mihail
 */
class Solution : MonotonicClock {

    private var c11 by RegularInt(0)
    private var c21 by RegularInt(0)
    private var c12 by RegularInt(0)
    private var c22 by RegularInt(0)
    private var c13 by RegularInt(0)

    fun readInRightOrder(time : Time) {
        c11 = time.d1
        c12 = time.d2
        c13 = time.d3
    }

    fun readInReverseOrder(time : Time) {
        c13 = time.d3
        c12 = time.d2
        c11 = time.d1
    }

    override fun write(time: Time) {
        c21 = time.d1
        c22 = time.d2
        //c23 = time.d3

        readInReverseOrder(time)
    }


    override fun read(): Time {
        // read left-to-right
        val c11 = c11
        val c12 = c12
        val c13 = c13
//        val c23 = c23
        val c22 = c22
        val c21 = c21
        if (c11 != c21) {
            return Time(c21, 0, 0)
        } else if (c12 != c22) {
            return Time(c21, c22, 0)
        } else {
            return Time(c21, c22, c13)
        }
    }
}