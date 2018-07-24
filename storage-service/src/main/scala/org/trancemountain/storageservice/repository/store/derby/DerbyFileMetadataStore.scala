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

package org.trancemountain.storageservice.repository.store.derby

import java.sql.Connection
import javax.annotation.PostConstruct

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.trancemountain.storageservice.SpringConfigKeys
import org.trancemountain.storageservice.repository.RepositoryRevisionNumber
import org.trancemountain.storageservice.repository.backend.DerbyService
import org.trancemountain.storageservice.repository.store.{FileMetadataHashedFileVirtualPointers, FileMetadataJournalEntry, IFileMetadataStore}

import scala.collection.mutable.ListBuffer

object DerbyFileMetadataStore {
	private val LOG = LoggerFactory.getLogger(getClass)
}

/**
	* @author michaelcoddington
	*/
@Lazy
@Service
@ConditionalOnProperty(name = Array(SpringConfigKeys.TM_FILE_METADATA_STORE_TYPE), havingValue = "derby")
class DerbyFileMetadataStore extends IFileMetadataStore {

	import DerbyFileMetadataStore.LOG

	@Autowired
	private val derbyService: DerbyService = null

	private val INSERT_JOURNAL_ENTRY_SQL = """ INSERT INTO virtual_file_links_journal (session_id, file_path, hash_file_path, revision, action) VALUES (?, ?, ?, ?, ?) """
	private val GET_REVISION_SQL = "SELECT revision FROM revision"
	private val UPDATE_REVISION_SQL = "UPDATE revision SET revision=?"

	private var currentRevision: RepositoryRevisionNumber = _

	@PostConstruct
	private def start(): Unit = {
		val dbConnection = derbyService.connection
		try {
			val dbMetadata = dbConnection.getMetaData
			val metadataResultSet = dbMetadata.getTables(null, null, null, Array("TABLE"))
			if (!metadataResultSet.next()) {
				LOG.debug("Initializing FS file store database")

				// virtual file links table
				val vlinkTableInitSQL =
				""" CREATE TABLE virtual_file_links (session_id VARCHAR(255), file_path VARCHAR(32672) NOT NULL, hash_file_path VARCHAR(255), revision VARCHAR(255) NOT NULL)  """
				val indexSessionIdInitSQL = """ CREATE INDEX ix_virtual_file_links_session_id on virtual_file_links (session_id) """
				val indexSourceLocationInitSQL = """ CREATE INDEX ix_virtual_file_links_file_path on virtual_file_links (file_path) """
				val indexLinkedHashFileInitSQL = """ CREATE INDEX ix_virtual_file_links_hash_file_path on virtual_file_links (hash_file_path) """
				val uniqueIndexSessionPathInitSQL = """ CREATE UNIQUE INDEX ix_virtual_file_links_unique_session_file_path ON virtual_file_links (session_id, file_path) """

				val stmt = dbConnection.createStatement()
				stmt.execute(vlinkTableInitSQL)
				stmt.execute(indexSourceLocationInitSQL)
				stmt.execute(indexLinkedHashFileInitSQL)
				stmt.execute(indexSessionIdInitSQL)
				stmt.execute(uniqueIndexSessionPathInitSQL)

				// journal entries table
				val journalTableInitSQL =
				""" CREATE TABLE virtual_file_links_journal (session_id VARCHAR(255) NOT NULL, file_path VARCHAR(32672) NOT NULL, hash_file_path VARCHAR(255) NOT NULL, revision VARCHAR(255) NOT NULL, action VARCHAR(10) NOT NULL)  """
				val indexJournalSessionIdInitSQL = """ CREATE INDEX ix_virtual_file_links_journal_session_id on virtual_file_links_journal (session_id) """
				stmt.execute(journalTableInitSQL)
				stmt.execute(indexJournalSessionIdInitSQL)

				// revision table
				val revisionTableInitSQL =
				""" CREATE TABLE revision (revision VARCHAR(255) NOT NULL)"""
				val revisionTableInsertSQL = """INSERT INTO revision(revision) VALUES ('0') """
				stmt.execute(revisionTableInitSQL)
				stmt.execute(revisionTableInsertSQL)

				dbConnection.commit()
				stmt.close()
			} else LOG.debug("FS file store database exists")
		} finally dbConnection.close()

		currentRevision = revision()

	}

