grammar QKM;

@header {
    package lang.qkm;
}

LINE_COMMENT    : '//' ~[\r\n]* -> skip;
BLOCK_COMMENT   : '/*' (BLOCK_COMMENT | .)*? '*/' -> skip;
WHITESPACE      : [ \t\r\n] -> skip;

VAL     : 'val';
COLON   : ':';
TRUE    : 'true';
FALSE   : 'false';
UBAR    : '_';
BAR     : '|';
SET     : '=';
ARROW   : '->';
TYPE    : 'type';
MATCH   : 'match';
WITH    : 'with';
FUN     : 'fun';
LB      : '{';
RB      : '}';
LS      : '[';
RS      : ']';
LP      : '(';
RP      : ')';
COMMA   : ',';

fragment DIGIT_2    : [01];
fragment DIGIT_8    : [0-7];
fragment DIGIT_10   : [0-9];
fragment DIGIT_16   : [0-9a-fA-F];
fragment ESCAPE
    : '\\' [abfnrtv'"\\]
    | '\\u' DIGIT_16 DIGIT_16 DIGIT_16 DIGIT_16
    | '\\U' DIGIT_16 DIGIT_16 DIGIT_16 DIGIT_16 DIGIT_16 DIGIT_16
    | '\\x' DIGIT_16 DIGIT_16
    | '\\' DIGIT_8 DIGIT_8 DIGIT_8
    ;

CHAR    : '\'' (~[\r\n'\\] | ESCAPE) '\'';
TEXT    : '"' (~[\r\n"\\] | ESCAPE)* '"';
IDENT   : [_\p{Lu}\p{Ll}\p{Lt}\p{Lo}\p{Lm}][_\p{Lu}\p{Ll}\p{Lt}\p{Lo}\p{Lm}\p{Nd}]*;

lines
    : line+
    ;

line
    : defEnum
    | expr
    ;

defEnum
    : 'type' n=IDENT p=poly? '=' '|'? r+=enumCase ('|' r+=enumCase)*
    ;

poly
    : '[' ((qs+=IDENT ',')* qs+=IDENT)? ']'
    ;

enumCase
    : k=IDENT arg=atomType
    ;

type
    : p=atomType ('->' q=type)?     # TypeFunc
    ;

atomType
    : n=IDENT ('[' ((ts+=type ',')* ts+=type)? ']')?    # TypeName
    | '(' ((ts+=type ',')* ts+=type)? ')'               # TypeGroup
    ;

expr
    : f=expr arg=expr                           # ExprApply
    | n=IDENT                                   # ExprIdent
    | TRUE                                      # ExprTrue
    | FALSE                                     # ExprFalse
    | CHAR                                      # ExprChar
    | TEXT                                      # ExprText
    | 'fun' f=matchCase                         # ExprLambda
    | '(' ((es+=expr ',')* es+=expr)? ')'       # ExprGroup
    | 'match' i=expr 'with'
        '|'? r+=matchCase ('|' r+=matchCase)*   # ExprMatch
    ;

matchCase
    : p=pattern '->' e=expr
    ;

pattern
    : '_'                                       # PatIgnore
    | TRUE                                      # PatTrue
    | FALSE                                     # PatFalse
    | CHAR                                      # PatChar
    | TEXT                                      # PatText
    | id=IDENT arg=pattern                      # PatDecons
    | n=IDENT                                   # PatBind
    | '(' ((ps+=pattern ',')* ps+=pattern)? ')' # PatGroup
    ;
