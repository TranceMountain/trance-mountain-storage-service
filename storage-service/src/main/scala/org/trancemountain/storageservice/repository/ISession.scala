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

import org.trancemountain.IEventProducer

/**
	* @author michaelcoddington
	*/
trait ISession extends IEventProducer[SessionCloseEvent] {

	def sessionID: String

	def revision: RepositoryRevisionNumber

	def rootNode: ISessionNode

	def node(path: String): Option[ISessionNode]

	private var closed = false

	/**
		* Creates a new node for this session, which exists virtually for this session until the session is saved.
		* @param path the path of the node to create
		* @param primaryNodeType the primary type of node to create
		* @param mixinNodeTypes a set of mixin node types to add to the node
		* @return a new node
		*/
	def createNode(path: String, primaryNodeType: String, mixinNodeTypes: Set[String], properties: Map[String, Any]): ISessionNode

	protected[repository] def children(node: ISessionNode): Option[Iterator[ISessionNode]]

	def refresh(keepChanges: Boolean): ISession

	def delete(node: ISessionNode): Unit

	/**
		* Saves this session and returns a new session with an updated
		* view of the underlying repository.
		*/
	def save(): ISession

	def assertOpen(): Unit = {
		if (closed) throw new SessionClosedException("Session is closed")
	}

	def close(): Unit = {
		closed = true
		notifyConsumers(SessionCloseEvent(sessionID))
	}
}
