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

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
import org.springframework.context.annotation.{Bean, ComponentScan, Configuration}
import org.trancemountain.storageservice.repository.SessionService
import org.trancemountain.storageservice.repository.backend.DerbyService
import org.trancemountain.storageservice.repository.nodetype.NodeTypeConstraintValidator
import org.trancemountain.storageservice.repository.search.EmbeddedSolrRepositorySearchService
import org.trancemountain.storageservice.repository.store.derby.DerbyFileMetadataStore
import org.trancemountain.storageservice.repository.store.fs.FSFileDataStore
import org.trancemountain.storageservice.repository.store.memory.{MemoryNodeDataStore, MemoryNodeMetadataStore, MemoryNodeTypeDataStore}

/**
	* @author michaelcoddington
	*/
@Configuration
@ComponentScan(
	basePackageClasses = Array(
		classOf[FileStore], classOf[DerbyService], classOf[DerbyFileMetadataStore], classOf[FSFileDataStore],
		classOf[MemoryNodeDataStore], classOf[MemoryNodeMetadataStore], classOf[INodeStore],
		classOf[NodeTypeStore], classOf[MemoryNodeTypeDataStore], classOf[NodeTypeConstraintValidator],
		classOf[SessionService], classOf[EmbeddedSolrRepositorySearchService]))
class NodeStoreConfig {
	@Bean
	def propConfig(): PropertyPlaceholderConfigurer = {
		val placeholderConfigurer = new PropertyPlaceholderConfigurer()
		placeholderConfigurer.setSearchSystemEnvironment(true)
		placeholderConfigurer
	}
}
