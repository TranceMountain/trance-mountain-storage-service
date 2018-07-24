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

import java.io.ByteArrayInputStream

import scala.language.postfixOps
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import org.trancemountain.OptionalUnpacker
import org.trancemountain.storageservice.repository.RepositoryRevisionNumber

/**
	* @author michaelcoddington
	*/
trait IFileStoreTest extends FlatSpec with Matchers with BeforeAndAfterEach {

	def description: String

	def fileStore: IFileStore

	override def afterEach(): Unit = {
		fileStore.clear()
	}

	description should "create a file local to a session" in {
		val bytes = Array[Byte](1, 2, 3, 4)
		val bais = new ByteArrayInputStream(bytes)
		val rev0 = new RepositoryRevisionNumber(0)
		fileStore.createFile("sessionABC", rev0, "/child1/child2/prop1", bais)
	}

	it should "move a session-local file to a different location" in {
		val bytes = Array[Byte](1, 2, 3, 4)
		val bais = new ByteArrayInputStream(bytes)
		val rev0 = new RepositoryRevisionNumber(0)
		fileStore.createFile("sessionABC", rev0, "/child1/child2/prop2", bais)
		fileStore.moveFile("sessionABC", rev0, "/child1/child2/prop2", "/child1/child2/prop3")
	}

	it should "retrieve a file local to a session" in {
		val bytes = Array[Byte](1, 2, 3, 4)
		val bais = new ByteArrayInputStream(bytes)
		val rev0 = new RepositoryRevisionNumber(0)
		fileStore.createFile("sessionABC", rev0, "/child1/child2/prop2", bais)
		val is = fileStore.getInputStreamForFile("sessionABC", "/child1/child2/prop2")!;
		is should not be null
	}

	it should "hide a file local to one session from a different session" in {
		val bytes = Array[Byte](1, 2, 3, 4)
		val bais = new ByteArrayInputStream(bytes)
		val rev0 = new RepositoryRevisionNumber(0)
		fileStore.createFile("sessionA", rev0, "/child1/child/prop2", bais)
		val is = fileStore.getInputStreamForFile("sessionB", "/child1/child2/prop2")!;
		is should be (null)
	}

	it should "delete a file local to a session" in {
		val bytes = Array[Byte](1, 2, 3, 4)
		val bais = new ByteArrayInputStream(bytes)
		val rev0 = new RepositoryRevisionNumber(0)
		fileStore.createFile("sessionA", rev0, "/child1/child2/prop3", bais)
		fileStore.prepareCommit("sessionA", rev0) should be(true)
		val rev1 = fileStore.commit("sessionA", rev0)

		fileStore.deleteFile("sessionA", rev1, "/child1/child2/prop3")
		// this file should appear to be deleted from this session's point of view
		fileStore.getInputStreamForFile("sessionA", "/child1/child2/prop3") should be(None)
		// but not from another session's point of view
		fileStore.getInputStreamForFile("sessionB", "/child1/child2/prop3") should not be None
		// and certainly not from the repository's point of view
		val fileOption = fileStore.committedFile("/child1/child2/prop3")
		fileOption should not be null
		fileOption.get should not be null
	}

	it should "make a session-local file permanent" in {
		val bytes = Array[Byte](1, 2, 3, 4)
		val bais = new ByteArrayInputStream(bytes)
		val rev0 = new RepositoryRevisionNumber(0)
		fileStore.createFile("sessionA", rev0, "/child1/child2/prop3", bais)
		fileStore.prepareCommit("sessionA", rev0) should be(true)
		fileStore.commit("sessionA", rev0)

		val permFileOption = fileStore.committedFile("/child1/child2/prop3")
		permFileOption should not be null
		permFileOption.get should not be null
	}

	it should "return a permenent file when no session-local file exists" in {
		val bytes = Array[Byte](1, 2, 3, 4)
		val bais = new ByteArrayInputStream(bytes)
		val rev0 = new RepositoryRevisionNumber(0)
		fileStore.createFile("sessionA", rev0, "/child1/child2/prop3", bais)
		fileStore.prepareCommit("sessionA", rev0) should be(true)
		fileStore.commit("sessionA", rev0)

		val permFileOption = fileStore.getInputStreamForFile("sessionB", "/child1/child2/prop3")
		permFileOption should not be null
		permFileOption.get should not be null
	}

