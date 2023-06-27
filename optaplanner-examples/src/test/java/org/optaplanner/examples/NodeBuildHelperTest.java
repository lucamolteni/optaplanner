package org.optaplanner.examples;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeBuildHelperTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testCreationOfSimpleRete(boolean withBavet) {

        SolutionDescriptor<TestdataSolution> testdataSolutionSolutionDescriptor = TestdataSolution.buildSolutionDescriptor();

        ConstraintProvider stubConstraintProvider = constraintFactory -> new Constraint[]{
                constraintFactory.forEach(String.class)
                        .filter((a) -> a.equalsIgnoreCase("foo"))
                        .penalize(SimpleScore.of(1))
                        .asConstraint("constraint1"),
        };

        AbstractConstraintStreamScoreDirectorFactory constraintFactory;
        if (withBavet) {
            constraintFactory = new
                    BavetConstraintStreamScoreDirectorFactory(testdataSolutionSolutionDescriptor, stubConstraintProvider,
                                                              EnvironmentMode.REPRODUCIBLE);
        } else {
            constraintFactory = new
                    DroolsConstraintStreamScoreDirectorFactory(testdataSolutionSolutionDescriptor, stubConstraintProvider,
                                                               true);
        }

        AbstractScoreInliner abstractScoreInliner = constraintFactory.fireAndForget("foo", "bar");

        Score score = abstractScoreInliner.extractScore(0); // 0 -1  one fire
        assertEquals("-1", score.toShortString());
    }
}
