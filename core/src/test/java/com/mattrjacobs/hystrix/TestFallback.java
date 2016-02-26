package com.mattrjacobs.hystrix;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestFallback {

    @Test
    public void testFoo() {
        assertEquals(0, 9);
    }
}
