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

import java.io._

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.trancemountain.storageservice.repository.RepositoryRevisionNumber

object FileStore {
	private val LOG = LoggerFactory.getLogger(getClass)
}

/**
	* @author michaelcoddington
	*/
@Lazy
@Service
class FileStore extends IFileStore {

	import FileStore.LOG

	@Autowired
	private val fileMetadataStore: IFileMetadataStore = null

	@Autowired
	private val fileDataStore: IFileDataStore = null

	override def revision(): RepositoryRevisionNumber = fileMetadataStore.revision()

	def createFile(sessionID: String, revision: RepositoryRevisionNumber, filePath: String, stream: InputStream): Unit = {
		val hashedFilePath = fileDataStore.createFile(stream)
		fileMetadataStore.insertVirtualFilePointer(sessionID, revision, filePath, hashedFilePath)
	}

	override def moveFile(sessionID: String, revision: RepositoryRevisionNumber, sourceFilePath: String, targetFilePath: String): Unit = {
		fileMetadataStore.moveVirtualFilePointer(sessionID, revision, sourceFilePath, targetFilePath)
	}

	override def getInputStreamForFile(sessionID: String, filePath: String): Option[InputStream] = {
		val hashFileOption = fileMetadataStore.getHashFilePathForVirtualFile(sessionID, filePath)
		hashFileOption match {
			case Some(s: String) => fileDataStore.getInputStreamForFile(s)
			case None => None
		}
	}

	override def deleteFile(sessionID: String, revision: RepositoryRevisionNumber, filePath: String): Unit = {
		fileMetadataStore.removeVirtualFilePointer(sessionID, revision, filePath)
	}

	override protected[store] def getMetadataJournalEntries(sessionID: String): Seq[FileMetadataJournalEntry] = {
		fileMetadataStore.getJournalEntries(sessionID)
	}

	override def revert(sessionID: String): Unit = {
		// remove journal entries, session-local entries in the metadata table, and delete files
		// that are only pointed to by this session
		val journalEntries = fileMetadataStore.getJournalEntries(sessionID)
		fileMetadataStore.revert(sessionID)
		val hashedFilePointers = journalEntries.map(entry => fileMetadataStore.getVirtualFilePointers(entry.hashFilePath))
		val unlinkedHashFilePaths = hashedFilePointers.view.filter(hfp => hfp.pointers.isEmpty).map(hfp => hfp.hashedFilePath)
		for (pathToRemove <- unlinkedHashFilePaths) {
			require(fileDataStore.fileExists(pathToRemove), s"Cannot delete nonexistent file $pathToRemove")
			LOG.info(s"Deleting hashed file $pathToRemove")
			val deleted = fileDataStore.delete(pathToRemove)
			require(deleted, s"Unable to delete hash file $pathToRemove")
		}
	}

	override def prepareCommit(sessionID: String, revision: RepositoryRevisionNumber): Boolean = {
		val journalEntries = fileMetadataStore.getJournalEntries(sessionID)
		val paths = journalEntries.map(entry => entry.filePath)
		val pathRevisionMap = fileMetadataStore.getCommittedRevisionNumbersForVirtualFilePointers(paths)
		var isOK = true
		for (entry <- journalEntries if isOK) {
			val entryRevision = pathRevisionMap(entry.filePath)
			if (entryRevision != null && entryRevision > revision) {
				isOK = false
			} else {
				if (entryRevision == null) LOG.info(s"Got null revision number for committed file pointer ${entry.filePath}")
			}
			entry.action match {
				case FileMetadataJournalEntry.ACTION_ADD =>
					if (!fileDataStore.fileExists(entry.hashFilePath)) isOK = false
				case FileMetadataJournalEntry.ACTION_REMOVE =>
			}
		}
		isOK
	}

	override def commit(sessionID: String, revision: RepositoryRevisionNumber): RepositoryRevisionNumber = {
		val journalEntries = fileMetadataStore.getJournalEntries(sessionID)
		val newRevision = fileMetadataStore.commitJournalEntries(sessionID)
		val removedJournalEntries = journalEntries.filter(_.action == FileMetadataJournalEntry.ACTION_REMOVE)
		val hashedFilePointers = removedJournalEntries.map(entry => fileMetadataStore.getVirtualFilePointers(entry.hashFilePath))
		val unlinkedHashFilePaths = hashedFilePointers.view.filter(hfp => hfp.pointers.isEmpty).map(hfp => hfp.hashedFilePath)
		for (pathToRemove <- unlinkedHashFilePaths) {
			require(fileDataStore.fileExists(pathToRemove), s"Cannot delete nonexistent file $pathToRemove")
			LOG.info(s"Deleting hashed file $pathToRemove")
			val deleted = fileDataStore.delete(pathToRemove)
			require(deleted, s"Unable to delete hash file $pathToRemove")
		}
		newRevision
	}

	def sessionClosed(sessionID: String): Unit = {
		revert(sessionID)
	}

	override def hashedFileCount: Int = fileDataStore.fileCount

	override def committedFile(filePath: String): Option[InputStream] = {
		val hashFileOption = fileMetadataStore.getHashFilePathForVirtualFile(null, filePath)
		LOG.info(s"Got hash file option $hashFileOption")
		hashFileOption match {
			case Some(s: String) => fileDataStore.getInputStreamForFile(s)
			case None => None
		}
	}

	override protected[store] def clear(): Unit = {
		fileMetadataStore.clear()
		fileDataStore.clear()
	}
}
