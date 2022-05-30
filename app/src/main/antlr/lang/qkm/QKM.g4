grammar QKM;

@header {
    package lang.qkm;
}

LINE_COMMENT    : '//' ~[\r\n]* -> skip;
BLOCK_COMMENT   : '/*' (BLOCK_COMMENT | .)*? '*/' -> skip;
WHITESPACE      : [ \t\r\n] -> skip;

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
LET     : 'let';
IN      : 'in';
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

INT : (
    '0b' ('_'? DIGIT_2)+
    | '0c' ('_'? DIGIT_8)+
    | '0x' ('_'? DIGIT_16)+
    | '0' | [1-9] ('_'? DIGIT_10)*) ('i' DIGIT_10+)?;

CHAR    : '\'' (~[\r\n'\\] | ESCAPE) '\'';
TEXT    : '"' (~[\r\n"\\] | ESCAPE)* '"';

fragment NAME
    : [_\p{Lu}\p{Ll}\p{Lt}\p{Lo}\p{Lm}][_\p{Lu}\p{Ll}\p{Lt}\p{Lo}\p{Lm}\p{Nd}]*
    ;

IDENT   : NAME;
CTOR    : '#' NAME;

lines
    : line+
    ;

line
    : expr
    ;

expr
    : f=expr0 args+=expr0*                              # ExprApply
    | 'let' (b+=binding ',')* b+=binding 'in' e=expr    # ExprLetrec
    | 'fun' '|'? k+=matchCase ('|' k+=matchCase)*       # ExprLambda
    | 'match' v=expr 'with'
        '|'? k+=matchCase ('|' k+=matchCase)*           # ExprMatch
    ;

binding
    : n=IDENT '=' e=expr
    ;

matchCase
    : p=pattern '->' e=expr
    ;

expr0
    : n=IDENT                               # ExprIdent
    | k=CTOR                                # ExprCtor
    | TRUE                                  # ExprTrue
    | FALSE                                 # ExprFalse
    | INT                                   # ExprInt
    | CHAR                                  # ExprChar
    | TEXT                                  # ExprText
    | '(' ((es+=expr ',')* es+=expr)? ')'   # ExprGroup
    ;

pattern
    : pattern0
    ;

pattern0
    : '_'                                       # PatIgnore
    | n=IDENT                                   # PatBind
    | TRUE                                      # PatTrue
    | FALSE                                     # PatFalse
    | INT                                       # PatInt
    | CHAR                                      # PatChar
    | TEXT                                      # PatText
    | '(' ((ps+=pattern ',')* ps+=pattern)? ')' # PatGroup
    ;

/*
line
    : defEnum
    | defRecBind
    | expr
    ;

defEnum
    : 'type' n=IDENT p=poly? '=' '|'? r+=enumCase ('|' r+=enumCase)*
    ;

poly
    : '[' ((qs+=IDENT ',')* qs+=IDENT)? ']'
    ;

enumCase
    : k=CTOR arg=type0?
    ;

type
    : p=type0 ('->' q=type)?     # TypeFunc
    ;

type0
    : n=IDENT ('[' ((ts+=type ',')* ts+=type)? ']')?    # TypeName
    | '(' ((ts+=type ',')* ts+=type)? ')'               # TypeGroup
    ;

defRecBind
    : 'let' n=IDENT '=' e=expr
    ;
*/
