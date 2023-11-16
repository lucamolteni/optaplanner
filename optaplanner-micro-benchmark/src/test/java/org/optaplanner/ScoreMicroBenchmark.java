package org.optaplanner;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.optaplanner.constraint.streams.common.AbstractConstraintStreamTest;
import org.optaplanner.constraint.streams.common.ConstraintStreamImplSupport;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;
import org.optaplanner.core.impl.score.director.InnerScoreDirector;
import org.optaplanner.core.impl.testdata.domain.score.lavish.TestdataLavishSolution;
import org.optaplanner.core.impl.testdata.domain.score.lavish.TestdataLavishValueGroup;

@State(Scope.Thread)
public class ScoreMicroBenchmark extends AbstractConstraintStreamTest {

    @Param({ "true", "false" })
    boolean isDrools;

    public ScoreMicroBenchmark() {
        ConstraintStreamImplSupport constraintStreamImplSupport;
        if (isDrools) {
            constraintStreamImplSupport = new DroolsConstraintStreamImplSupport(true);
        } else {
            constraintStreamImplSupport = new BavetConstraintStreamImplSupport(true);
        }
        this.implSupport = constraintStreamImplSupport;
    }

    @Benchmark
    @Fork(value = 4)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 10, time = 400, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 20, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    public void filter_problemFact() {
        TestdataLavishSolution solution = TestdataLavishSolution.generateSolution();
        TestdataLavishValueGroup valueGroup1 = new TestdataLavishValueGroup("MyValueGroup 1");
        solution.getValueGroupList().add(valueGroup1);
        TestdataLavishValueGroup valueGroup2 = new TestdataLavishValueGroup("MyValueGroup 2");
        solution.getValueGroupList().add(valueGroup2);

        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(factory -> factory.forEach(TestdataLavishValueGroup.class)
                        .filter(valueGroup -> valueGroup.getCode().startsWith("MyValueGroup"))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(valueGroup1),
                assertMatch(valueGroup2));

        // Incremental
        scoreDirector.beforeProblemPropertyChanged(valueGroup1);
        valueGroup1.setCode("Other code");
        scoreDirector.afterProblemPropertyChanged(valueGroup1);
        assertScore(scoreDirector,
                assertMatch(valueGroup2));
    }
}
