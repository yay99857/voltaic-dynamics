package com.voltaicdynamics.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A game test: a {@code public static void name(GameTestHelper)} method in one of the classes listed in
 * {@link VoltaicGameTests}. It runs in the empty structure ({@code voltaicdynamics:empty}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface VoltaicTest {
    /** Ticks before the test fails if it has not succeeded. */
    int timeoutTicks() default 100;
}
