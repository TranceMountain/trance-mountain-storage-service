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

import javax.annotation.PostConstruct

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.trancemountain.storageservice.repository.store.INodeStore

/**
	* @author michaelcoddington
	*/
@Component
@Scope("prototype")
class Session(val sessionID: String) extends ISession {

	@Autowired
	private val nodeStore: INodeStore = null

	protected[repository] var repositoryRevisionNumber: RepositoryRevisionNumber = _

	@PostConstruct
	private def init(): Unit = {
		repositoryRevisionNumber = nodeStore.revision
	}

	override def revision: RepositoryRevisionNumber = {
		assertOpen()
		repositoryRevisionNumber
	}

	override def rootNode: ISessionNode = {
		assertOpen()
		new SessionNode(this, node("/").get)
	}

	override def node(path: String): Option[ISessionNode] = {
		assertOpen()
		nodeStore.node(sessionID, repositoryRevisionNumber, path) match {
			case Some(n: INode) => Some(new SessionNode(this, n))
			case None => None
		}
	}

	override def createNode(path: String, primaryNodeType: String, mixinNodeTypes: Set[String], properties: Map[String, Any]): ISessionNode = {
		assertOpen()
		val bn = nodeStore.createNode(sessionID, repositoryRevisionNumber, path, primaryNodeType, mixinNodeTypes, properties)
		new SessionNode(this, bn)
	}

	override protected[repository] def children(node: ISessionNode): Option[Iterator[ISessionNode]] = {
		require(node != null, "Null node reference")
		assertOpen()
		val childPaths = nodeStore.nodeChildPaths(sessionID, repositoryRevisionNumber, node.path)
		if (childPaths.nonEmpty)
			Some(childPaths.iterator.map(nodePath => this.node(nodePath).get))
		else None
	}

	override def delete(node: ISessionNode): Unit = {
		nodeStore.deleteNode(sessionID, repositoryRevisionNumber, node.path)
	}

	override def refresh(keepChanges: Boolean): ISession = {
		assertOpen()
		repositoryRevisionNumber = nodeStore.refresh(sessionID, keepChanges)
		this
	}

	override def save(): ISession = {
		assertOpen()
		repositoryRevisionNumber = nodeStore.commit(sessionID)
		this
	}

}
