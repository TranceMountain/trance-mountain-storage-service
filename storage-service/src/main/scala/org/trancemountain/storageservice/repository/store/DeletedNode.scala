/*
 * Trance Mountain: A scalable digital asset management system
 *
 * Copyright (C) 2017  Michael Coddington
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
	* A special type of INode used by INodeDataStore implementations to store a deleted revision of a node.
	*
	* @param nodePath the path of the node
	* @param nodeRevision the revision of the node
	*/
protected[store] class DeletedNode(nodeID: String, nodePath: String, nodeRevision: RepositoryRevisionNumber) extends INode {
	override def id: String = nodeID
	override def update(name: String, value: Any): Unit = throw new RepositoryException(s"Cannot set property $name for $this")
	override def apply(name: String): Option[Any] = throw new RepositoryException(s"Cannot get property $name for $this")
	override def path: String = nodePath
	override def name: String = nodePath.split("/").last
	override def revision: RepositoryRevisionNumber = nodeRevision
	override def primaryNodeType: String = throw new RepositoryException(s"Cannot get primary node type for $this")
	override def mixinNodeTypes: Set[String] = throw new RepositoryException(s"Cannot get mixin node types for $this")
	override def addMixinNodeType(mixinType: String): Unit = throw new RepositoryException(s"Cannot add mixin type to $this")
	override def removeMixinNodeType(mixinType: String): Unit = throw new RepositoryException(s"Cannot remove mixin type from $this")
	override def properties: Map[String, Any] = throw new RepositoryException(s"Cannot get properties for $this")
	override def isDeleted(): Boolean = true
	override protected[repository] def setRevision(revision: RepositoryRevisionNumber): Unit = {
		throw new RuntimeException("not allowed")
	}

	override def toString: String = s"DeletedNode[path = $nodePath, revision=$nodeRevision]"
}
