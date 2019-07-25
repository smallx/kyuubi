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

package yaooqinn.kyuubi.operation.metadata

import org.apache.spark.sql.Row
import org.apache.spark.sql.types.StructType

import yaooqinn.kyuubi.operation.{FINISHED, GET_TABLE_TYPES, RUNNING}
import yaooqinn.kyuubi.schema.SparkTableTypes
import yaooqinn.kyuubi.session.KyuubiSession

class GetTableTypesOperation(session: KyuubiSession)
  extends MetadataOperation(session, GET_TABLE_TYPES) {

  /**
   * Implemented by subclasses to decide how to execute specific behavior.
   */
  override protected def runInternal(): Unit = {
    setState(RUNNING)
    iter = SparkTableTypes.tableTypes.map(Row(_)).toList.iterator
    setState(FINISHED)
  }

  /**
   * Get the schema of operation result set.
   */
  override val getResultSetSchema: StructType = {
    new StructType()
      .add("TABLE_TYPE", "string", nullable = true, "Table type name.")
  }

}
