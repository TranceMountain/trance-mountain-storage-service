// Generated from /Users/michaelcoddington/IdeaProjects/Trance Mountain/trance-mountain-storage-service/src/main/resources/tmql/TMQL.g4 by ANTLR 4.5.3
package org.trancemountain.storageservice.repository.search.parser.antlr;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class TMQLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.5.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, SELECT=16, 
		FROM=17, FILTER=18, JOIN=19, BOOLEAN_NOT=20, BOOLEAN_AND=21, BOOLEAN_OR=22, 
		OPEN_PAREN=23, CLOSE_PAREN=24, UNARY_COMPARISON_OPERATOR=25, BINARY_COMPARISON_OPERATOR=26, 
		PROPERTY_FUNCTION=27, IDENTIFIER=28, NUMBER=29, ANY_VAL=30, QUOTED_VAL=31, 
		WS=32;
	public static final int
		RULE_selectStatement = 0, RULE_identifierList = 1, RULE_nodeTypeIdentifier = 2, 
		RULE_aliasedNodeTypeIdentifierSequence = 3, RULE_aliasedNodeTypeIdentifier = 4, 
		RULE_filterStatement = 5, RULE_filterOrPredicate = 6, RULE_filterAndPredicate = 7, 
		RULE_filterNotPredicate = 8, RULE_filterBasePredicate = 9, RULE_filterPropertyUnaryClause = 10, 
		RULE_filterPropertyBinaryClause = 11, RULE_filterPropertyUnaryFunctionClause = 12, 
		RULE_filterPropertyBinaryFunctionClause = 13, RULE_joinStatement = 14, 
		RULE_joinOrPredicate = 15, RULE_joinAndPredicate = 16, RULE_joinNotPredicate = 17, 
		RULE_joinBasePredicate = 18, RULE_joinCriteria = 19;
	public static final String[] ruleNames = {
		"selectStatement", "identifierList", "nodeTypeIdentifier", "aliasedNodeTypeIdentifierSequence", 
		"aliasedNodeTypeIdentifier", "filterStatement", "filterOrPredicate", "filterAndPredicate", 
		"filterNotPredicate", "filterBasePredicate", "filterPropertyUnaryClause", 
		"filterPropertyBinaryClause", "filterPropertyUnaryFunctionClause", "filterPropertyBinaryFunctionClause", 
		"joinStatement", "joinOrPredicate", "joinAndPredicate", "joinNotPredicate", 
		"joinBasePredicate", "joinCriteria"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'*'", "','", "':'", "'.'", "'()'", "'parentOf'", "'ancestorOf'", 
		"'childOf'", "'descendantOf'", "'siblingOf'", "'strongRefTo'", "'weakRefTo'", 
		"'strongRefFrom'", "'weakRefFrom'", "'at'", null, null, null, null, null, 
		null, null, "'('", "')'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, "SELECT", "FROM", "FILTER", "JOIN", "BOOLEAN_NOT", 
		"BOOLEAN_AND", "BOOLEAN_OR", "OPEN_PAREN", "CLOSE_PAREN", "UNARY_COMPARISON_OPERATOR", 
		"BINARY_COMPARISON_OPERATOR", "PROPERTY_FUNCTION", "IDENTIFIER", "NUMBER", 
		"ANY_VAL", "QUOTED_VAL", "WS"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "TMQL.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public TMQLParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class SelectStatementContext extends ParserRuleContext {
		public TerminalNode SELECT() { return getToken(TMQLParser.SELECT, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public TerminalNode FROM() { return getToken(TMQLParser.FROM, 0); }
		public AliasedNodeTypeIdentifierSequenceContext aliasedNodeTypeIdentifierSequence() {
			return getRuleContext(AliasedNodeTypeIdentifierSequenceContext.class,0);
		}
		public FilterStatementContext filterStatement() {
			return getRuleContext(FilterStatementContext.class,0);
		}
		public List<JoinStatementContext> joinStatement() {
			return getRuleContexts(JoinStatementContext.class);
		}
		public JoinStatementContext joinStatement(int i) {
			return getRuleContext(JoinStatementContext.class,i);
		}
		public SelectStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectStatement; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitSelectStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectStatementContext selectStatement() throws RecognitionException {
		SelectStatementContext _localctx = new SelectStatementContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_selectStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(40);
			match(SELECT);
			setState(41);
			identifierList();
			setState(42);
			match(FROM);
			setState(43);
			aliasedNodeTypeIdentifierSequence();
			setState(45);
			_la = _input.LA(1);
			if (_la==FILTER) {
				{
				setState(44);
				filterStatement();
				}
			}

			setState(50);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==JOIN) {
				{
				{
				setState(47);
				joinStatement();
				}
				}
				setState(52);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IdentifierListContext extends ParserRuleContext {
		public IdentifierListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifierList; }
	 
		public IdentifierListContext() { }
		public void copyFrom(IdentifierListContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class NamedIDsContext extends IdentifierListContext {
		public Token IDENTIFIER;
		public List<Token> ids = new ArrayList<Token>();
		public List<TerminalNode> IDENTIFIER() { return getTokens(TMQLParser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(TMQLParser.IDENTIFIER, i);
		}
		public NamedIDsContext(IdentifierListContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitNamedIDs(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class WildcardIDContext extends IdentifierListContext {
		public WildcardIDContext(IdentifierListContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitWildcardID(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierListContext identifierList() throws RecognitionException {
		IdentifierListContext _localctx = new IdentifierListContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_identifierList);
		int _la;
		try {
			setState(62);
			switch (_input.LA(1)) {
			case T__0:
				_localctx = new WildcardIDContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(53);
				match(T__0);
				}
				break;
			case IDENTIFIER:
				_localctx = new NamedIDsContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(54);
				((NamedIDsContext)_localctx).IDENTIFIER = match(IDENTIFIER);
				((NamedIDsContext)_localctx).ids.add(((NamedIDsContext)_localctx).IDENTIFIER);
				setState(59);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(55);
					match(T__1);
					setState(56);
					((NamedIDsContext)_localctx).IDENTIFIER = match(IDENTIFIER);
					((NamedIDsContext)_localctx).ids.add(((NamedIDsContext)_localctx).IDENTIFIER);
					}
					}
					setState(61);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NodeTypeIdentifierContext extends ParserRuleContext {
		public Token namespace;
		public Token name;
		public Token version;
		public List<TerminalNode> IDENTIFIER() { return getTokens(TMQLParser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(TMQLParser.IDENTIFIER, i);
		}
		public TerminalNode NUMBER() { return getToken(TMQLParser.NUMBER, 0); }
		public NodeTypeIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nodeTypeIdentifier; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitNodeTypeIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NodeTypeIdentifierContext nodeTypeIdentifier() throws RecognitionException {
		NodeTypeIdentifierContext _localctx = new NodeTypeIdentifierContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_nodeTypeIdentifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(64);
			((NodeTypeIdentifierContext)_localctx).namespace = match(IDENTIFIER);
			setState(65);
			match(T__2);
			setState(66);
			((NodeTypeIdentifierContext)_localctx).name = match(IDENTIFIER);
			setState(69);
			_la = _input.LA(1);
			if (_la==T__2) {
				{
				setState(67);
				match(T__2);
				setState(68);
				((NodeTypeIdentifierContext)_localctx).version = match(NUMBER);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AliasedNodeTypeIdentifierSequenceContext extends ParserRuleContext {
		public List<AliasedNodeTypeIdentifierContext> aliasedNodeTypeIdentifier() {
			return getRuleContexts(AliasedNodeTypeIdentifierContext.class);
		}
		public AliasedNodeTypeIdentifierContext aliasedNodeTypeIdentifier(int i) {
			return getRuleContext(AliasedNodeTypeIdentifierContext.class,i);
		}
		public AliasedNodeTypeIdentifierSequenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aliasedNodeTypeIdentifierSequence; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitAliasedNodeTypeIdentifierSequence(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AliasedNodeTypeIdentifierSequenceContext aliasedNodeTypeIdentifierSequence() throws RecognitionException {
		AliasedNodeTypeIdentifierSequenceContext _localctx = new AliasedNodeTypeIdentifierSequenceContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_aliasedNodeTypeIdentifierSequence);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(71);
			aliasedNodeTypeIdentifier();
			setState(76);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(72);
				match(T__1);
				setState(73);
				aliasedNodeTypeIdentifier();
				}
				}
				setState(78);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AliasedNodeTypeIdentifierContext extends ParserRuleContext {
		public NodeTypeIdentifierContext nodetype;
		public Token identifier;
		public NodeTypeIdentifierContext nodeTypeIdentifier() {
			return getRuleContext(NodeTypeIdentifierContext.class,0);
		}
		public TerminalNode IDENTIFIER() { return getToken(TMQLParser.IDENTIFIER, 0); }
		public AliasedNodeTypeIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aliasedNodeTypeIdentifier; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitAliasedNodeTypeIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AliasedNodeTypeIdentifierContext aliasedNodeTypeIdentifier() throws RecognitionException {
		AliasedNodeTypeIdentifierContext _localctx = new AliasedNodeTypeIdentifierContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_aliasedNodeTypeIdentifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(79);
			((AliasedNodeTypeIdentifierContext)_localctx).nodetype = nodeTypeIdentifier();
			setState(81);
			_la = _input.LA(1);
			if (_la==IDENTIFIER) {
				{
				setState(80);
				((AliasedNodeTypeIdentifierContext)_localctx).identifier = match(IDENTIFIER);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FilterStatementContext extends ParserRuleContext {
		public TerminalNode FILTER() { return getToken(TMQLParser.FILTER, 0); }
		public FilterOrPredicateContext filterOrPredicate() {
			return getRuleContext(FilterOrPredicateContext.class,0);
		}
		public FilterStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterStatement; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitFilterStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterStatementContext filterStatement() throws RecognitionException {
		FilterStatementContext _localctx = new FilterStatementContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_filterStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(83);
			match(FILTER);
			setState(84);
			filterOrPredicate();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FilterOrPredicateContext extends ParserRuleContext {
		public List<FilterAndPredicateContext> filterAndPredicate() {
			return getRuleContexts(FilterAndPredicateContext.class);
		}
		public FilterAndPredicateContext filterAndPredicate(int i) {
			return getRuleContext(FilterAndPredicateContext.class,i);
		}
		public List<TerminalNode> BOOLEAN_OR() { return getTokens(TMQLParser.BOOLEAN_OR); }
		public TerminalNode BOOLEAN_OR(int i) {
			return getToken(TMQLParser.BOOLEAN_OR, i);
		}
		public FilterOrPredicateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterOrPredicate; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitFilterOrPredicate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterOrPredicateContext filterOrPredicate() throws RecognitionException {
		FilterOrPredicateContext _localctx = new FilterOrPredicateContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_filterOrPredicate);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(86);
			filterAndPredicate();
			setState(91);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==BOOLEAN_OR) {
				{
				{
				setState(87);
				match(BOOLEAN_OR);
				setState(88);
				filterAndPredicate();
				}
				}
				setState(93);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FilterAndPredicateContext extends ParserRuleContext {
		public List<FilterNotPredicateContext> filterNotPredicate() {
			return getRuleContexts(FilterNotPredicateContext.class);
		}
		public FilterNotPredicateContext filterNotPredicate(int i) {
			return getRuleContext(FilterNotPredicateContext.class,i);
		}
		public List<TerminalNode> BOOLEAN_AND() { return getTokens(TMQLParser.BOOLEAN_AND); }
		public TerminalNode BOOLEAN_AND(int i) {
			return getToken(TMQLParser.BOOLEAN_AND, i);
		}
		public FilterAndPredicateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterAndPredicate; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitFilterAndPredicate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterAndPredicateContext filterAndPredicate() throws RecognitionException {
		FilterAndPredicateContext _localctx = new FilterAndPredicateContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_filterAndPredicate);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(94);
			filterNotPredicate();
			setState(99);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==BOOLEAN_AND) {
				{
				{
				setState(95);
				match(BOOLEAN_AND);
				setState(96);
				filterNotPredicate();
				}
				}
				setState(101);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FilterNotPredicateContext extends ParserRuleContext {
		public Token notFlag;
		public FilterBasePredicateContext filterBasePredicate() {
			return getRuleContext(FilterBasePredicateContext.class,0);
		}
		public TerminalNode BOOLEAN_NOT() { return getToken(TMQLParser.BOOLEAN_NOT, 0); }
		public FilterNotPredicateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterNotPredicate; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitFilterNotPredicate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterNotPredicateContext filterNotPredicate() throws RecognitionException {
		FilterNotPredicateContext _localctx = new FilterNotPredicateContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_filterNotPredicate);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(103);
			_la = _input.LA(1);
			if (_la==BOOLEAN_NOT) {
				{
				setState(102);
				((FilterNotPredicateContext)_localctx).notFlag = match(BOOLEAN_NOT);
				}
			}

			setState(105);
			filterBasePredicate();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FilterBasePredicateContext extends ParserRuleContext {
		public FilterBasePredicateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterBasePredicate; }
	 
		public FilterBasePredicateContext() { }
		public void copyFrom(FilterBasePredicateContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class GroupFilterPredicateContext extends FilterBasePredicateContext {
		public TerminalNode OPEN_PAREN() { return getToken(TMQLParser.OPEN_PAREN, 0); }
		public FilterOrPredicateContext filterOrPredicate() {
			return getRuleContext(FilterOrPredicateContext.class,0);
		}
		public TerminalNode CLOSE_PAREN() { return getToken(TMQLParser.CLOSE_PAREN, 0); }
		public GroupFilterPredicateContext(FilterBasePredicateContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitGroupFilterPredicate(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SimpleFilterPredicateContext extends FilterBasePredicateContext {
		public FilterPropertyUnaryClauseContext filterPropertyUnaryClause() {
			return getRuleContext(FilterPropertyUnaryClauseContext.class,0);
		}
		public FilterPropertyBinaryClauseContext filterPropertyBinaryClause() {
			return getRuleContext(FilterPropertyBinaryClauseContext.class,0);
		}
		public FilterPropertyUnaryFunctionClauseContext filterPropertyUnaryFunctionClause() {
			return getRuleContext(FilterPropertyUnaryFunctionClauseContext.class,0);
		}
		public FilterPropertyBinaryFunctionClauseContext filterPropertyBinaryFunctionClause() {
			return getRuleContext(FilterPropertyBinaryFunctionClauseContext.class,0);
		}
		public SimpleFilterPredicateContext(FilterBasePredicateContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitSimpleFilterPredicate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterBasePredicateContext filterBasePredicate() throws RecognitionException {
		FilterBasePredicateContext _localctx = new FilterBasePredicateContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_filterBasePredicate);
		try {
			setState(117);
			switch (_input.LA(1)) {
			case OPEN_PAREN:
				_localctx = new GroupFilterPredicateContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(107);
				match(OPEN_PAREN);
				setState(108);
				filterOrPredicate();
				setState(109);
				match(CLOSE_PAREN);
				}
				break;
			case IDENTIFIER:
				_localctx = new SimpleFilterPredicateContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(115);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
				case 1:
					{
					setState(111);
					filterPropertyUnaryClause();
					}
					break;
				case 2:
					{
					setState(112);
					filterPropertyBinaryClause();
					}
					break;
				case 3:
					{
					setState(113);
					filterPropertyUnaryFunctionClause();
					}
					break;
				case 4:
					{
					setState(114);
					filterPropertyBinaryFunctionClause();
					}
					break;
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FilterPropertyUnaryClauseContext extends ParserRuleContext {
		public Token alias;
		public Token property;
		public Token unaryOperator;
		public List<TerminalNode> IDENTIFIER() { return getTokens(TMQLParser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(TMQLParser.IDENTIFIER, i);
		}
		public TerminalNode UNARY_COMPARISON_OPERATOR() { return getToken(TMQLParser.UNARY_COMPARISON_OPERATOR, 0); }
		public TerminalNode PROPERTY_FUNCTION() { return getToken(TMQLParser.PROPERTY_FUNCTION, 0); }
		public FilterPropertyUnaryClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterPropertyUnaryClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitFilterPropertyUnaryClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterPropertyUnaryClauseContext filterPropertyUnaryClause() throws RecognitionException {
		FilterPropertyUnaryClauseContext _localctx = new FilterPropertyUnaryClauseContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_filterPropertyUnaryClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(119);
			((FilterPropertyUnaryClauseContext)_localctx).alias = match(IDENTIFIER);
			setState(120);
			match(T__3);
			setState(121);
			((FilterPropertyUnaryClauseContext)_localctx).property = _input.LT(1);
			_la = _input.LA(1);
			if ( !(_la==PROPERTY_FUNCTION || _la==IDENTIFIER) ) {
				((FilterPropertyUnaryClauseContext)_localctx).property = (Token)_errHandler.recoverInline(this);
			} else {
				consume();
			}
			setState(122);
			((FilterPropertyUnaryClauseContext)_localctx).unaryOperator = match(UNARY_COMPARISON_OPERATOR);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FilterPropertyBinaryClauseContext extends ParserRuleContext {
		public Token alias;
		public Token property;
		public Token binaryOperator;
		public Token value;
		public List<TerminalNode> IDENTIFIER() { return getTokens(TMQLParser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(TMQLParser.IDENTIFIER, i);
		}
		public TerminalNode BINARY_COMPARISON_OPERATOR() { return getToken(TMQLParser.BINARY_COMPARISON_OPERATOR, 0); }
		public TerminalNode PROPERTY_FUNCTION() { return getToken(TMQLParser.PROPERTY_FUNCTION, 0); }
		public TerminalNode NUMBER() { return getToken(TMQLParser.NUMBER, 0); }
		public TerminalNode QUOTED_VAL() { return getToken(TMQLParser.QUOTED_VAL, 0); }
		public TerminalNode ANY_VAL() { return getToken(TMQLParser.ANY_VAL, 0); }
		public FilterPropertyBinaryClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterPropertyBinaryClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitFilterPropertyBinaryClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterPropertyBinaryClauseContext filterPropertyBinaryClause() throws RecognitionException {
		FilterPropertyBinaryClauseContext _localctx = new FilterPropertyBinaryClauseContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_filterPropertyBinaryClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(124);
			((FilterPropertyBinaryClauseContext)_localctx).alias = match(IDENTIFIER);
			setState(125);
			match(T__3);
			setState(126);
			((FilterPropertyBinaryClauseContext)_localctx).property = _input.LT(1);
			_la = _input.LA(1);
			if ( !(_la==PROPERTY_FUNCTION || _la==IDENTIFIER) ) {
				((FilterPropertyBinaryClauseContext)_localctx).property = (Token)_errHandler.recoverInline(this);
			} else {
				consume();
			}
			setState(127);
			((FilterPropertyBinaryClauseContext)_localctx).binaryOperator = match(BINARY_COMPARISON_OPERATOR);
			setState(128);
			((FilterPropertyBinaryClauseContext)_localctx).value = _input.LT(1);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << IDENTIFIER) | (1L << NUMBER) | (1L << ANY_VAL) | (1L << QUOTED_VAL))) != 0)) ) {
				((FilterPropertyBinaryClauseContext)_localctx).value = (Token)_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FilterPropertyUnaryFunctionClauseContext extends ParserRuleContext {
		public Token alias;
		public Token property;
		public Token propertyFunction;
		public Token NUMBER;
		public List<Token> propertyArgs = new ArrayList<Token>();
		public Token QUOTED_VAL;
		public Token _tset535;
		public Token _tset546;
		public List<TerminalNode> IDENTIFIER() { return getTokens(TMQLParser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(TMQLParser.IDENTIFIER, i);
		}
		public List<TerminalNode> PROPERTY_FUNCTION() { return getTokens(TMQLParser.PROPERTY_FUNCTION); }
		public TerminalNode PROPERTY_FUNCTION(int i) {
			return getToken(TMQLParser.PROPERTY_FUNCTION, i);
		}
		public List<TerminalNode> NUMBER() { return getTokens(TMQLParser.NUMBER); }
		public TerminalNode NUMBER(int i) {
			return getToken(TMQLParser.NUMBER, i);
		}
		public List<TerminalNode> QUOTED_VAL() { return getTokens(TMQLParser.QUOTED_VAL); }
		public TerminalNode QUOTED_VAL(int i) {
			return getToken(TMQLParser.QUOTED_VAL, i);
		}
		public FilterPropertyUnaryFunctionClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterPropertyUnaryFunctionClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitFilterPropertyUnaryFunctionClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterPropertyUnaryFunctionClauseContext filterPropertyUnaryFunctionClause() throws RecognitionException {
		FilterPropertyUnaryFunctionClauseContext _localctx = new FilterPropertyUnaryFunctionClauseContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_filterPropertyUnaryFunctionClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(130);
			((FilterPropertyUnaryFunctionClauseContext)_localctx).alias = match(IDENTIFIER);
			setState(131);
			match(T__3);
			setState(132);
			((FilterPropertyUnaryFunctionClauseContext)_localctx).property = _input.LT(1);
			_la = _input.LA(1);
			if ( !(_la==PROPERTY_FUNCTION || _la==IDENTIFIER) ) {
				((FilterPropertyUnaryFunctionClauseContext)_localctx).property = (Token)_errHandler.recoverInline(this);
			} else {
				consume();
			}
			setState(133);
			match(T__3);
			setState(134);
			((FilterPropertyUnaryFunctionClauseContext)_localctx).propertyFunction = match(PROPERTY_FUNCTION);
			setState(135);
			match(OPEN_PAREN);
			setState(136);
			((FilterPropertyUnaryFunctionClauseContext)_localctx)._tset535 = _input.LT(1);
			_la = _input.LA(1);
			if ( !(_la==NUMBER || _la==QUOTED_VAL) ) {
				((FilterPropertyUnaryFunctionClauseContext)_localctx)._tset535 = (Token)_errHandler.recoverInline(this);
			} else {
				consume();
			}
			((FilterPropertyUnaryFunctionClauseContext)_localctx).propertyArgs.add(((FilterPropertyUnaryFunctionClauseContext)_localctx)._tset535);
			setState(141);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(137);
				match(T__1);
				setState(138);
				((FilterPropertyUnaryFunctionClauseContext)_localctx)._tset546 = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==NUMBER || _la==QUOTED_VAL) ) {
					((FilterPropertyUnaryFunctionClauseContext)_localctx)._tset546 = (Token)_errHandler.recoverInline(this);
				} else {
					consume();
				}
				((FilterPropertyUnaryFunctionClauseContext)_localctx).propertyArgs.add(((FilterPropertyUnaryFunctionClauseContext)_localctx)._tset546);
				}
				}
				setState(143);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(144);
			match(CLOSE_PAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FilterPropertyBinaryFunctionClauseContext extends ParserRuleContext {
		public Token alias;
		public Token property;
		public Token propertyFunction;
		public Token binaryOperator;
		public Token value;
		public List<TerminalNode> IDENTIFIER() { return getTokens(TMQLParser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(TMQLParser.IDENTIFIER, i);
		}
		public List<TerminalNode> PROPERTY_FUNCTION() { return getTokens(TMQLParser.PROPERTY_FUNCTION); }
		public TerminalNode PROPERTY_FUNCTION(int i) {
			return getToken(TMQLParser.PROPERTY_FUNCTION, i);
		}
		public TerminalNode BINARY_COMPARISON_OPERATOR() { return getToken(TMQLParser.BINARY_COMPARISON_OPERATOR, 0); }
		public TerminalNode NUMBER() { return getToken(TMQLParser.NUMBER, 0); }
		public TerminalNode QUOTED_VAL() { return getToken(TMQLParser.QUOTED_VAL, 0); }
		public TerminalNode ANY_VAL() { return getToken(TMQLParser.ANY_VAL, 0); }
		public FilterPropertyBinaryFunctionClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterPropertyBinaryFunctionClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitFilterPropertyBinaryFunctionClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterPropertyBinaryFunctionClauseContext filterPropertyBinaryFunctionClause() throws RecognitionException {
		FilterPropertyBinaryFunctionClauseContext _localctx = new FilterPropertyBinaryFunctionClauseContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_filterPropertyBinaryFunctionClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(146);
			((FilterPropertyBinaryFunctionClauseContext)_localctx).alias = match(IDENTIFIER);
			setState(147);
			match(T__3);
			setState(148);
			((FilterPropertyBinaryFunctionClauseContext)_localctx).property = _input.LT(1);
			_la = _input.LA(1);
			if ( !(_la==PROPERTY_FUNCTION || _la==IDENTIFIER) ) {
				((FilterPropertyBinaryFunctionClauseContext)_localctx).property = (Token)_errHandler.recoverInline(this);
			} else {
				consume();
			}
			setState(149);
			match(T__3);
			setState(150);
			((FilterPropertyBinaryFunctionClauseContext)_localctx).propertyFunction = match(PROPERTY_FUNCTION);
			setState(151);
			match(T__4);
			setState(152);
			((FilterPropertyBinaryFunctionClauseContext)_localctx).binaryOperator = match(BINARY_COMPARISON_OPERATOR);
			setState(153);
			((FilterPropertyBinaryFunctionClauseContext)_localctx).value = _input.LT(1);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << IDENTIFIER) | (1L << NUMBER) | (1L << ANY_VAL) | (1L << QUOTED_VAL))) != 0)) ) {
				((FilterPropertyBinaryFunctionClauseContext)_localctx).value = (Token)_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class JoinStatementContext extends ParserRuleContext {
		public Token joinType;
		public TerminalNode JOIN() { return getToken(TMQLParser.JOIN, 0); }
		public List<JoinOrPredicateContext> joinOrPredicate() {
			return getRuleContexts(JoinOrPredicateContext.class);
		}
		public JoinOrPredicateContext joinOrPredicate(int i) {
			return getRuleContext(JoinOrPredicateContext.class,i);
		}
		public JoinStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinStatement; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitJoinStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinStatementContext joinStatement() throws RecognitionException {
		JoinStatementContext _localctx = new JoinStatementContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_joinStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(155);
			((JoinStatementContext)_localctx).joinType = match(JOIN);
			setState(157); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(156);
				joinOrPredicate();
				}
				}
				setState(159); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN_NOT) | (1L << OPEN_PAREN) | (1L << IDENTIFIER))) != 0) );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class JoinOrPredicateContext extends ParserRuleContext {
		public List<JoinAndPredicateContext> joinAndPredicate() {
			return getRuleContexts(JoinAndPredicateContext.class);
		}
		public JoinAndPredicateContext joinAndPredicate(int i) {
			return getRuleContext(JoinAndPredicateContext.class,i);
		}
		public List<TerminalNode> BOOLEAN_OR() { return getTokens(TMQLParser.BOOLEAN_OR); }
		public TerminalNode BOOLEAN_OR(int i) {
			return getToken(TMQLParser.BOOLEAN_OR, i);
		}
		public JoinOrPredicateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinOrPredicate; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitJoinOrPredicate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinOrPredicateContext joinOrPredicate() throws RecognitionException {
		JoinOrPredicateContext _localctx = new JoinOrPredicateContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_joinOrPredicate);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(161);
			joinAndPredicate();
			setState(166);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==BOOLEAN_OR) {
				{
				{
				setState(162);
				match(BOOLEAN_OR);
				setState(163);
				joinAndPredicate();
				}
				}
				setState(168);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class JoinAndPredicateContext extends ParserRuleContext {
		public List<JoinNotPredicateContext> joinNotPredicate() {
			return getRuleContexts(JoinNotPredicateContext.class);
		}
		public JoinNotPredicateContext joinNotPredicate(int i) {
			return getRuleContext(JoinNotPredicateContext.class,i);
		}
		public List<TerminalNode> BOOLEAN_AND() { return getTokens(TMQLParser.BOOLEAN_AND); }
		public TerminalNode BOOLEAN_AND(int i) {
			return getToken(TMQLParser.BOOLEAN_AND, i);
		}
		public JoinAndPredicateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinAndPredicate; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitJoinAndPredicate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinAndPredicateContext joinAndPredicate() throws RecognitionException {
		JoinAndPredicateContext _localctx = new JoinAndPredicateContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_joinAndPredicate);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(169);
			joinNotPredicate();
			setState(174);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==BOOLEAN_AND) {
				{
				{
				setState(170);
				match(BOOLEAN_AND);
				setState(171);
				joinNotPredicate();
				}
				}
				setState(176);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class JoinNotPredicateContext extends ParserRuleContext {
		public Token notFlag;
		public JoinBasePredicateContext joinBasePredicate() {
			return getRuleContext(JoinBasePredicateContext.class,0);
		}
		public TerminalNode BOOLEAN_NOT() { return getToken(TMQLParser.BOOLEAN_NOT, 0); }
		public JoinNotPredicateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinNotPredicate; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitJoinNotPredicate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinNotPredicateContext joinNotPredicate() throws RecognitionException {
		JoinNotPredicateContext _localctx = new JoinNotPredicateContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_joinNotPredicate);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(178);
			_la = _input.LA(1);
			if (_la==BOOLEAN_NOT) {
				{
				setState(177);
				((JoinNotPredicateContext)_localctx).notFlag = match(BOOLEAN_NOT);
				}
			}

			setState(180);
			joinBasePredicate();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class JoinBasePredicateContext extends ParserRuleContext {
		public JoinBasePredicateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinBasePredicate; }
	 
		public JoinBasePredicateContext() { }
		public void copyFrom(JoinBasePredicateContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class SimpleJoinPredicateContext extends JoinBasePredicateContext {
		public JoinCriteriaContext joinCriteria() {
			return getRuleContext(JoinCriteriaContext.class,0);
		}
		public SimpleJoinPredicateContext(JoinBasePredicateContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitSimpleJoinPredicate(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class GroupJoinPredicateContext extends JoinBasePredicateContext {
		public TerminalNode OPEN_PAREN() { return getToken(TMQLParser.OPEN_PAREN, 0); }
		public JoinOrPredicateContext joinOrPredicate() {
			return getRuleContext(JoinOrPredicateContext.class,0);
		}
		public TerminalNode CLOSE_PAREN() { return getToken(TMQLParser.CLOSE_PAREN, 0); }
		public GroupJoinPredicateContext(JoinBasePredicateContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitGroupJoinPredicate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinBasePredicateContext joinBasePredicate() throws RecognitionException {
		JoinBasePredicateContext _localctx = new JoinBasePredicateContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_joinBasePredicate);
		try {
			setState(187);
			switch (_input.LA(1)) {
			case OPEN_PAREN:
				_localctx = new GroupJoinPredicateContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(182);
				match(OPEN_PAREN);
				setState(183);
				joinOrPredicate();
				setState(184);
				match(CLOSE_PAREN);
				}
				break;
			case IDENTIFIER:
				_localctx = new SimpleJoinPredicateContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(186);
				joinCriteria();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class JoinCriteriaContext extends ParserRuleContext {
		public Token identifier;
		public Token functionName;
		public Token functionIdentifier;
		public Token functionParam;
		public Token atParam;
		public TerminalNode OPEN_PAREN() { return getToken(TMQLParser.OPEN_PAREN, 0); }
		public TerminalNode CLOSE_PAREN() { return getToken(TMQLParser.CLOSE_PAREN, 0); }
		public List<TerminalNode> IDENTIFIER() { return getTokens(TMQLParser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(TMQLParser.IDENTIFIER, i);
		}
		public List<TerminalNode> QUOTED_VAL() { return getTokens(TMQLParser.QUOTED_VAL); }
		public TerminalNode QUOTED_VAL(int i) {
			return getToken(TMQLParser.QUOTED_VAL, i);
		}
		public JoinCriteriaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinCriteria; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TMQLVisitor ) return ((TMQLVisitor<? extends T>)visitor).visitJoinCriteria(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinCriteriaContext joinCriteria() throws RecognitionException {
		JoinCriteriaContext _localctx = new JoinCriteriaContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_joinCriteria);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(189);
			((JoinCriteriaContext)_localctx).identifier = match(IDENTIFIER);
			setState(190);
			match(T__3);
			setState(191);
			((JoinCriteriaContext)_localctx).functionName = _input.LT(1);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13))) != 0)) ) {
				((JoinCriteriaContext)_localctx).functionName = (Token)_errHandler.recoverInline(this);
			} else {
				consume();
			}
			setState(192);
			match(OPEN_PAREN);
			setState(193);
			((JoinCriteriaContext)_localctx).functionIdentifier = match(IDENTIFIER);
			setState(196);
			_la = _input.LA(1);
			if (_la==T__1) {
				{
				setState(194);
				match(T__1);
				setState(195);
				((JoinCriteriaContext)_localctx).functionParam = match(QUOTED_VAL);
				}
			}

			setState(198);
			match(CLOSE_PAREN);
			setState(201);
			_la = _input.LA(1);
			if (_la==T__14) {
				{
				setState(199);
				match(T__14);
				setState(200);
				((JoinCriteriaContext)_localctx).atParam = match(QUOTED_VAL);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\"\u00ce\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\3\2\3\2\3\2\3\2\3\2\5\2\60\n\2\3\2\7\2"+
		"\63\n\2\f\2\16\2\66\13\2\3\3\3\3\3\3\3\3\7\3<\n\3\f\3\16\3?\13\3\5\3A"+
		"\n\3\3\4\3\4\3\4\3\4\3\4\5\4H\n\4\3\5\3\5\3\5\7\5M\n\5\f\5\16\5P\13\5"+
		"\3\6\3\6\5\6T\n\6\3\7\3\7\3\7\3\b\3\b\3\b\7\b\\\n\b\f\b\16\b_\13\b\3\t"+
		"\3\t\3\t\7\td\n\t\f\t\16\tg\13\t\3\n\5\nj\n\n\3\n\3\n\3\13\3\13\3\13\3"+
		"\13\3\13\3\13\3\13\3\13\5\13v\n\13\5\13x\n\13\3\f\3\f\3\f\3\f\3\f\3\r"+
		"\3\r\3\r\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\7\16"+
		"\u008e\n\16\f\16\16\16\u0091\13\16\3\16\3\16\3\17\3\17\3\17\3\17\3\17"+
		"\3\17\3\17\3\17\3\17\3\20\3\20\6\20\u00a0\n\20\r\20\16\20\u00a1\3\21\3"+
		"\21\3\21\7\21\u00a7\n\21\f\21\16\21\u00aa\13\21\3\22\3\22\3\22\7\22\u00af"+
		"\n\22\f\22\16\22\u00b2\13\22\3\23\5\23\u00b5\n\23\3\23\3\23\3\24\3\24"+
		"\3\24\3\24\3\24\5\24\u00be\n\24\3\25\3\25\3\25\3\25\3\25\3\25\3\25\5\25"+
		"\u00c7\n\25\3\25\3\25\3\25\5\25\u00cc\n\25\3\25\2\2\26\2\4\6\b\n\f\16"+
		"\20\22\24\26\30\32\34\36 \"$&(\2\6\3\2\35\36\3\2\36!\4\2\37\37!!\3\2\b"+
		"\20\u00cf\2*\3\2\2\2\4@\3\2\2\2\6B\3\2\2\2\bI\3\2\2\2\nQ\3\2\2\2\fU\3"+
		"\2\2\2\16X\3\2\2\2\20`\3\2\2\2\22i\3\2\2\2\24w\3\2\2\2\26y\3\2\2\2\30"+
		"~\3\2\2\2\32\u0084\3\2\2\2\34\u0094\3\2\2\2\36\u009d\3\2\2\2 \u00a3\3"+
		"\2\2\2\"\u00ab\3\2\2\2$\u00b4\3\2\2\2&\u00bd\3\2\2\2(\u00bf\3\2\2\2*+"+
		"\7\22\2\2+,\5\4\3\2,-\7\23\2\2-/\5\b\5\2.\60\5\f\7\2/.\3\2\2\2/\60\3\2"+
		"\2\2\60\64\3\2\2\2\61\63\5\36\20\2\62\61\3\2\2\2\63\66\3\2\2\2\64\62\3"+
		"\2\2\2\64\65\3\2\2\2\65\3\3\2\2\2\66\64\3\2\2\2\67A\7\3\2\28=\7\36\2\2"+
		"9:\7\4\2\2:<\7\36\2\2;9\3\2\2\2<?\3\2\2\2=;\3\2\2\2=>\3\2\2\2>A\3\2\2"+
		"\2?=\3\2\2\2@\67\3\2\2\2@8\3\2\2\2A\5\3\2\2\2BC\7\36\2\2CD\7\5\2\2DG\7"+
		"\36\2\2EF\7\5\2\2FH\7\37\2\2GE\3\2\2\2GH\3\2\2\2H\7\3\2\2\2IN\5\n\6\2"+
		"JK\7\4\2\2KM\5\n\6\2LJ\3\2\2\2MP\3\2\2\2NL\3\2\2\2NO\3\2\2\2O\t\3\2\2"+
		"\2PN\3\2\2\2QS\5\6\4\2RT\7\36\2\2SR\3\2\2\2ST\3\2\2\2T\13\3\2\2\2UV\7"+
		"\24\2\2VW\5\16\b\2W\r\3\2\2\2X]\5\20\t\2YZ\7\30\2\2Z\\\5\20\t\2[Y\3\2"+
		"\2\2\\_\3\2\2\2][\3\2\2\2]^\3\2\2\2^\17\3\2\2\2_]\3\2\2\2`e\5\22\n\2a"+
		"b\7\27\2\2bd\5\22\n\2ca\3\2\2\2dg\3\2\2\2ec\3\2\2\2ef\3\2\2\2f\21\3\2"+
		"\2\2ge\3\2\2\2hj\7\26\2\2ih\3\2\2\2ij\3\2\2\2jk\3\2\2\2kl\5\24\13\2l\23"+
		"\3\2\2\2mn\7\31\2\2no\5\16\b\2op\7\32\2\2px\3\2\2\2qv\5\26\f\2rv\5\30"+
		"\r\2sv\5\32\16\2tv\5\34\17\2uq\3\2\2\2ur\3\2\2\2us\3\2\2\2ut\3\2\2\2v"+
		"x\3\2\2\2wm\3\2\2\2wu\3\2\2\2x\25\3\2\2\2yz\7\36\2\2z{\7\6\2\2{|\t\2\2"+
		"\2|}\7\33\2\2}\27\3\2\2\2~\177\7\36\2\2\177\u0080\7\6\2\2\u0080\u0081"+
		"\t\2\2\2\u0081\u0082\7\34\2\2\u0082\u0083\t\3\2\2\u0083\31\3\2\2\2\u0084"+
		"\u0085\7\36\2\2\u0085\u0086\7\6\2\2\u0086\u0087\t\2\2\2\u0087\u0088\7"+
		"\6\2\2\u0088\u0089\7\35\2\2\u0089\u008a\7\31\2\2\u008a\u008f\t\4\2\2\u008b"+
		"\u008c\7\4\2\2\u008c\u008e\t\4\2\2\u008d\u008b\3\2\2\2\u008e\u0091\3\2"+
		"\2\2\u008f\u008d\3\2\2\2\u008f\u0090\3\2\2\2\u0090\u0092\3\2\2\2\u0091"+
		"\u008f\3\2\2\2\u0092\u0093\7\32\2\2\u0093\33\3\2\2\2\u0094\u0095\7\36"+
		"\2\2\u0095\u0096\7\6\2\2\u0096\u0097\t\2\2\2\u0097\u0098\7\6\2\2\u0098"+
		"\u0099\7\35\2\2\u0099\u009a\7\7\2\2\u009a\u009b\7\34\2\2\u009b\u009c\t"+
		"\3\2\2\u009c\35\3\2\2\2\u009d\u009f\7\25\2\2\u009e\u00a0\5 \21\2\u009f"+
		"\u009e\3\2\2\2\u00a0\u00a1\3\2\2\2\u00a1\u009f\3\2\2\2\u00a1\u00a2\3\2"+
		"\2\2\u00a2\37\3\2\2\2\u00a3\u00a8\5\"\22\2\u00a4\u00a5\7\30\2\2\u00a5"+
		"\u00a7\5\"\22\2\u00a6\u00a4\3\2\2\2\u00a7\u00aa\3\2\2\2\u00a8\u00a6\3"+
		"\2\2\2\u00a8\u00a9\3\2\2\2\u00a9!\3\2\2\2\u00aa\u00a8\3\2\2\2\u00ab\u00b0"+
		"\5$\23\2\u00ac\u00ad\7\27\2\2\u00ad\u00af\5$\23\2\u00ae\u00ac\3\2\2\2"+
		"\u00af\u00b2\3\2\2\2\u00b0\u00ae\3\2\2\2\u00b0\u00b1\3\2\2\2\u00b1#\3"+
		"\2\2\2\u00b2\u00b0\3\2\2\2\u00b3\u00b5\7\26\2\2\u00b4\u00b3\3\2\2\2\u00b4"+
		"\u00b5\3\2\2\2\u00b5\u00b6\3\2\2\2\u00b6\u00b7\5&\24\2\u00b7%\3\2\2\2"+
		"\u00b8\u00b9\7\31\2\2\u00b9\u00ba\5 \21\2\u00ba\u00bb\7\32\2\2\u00bb\u00be"+
		"\3\2\2\2\u00bc\u00be\5(\25\2\u00bd\u00b8\3\2\2\2\u00bd\u00bc\3\2\2\2\u00be"+
		"\'\3\2\2\2\u00bf\u00c0\7\36\2\2\u00c0\u00c1\7\6\2\2\u00c1\u00c2\t\5\2"+
		"\2\u00c2\u00c3\7\31\2\2\u00c3\u00c6\7\36\2\2\u00c4\u00c5\7\4\2\2\u00c5"+
		"\u00c7\7!\2\2\u00c6\u00c4\3\2\2\2\u00c6\u00c7\3\2\2\2\u00c7\u00c8\3\2"+
		"\2\2\u00c8\u00cb\7\32\2\2\u00c9\u00ca\7\21\2\2\u00ca\u00cc\7!\2\2\u00cb"+
		"\u00c9\3\2\2\2\u00cb\u00cc\3\2\2\2\u00cc)\3\2\2\2\26/\64=@GNS]eiuw\u008f"+
		"\u00a1\u00a8\u00b0\u00b4\u00bd\u00c6\u00cb";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}