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

package org.trancemountain.storageservice.repository.search.parser

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import org.trancemountain.storageservice.repository.nodetype.NodeTypeIdentifier
import org.trancemountain.storageservice.repository.search.IQuery

/**
	* @author michaelcoddington
	*/
@RunWith(classOf[JUnitRunner])
class QueryParserTest extends FlatSpec with Matchers {

	private val queryParser = new AntlrQueryParser

	private val simpleSelect = new SelectClause("file")
	private val simpleFrom = new FromClause(FromClauseIdentifier(NodeTypeIdentifier("tm", "file"), "file"))

	private def parse(queryString: String): IQuery = {
		val query = queryParser.parse(queryString)
		query shouldNot be(null)
		query
	}

	"A query parser" should "parse a simple query" in {
		val query = parse("SELECT file FROM tm:file file")
		query shouldBe Query(SelectClause(List("file")), FromClause(List(FromClauseIdentifier(NodeTypeIdentifier("tm", "file"), "file"))), null, null)
	}

	it should "parse a simple query with a unary property filter" in {
		val query = parse("SELECT file FROM tm:file file FILTER file.name IS NOT NULL")
		query shouldBe Query(simpleSelect, simpleFrom,
			new FilterClause(
				FilterUnaryPropertyPredicate("file", "name", "IS NOT NULL")))
	}

	it should "parse a simple query with a binary property filter" in {
		val query = parse("SELECT file FROM tm:file file FILTER file.name = hello")
		query shouldBe Query(simpleSelect, simpleFrom, FilterClause(List(FilterBinaryPropertyPredicate("file", "name", "=", "hello"))))
	}

	it should "parse a simple query with a unary property function filter" in {
		val query = parse("SELECT file FROM tm:file file FILTER file.name.in('A', 'B')")
		query shouldBe Query(simpleSelect, simpleFrom,
			new FilterClause(
				FilterUnaryPropertyFunctionPredicate("file", "name", "in", List("'A'", "'B'"))))
	}

	it should "parse a simple query with a binary property function filter" in {
		val query = parse("SELECT file FROM tm:file file FILTER file.name.length() = 3")
		query shouldBe Query(simpleSelect, simpleFrom,
			new FilterClause(
				FilterBinaryPropertyFunctionPredicate("file", "name", "length", "=", "3")))
	}

	it should "parse a simple query with an AND filter" in {
		val query = parse("SELECT file FROM tm:file file FILTER file.name = hello AND file.size > 2")
		query shouldBe Query(simpleSelect, simpleFrom,
			new FilterClause(
				FilterAndPredicate(
					List(
						FilterBinaryPropertyPredicate("file", "name", "=", "hello"),
						FilterBinaryPropertyPredicate("file", "size", ">", "2")))))
	}

	it should "parse a simple query with an OR filter" in {
		val query = parse("SELECT file FROM tm:file file FILTER file.name = hello OR file.size > 2")
		query shouldBe Query(simpleSelect, simpleFrom,
			new FilterClause(
				FilterOrPredicate(
					List(
						FilterBinaryPropertyPredicate("file", "name", "=", "hello"),
						FilterBinaryPropertyPredicate("file", "size", ">", "2")))))
	}

	it should "parse a simple query with a NOT filter" in {
		val query = parse("SELECT file FROM tm:file file FILTER NOT file.name = hello")
		query shouldBe Query(simpleSelect, simpleFrom,
			new FilterClause(
				FilterNotPredicate("NOT", FilterBinaryPropertyPredicate("file", "name", "=", "hello"))))
	}

	it should "parse a simple query with a group filter" in {
		val query = parse("SELECT file FROM tm:file file FILTER (file.name = hello AND file.size = 3)")
		query shouldBe Query(simpleSelect, simpleFrom,
			new FilterClause(
				FilterAndPredicate(
					List(
						FilterBinaryPropertyPredicate("file", "name", "=", "hello"),
						FilterBinaryPropertyPredicate("file", "size", "=", "3")))))
	}

	it should "parse a simple query with a group filter that overrides AND" in {
		val query = parse("SELECT file FROM tm:file file FILTER file.name=x AND (file.name = hello OR file.size = 3)")
		query shouldBe Query(simpleSelect, simpleFrom,
			new FilterClause(
				FilterAndPredicate(
					List(
						FilterBinaryPropertyPredicate("file", "name", "=", "x"),
						FilterOrPredicate(
							List(
								FilterBinaryPropertyPredicate("file", "name", "=", "hello"),
								FilterBinaryPropertyPredicate("file", "size", "=", "3")))))))
	}

