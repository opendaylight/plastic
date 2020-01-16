package org.opendaylight.plastic.implementation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Map;
import java.util.concurrent.TimeUnit;

// src/test/scripts/json-finder-binder-benchmark
// or
// java -jar target/odl-plastic-*-fat-tests.jar JsonFinderBinderBenchmark

public class JsonFinderBinderBenchmark {

    @State(Scope.Benchmark)
    public static class Parameters {
        Gson gson =  new GsonBuilder().serializeNulls().setPrettyPrinting().disableHtmlEscaping().create();
        JsonFinderBinder instance = new JsonFinderBinder();

        String schema =
        "{" +
                "\"addresses\": [ \"${ADD[*]}\" ]" +
        "}";

        String jpayload =
        "{" +
            "\"addresses\": [ \"1.2.3.4\", \"5.6.7.8\", \"9.10.11.12\" ]" +
        "}";

        Map model = gson.fromJson(schema, Map.class);
        Map payload = gson.fromJson(jpayload, Map.class);
    }

    public static Parameters parms = new Parameters();

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Fork(1)
    @Warmup(iterations = 3)
    @Measurement(iterations = 8)
    public void profile() {

        long N = 1000000;

        for(long i = 0; i< N; i++) {
            parms.instance.process(parms.model, parms.payload);
        }
    }
}