	override def revision(): RepositoryRevisionNumber = {
		val dbConnection = derbyService.connection
		try {
			val ps = dbConnection.prepareStatement("SELECT revision FROM revision")
			val rs = ps.executeQuery()
			rs.next()
			new RepositoryRevisionNumber(rs.getString(1))
		} finally {
			dbConnection.close()
		}
	}

	def insertVirtualFilePointer(sessionID: String, revision: RepositoryRevisionNumber, filePath: String, hashedFilePath: String): Unit = {
		val dbConnection = derbyService.connection
		val virtualLinkInsertStmt = dbConnection.prepareStatement(""" INSERT INTO virtual_file_links (session_id, file_path, hash_file_path, revision) VALUES (?, ?, ?, ?) """)
		virtualLinkInsertStmt.setString(1, sessionID)
		virtualLinkInsertStmt.setString(2, filePath)
		virtualLinkInsertStmt.setString(3, hashedFilePath)
		virtualLinkInsertStmt.setString(4, revision.toString)
		virtualLinkInsertStmt.executeUpdate()

		insertJournalEntry(dbConnection, sessionID, filePath, hashedFilePath, revision, FileMetadataJournalEntry.ACTION_ADD)

		dbConnection.commit()
		dbConnection.close()
	}


	override def moveVirtualFilePointer(sessionID: String, revision: RepositoryRevisionNumber, sourcePath: String, targetPath: String): Unit = {
		val dbConnection = derbyService.connection

		val hashedFilePath = getHashFilePathForVirtualFile(sessionID, sourcePath).get

		val stmt = dbConnection.prepareStatement("UPDATE virtual_file_links SET file_path = ?, revision=? WHERE session_id = ? AND file_path = ?")
		stmt.setString(1, targetPath)
		stmt.setString(2, revision.toString())
		stmt.setString(3, sessionID)
		stmt.setString(4, sourcePath)
		try {
			val updateCount = stmt.executeUpdate()
			if (updateCount != 1) throw new RuntimeException(s"No virtual file path $sourcePath found for session $sessionID")

			insertJournalEntry(dbConnection, sessionID, sourcePath, hashedFilePath, revision, FileMetadataJournalEntry.ACTION_REMOVE)
			insertJournalEntry(dbConnection, sessionID, targetPath, hashedFilePath, revision, FileMetadataJournalEntry.ACTION_ADD)

			dbConnection.commit()
		} finally {
			dbConnection.close()
		}
	}

	def getHashFilePathForVirtualFile(sessionID: String, filePath: String): Option[String] = {
		val dbConnection = derbyService.connection

		val retrieveSessionFilePreparedStatement = sessionID match {
			case null =>
				val nullStmt = dbConnection.prepareStatement("""SELECT session_id, hash_file_path FROM virtual_file_links WHERE session_id is null AND file_path=?""")
				nullStmt.setString(1, filePath)
				nullStmt
			case s: String =>
				val stmt = dbConnection.prepareStatement("""SELECT session_id, hash_file_path FROM virtual_file_links WHERE (session_id is null OR session_id=?) AND file_path=?""")
				stmt.setString(1, sessionID)
				stmt.setString(2, filePath)
				stmt
		}

		// look for hashed files for a given file path and either no session ID, or a specific session ID.
		// if there's a result for a given session ID, that's a session-local file. Return that.
		// otherwise, return the file that isn't associated with a session ID. It's a permanently committed file.
		val rs = retrieveSessionFilePreparedStatement.executeQuery()
		try {
			val resultSetIter = new Iterator[(String, String)] {
				def hasNext = rs.next()
				def next() = (rs.getString(1), rs.getString(2))
			}
			val (nullResults, sessionResults) = resultSetIter.toList.partition(tuple => tuple._1 == null)
			if (sessionResults.nonEmpty) {
				sessionResults.head._2 match {
					case null => None
					case s: String => Some(s)
				}
			} else if (nullResults.nonEmpty) {
				Some(nullResults.head._2)
			} else None

		} finally {
			rs.close()
			retrieveSessionFilePreparedStatement.close()
			dbConnection.close()
		}
	}

