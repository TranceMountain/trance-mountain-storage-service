/*
 * Trance Mountain: A scalable digital asset management system
 *
 * Copyright (C) 2016  Michael Coddington
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.trancemountain.storageservice.repository

import java.util.UUID

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.trancemountain.IEventConsumer
import org.trancemountain.storageservice.repository.store.INodeStore

/**
	* @author michaelcoddington
	*/
@Lazy
@Service
class SessionService extends ISessionService with IEventConsumer[SessionCloseEvent] {
	@Autowired
	private val context: ApplicationContext = null

	@Autowired
	private val nodeStore: INodeStore = null

	override def getAdminSession(): ISession = {
		// TODO: actually authenticate
		subscribe(context.getAutowireCapableBeanFactory.getBean(classOf[Session], UUID.randomUUID().toString))
	}

	override def defSession(username: String, password: String): ISession = {
		// TODO: actually authenticate
		subscribe(context.getAutowireCapableBeanFactory.getBean(classOf[Session], UUID.randomUUID().toString))
	}

	def getSession(sessionID: String): ISession = {
		// TODO: should only work if there really is a valid session with this ID
		subscribe(context.getAutowireCapableBeanFactory.getBean(classOf[Session], sessionID))
	}

	private def subscribe(session: ISession): ISession = {
		session.addConsumer(this)
		session
	}

	override def consume(e: SessionCloseEvent): Unit = {
		// TODO: clear session from session cache and unsubscribe from it
	}
}
