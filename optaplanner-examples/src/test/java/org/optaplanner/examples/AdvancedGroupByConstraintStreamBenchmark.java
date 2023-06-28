package org.optaplanner.examples;

import static org.optaplanner.core.api.score.stream.ConstraintCollectors.count;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.optaplanner.constraint.streams.bavet.BavetConstraintStreamImplSupport;
import org.optaplanner.constraint.streams.common.AbstractAdvancedGroupByConstraintStreamTest;
import org.optaplanner.constraint.streams.drools.DroolsConstraintStreamImplSupport;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;
import org.optaplanner.core.impl.score.director.InnerScoreDirector;
import org.optaplanner.core.impl.testdata.domain.score.lavish.TestdataLavishEntity;
import org.optaplanner.core.impl.testdata.domain.score.lavish.TestdataLavishSolution;
import org.optaplanner.core.impl.util.Pair;

@State(Scope.Benchmark)
public class AdvancedGroupByConstraintStreamBenchmark extends AbstractAdvancedGroupByConstraintStreamTest {

    public AdvancedGroupByConstraintStreamBenchmark() {
        super(new DroolsConstraintStreamImplSupport(true));
    }

    @Param({ "true", "false" })
    public boolean bavet;

    private TestdataLavishSolution solution;
    private InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector;
    private TestdataLavishEntity entity;

    @Setup(Level.Trial)
    public void doForkSetup() {
        if (!bavet) {
            this.implSupport = new DroolsConstraintStreamImplSupport(true);
        } else {
            this.implSupport = new BavetConstraintStreamImplSupport(true);
        }

        solution = TestdataLavishSolution.generateSolution(2, 5, 1, 7);
        entity = new TestdataLavishEntity("MyEntity 1", solution.getFirstEntityGroup(),
                solution.getFirstValue());

        solution.getEntityList().add(entity);

        scoreDirector = buildScoreDirector(
                factory -> factory.forEach(TestdataLavishEntity.class)
                        .groupBy(e -> e.getCode().substring(0, 1), count())
                        .groupBy(Pair::of)
                        .filter(pair -> !pair.getKey().equals("G"))
                        .penalize(SimpleScore.ONE, Pair::getValue)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        scoreDirector.setWorkingSolution(solution);

    }

    @Benchmark
    @Fork(value = 3)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 10, time = 400, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    public void collectedDowngradedAndFilteredBenchmark(Blackhole blackhole) {

        // From scratch

        scoreDirector.triggerVariableListeners();

        SimpleScore score = scoreDirector.calculateScore();
        blackhole.consume(score);
    }
}
