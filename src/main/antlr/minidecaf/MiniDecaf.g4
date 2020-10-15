grammar MiniDecaf;

program
    : function EOF
    ;

function
    : type IDENT '(' ')' '{' blockItem* '}'
    ;

type
    : 'int'
    ;

blockItem
    : statement         # blockStmt
    | declearation      # blockDecl
    ;

statement
    : 'return' expression ';'	# stmtRet
    | expression? ';' 			# stmtExpr
    // | declearation 				# stmtDecl
    // declearation is no longer a statement after step-6
    | 'if' '(' expression ')' statement ('else' statement)? # stmtIf
    ;

declearation
    : type IDENT ('=' expression)? ';'
    ;

expression
    : exprAssign
    ;

exprAssign
    : exprTernary
    | IDENT '=' expression
    ;

exprTernary
    : exprOr
    | exprOr '?' expression ':' exprTernary
    ;

exprOr
    : exprAnd
    | exprOr '||' exprAnd
    ;

exprAnd
    : exprEqual
    | exprAnd '&&' exprEqual
    ;

exprEqual
    : exprRelation
    | exprEqual ('=='|'!=') exprRelation
    ;

exprRelation
    : exprAdd
    | exprRelation ('<'|'>'|'<='|'>=') exprAdd
    ;

exprAdd
    : exprMultiply
    | exprAdd ('+'|'-') exprMultiply
    ;

exprMultiply
    : unary
    | exprMultiply ('*'|'/'|'%') unary
    ;

unary
    : primary				# unaryPrimary
    | ('-'|'~'|'!') unary	# unaryOp
    ;

primary
    : INTEGER				# primIntLit
    | '(' expression ')'	# primParen
    | IDENT					# primIdent
    ;

/* lexer */
WS: [ \t\r\n\u000C]+ -> skip;

// comment The specification of minidecaf doesn't allow commenting, but we provide the comment
// feature here for the convenience of debugging.
LONG_COMMENT: '/*' .*? '*/' -> skip;
LINE_COMMENT: '//' ~[\r\n]* -> skip;

IDENT: [a-zA-Z_][a-zA-Z_0-9]*;
INTEGER: [1-9][0-9]* | '0';
