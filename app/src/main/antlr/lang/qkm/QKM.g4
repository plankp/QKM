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
DATA    : 'data';
AND     : 'and';
MATCH   : 'match';
WITH    : 'with';
FMATCH  : 'function';
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
ADD     : '+';
SUB     : '-';
DOT     : '.';

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
STYPE   : '\'' NAME;

lines
    : line+
    ;

line
    : defType
    | defData
    | defBind
    | topExpr
    ;

type
    : f=type0 args+=type0*              # TypeApply
    |<assoc=right> p=type '->' q=type   # TypeFunc
    ;

type0
    : '_'                                   # TypeIgnore
    | n=STYPE                               # TypeName
    | n=IDENT                               # TypeCtor
    | '(' ((ts+=type ',')* ts+=type)? ')'   # TypeGroup
    ;

typePoly
    : qs+=STYPE+ '.' t=type
    | t=type
    ;

defType
    : 'type' n=IDENT qs+=STYPE* '=' t=type
    ;

defData
    : 'data' d+=enumDef ('and' d+=enumDef)*
    ;

enumDef
    : n=IDENT qs+=STYPE* '='
        '|'? k+=enumCase ('|' k+=enumCase)*
    ;

enumCase
    : k=CTOR args+=type0*
    ;

defBind
    : 'let' b+=binding ('and' b+=binding)*
    ;

topExpr
    : e=expr
    ;

expr
    : f=expr0 args+=expr0*                              # ExprApply
    | 'let' b+=binding ('and' b+=binding)* 'in' e=expr  # ExprLetrec
    | 'fun' ps+=pattern0+ '->' e=expr                   # ExprFun
    | 'function' '|'? k+=matchCase ('|' k+=matchCase)*  # ExprFunction
    | 'match' v=expr 'with'
        '|'? k+=matchCase ('|' k+=matchCase)*           # ExprMatch
    ;

binding
    : n=IDENT (':' t=typePoly)? '=' e=expr
    ;

matchCase
    : p=pattern '->' e=expr
    ;

expr0
    : n=IDENT                               # ExprIdent
    | k=CTOR                                # ExprCtor
    | TRUE                                  # ExprTrue
    | FALSE                                 # ExprFalse
    | ('+' | '-')? INT                      # ExprInt
    | CHAR                                  # ExprChar
    | TEXT                                  # ExprText
    | '(' ((es+=expr ',')* es+=expr)? ')'   # ExprGroup
    ;

pattern
    : k=CTOR args+=pattern0+                # PatDecons
    |<assoc=right> l=pattern '|' r=pattern  # PatOr
    | pattern0                              # PatPattern0
    ;

pattern0
    : '_'                                       # PatIgnore
    | n=IDENT                                   # PatBind
    | k=CTOR                                    # PatCtor
    | TRUE                                      # PatTrue
    | FALSE                                     # PatFalse
    | ('+' | '-')? INT                          # PatInt
    | CHAR                                      # PatChar
    | TEXT                                      # PatText
    | '(' ((ps+=pattern ',')* ps+=pattern)? ')' # PatGroup
    | '(' p=pattern ':' t=type ')'              # PatTyped
    ;
