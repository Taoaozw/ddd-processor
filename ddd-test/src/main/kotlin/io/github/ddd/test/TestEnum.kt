package io.github.ddd.test

import ifA
import io.ddd.annotation.*
import isA
import onA


@AssertEnum
enum class TestEnum {
    A,
    B,
    C;


    companion object {
        fun valueOf(code: Int) {
            A.ordinal
        }
    }
}

class Main(val name: TestEnum = TestEnum.B)

fun main() {
    println(TestEnum.A.isA)
    val onA = TestEnum.A.onA {
        println("Unit type")
    }
    println(onA)
    TestEnum.A.ifA {
        println("A")
        TestEnum.B
    }
    println("Hello World !")
}


//
//fun F(
//    name: String,
//    age: Int
//): F {
//    return FImpl(
//        name = name,
//        age = age
//    )
//}
//
//val c = F(",a", 2)
//
//data class FImpl(
//    override val name: String,
//    override val age: Int
//) : F
//