	it should "shadow a permanent file with a file that exists for a session" in {
		val bytes1 = Array[Byte](1, 2, 3, 4)
		val bais1 = new ByteArrayInputStream(bytes1)
		val rev0 = new RepositoryRevisionNumber(0)
		fileStore.createFile("sessionA", rev0, "/child1/child2/prop3", bais1)
		fileStore.prepareCommit("sessionA", rev0) should be(true)
		val rev1 = fileStore.commit("sessionA", rev0)

		val bytes2 = Array[Byte](2, 3, 4, 5)
		val bais2 = new ByteArrayInputStream(bytes2)
		fileStore.createFile("sessionB", rev1, "/child1/child2/prop3", bais2)

		val sessionBFile = fileStore.getInputStreamForFile("sessionB", "/child1/child2/prop3")
		sessionBFile should not be null
		val is = sessionBFile.get
		is should not be null
		val readBytes = new Array[Byte](4)
		is.read(readBytes, 0, 4)
		readBytes should be(bytes2)
	}

	it should "deduplicate files written to permanent storage" in {
		val bytes1 = Array[Byte](1, 2, 3, 4)
		val rev0 = new RepositoryRevisionNumber(0)
		fileStore.createFile("sessionA", rev0, "/child1/child2/prop3", new ByteArrayInputStream(bytes1))
		fileStore.createFile("sessionB", rev0, "/child1/child2/prop3", new ByteArrayInputStream(bytes1))
		fileStore.hashedFileCount should be(1)
	}

	it should "not immediately remove deleted files" in {
		val bytes1 = Array[Byte](1, 2, 3, 4)
		val rev0 = new RepositoryRevisionNumber(0)
		fileStore.createFile("sessionA", rev0, "/child1/child2/prop3", new ByteArrayInputStream(bytes1))
		fileStore.hashedFileCount should be(1)
		fileStore.deleteFile("sessionA", rev0, "/child1/child2/prop3")
		fileStore.hashedFileCount should be(1)
	}

	it should "remove deleted files on commit" in {
		val bytes1 = Array[Byte](1, 2, 3, 4)
		val rev0 = new RepositoryRevisionNumber(0)
		fileStore.createFile("sessionA", rev0, "/child1/child2/prop3", new ByteArrayInputStream(bytes1))
		fileStore.hashedFileCount should be(1)
		fileStore.prepareCommit("sessionA", rev0) should be(true)
		val rev1 = fileStore.commit("sessionA", rev0)
		fileStore.hashedFileCount should be(1)
		fileStore.deleteFile("sessionA", rev1, "/child1/child2/prop3")
		fileStore.hashedFileCount should be(1)
		fileStore.prepareCommit("sessionA", rev1) should be(true)
		fileStore.commit("sessionA", rev1)
		fileStore.hashedFileCount should be(0)
		fileStore.committedFile("/child1/child2/prop3") should be(None)
	}

	it should "only delete a file when the last pointer to it is removed" in {
		val bytes1 = Array[Byte](1, 2, 3, 4)
		val rev0 =  new RepositoryRevisionNumber(0)
		fileStore.createFile("sessionA", rev0, "/child1/child2/prop1", new ByteArrayInputStream(bytes1))
		fileStore.prepareCommit("sessionA", rev0) should be(true)
		val rev1 = fileStore.commit("sessionA", rev0)
		fileStore.createFile("sessionB", rev1, "/child1/child2/prop3", new ByteArrayInputStream(bytes1))
		fileStore.prepareCommit("sessionB", rev1) should be(true)
		val rev2 = fileStore.commit("sessionB", rev1)

		fileStore.hashedFileCount should be(1)

		fileStore.deleteFile("sessionA", rev2, "/child1/child2/prop1")
		fileStore.prepareCommit("sessionA", rev2) should be(true)
		val rev3 = fileStore.commit("sessionA", rev2)

		fileStore.hashedFileCount should be(1)

		fileStore.deleteFile("sessionB", rev3, "/child1/child2/prop3")
		fileStore.prepareCommit("sessionB", rev3) should be(true)
		fileStore.commit("sessionB", rev3)

		fileStore.hashedFileCount should be(0)
	}

