package io.ddd.processor.annotation.processor

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import io.ddd.annotation.*
import io.ddd.processor.*

class InterfaceToImplProcessor(
    private val logger: KSPLogger,
    private val options: Map<String, String>,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(InterfaceToImplEnumName)
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
                fileName = "InterfaceToImpl",
            ).use { os ->
                os+="package $packageName\n\n"
                value.forEach {
                    os += it.toString()
                }
            }
        }
        return symbols.filterNot { it.validate() }.toList()
    }

    inner class Visitor(private val funcMap: MutableMap<String, MutableList<OriginatingElementsHolder>>) :
        KSVisitorVoid() {


        @OptIn(KspExperimental::class)
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            if (classDeclaration.classKind != ClassKind.INTERFACE) {
                logger.error("InterfaceToImpl Annotation is only for interface")
                return
            } else {
                val interfaceType = ClassName.bestGuess(classDeclaration.qualifiedName!!.asString())
                val map = funcMap.getOrPut(classDeclaration.packageName.asString()) { mutableListOf() }
                val toImplBuilder =
                    TypeSpec.classBuilder("${classDeclaration.simpleName.asString()}Impl").addModifiers(KModifier.DATA).addSuperinterface(interfaceType)
                val constructorBuilder = FunSpec.constructorBuilder()
                val funcBuilder = FunSpec.builder(classDeclaration.simpleName.asString()).returns(interfaceType)
                classDeclaration.getDeclaredProperties().forEach { kProp ->
                    val bestGuess = ClassName.bestGuess(kProp.type.resolve().declaration.qualifiedName!!.asString())
                    val parameterSpec = ParameterSpec.builder(kProp.simpleName.asString(), bestGuess)
                    kProp.getAnnotationsByType(ImplDefaultValue::class).firstOrNull()?.let {
                        parameterSpec.defaultValue(it.value)
                    }
                    funcBuilder.addParameter(parameterSpec.build())
                    constructorBuilder.addParameter(parameterSpec.build())
                    toImplBuilder.addProperty(
                        PropertySpec.builder(kProp.simpleName.asString(), bestGuess)
                            .initializer(kProp.simpleName.asString())
                            .addModifiers(KModifier.OVERRIDE)
                            .build()
                    )
                }
                funcBuilder.addCode(
                    """
                        return ${classDeclaration.simpleName.asString()}Impl(
                            ${classDeclaration.getDeclaredProperties().joinToString { it.simpleName.asString() }}
                        )
                    """.trimIndent()
                )
                toImplBuilder.primaryConstructor(constructorBuilder.build())
                map += toImplBuilder.build()
                map += funcBuilder.build()
            }
        }

        override fun visitPropertyGetter(getter: KSPropertyGetter, data: Unit) {
            super.visitPropertyGetter(getter, data)
        }
    }


}