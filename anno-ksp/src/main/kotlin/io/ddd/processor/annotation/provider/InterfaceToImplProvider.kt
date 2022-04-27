package io.ddd.processor.annotation.provider

import com.google.devtools.ksp.processing.*
import io.ddd.processor.annotation.processor.*

class InterfaceToImplProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return InterfaceToImplProcessor(
            environment.logger,
            environment.options,
            environment.codeGenerator
        )
    }
}