	it should "create journal entries describing an atomic set of changes to be made for a given session" in {
		val bytes1 = Array[Byte](1, 2, 3, 4)
		val rev0 = new RepositoryRevisionNumber(0)
		fileStore.createFile("sessionA", rev0, "/child1/child2/prop1", new ByteArrayInputStream(bytes1))
		fileStore.getMetadataJournalEntries("sessionA").size should be(1)
		fileStore.deleteFile("sessionA", rev0, "/child1/child2/prop1")
		fileStore.getMetadataJournalEntries("sessionA").size should be(2)
		fileStore.createFile("sessionA", rev0, "/child1/child2/prop3", new ByteArrayInputStream(bytes1))
		fileStore.getMetadataJournalEntries("sessionA").size should be(3)
	}

	it should "be able to revert changes for a session" in {
		val bytes1 = Array[Byte](1, 2, 3, 4)
		val rev0 = new RepositoryRevisionNumber(0)
		fileStore.createFile("sessionA", rev0, "/child1/child2/prop1", new ByteArrayInputStream(bytes1))
		fileStore.revert("sessionA")
		fileStore.getMetadataJournalEntries("sessionA").size should be(0)
		fileStore.hashedFileCount should be(0)
	}

	it should "increment its revision on commit" in {
		val bytes1 = Array[Byte](1, 2, 3, 4)
		val rev0 = new RepositoryRevisionNumber(0)
		fileStore.createFile("sessionA", rev0, "/child1/child2/prop1", new ByteArrayInputStream(bytes1))
		fileStore.prepareCommit("sessionA", rev0) should be(true)
		fileStore.commit("sessionA", rev0)
		fileStore.revision() should be (new RepositoryRevisionNumber(1))
	}

	it should "prohibit a session from changing files at a higher revision than itself" in {
		val bytes1 = Array[Byte](1, 2, 3, 4)
		val rev0 = new RepositoryRevisionNumber(0)
		fileStore.createFile("sessionA", rev0, "/child1/child2/prop1", new ByteArrayInputStream(bytes1))
		fileStore.prepareCommit("sessionA", rev0) should be(true)
		fileStore.commit("sessionA", rev0)
		// repository should be at revision 1 at this point

		fileStore.createFile("sessionB", rev0, "/child1/child2/prop1", new ByteArrayInputStream(bytes1))
		fileStore.prepareCommit("sessionB", rev0) should be(false)
	}

	it should "not prohibit a session from changing files at an equal revision to itself but lower than the repository revision" in {
		val bytes1 = Array[Byte](1, 2, 3, 4)
		val rev0 = new RepositoryRevisionNumber(0)
		fileStore.createFile("sessionA", rev0, "/child1/child2/prop1", new ByteArrayInputStream(bytes1))
		fileStore.prepareCommit("sessionA", rev0) should be(true)
		val rev1: RepositoryRevisionNumber = fileStore.commit("sessionA", rev0)
		// repository should be at revision 1 at this point

		fileStore.createFile("sessionA", rev1, "/child1/child2/prop2", new ByteArrayInputStream(bytes1))
		fileStore.prepareCommit("sessionA", rev1) should be(true)
		fileStore.commit("sessionA", rev1)
		// repository should be at revision 2 now, but /child1/child2/prop1 is still at revision 1

		fileStore.createFile("sessionB", rev1, "/child1/child2/prop1", new ByteArrayInputStream(bytes1))
		fileStore.prepareCommit("sessionB", rev1) should be(true)
		val rev3 = fileStore.commit("sessionB", rev1)
		rev3 should be(new RepositoryRevisionNumber(3))
	}

}
