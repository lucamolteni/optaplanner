/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.optaplanner.examples.cloudbalancing.score;

import static org.optaplanner.core.api.score.stream.ConstraintCollectors.sum;
import static org.optaplanner.core.api.score.stream.Joiners.equal;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.examples.cloudbalancing.domain.CloudComputer;
import org.optaplanner.examples.cloudbalancing.domain.CloudProcess;

public class CloudBalancingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                requiredCpuPowerTotal(constraintFactory),
                requiredMemoryTotal(constraintFactory),
                requiredNetworkBandwidthTotal(constraintFactory),
                computerCost(constraintFactory)
        };
    }

    // ************************************************************************
    // Hard constraints
    // ************************************************************************

    Constraint requiredCpuPowerTotal(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(CloudProcess.class)
                .groupBy(getComputer, sum(fieldbyCPU))
                .filter(byCPU)
                .penalize(HardSoftScore.ONE_HARD,
                        penalizebyCPU)
                .asConstraint("requiredCpuPowerTotal");
    }

    Constraint requiredMemoryTotal(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(CloudProcess.class)
                .groupBy(getComputer, sum(fieldbyMemory))
                .filter(byMemory)
                .penalize(HardSoftScore.ONE_HARD,
                        penalizebyMemory)
                .asConstraint("requiredMemoryTotal");
    }

    Constraint requiredNetworkBandwidthTotal(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(CloudProcess.class)
                .groupBy(getComputer, sum(fieldbyNetwork))
                .filter(byNetwork)
                .penalize(HardSoftScore.ONE_HARD,
                        penalizebyNetwork)
                .asConstraint("requiredNetworkBandwidthTotal");
    }

    // ************************************************************************
    // Soft constraints
    // ************************************************************************

    Constraint computerCost(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(CloudComputer.class)
                .ifExists(CloudProcess.class, equal(Function.identity(), getComputer))
                .penalize(HardSoftScore.ONE_SOFT, getCost)
                .asConstraint("computerCost");
    }

    static final GetComputer getComputer = new GetComputer();

    private static class GetComputer implements Function<CloudProcess, CloudComputer> {

        @Override
        public CloudComputer apply(CloudProcess cloudProcess) {
            return cloudProcess.getComputer();
        }
    }

    static final Penalize penalizebyCPU = new Penalize(Penalize.PenalizeType.ByCPUPower);
    static final Penalize penalizebyMemory = new Penalize(Penalize.PenalizeType.ByMemory);
    static final Penalize penalizebyNetwork = new Penalize(Penalize.PenalizeType.ByNetwork);

    private static class Penalize implements ToIntBiFunction<CloudComputer, Integer> {

        private final PenalizeType penalizeType;

        private Penalize(PenalizeType penalizeType) {
            this.penalizeType = penalizeType;
        }

        enum PenalizeType {
            ByCPUPower,
            ByMemory,
            ByNetwork
        }

        @Override
        public int applyAsInt(CloudComputer computer, Integer val) {
            if (penalizeType.equals(PenalizeType.ByCPUPower)) {
                return val - computer.getCpuPower();
            } else if (penalizeType.equals(PenalizeType.ByMemory)) {
                return val - computer.getMemory();
            } else if (penalizeType.equals(PenalizeType.ByNetwork)) {
                return val - computer.getNetworkBandwidth();
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    static final GetField fieldbyCPU = new GetField(GetField.FieldType.CPUPower);
    static final GetField fieldbyMemory = new GetField(GetField.FieldType.Memory);
    static final GetField fieldbyNetwork = new GetField(GetField.FieldType.Network);

    private static class GetField implements ToIntFunction<CloudProcess> {

        final FieldType fieldType;

        private GetField(FieldType fieldType) {
            this.fieldType = fieldType;
        }

        enum FieldType {
            CPUPower,
            Memory,
            Network
        }

        @Override
        public int applyAsInt(CloudProcess cloudProcess) {
            if (fieldType.equals(FieldType.CPUPower)) {
                return cloudProcess.getRequiredCpuPower();
            } else if (fieldType.equals(FieldType.Memory)) {
                return cloudProcess.getRequiredMemory();
            } else if (fieldType.equals(FieldType.Network)) {
                return cloudProcess.getRequiredNetworkBandwidth();
            }
            throw new UnsupportedOperationException();
        }
    }

    private static GetCost getCost = new GetCost();

    private static class GetCost implements ToIntFunction<CloudComputer> {

        @Override
        public int applyAsInt(CloudComputer cloudComputer) {
            return cloudComputer.getCost();
        }
    }

    static final Compare byCPU = new Compare(Compare.FieldType.CPUPower);
    static final Compare byMemory = new Compare(Compare.FieldType.Memory);
    static final Compare byNetwork = new Compare(Compare.FieldType.Network);

    private static class Compare implements BiPredicate<CloudComputer, Integer> {

        final FieldType fieldType;

        private Compare(FieldType fieldType) {
            this.fieldType = fieldType;
        }

        enum FieldType {
            CPUPower,
            Memory,
            Network
        }

        @Override
        public boolean test(CloudComputer computer, Integer val) {
            if (fieldType.equals(FieldType.CPUPower)) {
                return val > computer.getCpuPower();
            } else if (fieldType.equals(FieldType.Memory)) {
                return val > computer.getMemory();
            } else if (fieldType.equals(FieldType.Network)) {
                return val > computer.getNetworkBandwidth();
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }
}
