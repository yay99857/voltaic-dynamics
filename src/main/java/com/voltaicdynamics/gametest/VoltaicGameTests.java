package com.voltaicdynamics.gametest;

import com.voltaicdynamics.VoltaicDynamics;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Registers every {@link VoltaicTest} method as a game test. Game tests are data now: a test function (the method)
 * plus a test instance that says where and how long it runs. Run them with {@code ./gradlew runGameTestServer}.
 */
public final class VoltaicGameTests {
    private static final List<Class<?>> TEST_CLASSES = List.of(CoreGameTests.class, PowerGameTests.class,
            GeneratorGameTests.class, MachineGameTests.class, TierGameTests.class, LogisticsGameTests.class,
            GadgetGameTests.class, CompatGameTests.class, ModStorageGameTests.class);
    private static final Identifier STRUCTURE = VoltaicDynamics.id("empty");

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, VoltaicDynamics.MOD_ID);
    private static final List<Test> TESTS = new ArrayList<>();

    private record Test(Identifier id, int timeoutTicks) {
    }

    private VoltaicGameTests() {
    }

    public static void register(IEventBus modEventBus) {
        for (Class<?> type : TEST_CLASSES) {
            for (Method method : type.getDeclaredMethods()) {
                VoltaicTest test = method.getAnnotation(VoltaicTest.class);
                if (test == null) continue;
                if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1
                        || method.getParameterTypes()[0] != GameTestHelper.class) {
                    throw new IllegalStateException("@VoltaicTest needs a static method taking a GameTestHelper: " + method);
                }
                String name = snake(type.getSimpleName().replace("GameTests", "")) + "/" + snake(method.getName());
                FUNCTIONS.register(name, () -> helper -> invoke(method, helper));
                TESTS.add(new Test(VoltaicDynamics.id(name), test.timeoutTicks()));
            }
        }
        FUNCTIONS.register(modEventBus);
        modEventBus.addListener(VoltaicGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(VoltaicDynamics.id("default"));
        for (Test test : TESTS) {
            event.registerTest(test.id(), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, test.id()),
                    new TestData<>(environment, STRUCTURE, test.timeoutTicks(), 0, true)));
        }
    }

    private static void invoke(Method method, GameTestHelper helper) {
        try {
            method.invoke(null, helper);
        } catch (InvocationTargetException e) {
            // Assertion failures are what the test framework reports; rethrow them as they are.
            if (e.getCause() instanceof RuntimeException runtime) throw runtime;
            if (e.getCause() instanceof Error error) throw error;
            throw new RuntimeException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String snake(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
