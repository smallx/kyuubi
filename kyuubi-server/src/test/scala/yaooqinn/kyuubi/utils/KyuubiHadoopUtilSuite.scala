/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package yaooqinn.kyuubi.utils

import org.apache.hadoop.security.UserGroupInformation
import org.apache.hadoop.yarn.api.records._
import org.apache.hadoop.yarn.client.api.YarnClient
import org.apache.hadoop.yarn.conf.YarnConfiguration
import org.apache.hadoop.yarn.server.MiniYARNCluster
import org.apache.hadoop.yarn.util.Records
import org.apache.spark.SparkFunSuite
import org.scalatest.BeforeAndAfterEach

class KyuubiHadoopUtilSuite extends SparkFunSuite with BeforeAndAfterEach {

  private val cluster: MiniYARNCluster =
    new MiniYARNCluster(this.getClass.getSimpleName, 1, 1, 1, 1)
  private val yarnClient = YarnClient.createYarnClient()

  override def beforeAll(): Unit = {
    val yarnConf = new YarnConfiguration()
    yarnConf.set(YarnConfiguration.IS_MINI_YARN_CLUSTER, "true")
    cluster.init(yarnConf)
    cluster.start()
    yarnClient.init(yarnConf)
    yarnClient.start()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    yarnClient.stop()
    cluster.stop()
    super.afterAll()
  }

  test("kill application by name") {
    withYarnApplication { app =>
      assert(yarnClient.getApplications.size() === 1)
      assert(yarnClient.getApplications.get(0).getYarnApplicationState !==
        YarnApplicationState.KILLED)
      KyuubiHadoopUtil.killYarnAppByName(app.toString + "1")
      assert(yarnClient.getApplications.get(0).getYarnApplicationState !==
        YarnApplicationState.KILLED)
      KyuubiHadoopUtil.killYarnAppByName(app.toString)
      assert(yarnClient.getApplications.get(0).getYarnApplicationState ===
        YarnApplicationState.KILLED)
      KyuubiHadoopUtil.killYarnAppByName(app.toString)
    }
  }

  test("do as") {
    val user1 = UserGroupInformation.getCurrentUser
    val userName1 = user1.getShortUserName
    val userName2 = "test"
    val user2 = UserGroupInformation.createProxyUser(userName2, user1)

    def testf(expectedUser: String): Boolean = {
      UserGroupInformation.getCurrentUser.getShortUserName == expectedUser
    }

    def testf2(expectedUser: String): Boolean = {
      throw new RuntimeException("testf2")
    }

    def testf3(expectedUser: String): Boolean = {
      throw new OutOfMemoryError("testf3")
    }

    KyuubiHadoopUtil.doAs(user1)(assert(testf(userName1)))
    val e1 = intercept[RuntimeException](KyuubiHadoopUtil.doAs(user1)(assert(testf2(userName1))))
    assert(e1.getMessage === "testf2")
    val e2 = intercept[OutOfMemoryError](KyuubiHadoopUtil.doAs(user1)(assert(testf3(userName1))))
    assert(e2.getMessage === "testf3")

    KyuubiHadoopUtil.doAsAndLogNonFatal(user1)(assert(testf(userName1)))
    KyuubiHadoopUtil.doAsAndLogNonFatal(user1)(assert(testf2(userName1)))
    val e3 = intercept[OutOfMemoryError](
      KyuubiHadoopUtil.doAsAndLogNonFatal(user1)(assert(testf3(userName1))))
    assert(e3.getMessage === "testf3")

    KyuubiHadoopUtil.doAs(user2)(assert(testf(userName2)))

    KyuubiHadoopUtil.doAs(user1) {
      KyuubiHadoopUtil.doAsRealUser {
        assert(testf(userName1))
      }
    }

    KyuubiHadoopUtil.doAs(user2) {
      KyuubiHadoopUtil.doAsRealUser {
        assert(testf(userName1))
      }
    }
  }

  def withYarnApplication(f: ApplicationId => Unit): Unit = {
    val application = yarnClient.createApplication()
    val response = application.getNewApplicationResponse
    val applicationId = response.getApplicationId
    val context = application.getApplicationSubmissionContext
    context.setApplicationName(applicationId.toString)
    context.setApplicationType("SPARK")
    val capability = Records.newRecord(classOf[Resource])
    capability.setMemory(10)
    capability.setVirtualCores(1)
    context.setResource(capability)
    context.setAMContainerSpec(Records.newRecord(classOf[ContainerLaunchContext]))
    yarnClient.submitApplication(context)
    f(applicationId)
  }
}
