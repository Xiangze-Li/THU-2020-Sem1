grammar MiniDecaf;

program
	: function EOF
	;

function
	: type IDENT '(' ')' '{' statement '}'
	;

type
	: 'int'
	;

statement
	: 'return' expression ';'	# returnStatement
	;

expression
	: expr_or
	;

expr_or
	: expr_and
	| expr_or '||' expr_and
	;

expr_and
	: expr_equal
	| expr_and '&&' expr_equal
	;

expr_equal
	: expr_relation
	| expr_equal ('=='|'!=') expr_relation
	;

expr_relation
	: expr_add
	| expr_relation ('<'|'>'|'<='|'>=') expr_add
	;

expr_add
	: expr_multiply
	| expr_add ('+'|'-') expr_multiply
	;

expr_multiply
	: unary
	| expr_multiply ('*'|'/'|'%') unary
	;

unary
	: primary				# unaryPrimary
	| ('-'|'~'|'!') unary	# unaryOp
	;

primary
	: INTEGER				# primIntLit
	| '(' expression ')'	# primParen
	;

/* lexer */
WS: [ \t\r\n\u000C]+ -> skip;

// comment The specification of minidecaf doesn't allow commenting, but we provide the comment
// feature here for the convenience of debugging.
LONG_COMMENT: '/*' .*? '*/' -> skip;
LINE_COMMENT: '//' ~[\r\n]* -> skip;

IDENT: [a-zA-Z_][a-zA-Z_0-9]*;
INTEGER: [1-9][0-9]* | '0';