	/**
		* Returns all session ID / virtual file path combinations that point to the hashed file
		* that this session and file path point to.
		*
		* @param sessionID the ID of the session to use for lookup
		* @param filePath  the virtual file path to use for lookup
		* @return a sequence of sessionID / virtual file path tuples pointing to the hashed file
		*/
	override def getVirtualFilePointers(sessionID: String, filePath: String): FileMetadataHashedFileVirtualPointers = {
		val dbConnection = derbyService.connection
		val ps = dbConnection.prepareStatement(
			"""
				|SELECT fl2.session_id, fl2.file_path, fl2.hash_file_path
				|FROM virtual_file_links fl1, virtual_file_links fl2
				|WHERE fl1.hash_file_path = fl2.hash_file_path
				|AND fl1.session_id=? AND fl1.file_path=?
				|""".stripMargin)
		ps.setString(1, sessionID)
		ps.setString(2, filePath)
		val rs = ps.executeQuery()
		val retBuffer = new ListBuffer[(String, String)]
		var hashFilePath: String = null
		try {
			while (rs.next()) {
				val sessionID = rs.getString(1)
				val filePath = rs.getString(2)
				if (hashFilePath == null) hashFilePath = rs.getString(3)
				val tuple = (sessionID, filePath)
				retBuffer += tuple
			}
			FileMetadataHashedFileVirtualPointers(hashFilePath, retBuffer)
		} finally {
			rs.close()
			ps.close()
			dbConnection.close()
		}
	}


	/**
		* Returns a list of hashed file pointers that point at the given hashed file path
		*/
	override def getVirtualFilePointers(hashFilePath: String): FileMetadataHashedFileVirtualPointers = {
		val dbConnection = derbyService.connection
		try {
			val ps = dbConnection.prepareStatement("SELECT session_id, file_path FROM virtual_file_links WHERE hash_file_path = ?")
			ps.setString(1, hashFilePath)
			val rs = ps.executeQuery()
			val iter = new Iterator[(String, String)] {
				override def hasNext(): Boolean = rs.next()
				override def next(): (String, String) = (rs.getString(1), rs.getString(2))
			}
			val pointers = iter.toList
			FileMetadataHashedFileVirtualPointers(hashFilePath, pointers)
		} finally dbConnection.close()
	}

	/**
		* Removes a virtual file pointer for a given session ID and file path.
		*
		* @param sessionID the session ID to use in link removal
		* @param filePath  the file path to use in link removal
		*/
	override def removeVirtualFilePointer(sessionID: String, revision: RepositoryRevisionNumber, filePath: String): Unit = {
		val hashedFilePath = getHashFilePathForVirtualFile(sessionID, filePath).get
		val dbConnection = derbyService.connection
		try {
			val sps = dbConnection.prepareStatement("SELECT session_id, file_path FROM virtual_file_links WHERE session_id = ? and file_path = ?")
			sps.setString(1, sessionID)
			sps.setString(2, filePath)
			val srs = sps.executeQuery()
			if (srs.next()) {
				val ps = dbConnection.prepareStatement(""" UPDATE virtual_file_links SET hash_file_path=null, revision=? WHERE session_id = ? and file_path = ? """)
				ps.setString(1, revision.toString)
				ps.setString(2, sessionID)
				ps.setString(3, filePath)
				ps.executeUpdate()
			} else {
				val ps = dbConnection.prepareStatement(
					"""
					|INSERT INTO virtual_file_links (session_id, file_path, hash_file_path, revision)
					|VALUES (?, ?, null, ?)
					|""".stripMargin)
				ps.setString(1, sessionID)
				ps.setString(2, filePath)
				ps.setString(3, revision.toString)

				ps.executeUpdate()
			}
			insertJournalEntry(dbConnection, sessionID, filePath, hashedFilePath, revision, FileMetadataJournalEntry.ACTION_REMOVE)
			dbConnection.commit()
		} finally dbConnection.close()
	}


	override def getCommittedRevisionNumbersForVirtualFilePointers(pointers: Seq[String]): Map[String, RepositoryRevisionNumber] = {
		val dbConnection = derbyService.connection
		try {
			val ps = dbConnection.prepareStatement("SELECT revision FROM virtual_file_links WHERE session_id IS NULL and file_path=?")
			val tuples = for (path <- pointers) yield {
				ps.setString(1, path)
				val rs = ps.executeQuery()
				rs.next() match {
					case true => (path, new RepositoryRevisionNumber(rs.getString(1)))
					case false => (path, null)
				}
			}
			tuples.toMap
		} finally dbConnection.close()
	}

	override def getJournalEntries(sessionID: String): Seq[FileMetadataJournalEntry] = {
		val dbConnection = derbyService.connection
		try {
			val ps = dbConnection.prepareStatement("SELECT session_id, file_path, hash_file_path, revision, action FROM virtual_file_links_journal WHERE session_id = ?")
			ps.setString(1, sessionID)
			val rs = ps.executeQuery()
			val iter = new Iterator[FileMetadataJournalEntry] {
				override def hasNext: Boolean = rs.next()
				override def next(): FileMetadataJournalEntry = new FileMetadataJournalEntry(rs.getString(1), rs.getString(2), rs.getString(3), new RepositoryRevisionNumber(rs.getString(4)), rs.getString(5))
			}
			iter.toList
		} finally dbConnection.close()
	}


