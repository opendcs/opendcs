grammar decodes;

//options {tokenVocab=decodesLexer;}

decodesScript: (formatStatement NEWLINE)+ EOF;

formatStatement: IDENTIFIER ':' operations ','?;

operations: ','? (redirect | operation (',' operation)* redirect?);

redirect: SWITCH_FORMAT IDENTIFIER;

operation: NUMBER? (SKIP_LINE_FORWARD | SKIP_LINE_BACKWARD | SKIP_CHARS 
                   | SKIP_WHITESPACE | POSITION | CHECK | SCAN | function 
                   | group);

group: LEFT_PAREN (operation (',' operation)*)+ RIGHT_PAREN;

function: IDENTIFIER (LEFT_PAREN (argument (',' argument)*?)? RIGHT_PAREN)?;

argument:  (IDENTIFIER | NUMBER |STRING | NUMBER_FORMAT);

IDENTIFIER: (UPPER_LETTER | LOWER_LETTER) (
		UPPER_LETTER
		| LOWER_LETTER
		| OTHER_PRINTABLE
		| DIGIT
	)*;
SWITCH_FORMAT: '>';
SKIP_LINE_FORWARD: '/';
SKIP_LINE_BACKWARD: '\\';
SKIP_WHITESPACE: [wW];
SKIP_CHARS: [xX];
POSITION: [Pp];
CHECK: [Cc];
SCAN: [Ss];
FIELD: [fF];
LEFT_PAREN: '(';
RIGHT_PAREN: ')';
NEWLINE: NEWLINE_CHARS;
COMMENT: COMMENT_START .*? COMMENT_END;
NUMBER: DIGIT DIGIT?;
STRING: SINGLE_QUOTE .*? SINGLE_QUOTE;
NUMBER_FORMAT: NUMBER (ALPHA STRING)?;
SKIP_WS: WS -> skip;
fragment COMMENT_END: NEWLINE_CHARS;
fragment COMMENT_START: '#';
fragment WS: [ \tNEW]+;
fragment LABEL_CHARS: ALPHA | DIGIT | OTHER_PRINTABLE;
fragment OTHER_PRINTABLE: '.' | '-' | '_';
fragment SINGLE_QUOTE: '\'';
fragment UPPER_LETTER: [A-Z];
fragment LOWER_LETTER: [a-z];
fragment NEWLINE_CHARS: [\r\n\f]+;
fragment ALPHA: [\p{Alpha}\p{General_Category=Other_letter}];
fragment DIGIT: [0-9];