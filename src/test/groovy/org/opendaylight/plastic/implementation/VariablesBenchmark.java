package org.opendaylight.plastic.implementation;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// src/test/scripts/json-finder-binder-benchmark
// or
// java -jar target/odl-plastic-*-fat-tests.jar JsonFinderBinderBenchmark

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3)
@Measurement(iterations = 8)
public class VariablesBenchmark {

    static long N = 5000000;

    public static List<String> singleVar = new ArrayList<>();
    static {
        singleVar.add("abcdef");
        singleVar.add("${abcdef}");
        singleVar.add("${abcdefghijklmnopqrstuvwxyz=123456790}");
    }

    public static List<String> multiVar = new ArrayList<>();
    static {
        multiVar.add("${abcdef}${ghijkl}");
        multiVar.add("${abcdef}${ghijkl}${mnopq}");
        multiVar.add("${abcdef=123}${ghijkl=1234567890}");
    }

    @Benchmark
    public void parsingSingle(Blackhole blackhole) {

        Variables instance = new Variables();

        for(long i = 0; i< N; i++) {
            for (String input : singleVar) {
                Map<String, Variables.Finding> result = instance.parse(input);
                blackhole.consume(result);
            }
        }
    }

    @Benchmark
    public void parsingMultiple(Blackhole blackhole) {

        Variables instance = new Variables();

        for(long i = 0; i< N; i++) {
            for (String input : multiVar) {
                Map<String, Variables.Finding> result = instance.parse(input);
                blackhole.consume(result);
            }
        }
    }
}
