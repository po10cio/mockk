package io.mockk.impl.stub

import io.mockk.Invocation
import kotlin.reflect.KClass

class SpyKStub<T : Any>(cls: KClass<T>, name: String, gatewayAccess: StubGatewayAccess)
    : MockKStub(cls, name, false, gatewayAccess) {

    override fun defaultAnswer(invocation: Invocation): Any? {
        return invocation.originalCall()
    }

    override fun toStr(): String = "spyk<" + type.simpleName + ">($name)#$hashCodeStr"
}