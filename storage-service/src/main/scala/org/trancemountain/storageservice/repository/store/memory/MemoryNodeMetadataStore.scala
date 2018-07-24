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
import org.trancemountain.storageservice.repository.RepositoryRevisionNumber
import org.trancemountain.storageservice.repository.store.{INodeMetadataStore, NodeDataChangeSet}

import scala.collection.mutable

/**
	* @author michaelcoddington
	*/
@Lazy
@Service
@ConditionalOnProperty(name = Array(SpringConfigKeys.TM_NODE_METADATA_STORE_TYPE), havingValue = "memory")
class MemoryNodeMetadataStore extends INodeMetadataStore {
	private var revisionNumber = new RepositoryRevisionNumber(0)

	override def currentRevision: RepositoryRevisionNumber = revisionNumber

	override def nextRevision: RepositoryRevisionNumber = revisionNumber + 1

	private val changeSets = mutable.ListBuffer.empty[NodeDataChangeSet]

	override def commitNextRevision(): Unit = {
		revisionNumber = nextRevision
	}

	override def saveChangeSet(cs: NodeDataChangeSet): Unit = {
		changeSets += cs
	}

	override def savedChangeSets: Seq[NodeDataChangeSet] = changeSets.clone()

	override def removeChangeSet(cs: NodeDataChangeSet): Unit = {
		changeSets -= cs
	}

	override def reset(): Unit = {
		changeSets.clear()
		revisionNumber = new RepositoryRevisionNumber(0)
	}
}
