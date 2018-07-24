grammar TMQL;

// Lexer rules
SELECT : 'SELECT' | 'select';

FROM : 'FROM' | 'from' ;

FILTER : 'FILTER' | 'filter' ;

JOIN : 'OUTER JOIN' | 'outer join' | 'JOIN' | 'join' ;

BOOLEAN_NOT : 'NOT' | 'not' ;
BOOLEAN_AND : 'AND' | 'and' ;
BOOLEAN_OR : 'OR' | 'or' ;

OPEN_PAREN : '(' ;

CLOSE_PAREN : ')' ;

UNARY_COMPARISON_OPERATOR : 'IS NULL' | 'is null' | 'IS NOT NULL' | 'is not null' ;
BINARY_COMPARISON_OPERATOR : '==' | '=' | '>' | '<' | '!=' ;
PROPERTY_FUNCTION : 'length' | 'upper' | 'lower' | 'in' | 'ceil' | 'floor' | 'abs' | 'round' | 'contains' ;

IDENTIFIER : [A-Za-z]+ ;

NUMBER : [0-9]+ ;

ANY_VAL : [0-9A-Za-z]+ ;

QUOTED_VAL : ('\'' [0-9A-Za-z./]+ '\'') | ('"' [0-9A-Za-z./]+ '"') ;

WS: [ \n\t\r]+ -> skip ;

// Parser rules

selectStatement : SELECT identifierList FROM aliasedNodeTypeIdentifierSequence filterStatement? joinStatement* ;

identifierList: '*' #wildcardID
	| ids+=IDENTIFIER (',' ids+=IDENTIFIER)* #namedIDs ;

nodeTypeIdentifier: namespace=IDENTIFIER ':' name=IDENTIFIER (':' version=NUMBER)? ;

aliasedNodeTypeIdentifierSequence : aliasedNodeTypeIdentifier (',' aliasedNodeTypeIdentifier)* ;

aliasedNodeTypeIdentifier : nodetype=nodeTypeIdentifier identifier=IDENTIFIER? ;

filterStatement : FILTER filterOrPredicate ;

filterOrPredicate:
	filterAndPredicate (BOOLEAN_OR filterAndPredicate)* ;

filterAndPredicate:
	filterNotPredicate (BOOLEAN_AND filterNotPredicate)* ;

filterNotPredicate:
	notFlag=BOOLEAN_NOT? filterBasePredicate;

filterBasePredicate:
	OPEN_PAREN filterOrPredicate CLOSE_PAREN #groupFilterPredicate
	| (filterPropertyUnaryClause | filterPropertyBinaryClause | filterPropertyUnaryFunctionClause | filterPropertyBinaryFunctionClause) #simpleFilterPredicate ;

filterPropertyUnaryClause:
	alias=IDENTIFIER '.' property=(IDENTIFIER|PROPERTY_FUNCTION) unaryOperator=UNARY_COMPARISON_OPERATOR;

filterPropertyBinaryClause:
	alias=IDENTIFIER '.' property=(IDENTIFIER|PROPERTY_FUNCTION) binaryOperator=BINARY_COMPARISON_OPERATOR value=(NUMBER|IDENTIFIER|QUOTED_VAL|ANY_VAL) ;

filterPropertyUnaryFunctionClause:
	alias=IDENTIFIER '.' property=(IDENTIFIER|PROPERTY_FUNCTION) '.' propertyFunction=PROPERTY_FUNCTION '(' propertyArgs+=(NUMBER|QUOTED_VAL) (',' propertyArgs+=(NUMBER|QUOTED_VAL))* ')';

filterPropertyBinaryFunctionClause:
	alias=IDENTIFIER '.' property=(IDENTIFIER|PROPERTY_FUNCTION) '.' propertyFunction=PROPERTY_FUNCTION '()' binaryOperator=BINARY_COMPARISON_OPERATOR value=(NUMBER|IDENTIFIER|QUOTED_VAL|ANY_VAL) ;

joinStatement : joinType=JOIN joinOrPredicate+ ;

joinOrPredicate:
	joinAndPredicate (BOOLEAN_OR joinAndPredicate)* ;

joinAndPredicate:
	joinNotPredicate (BOOLEAN_AND joinNotPredicate)* ;

joinNotPredicate:
	notFlag=BOOLEAN_NOT? joinBasePredicate;

joinBasePredicate:
	OPEN_PAREN joinOrPredicate CLOSE_PAREN #groupJoinPredicate
	| joinCriteria #simpleJoinPredicate ;

joinCriteria: identifier=IDENTIFIER '.' functionName=('parentOf' | 'ancestorOf' | 'childOf' | 'descendantOf' | 'siblingOf' | 'strongRefTo' | 'weakRefTo' | 'strongRefFrom' | 'weakRefFrom') OPEN_PAREN functionIdentifier=IDENTIFIER (',' functionParam=QUOTED_VAL)? CLOSE_PAREN ('at' atParam=QUOTED_VAL)?;



