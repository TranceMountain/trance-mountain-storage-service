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

package org.trancemountain.storageservice

/**
	* @author michaelcoddington
	*/
object SpringConfigKeys {

	final val TM_BACKEND_DERBY_LOC = "tm.service.storage.backend.derby.databaseFolderLocation"
	final val TM_BACKEND_DERBY_LOC_VALUE = "${tm.service.storage.backend.derby.databaseFolderLocation}"

	final val TM_FILE_DATA_STORE_TYPE = "tm.service.storage.file.data.store.type"
	final val TM_FILE_METADATA_STORE_TYPE = "tm.service.storage.file.metadata.store.type"

	final val TM_FILE_STORE_FS_LOC = "tm.service.storage.file.data.store.fs.topLevelLocation"
	final val TM_FILE_STORE_FS_LOC_VALUE = "${tm.service.storage.file.data.store.fs.topLevelLocation}"

	final val TM_NODE_METADATA_STORE_TYPE = "tm.service.storage.node.metadata.store.type"
	final val TM_NODE_DATA_STORE_TYPE = "tm.service.storage.node.data.store.type"

	final val TM_NODETYPE_DATA_STORE_TYPE = "tm.service.storage.nodetype.data.store.type"

	final val TM_SEARCH_SERVICE_TYPE = "tm.service.search.type"
	final val TM_SEARCH_SERVICE_INDEXING_THREAD_COUNT = "tm.service.search.indexing.threadCount"
	final val TM_SEARCH_SERVICE_INDEXING_THREAD_COUNT_VALUE = "${tm.service.search.indexing.threadCount}"
	final val TM_SEARCH_SERVICE_EMBEDDED_CONTAINER_LOCATION = "tm.service.search.indexing.embedded.containerLocation"
	final val TM_SEARCH_SERVICE_EMBEDDED_CONTAINER_LOCATION_VALUE = "${tm.service.search.indexing.embedded.containerLocation}"

}
