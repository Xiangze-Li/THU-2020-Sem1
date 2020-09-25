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
	: 'return' expr ';'	# returnStatement
	;

expr
	: INTEGER
	;

/* lexer */
WS: [ \t\r\n\u000C]+ -> skip;

// comment The specification of minidecaf doesn't allow commenting, but we provide the comment
// feature here for the convenience of debugging.
COMMENT: '/*' .*? '*/' -> skip;
LINE_COMMENT: '//' ~[\r\n]* -> skip;

IDENT: [a-zA-Z_] [a-zA-Z_0-9]*;
INTEGER: [0-9]+;
