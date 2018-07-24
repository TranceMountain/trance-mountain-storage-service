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

package org.trancemountain.storageservice.repository.store.fs

import java.io.{File, FileInputStream, FileOutputStream, InputStream}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}
import java.security.{DigestOutputStream, MessageDigest}
import javax.annotation.PostConstruct

import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.{Required, Value}
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.trancemountain.storageservice.SpringConfigKeys
import org.trancemountain.storageservice.repository.store.IFileDataStore

object FSFileDataStore {
	private val LOG = LoggerFactory.getLogger(getClass)
}

/**
	* @author michaelcoddington
	*/
@Lazy
@Service
@ConditionalOnProperty(name = Array(SpringConfigKeys.TM_FILE_DATA_STORE_TYPE), havingValue = "fs")
class FSFileDataStore extends IFileDataStore {
	import FSFileDataStore.LOG

	@Required @Value(SpringConfigKeys.TM_FILE_STORE_FS_LOC_VALUE)
	private val topLevelStorageLocation: String = null

	private var topLevelFileFolder: File = _

	private var tempFileFolder: File = _

	private var hashedFileFolder: File = _

	LOG.info("Setting up FS File data store")

	@PostConstruct
	def start(): Unit = {
		topLevelFileFolder = new File(topLevelStorageLocation)

		LOG.debug(s"Initializing FS file storage in location $topLevelStorageLocation")

		tempFileFolder = new File(topLevelFileFolder, "tmp")
		LOG.debug(s"Initializing FS temp file storage in location ${tempFileFolder.getAbsolutePath}")

		hashedFileFolder = new File(topLevelFileFolder, "hashed")
		LOG.debug(s"Initializing FS hashed file storage in location ${hashedFileFolder.getAbsolutePath}")

		tempFileFolder.mkdirs()
		hashedFileFolder.mkdirs()
	}

	override def createFile(stream: InputStream): String = {
		require(stream.available() > 0, "Input stream is not available for reading.")

		// write the file to a temp file, calculating the SHA-1 hash of it at the same time
		val tempFile = Files.createTempFile(tempFileFolder.toPath, null, null).toFile
		val fos = new FileOutputStream(tempFile)
		val messageDigest = MessageDigest.getInstance("SHA-1")
		val dos = new DigestOutputStream(fos, messageDigest)

		IOUtils.copy(stream, dos)
		IOUtils.closeQuietly(dos)

		val digestBytes = messageDigest.digest()
		val sb = new StringBuffer()
		for (b <- digestBytes) sb.append(f"$b%02x") // convert byte to hex char
		val sha1hash = sb.toString

		val sha1Path = sha1hash.grouped(2).toList.take(4).mkString(File.separator)
		val sha1Folder = hashedFileFolder.toPath.resolve(sha1Path).toFile
		sha1Folder.mkdirs()
		val sha1File = new File(sha1Folder, s"$sha1hash.dat")
		val sha1FileRelPath = hashedFileFolder.toPath.relativize(sha1File.toPath).toString

		// write the hashed file if it doesn't already exist
		if (!sha1File.exists()) {
			Files.move(tempFile.toPath, sha1File.toPath)
			LOG.debug(s"Wrote file data to hashed path $sha1FileRelPath")
		} else {
			LOG.debug(s"Using existing file at hashed path $sha1FileRelPath")
			tempFile.delete()
		}

		sha1FileRelPath
	}

	/**
		* Returns an input stream for the hashed file at the given path, or None if no such file exists
		*
		* @param hashPath a hashed file path
		* @return an input stream for the file
		*/
	override def getInputStreamForFile(hashPath: String): Option[InputStream] = {
		val hashFile = file(hashPath)
		hashFile.exists() match {
			case true => Some(new FileInputStream(hashFile))
			case false => None
		}
	}

	override def fileExists(hashPath: String): Boolean = {
		val hashFile = file(hashPath)
		hashFile.exists && hashFile.isFile
	}

	override def delete(hashPath: String): Boolean = file(hashPath).delete()

	private def file(hashPath: String): File = hashedFileFolder.toPath.resolve(hashPath).toFile

	override def fileCount: Int = {
		var count = 0
		val countVisitor = new SimpleFileVisitor[Path]() {
			override def visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult = {
				val file = path.toFile
				if (file.isFile) count += 1
				super.visitFile(path, attrs)
			}
		}
		Files.walkFileTree(hashedFileFolder.toPath, countVisitor)
		count
	}

	override protected[store] def clear(): Unit = {
		val deleteVisitor = new SimpleFileVisitor[Path]() {
			override def visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult = {
				val file = path.toFile
				require(file.exists())
				LOG.info(s"Deleting file ${file.getAbsolutePath}")
				val deleted = file.delete()
				require(deleted, s"Unable to delete file ${file.getAbsolutePath}")
				super.visitFile(path, attrs)
			}
		}
		Files.walkFileTree(hashedFileFolder.toPath, deleteVisitor)
	}
}
