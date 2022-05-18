grammar QKM;

@header {
    package lang.qkm;
}

LINE_COMMENT    : '//' ~[\r\n]* -> skip;
BLOCK_COMMENT   : '/*' (BLOCK_COMMENT | .)*? '*/' -> skip;
WHITESPACE      : [ \t\r\n] -> skip;

VAL     : 'val';
COLON   : ':';
UBAR    : '_';
ARROW   : '=>';
ENUM    : 'enum';
MATCH   : 'match';
WITH    : 'with';
LB      : '{';
RB      : '}';
LP      : '(';
RP      : ')';
COMMA   : ',';

IDENT   : [_\p{Lu}\p{Ll}\p{Lt}\p{Lo}\p{Lm}][_\p{Lu}\p{Ll}\p{Lt}\p{Lo}\p{Lm}\p{Nd}]*;

lines
    : line+
    ;

line
    : defEnum
    | expr
    ;

defEnum
    : 'enum' n=IDENT r+=enumCase
    | 'enum' n=IDENT '{' ((r+=enumCase ',')* r+=enumCase)? '}'
    ;

enumCase
    : k=IDENT arg=type
    ;

type
    : n=IDENT                               # TypeName
    | '(' ((ts+=type ',')* ts+=type)? ')'   # TypeGroup
    ;

expr
    : f=expr arg=expr                       # ExprApply
    | n=IDENT                               # ExprIdent
    | '(' ((es+=expr ',')* es+=expr)? ')'   # ExprGroup
    | 'match' i=expr 'with' (
        (r+=matchCase)
        | '{' ((r+=matchCase ',')* r+=matchCase)? '}'
    )                                       # ExprMatch
    ;

matchCase
    : p=pattern '=>' e=expr
    ;

pattern
    : '_'                                       # PatIgnore
    | id=IDENT arg=pattern                      # PatDecons
    | VAL n=IDENT                               # PatBind
    | '(' ((ps+=pattern ',')* ps+=pattern)? ')' # PatGroup
    ;
