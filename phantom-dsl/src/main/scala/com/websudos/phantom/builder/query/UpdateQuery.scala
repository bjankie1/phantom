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
package com.websudos.phantom.builder.query

import com.websudos.phantom.builder.query.prepared.PreparedUpdateQuery

import scala.annotation.implicitNotFound

import com.datastax.driver.core.{ProtocolVersion, Session, ConsistencyLevel, Row}
import com.websudos.phantom.CassandraTable
import com.websudos.phantom.builder._
import com.websudos.phantom.builder.clauses.{WhereClause, UpdateClause, CompareAndSetClause}
import com.websudos.phantom.connectors.KeySpace

class UpdateQuery[
  Table <: CassandraTable[Table, _],
  Record,
  Limit <: LimitBound,
  Order <: OrderBound,
  Status <: ConsistencyBound,
  Chain <: WhereBound
](table: Table,
  init: CQLQuery,
  usingPart: UsingPart = Defaults.EmptyUsingPart,
  wherePart : WherePart = Defaults.EmptyWherePart,
  setPart : SetPart = Defaults.EmptySetPart,
  casPart : CompareAndSetPart = Defaults.EmptyCompareAndSetPart,
  override val consistencyLevel: ConsistencyLevel = null
) extends Query[Table, Record, Limit, Order, Status, Chain](table, init, null) with Batchable {

  override val qb: CQLQuery = {
    usingPart merge setPart merge wherePart build init
  }

  def prepare: PreparedUpdateQuery[Table, Record, Limit, Order, Status, Chain] = {
    new PreparedUpdateQuery(table, init, usingPart, wherePart, setPart, casPart)
  }

  override protected[this] type QueryType[
    T <: CassandraTable[T, _],
    R,
    L <: LimitBound,
    O <: OrderBound,
    S <: ConsistencyBound,
    C <: WhereBound
  ] = UpdateQuery[T, R, L, O, S, C]


  protected[this] def create[
    T <: CassandraTable[T, _],
    R,
    L <: LimitBound,
    O <: OrderBound,
    S <: ConsistencyBound,
    C <: WhereBound
  ](t: T, q: CQLQuery, r: Row => R, consistencyLevel: ConsistencyLevel = null): QueryType[T, R, L, O, S, C] = {
    new UpdateQuery[T, R, L, O, S, C](
      t,
      q,
      Defaults.EmptyUsingPart,
      Defaults.EmptyWherePart,
      Defaults.EmptySetPart,
      Defaults.EmptyCompareAndSetPart,
      consistencyLevel
    )
  }

  /**
   * The where method of a select query.
   * @param condition A where clause condition restricted by path dependant types.
   * @param ev An evidence request guaranteeing the user cannot chain multiple where clauses on the same query.
   * @return
   */
  @implicitNotFound("You cannot use multiple where clauses in the same builder")
  override def where(condition: Table => WhereClause.Condition)(implicit ev: Chain =:= Unchainned): UpdateQuery[Table, Record, Limit, Order, Status, Chainned] = {
    val query = QueryBuilder.Update.where(condition(table).qb)
    new UpdateQuery(table, init, usingPart, wherePart append query, setPart, casPart, consistencyLevel)
  }

  /**
   * And clauses require overriding for count queries for the same purpose.
   * Without this override, the CQL query executed to fetch the count would still have a "LIMIT 1".
   * @param condition The Query condition to execute, based on index operators.
   * @return A SelectCountWhere.
   */
  @implicitNotFound("You have to use an where clause before using an AND clause")
  override def and(condition: Table => WhereClause.Condition)(implicit ev: Chain =:= Chainned): UpdateQuery[Table, Record, Limit, Order, Status, Chainned] = {
    val query = QueryBuilder.Update.and(condition(table).qb)
    new UpdateQuery(table, init, usingPart, wherePart append query, setPart, casPart, consistencyLevel)
  }

  final def modify(clause: Table => UpdateClause.Condition): AssignmentsQuery[Table, Record, Limit, Order, Status, Chain] = {
    val query = QueryBuilder.Update.set(clause(table).qb)
    new AssignmentsQuery(table, init, usingPart, wherePart, setPart append query, casPart, consistencyLevel)
  }

  /**
   * Generates a conditional query clause based on CQL lightweight transactions.
   * Compare and set transactions only get executed if a particular condition is true.
   *
   *
   * @param clause The Compare-And-Set clause to append to the builder.
   * @return A conditional query, now bound by a compare-and-set part.
   */
  def onlyIf(clause: Table => CompareAndSetClause.Condition): ConditionalQuery[Table, Record, Limit, Order, Status, Chain] = {
    val query = QueryBuilder.Update.onlyIf(clause(table).qb)
    new ConditionalQuery(table, init, usingPart, wherePart, setPart, casPart append query, consistencyLevel)
  }
}

