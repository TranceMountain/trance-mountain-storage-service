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

import org.trancemountain.storageservice.repository.RepositoryRevisionNumber

/**
	* @author michaelcoddington
	*/
trait IFileMetadataStore {

	/**
		* Returns the current revision number of the metadata store.
		*/
	def revision(): RepositoryRevisionNumber

	/**
		* Creates a link from a virtual file path for a session to the real underlying hashed file path.
		* @param sessionID the ID of the session that is creating the file
		* @param filePath the virtual file path for the file
		* @param hashedFilePath the path to the true, underlying file with a hashed file name
		*/
	def insertVirtualFilePointer(sessionID: String, revision: RepositoryRevisionNumber, filePath: String, hashedFilePath: String): Unit

	/**
		* Moves a virtual file pointer for a given session from one path to another.
		* @param sessionID the ID of the session to use
		* @param sourcePath the current virtual file pointer path
		* @param targetPath the new virtual file pointer path
		*/
	def moveVirtualFilePointer(sessionID: String, revision: RepositoryRevisionNumber, sourcePath: String, targetPath: String): Unit

	/**
		* Gets the true underlying file path for the given virtual file path.
		* @param sessionID the ID of the session to use for file lookup
		* @param filePath the virtual file path in the session to use for file lookup
		* @return an option containing the hashed file path, or none if no such file exists
		*/
	def getHashFilePathForVirtualFile(sessionID: String, filePath: String): Option[String]

	/**
		* Returns all session ID / virtual file path combinations that point to the hashed file
		* that this session and file path point to.
		* @param sessionID the ID of the session to use for lookup
		* @param filePath the virtual file path to use for lookup
		* @return a sequence of sessionID / virtual file path tuples pointing to the hashed file
		*/
	def getVirtualFilePointers(sessionID: String, filePath: String): FileMetadataHashedFileVirtualPointers

	/**
		* Returns a list of hashed file pointers that point at the given hashed file path
		*/
	def getVirtualFilePointers(hashFilePath: String): FileMetadataHashedFileVirtualPointers

	/**
		* Removes a virtual file pointer for a given session ID and file path.
		* @param sessionID the session ID to use in link removal
		* @param filePath the file path to use in link removal
		*/
	def removeVirtualFilePointer(sessionID: String, revision: RepositoryRevisionNumber, filePath: String)

	/**
		* Returns the current set of journal entries for the given session ID.
		*/
	def getJournalEntries(sessionID: String): Seq[FileMetadataJournalEntry]

	/**
		* Returns a map of virtual file pointer to repository revision number.
		* @param pointers a sequence of virtual file pointers (file paths)
		*/
	def getCommittedRevisionNumbersForVirtualFilePointers(pointers: Seq[String]): Map[String, RepositoryRevisionNumber]

	/**
		* Marks the virtual file pointers for a given session ID as permanent (i.e., not associated with a specific session)
		* @param sessionID the ID of the session for which virtual file pointers should be made permanent
		*/
	def commitJournalEntries(sessionID: String): RepositoryRevisionNumber

	/**
		* Removes all transient file changes and journal entries for the given session ID
		*/
	def revert(sessionID: String)

	protected[store] def clear(): Unit
}
