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

package org.trancemountain.storageservice.repository.store

import org.trancemountain.storageservice.repository.{INode, RepositoryRevisionNumber}

/**
	* @author michaelcoddington
	*/
trait INodeStore {

	def revision: RepositoryRevisionNumber

	/**
		* Creates a new, uncommitted node with the given path, type and properties.
		*
		* @param sessionID the ID of the session to use to create the node
		* @param sessionRevision the current revision of the session
		* @param targetNodePath the path of the node to create
		* @param primaryNodeType the primary type of node to create
		* @param mixinNodeTypes a set of mixin node types to add to the node
		* @param properties the initial properties of the new node
		* @return the newly created node
		*/
	def createNode(sessionID: String, sessionRevision: RepositoryRevisionNumber, targetNodePath: String, primaryNodeType: String, mixinNodeTypes: Set[String] = null, properties: Map[String, Any] = null): INode

	/**
		* Returns the last available node for a session at the given revision number.
		* @param sessionID the ID of the session to use
		* @param sessionRevision the revision of the session
		* @param targetNodePath the path of the node to retrieve
		*/
	def node(sessionID: String, sessionRevision: RepositoryRevisionNumber, targetNodePath: String): Option[INode]

	def parentNode(sessionID: String, sessionRevision: RepositoryRevisionNumber, targetNodePath: String): INode

	def deleteNode(sessionID: String, sessionRevision: RepositoryRevisionNumber, targetNodePath: String)

	def nodeChildPaths(sessionID: String, sessionRevision: RepositoryRevisionNumber, targetNodePath: String): Seq[String]

	def strongReferencesTo(sessionID: String, sessionRevision: RepositoryRevisionNumber, targetNodePath: String): Option[Set[StoredStrongReference]]

	def weakReferencesTo(sessionID: String, sessionRevision: RepositoryRevisionNumber, targetNodePath: String): Option[Set[StoredWeakReference]]

	def refresh(sessionID: String, keepChanges: Boolean): RepositoryRevisionNumber
	def commit(sessionID: String): RepositoryRevisionNumber
	def commit(sessionID: String, asyncIndex: Boolean): RepositoryRevisionNumber

	def sessionClosed(sessionID: String)

	protected[store] def applyExistingChangeSets()
	protected[store] def prepareChangeSet(sessionID: String): (NodeDataChangeSet, Seq[INode])

	protected[store] def executeChangeSet(sessionID: String, cs: NodeDataChangeSet)
	protected[repository] def reset()
}
