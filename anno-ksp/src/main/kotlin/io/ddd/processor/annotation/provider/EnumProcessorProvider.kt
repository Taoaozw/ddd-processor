package io.ddd.processor.annotation.provider

import com.google.devtools.ksp.processing.*
import io.ddd.processor.annotation.processor.*

class EnumProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        EnumProcessor(
            environment.logger,
            environment.options,
            environment.codeGenerator,
        )
}