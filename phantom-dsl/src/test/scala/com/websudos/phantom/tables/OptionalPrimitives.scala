/*
 * Copyright 2013-2015 Websudos, Limited.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Explicit consent must be obtained from the copyright owner, Websudos Limited before any redistribution is made.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.websudos.phantom.tables

import com.websudos.phantom.builder.query.InsertQuery
import com.websudos.phantom.dsl._
import com.websudos.phantom.testkit._
import com.websudos.util.testing._

case class OptionalPrimitive(
  pkey: String,
  string: Option[String],
  long: Option[Long],
  boolean: Option[Boolean],
  bDecimal: Option[BigDecimal],
  double: Option[Double],
  float: Option[Float],
  inet: Option[java.net.InetAddress],
  int: Option[Int],
  date: Option[java.util.Date],
  uuid: Option[java.util.UUID],
  timeuuid: Option[java.util.UUID],
  bi: Option[BigInt]
)

object OptionalPrimitive {

  def none: OptionalPrimitive = {
    OptionalPrimitive(
      gen[String],
      None, None, None, None, None, None, None, None, None, None, None, None
    )
  }
}

sealed class OptionalPrimitives extends CassandraTable[OptionalPrimitives, OptionalPrimitive] {
  override def fromRow(r: Row): OptionalPrimitive = {
    OptionalPrimitive(pkey(r), string(r), long(r), boolean(r), bDecimal(r), double(r), float(r), inet(r),
      int(r), date(r), uuid(r), timeuuid(r), bi(r))
  }

  object pkey extends StringColumn(this) with PartitionKey[String]

  object string extends OptionalStringColumn(this)

  object long extends OptionalLongColumn(this)

  object boolean extends OptionalBooleanColumn(this)

  object bDecimal extends OptionalBigDecimalColumn(this)

  object double extends OptionalDoubleColumn(this)

  object float extends OptionalFloatColumn(this)

  object inet extends OptionalInetAddressColumn(this)

  object int extends OptionalIntColumn(this)

  object date extends OptionalDateColumn(this)

  object uuid extends OptionalUUIDColumn(this)

  object timeuuid extends OptionalTimeUUIDColumn(this)

  object bi extends OptionalBigIntColumn(this)
}

object OptionalPrimitives extends OptionalPrimitives with PhantomCassandraConnector {

  override val tableName = "OptionalPrimitives"

  def store(row: OptionalPrimitive): InsertQuery.Default[OptionalPrimitives, OptionalPrimitive] = {
    insert
      .value(_.pkey, row.pkey)
      .value(_.long, row.long)
      .value(_.boolean, row.boolean)
      .value(_.bDecimal, row.bDecimal)
      .value(_.double, row.double)
      .value(_.float, row.float)
      .value(_.inet, row.inet)
      .value(_.int, row.int)
      .value(_.date, row.date)
      .value(_.uuid, row.uuid)
      .value(_.bi, row.bi)
      .value(_.string, row.string)
      .value(_.timeuuid, row.timeuuid)
  }
}