sealed class AssignmentsQuery[
  Table <: CassandraTable[Table, _],
  Record,
  Limit <: LimitBound,
  Order <: OrderBound,
  Status <: ConsistencyBound,
  Chain <: WhereBound
](table: Table,
  val init: CQLQuery,
  usingPart: UsingPart = Defaults.EmptyUsingPart,
  wherePart : WherePart = Defaults.EmptyWherePart,
  setPart : SetPart = Defaults.EmptySetPart,
  casPart : CompareAndSetPart = Defaults.EmptyCompareAndSetPart,
  override val consistencyLevel: ConsistencyLevel = null
) extends ExecutableStatement with Batchable {

  val qb: CQLQuery = {
    usingPart merge setPart merge wherePart merge casPart build init
  }

  final def and(clause: Table => UpdateClause.Condition): AssignmentsQuery[Table, Record, Limit, Order, Status, Chain] = {
    val query = clause(table).qb
    new AssignmentsQuery(table, init, usingPart, wherePart, setPart append query, casPart, consistencyLevel)
  }

  final def timestamp(value: Long): AssignmentsQuery[Table, Record, Limit, Order, Status, Chain] = {
    val query = QueryBuilder.using(QueryBuilder.timestamp(init, value.toString))
    new AssignmentsQuery(table, init, usingPart append query, wherePart, setPart, casPart, consistencyLevel)
  }

  /**
   * Generates a conditional query clause based on CQL lightweight transactions.
   * Compare and set transactions only get executed if a particular condition is true.
   *
   *
   * @param clause The Compare-And-Set clause to append to the builder.
   * @return A conditional query, now bound by a compare-and-set part.
   */
  def onlyIf(clause: Table => CompareAndSetClause.Condition): ConditionalQuery[Table, Record, Limit, Order, Status, Chain] = {
    val query = QueryBuilder.Update.onlyIf(clause(table).qb)
    new ConditionalQuery(table, init, usingPart, wherePart, setPart, casPart append query, consistencyLevel)
  }

  def consistencyLevel_=(level: ConsistencyLevel)(implicit ev: Status =:= Unspecified, session: Session): AssignmentsQuery[Table, Record, Limit, Order, Specified, Chain] = {

    val protocol = session.getCluster.getConfiguration.getProtocolOptions.getProtocolVersionEnum

    if (protocol.compareTo(ProtocolVersion.V2) == 1) {
      new AssignmentsQuery(
        table,
        init,
        usingPart,
        wherePart,
        setPart,
        casPart,
        level
      )
    } else {
      new AssignmentsQuery(
        table,
        init,
        usingPart append QueryBuilder.consistencyLevel(level.toString),
        wherePart,
        setPart,
        casPart
      )
    }

  }
}

sealed class ConditionalQuery[
  Table <: CassandraTable[Table, _],
  Record,
  Limit <: LimitBound,
  Order <: OrderBound,
  Status <: ConsistencyBound,
  Chain <: WhereBound
](table: Table,
  val init: CQLQuery,
  usingPart: UsingPart = Defaults.EmptyUsingPart,
  wherePart : WherePart = Defaults.EmptyWherePart,
  setPart : SetPart = Defaults.EmptySetPart,
  casPart : CompareAndSetPart = Defaults.EmptyCompareAndSetPart,
  override val consistencyLevel: ConsistencyLevel = null
   ) extends ExecutableStatement with Batchable {

  val qb: CQLQuery = {
    usingPart merge setPart merge wherePart merge casPart build init
  }

  final def and(clause: Table => CompareAndSetClause.Condition): ConditionalQuery[Table, Record, Limit, Order, Status, Chain] = {
    val query = QueryBuilder.Update.and(clause(table).qb)

    new ConditionalQuery(
      table,
      init,
      usingPart,
      wherePart,
      setPart,
      casPart append query
    )
  }

  def consistencyLevel_=(level: ConsistencyLevel)(implicit ev: Status =:= Unspecified, session: Session): ConditionalQuery[Table, Record, Limit, Order, Specified, Chain] = {

    val protocol = session.getCluster.getConfiguration.getProtocolOptions.getProtocolVersionEnum

    if (protocol.compareTo(ProtocolVersion.V2) == 1) {
      new ConditionalQuery(
        table = table,
        init = init,
        usingPart = usingPart,
        wherePart = wherePart,
        setPart = setPart,
        casPart = casPart,
        consistencyLevel = level
      )
    } else {
      new ConditionalQuery(
        table = table,
        init = init,
        usingPart = usingPart append QueryBuilder.consistencyLevel(level.toString),
        wherePart = wherePart,
        setPart = setPart,
        casPart = casPart
      )
    }
  }

}

object UpdateQuery {

  type Default[T <: CassandraTable[T, _], R] = UpdateQuery[T, R, Unlimited, Unordered, Unspecified, Unchainned]

  def apply[T <: CassandraTable[T, _], R](table: T)(implicit keySpace: KeySpace): UpdateQuery.Default[T, R] = {
    new UpdateQuery[T, R, Unlimited, Unordered, Unspecified, Unchainned](
      table,
      QueryBuilder.Update.update(QueryBuilder.keyspace(keySpace.name, table.tableName).queryString))
  }

}
