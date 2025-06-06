/*
 *************************************************************************************
 * Copyright 2011-2013 Normation SAS
 *************************************************************************************
 *
 * This file is part of Rudder.
 *
 * Rudder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * In accordance with the terms of section 7 (7. Additional Terms.) of
 * the GNU General Public License version 3, the copyright holders add
 * the following Additional permissions:
 * Notwithstanding to the terms of section 5 (5. Conveying Modified Source
 * Versions) and 6 (6. Conveying Non-Source Forms.) of the GNU General
 * Public License version 3, when you create a Related Module, this
 * Related Module is not considered as a part of the work and may be
 * distributed under the license agreement of your choice.
 * A "Related Module" means a set of sources files including their
 * documentation that, without modification of the Source Code, enables
 * supplementary functions or services in addition to those offered by
 * the Software.
 *
 * Rudder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Rudder.  If not, see <http://www.gnu.org/licenses/>.

 *
 *************************************************************************************
 */

package com.normation.rudder.services.eventlog

import com.normation.errors.IOResult
import com.normation.eventlog.*
import com.normation.rudder.domain.eventlog.ChangeRequestDiff
import com.normation.rudder.domain.eventlog.ChangeRequestEventLog
import com.normation.rudder.domain.eventlog.ChangeRequestLogsFilter
import com.normation.rudder.domain.workflows.ChangeRequestId
import com.normation.rudder.repository.EventLogRepository
import net.liftweb.common.*

/**
 * Allow to query relevant information about change request
 * status.
 */
trait ChangeRequestEventLogService {

  // we certainly won't keep that one in the end
  def saveChangeRequestLog(
      modId:     ModificationId,
      principal: EventActor,
      diff:      ChangeRequestDiff,
      reason:    Option[String]
  ): IOResult[EventLog]

  /**
   * Return the complet history, unsorted
   */
  def getChangeRequestHistory(id: ChangeRequestId): IOResult[Seq[ChangeRequestEventLog]]

  /**
   * Return the first logged action for the given ChangeRequest.
   * If the change request is not found, a Full(None) is returned.
   * Else, Full(Some(action)) in case of success, and a Failure
   * describing what happened in other cases.
   */
  def getFirstLog(id: ChangeRequestId): IOResult[Option[ChangeRequestEventLog]]

  /**
   * Return the last logged action for the given ChangeRequest.
   * If the change request is not find, a Full(None) is returned.
   * Else, Full(Some(action)) in case of success, and a Failure
   * describing what happened in other cases.
   */
  def getLastLog(id: ChangeRequestId): IOResult[Option[ChangeRequestEventLog]]

  def getLastCREvents: IOResult[Map[ChangeRequestId, EventLog]]
}

class ChangeRequestEventLogServiceImpl(
    eventLogRepository: EventLogRepository
) extends ChangeRequestEventLogService with Loggable {

  def saveChangeRequestLog(
      modId:     ModificationId,
      principal: EventActor,
      diff:      ChangeRequestDiff,
      reason:    Option[String]
  ): IOResult[EventLog] = {
    eventLogRepository.saveChangeRequest(modId, principal, diff, reason)
  }

  def getChangeRequestHistory(id: ChangeRequestId): IOResult[Seq[ChangeRequestEventLog]] = {
    eventLogRepository
      .getEventLogByChangeRequest(id, "/entry/changeRequest/id/text()", eventTypeFilter = ChangeRequestLogsFilter.eventList)
      .map(_.collect { case c: ChangeRequestEventLog => c })
  }

  def getFirstLog(id: ChangeRequestId): IOResult[Option[ChangeRequestEventLog]] = {
    getFirstOrLastLog(id, "creationDate asc")
  }

  def getLastLog(id: ChangeRequestId): IOResult[Option[ChangeRequestEventLog]] = {
    getFirstOrLastLog(id, "creationDate desc")
  }

  private def getFirstOrLastLog(id: ChangeRequestId, sortMethod: String): IOResult[Option[ChangeRequestEventLog]] = {
    eventLogRepository
      .getEventLogByChangeRequest(
        id,
        "/entry/changeRequest/id/text()",
        Some(1),
        Some(sortMethod),
        ChangeRequestLogsFilter.eventList
      )
      .map(_.collect { case c: ChangeRequestEventLog => c }.headOption)
  }

  /**
   * Get Last Change Request event for all change Request
   */
  def getLastCREvents: IOResult[Map[ChangeRequestId, EventLog]] = {
    eventLogRepository.getLastEventByChangeRequest("/entry/changeRequest/id/text()", ChangeRequestLogsFilter.eventList)
  }
}
