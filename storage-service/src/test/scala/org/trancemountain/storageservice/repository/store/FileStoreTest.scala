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

import java.nio.file.Files

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.test.context.support.AnnotationConfigContextLoader
import org.springframework.test.context.{ContextConfiguration, TestContextManager}
import org.trancemountain.storageservice.SpringConfigKeys
import org.trancemountain.storageservice.repository.backend.DerbyService
import org.trancemountain.storageservice.repository.store.derby.DerbyFileMetadataStore
import org.trancemountain.storageservice.repository.store.fs.FSFileDataStore


@Configuration
class FSFileStoreConfig {
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
	classes = Array(classOf[FSFileStoreConfig], classOf[FileStore],
		classOf[DerbyService], classOf[DerbyFileMetadataStore],
		classOf[FSFileDataStore]),
	initializers = Array(classOf[ConfigFileApplicationContextInitializer]))
class FileStoreTest extends IFileStoreTest with BeforeAndAfterAll {

	@Autowired
	val fileStore: IFileStore = null

	def description = "A FileStore"

	override def beforeAll(): Unit = {
		val tmpFile = Files.createTempDirectory("tm-fs").toFile
		val dbFile = tmpFile.toPath.resolve("db").toFile

		System.setProperty(SpringConfigKeys.TM_FILE_METADATA_STORE_TYPE, "derby")
		System.setProperty(SpringConfigKeys.TM_FILE_DATA_STORE_TYPE, "fs")
		System.setProperty(SpringConfigKeys.TM_BACKEND_DERBY_LOC, dbFile.getAbsolutePath)
		System.setProperty(SpringConfigKeys.TM_FILE_STORE_FS_LOC, tmpFile.getAbsolutePath)

		val mgr = new TestContextManager(this.getClass)
		mgr.prepareTestInstance(this)
	}
}
