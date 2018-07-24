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

import java.nio.file.Files

import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.test.context.{ContextConfiguration, TestContextManager}
import org.springframework.test.context.support.AnnotationConfigContextLoader
import org.trancemountain.storageservice.SpringConfigKeys
import org.trancemountain.storageservice.repository.backend.DerbyService
import org.trancemountain.storageservice.repository.nodetype.NodeTypeConstraintValidator
import org.trancemountain.storageservice.repository.search.EmbeddedSolrRepositorySearchService
import org.trancemountain.storageservice.repository.store.{FileStore, INodeStore, NodeStore, NodeTypeStore}
import org.trancemountain.storageservice.repository.store.derby.DerbyFileMetadataStore
import org.trancemountain.storageservice.repository.store.fs.FSFileDataStore
import org.trancemountain.storageservice.repository.store.memory.{MemoryNodeDataStore, MemoryNodeMetadataStore, MemoryNodeTypeDataStore}

@Configuration
class SessionServiceConfig {
	@Bean
	def propConfig(): PropertyPlaceholderConfigurer = {
		val placeholderConfigurer = new PropertyPlaceholderConfigurer()
		placeholderConfigurer.setSearchSystemEnvironment(true)
		placeholderConfigurer
	}
}

/**
	* @author michaelcoddington
	*/
@RunWith(classOf[JUnitRunner])
@ContextConfiguration(
	loader = classOf[AnnotationConfigContextLoader],
	classes = Array(classOf[SessionServiceConfig], classOf[SessionService],
		classOf[NodeStore], classOf[MemoryNodeDataStore],
		classOf[Session], classOf[MemoryNodeMetadataStore],
		classOf[FileStore], classOf[FSFileDataStore], classOf[DerbyFileMetadataStore],
		classOf[DerbyService], classOf[NodeTypeStore],
		classOf[NodeTypeConstraintValidator], classOf[MemoryNodeTypeDataStore],
		classOf[EmbeddedSolrRepositorySearchService]), initializers = Array(classOf[ConfigFileApplicationContextInitializer]))
class SessionServiceTest extends FlatSpec with Matchers with BeforeAndAfterEach with BeforeAndAfterAll with MockitoSugar {

	@Autowired
	private val sessionService: ISessionService = null

	override def beforeAll(): Unit = {
		// TODO: don't use an FS/Derby file store -- use memory
		val tmpFile = Files.createTempDirectory("tm-fs").toFile
		val dbFile = tmpFile.toPath.resolve("db").toFile
		val solrFile = tmpFile.toPath.resolve("solr").toFile

		System.setProperty(SpringConfigKeys.TM_BACKEND_DERBY_LOC, dbFile.getAbsolutePath)
		System.setProperty(SpringConfigKeys.TM_FILE_STORE_FS_LOC, tmpFile.getAbsolutePath)
		System.setProperty(SpringConfigKeys.TM_SEARCH_SERVICE_EMBEDDED_CONTAINER_LOCATION, solrFile.getAbsolutePath)
		System.setProperty(SpringConfigKeys.TM_FILE_METADATA_STORE_TYPE, "derby")
		System.setProperty(SpringConfigKeys.TM_FILE_DATA_STORE_TYPE, "fs")
		System.setProperty(SpringConfigKeys.TM_NODE_METADATA_STORE_TYPE, "memory")
		System.setProperty(SpringConfigKeys.TM_NODE_DATA_STORE_TYPE, "memory")
		System.setProperty(SpringConfigKeys.TM_NODETYPE_DATA_STORE_TYPE, "memory")
		System.setProperty(SpringConfigKeys.TM_SEARCH_SERVICE_TYPE, "embedded")
		System.setProperty(SpringConfigKeys.TM_SEARCH_SERVICE_INDEXING_THREAD_COUNT, "10")

		val mgr = new TestContextManager(this.getClass)
		mgr.prepareTestInstance(this)
	}

	"A session service" should "allow admin login" in {
		sessionService.getAdminSession() should not be null
	}

}
