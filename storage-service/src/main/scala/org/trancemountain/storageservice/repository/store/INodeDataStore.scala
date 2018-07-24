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

import org.trancemountain.storageservice.repository.{INode, RepositoryException, RepositoryRevisionNumber}

/**
	* @author michaelcoddington
	*/
protected[store] trait INodeDataStore {

	def node(path: String, revision: RepositoryRevisionNumber): Option[INode]

	/**
		* Returns all persistent revisions of the node at the given path.
		* @param targetNodePath the path of the node to retrieve
		* @return all persistent revisions of the given node, in order, or None if there are no revisions of that node
		*/
	def availableRevisionsOfNode(targetNodePath: String): Option[Seq[INode]]

	def pruneNodesUpToRevision(revision: RepositoryRevisionNumber)

	def pruneNodesAtRevision(revision: RepositoryRevisionNumber)

	def exists(path: String, sessionRevision: RepositoryRevisionNumber): Boolean

	def applyChangeSet(change: NodeDataChangeSet)

	def nodeChildPaths(sessionID: String, sessionRevision: RepositoryRevisionNumber, nodePath: String): Seq[String]

	def strongReferencesTo(targetNodePath: String): Option[Set[StoredStrongReference]]

	def weakReferencesTo(targetNodePath: String): Option[Set[StoredWeakReference]]

	def reset()

}
