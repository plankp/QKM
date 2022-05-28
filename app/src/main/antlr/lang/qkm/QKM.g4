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
LET     : 'let';
LB      : '{';
RB      : '}';
LS      : '[';
RS      : ']';
LP      : '(';
RP      : ')';
COMMA   : ',';
ADD     : '+';

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

expr
    : f=expr0 args+=expr0*                      # ExprApply
    | l=expr '+' r=expr                         # ExprAdd
    | 'fun' f=matchCase                         # ExprLambda
    | 'match' i=expr 'with'
        '|'? r+=matchCase ('|' r+=matchCase)*   # ExprMatch
    ;

matchCase
    : p=pattern '->' e=expr
    ;

expr0
    : n=IDENT                                   # ExprIdent
    | k=CTOR                                    # ExprCtor
    | TRUE                                      # ExprTrue
    | FALSE                                     # ExprFalse
    | INT                                       # ExprInt
    | CHAR                                      # ExprChar
    | TEXT                                      # ExprText
    | '(' ((es+=expr ',')* es+=expr)? ')'       # ExprGroup
    ;

pattern
    : '_'                                       # PatIgnore
    | TRUE                                      # PatTrue
    | FALSE                                     # PatFalse
    | INT                                       # PatInt
    | CHAR                                      # PatChar
    | TEXT                                      # PatText
    | id=CTOR arg=pattern?                      # PatDecons
    | n=IDENT                                   # PatBind
    | '(' ((ps+=pattern ',')* ps+=pattern)? ')' # PatGroup
    ;
