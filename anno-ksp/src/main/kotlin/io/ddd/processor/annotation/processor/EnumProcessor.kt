package io.ddd.processor.annotation.processor

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import io.ddd.processor.*
import java.io.*


class EnumProcessor(
    private val logger: KSPLogger,
    private val options: Map<String, String>,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(AssertEnumName)
            .filterIsInstance<KSClassDeclaration>()
        if (!symbols.iterator().hasNext()) {
            return emptyList()
        }
        val funcMap = mutableMapOf<String, MutableList<OriginatingElementsHolder>>()
        symbols.forEach { it.accept(Visitor(funcMap), Unit) }
        funcMap.forEach { (packageName, value) ->
            codeGenerator.createNewFile(
                // Make sure to associate the generated file with sources to keep/maintain it across incremental builds.
                // Learn more about incremental processing in KSP from the official docs:
                // https://kotlinlang.org/docs/ksp-incremental.html
                dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray()),
                packageName = packageName,
                fileName = "GeneratedFunctions"
            ).use { os ->
                value.forEach {
                    os += it.toString()
                }
            }
        }
        return symbols.filterNot { it.validate() }.toList()
    }


    inner class Visitor(private val funcMap: MutableMap<String, MutableList<OriginatingElementsHolder>>) :
        KSVisitorVoid() {


        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            if (classDeclaration.classKind != ClassKind.ENUM_CLASS) {
                return
            }
            classDeclaration.declarations.filter {
                it is KSClassDeclaration && it.classKind == ClassKind.ENUM_ENTRY
            }.forEach {
                val name = it.simpleName
                    .asString()
                    .lowercase()
                    .replace("_([a-z])".toRegex()) { r -> r.groupValues[1].uppercase() }
                    .replaceFirstChar { it.uppercase() }
                val map = funcMap.getOrPut(classDeclaration.packageName.asString()) { mutableListOf() }
                val enumClassName = ClassName.bestGuess(classDeclaration.qualifiedName!!.asString())
                map += PropertySpec.builder("is$name", Boolean::class)
                    .getter(FunSpec.getterBuilder().addCode("return this == ${it.qualifiedName!!.asString()}").build())
                    .receiver(enumClassName)
                    .build()

                map += FunSpec.builder("if$name")
                    .addModifiers(KModifier.INLINE)
                    .addParameter(
                        ParameterSpec.builder(
                            "block", LambdaTypeName.get(returnType = enumClassName)
                        ).build()
                    ).addCode(" return if(this == ${it.qualifiedName!!.asString()}) {  block()} else {  this }")
                    .receiver(enumClassName)
                    .returns(enumClassName)
                    .build()

                map += FunSpec.builder("on$name")
                    .addModifiers(KModifier.INLINE)
                    .addParameter(
                        ParameterSpec.builder(
                            "block", LambdaTypeName.get(returnType = UNIT)
                        ).build()
                    ).addCode("  if(this == ${it.qualifiedName!!.asString()}) {  block()} ")
                    .receiver(enumClassName)
                    .build()
            }
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
            super.visitPropertyDeclaration(property, data)
        }

        override fun visitTypeArgument(typeArgument: KSTypeArgument, data: Unit) {
            super.visitTypeArgument(typeArgument, data)
        }
    }


}


operator fun OutputStream.plusAssign(str: String) {
    this.write(str.toByteArray())
}
