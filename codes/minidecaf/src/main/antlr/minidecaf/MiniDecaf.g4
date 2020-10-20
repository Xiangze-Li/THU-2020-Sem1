grammar MiniDecaf;

program
    : (function | declGlobal)* EOF
    ;

declGlobal
    : type IDENT ('=' INTEGER)? ';'
    ;

function
    : type IDENT '(' (type IDENT (',' type IDENT)*)? ')' ';'                # funcDecl
	| type IDENT '(' (type IDENT (',' type IDENT)*)? ')' '{' blockItem* '}' # funcDef
    ;

type
    : 'int'
    ;

compoundStatement
    : '{' blockItem* '}'
    ;

blockItem
    : statement         # blockStmt
    | declearation      # blockDecl
    ;

statement
    : 'return' expression ';'                               # stmtRet
    | expression? ';'                                       # stmtExpr
    | 'if' '(' expression ')' statement ('else' statement)? # stmtIf
    | compoundStatement                                     # stmtCompound
	| 'for' '(' (declearation | expression? ';') expression? ';' expression? ')' statement   # stmtFor
	| 'while' '(' expression ')' statement                                  # stmtWhile
	| 'do' statement 'while' '(' expression ')' ';'                         # stmtDo
	| 'break' ';'                                                           # stmtBreak
	| 'continue' ';'                                                        # stmtConti
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
    : postfix               # unaryPostfix
    | ('-'|'~'|'!') unary   # unaryOp
    ;

postfix
    : primary
	| IDENT '(' (expression (',' expression)* )? ')'
    ;

primary
    : INTEGER               # primIntLit
    | '(' expression ')'    # primParen
    | IDENT                 # primIdent
    ;

/* lexer */
WS: [ \t\r\n\u000C]+ -> skip;

// comment The specification of minidecaf doesn't allow commenting, but we provide the comment
// feature here for the convenience of debugging.
LONG_COMMENT: '/*' .*? '*/' -> skip;
LINE_COMMENT: '//' ~[\r\n]* -> skip;

IDENT: [a-zA-Z_][a-zA-Z_0-9]*;
INTEGER: [1-9][0-9]* | '0';
