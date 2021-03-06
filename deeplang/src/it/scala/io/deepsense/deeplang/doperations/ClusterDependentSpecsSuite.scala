/**
 * Copyright 2016, deepsense.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.deepsense.deeplang.doperations

import org.scalatest.{BeforeAndAfterAll, Suites}

import io.deepsense.deeplang.StandaloneSparkClusterForTests

class ClusterDependentSpecsSuite extends Suites(new InputOutputSpec) with BeforeAndAfterAll {
  override def beforeAll(): Unit = StandaloneSparkClusterForTests.startDockerizedCluster()
  override def afterAll(): Unit = StandaloneSparkClusterForTests.stopDockerizedCluster()
}
