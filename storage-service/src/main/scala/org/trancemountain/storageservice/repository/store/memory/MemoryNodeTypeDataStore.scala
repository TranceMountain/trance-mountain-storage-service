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

package org.trancemountain.storageservice.repository.store.memory

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.trancemountain.storageservice.SpringConfigKeys
import org.trancemountain.storageservice.repository.RepositoryException
import org.trancemountain.storageservice.repository.nodetype.{NodeTypeIdentifier, INodeTypeDefinition}
import org.trancemountain.storageservice.repository.store.INodeTypeDataStore

import scala.collection.mutable

/**
	* @author michaelcoddington
	*/
@Lazy
@Service
@ConditionalOnProperty(name = Array(SpringConfigKeys.TM_NODETYPE_DATA_STORE_TYPE), havingValue = "memory")
class MemoryNodeTypeDataStore extends INodeTypeDataStore {

	private val nodeTypeMap = mutable.Map.empty[NodeTypeIdentifier, INodeTypeDefinition]

	override def nodeType(nodeTypeIdentifier: NodeTypeIdentifier): Option[INodeTypeDefinition] = {
		nodeTypeMap.get(nodeTypeIdentifier)
	}

	override def createNodeType(typeDefinition: INodeTypeDefinition): Unit = {
		val id = typeDefinition.nodeTypeIdentifier
		if (nodeTypeMap.contains(id)) throw new RepositoryException(s"Cannot create existing node type $id")
		nodeTypeMap(id) = typeDefinition
	}

	override def removeNodeType(typeIdentifier: NodeTypeIdentifier): Unit = {
		if (nodeTypeMap.contains(typeIdentifier)) nodeTypeMap.remove(typeIdentifier)
	}

	override protected[store] def clear(): Unit = nodeTypeMap.clear()
}
