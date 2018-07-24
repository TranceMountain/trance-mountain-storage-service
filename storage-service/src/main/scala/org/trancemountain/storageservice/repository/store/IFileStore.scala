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

import org.trancemountain.storageservice.repository.RepositoryRevisionNumber

/**
	* @author michaelcoddington
	*/
trait IFileStore {
	def revision(): RepositoryRevisionNumber

	def createFile(sessionID: String, revision: RepositoryRevisionNumber, filePath: String, stream: InputStream)
	def moveFile(sessionID: String, revision: RepositoryRevisionNumber, sourceFilePath: String, targetFilePath: String)
	def getInputStreamForFile(sessionID: String, filePath: String): Option[InputStream]
	def deleteFile(sessionID: String, revision: RepositoryRevisionNumber, filePath: String)

	def revert(sessionID: String)
	def prepareCommit(sessionID: String, revision: RepositoryRevisionNumber): Boolean
	def commit(sessionID: String, revision: RepositoryRevisionNumber): RepositoryRevisionNumber

	def committedFile(filePath: String): Option[InputStream]
	def hashedFileCount: Int

	def sessionClosed(sessionID: String)

	protected[store] def getMetadataJournalEntries(sessionID: String): Seq[FileMetadataJournalEntry]
	protected[store] def clear()
}
