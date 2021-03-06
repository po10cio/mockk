package io.mockk.impl.instantiation

import io.mockk.MethodDescription
import io.mockk.MockKException
import io.mockk.impl.stub.Stub
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.Callable
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.kotlinFunction

internal object JvmMockFactoryHelper {
    fun mockHandler(stub: Stub): (Any, Method, Callable<*>?, Array<Any?>) -> Any? {
        return { self, method, originalMethod, args ->
            stdFunctions(self, method, args) {
                stub.handleInvocation(self, method.toDescription(), {
                    handleOriginalCall(originalMethod, method)
                }, args)
            }
        }
    }

    inline fun stdFunctions(self: Any,
                            method: Method,
                            args: Array<Any?>,
                            otherwise: () -> Any?): Any? {
        if (self is Class<*>) {
            if (method.isHashCode()) {
                return System.identityHashCode(self)
            } else if (method.isEquals()) {
                return self === args[0]
            }
        }
        return otherwise()
    }

    fun handleOriginalCall(originalMethod: Callable<*>?, method: Method): Any? {
        if (originalMethod == null) {
            throw MockKException("No way to call original method ${method.toDescription()}")
        }

        return try {
            originalMethod.call()
        } catch (ex: InvocationTargetException) {
            throw ex.cause ?: throw ex
        }
    }


    fun Method.varArgPosition(): Int {
        val kFunc = kotlinFunction

        return if (kFunc != null)
            kFunc.parameters
                    .filter { it.kind != KParameter.Kind.INSTANCE }
                    .indexOfFirst { it.isVararg }
        else
            parameters.indexOfFirst { it.isVarArgs }
    }

    fun Method.toDescription() =
            MethodDescription(name,
                    returnType.kotlin,
                    declaringClass.kotlin,
                    parameterTypes.map { it.kotlin },
                    varArgPosition())

    fun Method.isHashCode() = name == "hashCode" && parameterTypes.isEmpty()
    fun Method.isEquals() = name == "equals" && parameterTypes.size == 1 && parameterTypes[0] === Object::class.java
}