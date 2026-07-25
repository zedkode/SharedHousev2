package com.sharedhouse.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ApiContractTest {
    @Test
    fun exposesVersionOne() {
        assertEquals("v1", ApiContract.VERSION)
    }
}
