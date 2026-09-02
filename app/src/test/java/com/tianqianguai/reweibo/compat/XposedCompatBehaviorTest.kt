package com.tianqianguai.reweibo.compat

import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class XposedCompatBehaviorTest {
    private class Fixture {
        companion object {
            @JvmStatic
            fun echo(value: String): String = value
        }
    }

    private val method = Fixture::class.java.getDeclaredMethod("echo", String::class.java)

    @Test
    fun beforeAndAfterCanTransformTheCall() {
        val callback = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.args[0] = "changed"
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                param.result = "${param.result}:after"
            }
        }

        val result = XposedBridge.invokeLegacyForTest(
            method,
            callback,
            null,
            arrayOf("original")
        ) { args -> "origin:${args[0]}" }

        assertEquals("origin:changed:after", result)
    }

    @Test
    fun earlyResultSkipsTheOriginalCall() {
        val proceeded = AtomicBoolean(false)
        val callback = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = "early"
            }
        }

        val result = XposedBridge.invokeLegacyForTest(
            method,
            callback,
            null,
            arrayOf("original")
        ) {
            proceeded.set(true)
            "origin"
        }

        assertEquals("early", result)
        assertFalse(proceeded.get())
    }

    @Test
    fun beforeThrowableSkipsOriginalAndPropagates() {
        val proceeded = AtomicBoolean(false)
        val expected = IllegalArgumentException("blocked")
        val callback = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.throwable = expected
            }
        }

        val thrown = assertThrows(IllegalArgumentException::class.java) {
            XposedBridge.invokeLegacyForTest(
                method,
                callback,
                null,
                arrayOf("original")
            ) {
                proceeded.set(true)
                "origin"
            }
        }

        assertSame(expected, thrown)
        assertFalse(proceeded.get())
    }

    @Test
    fun afterCanRecoverOriginalThrowable() {
        val callback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (param.hasThrowable()) param.result = "recovered"
            }
        }

        val result = XposedBridge.invokeLegacyForTest(
            method,
            callback,
            null,
            arrayOf("original")
        ) { throw IllegalStateException("boom") }

        assertEquals("recovered", result)
    }

    @Test
    fun crashingBeforeFallsBackToOriginalArguments() {
        val callback = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.args[0] = "mutated"
                throw IllegalStateException("callback bug")
            }
        }

        val result = XposedBridge.invokeLegacyForTest(
            method,
            callback,
            null,
            arrayOf("original")
        ) { args -> "origin:${args[0]}" }

        assertEquals("origin:original", result)
    }

    @Test
    fun methodReplacementUsesEarlyReturnSemantics() {
        val proceeded = AtomicBoolean(false)
        val callback = XC_MethodReplacement.returnConstant("replacement")

        val result = XposedBridge.invokeLegacyForTest(
            method,
            callback,
            null,
            arrayOf("original")
        ) {
            proceeded.set(true)
            "origin"
        }

        assertEquals("replacement", result)
        assertFalse(proceeded.get())
    }
}
