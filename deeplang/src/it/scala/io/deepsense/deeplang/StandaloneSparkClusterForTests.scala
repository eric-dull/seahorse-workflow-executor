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

package io.deepsense.deeplang

import io.deepsense.commons.BuildInfo
import io.deepsense.commons.spark.sql.UserDefinedFunctions
import io.deepsense.deeplang.catalogs.doperable.DOperableCatalog
import io.deepsense.deeplang.doperables.dataframe.DataFrameBuilder
import io.deepsense.deeplang.inference.InferContext
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}

object StandaloneSparkClusterForTests {

  lazy val executionContext: ExecutionContext = {
    import org.scalatest.mock.MockitoSugar._

    System.setProperty("HADOOP_USER_NAME", "hdfs")

    val sparkConf: SparkConf = new SparkConf()
      .setMaster("spark://10.10.1.121:7077")
      .setAppName("TestApp")
      .setJars(Seq(
        "./deeplang/target/scala-2.10/" +
          "deepsense-seahorse-deeplang-assembly-" + BuildInfo.version  + ".jar"
      ))
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .registerKryoClasses(Array())

    val sparkContext = new SparkContext(sparkConf)
    val sqlContext = new SQLContext(sparkContext)

    UserDefinedFunctions.registerFunctions(sqlContext.udf)

    val dOperableCatalog = {
      val catalog = new DOperableCatalog
      CatalogRecorder.registerDOperables(catalog)
      catalog
    }

    val inferContext = InferContext(
      DataFrameBuilder(sqlContext),
      "testTenantId",
      dOperableCatalog,
      mock[InnerWorkflowParser])

    new MockedExecutionContext(
      sparkContext,
      sqlContext,
      inferContext,
      LocalFileSystemClient(),
      "testTenantId",
      mock[InnerWorkflowExecutor],
      mock[ContextualDataFrameStorage],
      new MockedContextualCodeExecutor)
  }

}
