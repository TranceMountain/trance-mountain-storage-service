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

import java.io.InputStream

/**
	* @author michaelcoddington
	*/
trait IFileDataStore {
	/**
		* Creates a unique, de-duplicated file from the given input stream and returns its relative path within
		* the file storage system.
		* @param stream the stream to process
		* @return the relative path of this file within the data store
		*/
	def createFile(stream: InputStream): String

	/**
		* Returns an input stream for the hashed file at the given path, or None if no such file exists
		* @param hashPath a hashed file path
		* @return an input stream for the file
		*/
	def getInputStreamForFile(hashPath: String): Option[InputStream]

	def fileExists(hashPath: String): Boolean

	def delete(hashPath: String): Boolean

	def fileCount: Int

	protected[store] def clear(): Unit
}