	it should "parse a simple query with a JOIN clause" in {
		val query = parse("SELECT file, folder FROM tm:file file, tm:folder folder JOIN folder.parentOf(file)")
		query shouldBe Query(
			SelectClause(Seq("file", "folder")),
			FromClause(Seq(FromClauseIdentifier(NodeTypeIdentifier("tm", "file"), "file"), FromClauseIdentifier(NodeTypeIdentifier("tm", "folder"), "folder"))),
			null,
			Seq(JoinClause("JOIN", List(JoinCriteriaPredicate("folder", "parentOf", "file"))))
		)
	}

	it should "parse a simple query with a JOIN OR clause" in {
		val query = parse("SELECT file, folder FROM tm:file file, tm:folder folder JOIN folder.parentOf(file) OR folder.childOf(file)")
		query shouldBe Query(
			SelectClause(Seq("file", "folder")),
			FromClause(Seq(FromClauseIdentifier(NodeTypeIdentifier("tm", "file"), "file"), FromClauseIdentifier(NodeTypeIdentifier("tm", "folder"), "folder"))),
			null,
			Seq(new JoinClause("JOIN", JoinOrPredicate(List(JoinCriteriaPredicate("folder", "parentOf", "file"), JoinCriteriaPredicate("folder", "childOf", "file")))))
		)
	}

	it should "parse a simple query with a JOIN AND clause" in {
		val query = parse("SELECT file, folder FROM tm:file file, tm:folder folder JOIN folder.parentOf(file) AND folder.childOf(file)")
		query shouldBe Query(
			SelectClause(Seq("file", "folder")),
			FromClause(Seq(FromClauseIdentifier(NodeTypeIdentifier("tm", "file"), "file"), FromClauseIdentifier(NodeTypeIdentifier("tm", "folder"), "folder"))),
			null,
			Seq(new JoinClause("JOIN", JoinAndPredicate(List(JoinCriteriaPredicate("folder", "parentOf", "file"), JoinCriteriaPredicate("folder", "childOf", "file")))))
		)
	}

	it should "parse a simple query with a JOIN NOT clause" in {
		val query = parse("SELECT file, folder FROM tm:file file, tm:folder folder JOIN NOT folder.parentOf(file)")
		query shouldBe Query(
			SelectClause(Seq("file", "folder")),
			FromClause(Seq(FromClauseIdentifier(NodeTypeIdentifier("tm", "file"), "file"), FromClauseIdentifier(NodeTypeIdentifier("tm", "folder"), "folder"))),
			null,
			Seq(new JoinClause("JOIN", JoinNotPredicate("NOT", JoinCriteriaPredicate("folder", "parentOf", "file"))))
		)
	}

	it should "parse a simple query with an OUTER JOIN clause" in {
		val query = parse("SELECT file, folder FROM tm:file file, tm:folder folder OUTER JOIN folder.parentOf(file)")
		query shouldBe Query(
			SelectClause(Seq("file", "folder")),
			FromClause(Seq(FromClauseIdentifier(NodeTypeIdentifier("tm", "file"), "file"), FromClauseIdentifier(NodeTypeIdentifier("tm", "folder"), "folder"))),
			null,
			Seq(JoinClause("OUTER JOIN", List(JoinCriteriaPredicate("folder", "parentOf", "file"))))
		)
	}

	it should "parse a simple query with multiple JOIN clauses" in {
		val query = parse("SELECT file, folder FROM tm:file file, tm:folder folder JOIN folder.parentOf(file) JOIN file.childOf(folder)")
		query shouldBe Query(
			SelectClause(Seq("file", "folder")),
			FromClause(Seq(FromClauseIdentifier(NodeTypeIdentifier("tm", "file"), "file"), FromClauseIdentifier(NodeTypeIdentifier("tm", "folder"), "folder"))),
			null,
			Seq(JoinClause("JOIN", List(JoinCriteriaPredicate("folder", "parentOf", "file"))), JoinClause("JOIN", List(JoinCriteriaPredicate("file", "childOf", "folder"))))
		)
	}

}
