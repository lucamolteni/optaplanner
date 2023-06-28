package org.optaplanner.examples;

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
import org.optaplanner.constraint.streams.bavet.BavetConstraintStreamScoreDirectorFactory;
import org.optaplanner.constraint.streams.common.AbstractConstraintStreamScoreDirectorFactory;
import org.optaplanner.constraint.streams.common.inliner.AbstractScoreInliner;
import org.optaplanner.constraint.streams.drools.DroolsConstraintStreamScoreDirectorFactory;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.config.solver.EnvironmentMode;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.core.impl.testdata.domain.TestdataSolution;

@State(Scope.Benchmark)
public class DroolsBavetBenchmark {

    @Param({ "true", "false" })
    public boolean bavet;

    public String[] facts;

    public int initialScore = 0;

    AbstractConstraintStreamScoreDirectorFactory constraintFactory;

    @Setup(Level.Trial)
    public void doForkSetup() {
        constraintFactory = createConstraintFactory(bavet);
        facts = new String[] { "foo", "bar" };
    }

    AbstractConstraintStreamScoreDirectorFactory createConstraintFactory(boolean withBavet) {
        SolutionDescriptor<TestdataSolution> testdataSolutionSolutionDescriptor = TestdataSolution.buildSolutionDescriptor();

        ConstraintProvider stubConstraintProvider = constraintFactory -> new Constraint[] {
                constraintFactory.forEach(String.class)
                        .filter((a) -> a.equalsIgnoreCase("foo"))
                        .penalize(SimpleScore.of(1))
                        .asConstraint("constraint1"),
        };

        AbstractConstraintStreamScoreDirectorFactory constraintFactory1;
        if (withBavet) {
            constraintFactory1 =
                    new BavetConstraintStreamScoreDirectorFactory(testdataSolutionSolutionDescriptor, stubConstraintProvider,
                            EnvironmentMode.REPRODUCIBLE);
        } else {
            constraintFactory1 =
                    new DroolsConstraintStreamScoreDirectorFactory(testdataSolutionSolutionDescriptor, stubConstraintProvider,
                            true);
        }
        return constraintFactory1;
    }

    @Benchmark
    @Fork(value = 3)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 10, time = 400, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    public String constraintEvaluation() {
        AbstractScoreInliner abstractScoreInliner = constraintFactory.fireAndForget((Object[]) facts);

        Score score = abstractScoreInliner.extractScore(initialScore); // 0 -1  one fire
        return score.toShortString();
    }
}
