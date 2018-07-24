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

import org.trancemountain.storageservice.repository.nodetype.{NodeTypeIdentifier, INodeTypeDefinition}

/**
	* @author michaelcoddington
	*/
trait INodeTypeDataStore {
	def nodeType(nodeTypeIdentifier: NodeTypeIdentifier): Option[INodeTypeDefinition]
	def createNodeType(definition: INodeTypeDefinition): Unit
	def removeNodeType(definition: NodeTypeIdentifier): Unit
	protected[store] def clear()
}
