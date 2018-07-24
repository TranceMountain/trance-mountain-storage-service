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

package org.trancemountain.storageservice.repository.backend

import java.sql.{Connection, DriverManager, SQLException}
import javax.annotation.{PostConstruct, PreDestroy}

import org.apache.commons.dbcp.BasicDataSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.{Required, Value}
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.trancemountain.storageservice.SpringConfigKeys

object DerbyService {
	private val LOG = LoggerFactory.getLogger(getClass)
}

/**
	* @author michaelcoddington
	*/
@Lazy
@Service
class DerbyService {
	import DerbyService.LOG

	@Required @Value(SpringConfigKeys.TM_BACKEND_DERBY_LOC_VALUE)
	private val databaseFolderLocation: String = null

	private var dataSource: BasicDataSource = _

	@PostConstruct
	def start(): Unit = {
		// init the database
		LOG.debug("Starting Derby database")

		dataSource = new BasicDataSource()
		dataSource.setUrl(s"jdbc:derby:$databaseFolderLocation;create=true")
		dataSource.setDefaultAutoCommit(false)
		dataSource.setMinIdle(5)
		dataSource.setMaxIdle(10)
	}

	@PreDestroy
	def stop(): Unit = {
		try {
			DriverManager.getConnection(s"jdbc:derby:$databaseFolderLocation;shutdown=true")
		} catch {
			case sqle: SQLException =>
		}
	}

	def connection: Connection = dataSource.getConnection()


}