	/**
		* Removes all transient file changes and journal entries for the given session ID
		*/
	override def revert(sessionID: String): Unit = {
		val dbConnection = derbyService.connection
		try {
			val entryPS = dbConnection.prepareStatement("DELETE FROM virtual_file_links WHERE session_id = ?")
			val journalPS = dbConnection.prepareStatement("DELETE FROM virtual_file_links_journal WHERE session_id = ?")
			entryPS.setString(1, sessionID)
			entryPS.executeUpdate()
			journalPS.setString(1, sessionID)
			journalPS.executeUpdate()
			dbConnection.commit()
		} finally dbConnection.close()
	}

	/**
		* Marks the virtual file pointers for a given session ID as permanent (i.e., not associated with a specific session)
		*
		* @param sessionID the ID of the session for which virtual file pointers should be made permanent
		*/
	override def commitJournalEntries(sessionID: String): RepositoryRevisionNumber = {
		val entries = getJournalEntries(sessionID)
		val dbConnection = derbyService.connection
		try {
			val newRevision = incrementRevision(dbConnection)
			val removeCurrentPS = dbConnection.prepareStatement("DELETE FROM virtual_file_links WHERE session_id is null and file_path = ?")
			val makePermanentPS = dbConnection.prepareStatement("UPDATE virtual_file_links SET session_id = null, revision = ? WHERE session_id = ? AND file_path = ?")
			for (entry <- entries) {
				LOG.debug(s"Processing journal entry $entry")
				entry.action match {
					case FileMetadataJournalEntry.ACTION_ADD =>
						removeCurrentPS.setString(1, entry.filePath)
						removeCurrentPS.executeUpdate()
						makePermanentPS.setString(1, newRevision.toString)
						makePermanentPS.setString(2, entry.sessionID)
						makePermanentPS.setString(3, entry.filePath)
						makePermanentPS.executeUpdate()
					case FileMetadataJournalEntry.ACTION_REMOVE =>
						removeCurrentPS.setString(1, entry.filePath)
						removeCurrentPS.executeUpdate()
				}
			}
			val clearPS = dbConnection.prepareStatement("DELETE FROM virtual_file_links_journal where session_id = ?")
			clearPS.setString(1, sessionID)
			clearPS.executeUpdate()
			dbConnection.commit()
			newRevision
		} finally {
			dbConnection.close()
		}
	}

	override protected[store] def clear(): Unit = {
		val dbConnection = derbyService.connection
		val ps = dbConnection.prepareStatement("DELETE FROM virtual_file_links")
		val jps = dbConnection.prepareStatement("DELETE FROM virtual_file_links_journal")
		val revps = dbConnection.prepareStatement("UPDATE revision SET revision='0'")
		try {
			ps.executeUpdate()
			jps.executeUpdate()
			revps.executeUpdate()
			dbConnection.commit()
			currentRevision = new RepositoryRevisionNumber(0)
		} finally {
			dbConnection.close()
		}
	}

	private def incrementRevision(dbConnection: Connection): RepositoryRevisionNumber = {
		val stmt = dbConnection.prepareStatement(GET_REVISION_SQL)
		val rs = stmt.executeQuery()
		rs.next()
		val rev = new RepositoryRevisionNumber(rs.getString(1))
		val newRev = rev + 1
		val stmt2 = dbConnection.prepareStatement(UPDATE_REVISION_SQL)
		stmt2.setString(1, newRev.toString)
		stmt2.executeUpdate()
		currentRevision = newRev
		LOG.info(s"Set revision to $newRev")
		currentRevision
	}

	private def insertJournalEntry(dbConnection: Connection, sessionID: String, filePath: String, hashedFilePath: String, revision: RepositoryRevisionNumber, action: String): Unit = {
		val journalInsertStatement = dbConnection.prepareStatement(INSERT_JOURNAL_ENTRY_SQL)
		journalInsertStatement.setString(1, sessionID)
		journalInsertStatement.setString(2, filePath)
		journalInsertStatement.setString(3, hashedFilePath)
		journalInsertStatement.setString(4, revision.toString)
		journalInsertStatement.setString(5, action)
		journalInsertStatement.execute()
	}
}
