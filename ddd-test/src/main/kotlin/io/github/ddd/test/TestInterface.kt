package io.github.ddd.test

import io.ddd.annotation.*


@InterfaceToImpl
interface F {

    @ImplDefaultValue(" \"su\"  ")
    val name: String

    @ImplDefaultValue("22")
    val age: Int
}

class TestInterface {


}

fun main() {
    val f = F()
}