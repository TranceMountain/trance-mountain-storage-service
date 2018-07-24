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

package org.trancemountain.storageservice.repository

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

/**
	* @author michaelcoddington
	*/
@RunWith(classOf[JUnitRunner])
class RepositoryRevisionNumberTest extends FlatSpec with Matchers {

	"A revision number" should "be able to print itself as a hex string" in {
		val repRevNum = new RepositoryRevisionNumber(26)
		repRevNum.toString should be ("1a")
	}

	it should "be able to be constructed from a hex string" in {
		val hex = "a1"
		val num = new RepositoryRevisionNumber(hex)
		num should be (161)
	}

	it should "be comparable to an int" in {
		val num = new RepositoryRevisionNumber(161)
		num should be (161)
	}

	it should "be comparable to an long" in {
		val num = new RepositoryRevisionNumber(161)
		num should be (161L)
	}

	it should "not be comparable to incomparable types" in {
		val num = new RepositoryRevisionNumber(161)
		num should not be new Object
	}

	it should "never be negative" in {
		an[IllegalArgumentException] should be thrownBy new RepositoryRevisionNumber(-1)
	}

	it should "normalize an input hex string to a lowercase representation" in {
		val hex = "A1"
		val num = new RepositoryRevisionNumber(hex)
		num should be (161)
		num.toString should be ("a1")
	}

	it should "be able to increment by 1" in {
		val num1 = new RepositoryRevisionNumber(17)
		val num2 = new RepositoryRevisionNumber(18)
		val num1plus = num1 + 1
		num1plus should be (num2)
	}

	it should "be able to decrement by 1" in {
		val num1 = new RepositoryRevisionNumber(17)
		val num2 = new RepositoryRevisionNumber(16)
		val num1minus = num1 - 1
		num1minus should be (num2)
	}

	it should "be accurately comparable to another revision" in {
		val num1 = new RepositoryRevisionNumber(17)
		val num2 = new RepositoryRevisionNumber(161922)

		num1 < num2 should be (true)
		num2 > num1 should be (true)
	}

	it should "be accurately comparable to a hex string" in {
		val num1 = new RepositoryRevisionNumber(17)
		num1 compareToHex "0" should be (1)
		num1 compareToHex "11" should be (0)
		num1 compareToHex "a3465" should be (-1)
	}

}
