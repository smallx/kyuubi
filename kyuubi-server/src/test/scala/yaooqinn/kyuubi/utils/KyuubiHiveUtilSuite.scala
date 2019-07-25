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

import scala.util.Try

import org.apache.hadoop.hive.ql.session.SessionState
import org.apache.hadoop.security.UserGroupInformation
import org.apache.spark.{SparkConf, SparkFunSuite}

class KyuubiHiveUtilSuite extends SparkFunSuite {
  private val user = UserGroupInformation.getCurrentUser

  test("hive conf") {
    val uris = "thrift://yaooqinn.kyuubi"
    val conf = new SparkConf()
      .set("spark.hadoop.hive.metastore.uris", uris)
    val hiveConf = KyuubiHiveUtil.hiveConf(conf)
    assert(hiveConf.get(KyuubiHiveUtil.URIS) === uris)
  }

  test("testURIS") {
    assert(KyuubiHiveUtil.URIS === "hive.metastore.uris")
  }

  test("metastore principal") {
    assert(KyuubiHiveUtil.METASTORE_PRINCIPAL === "hive.metastore.kerberos.principal")

  }

  test("add delegation tokens without hive session state ") {
    assert(Try {KyuubiHiveUtil.addDelegationTokensToHiveState(user)}.isSuccess)

  }

  test("add delegation token with hive session state, local fs") {
    val state = new SessionState(KyuubiHiveUtil.hiveConf(new SparkConf()))
    assert(Try {KyuubiHiveUtil.addDelegationTokensToHiveState(state, user) }.isSuccess)
  }

}
