grammar decodes;

decodesScript: (line)+ EOF;

line: COMMENT | formatStatement;

formatStatement: IDENTIFIER ':' operations OPERATION_SEPARATOR?;

operations: OPERATION_SEPARATOR? (redirect | operation (',' operation)* redirect?);

redirect: SWITCH_FORMAT IDENTIFIER;


operation: repeat? op;

op: identifier=SKIP_LINE_FORWARD  # position
  | identifier=SKIP_LINE_BACKWARD  # position
  | identifier=CHECK LEFT_PAREN argument ',' argument RIGHT_PAREN # check
  | identifier=IDENTIFIER LEFT_PAREN (argument (',' argument)*?)? RIGHT_PAREN # function
  | identifier=IDENTIFIER  # position
  | LEFT_PAREN (operation (OPERATION_SEPARATOR operation)*) RIGHT_PAREN # group
;

repeat: NUMBER;


// operation: repeat=repeatVal? identifier=SKIP_LINE_FORWARD  # position
// 		 | repeat=repeatVal? identifier=SKIP_LINE_BACKWARD  # position
// 		 | repeat=repeatVal? identifier=CHECK LEFT_PAREN argument ',' argument RIGHT_PAREN # check
// 		 | repeat=repeatVal? identifier=IDENTIFIER LEFT_PAREN (argument (',' argument)*?)? RIGHT_PAREN # function
// 		 | repeat=repeatVal? identifier=IDENTIFIER  # position
// 		 | repeat=repeatVal? LEFT_PAREN (operation (OPERATION_SEPARATOR operation)*) RIGHT_PAREN # group
// 		 ;

// repeatVal: NUMBER;

argument:  (IDENTIFIER | NUMBER | STRING | NUMBER_FORMAT);

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
OPERATION_SEPARATOR: ',';
//NEWLINE: NEWLINE_CHAR;
COMMENT: COMMENT_START .*? COMMENT_END;
NUMBER: DIGIT DIGIT?;
STRING: SINGLE_QUOTE .*? SINGLE_QUOTE;
NUMBER_FORMAT: NUMBER (ALPHA STRING)?;
SKIP_WS: WS -> skip;
fragment COMMENT_END: (NEWLINE_CHAR | EOF);
fragment COMMENT_START: '#';
fragment WS: [ \t\n]+ | OTHER_NEWLINE_CHARS+ | NEWLINE_CHAR+;
fragment LABEL_CHARS: ALPHA | DIGIT | OTHER_PRINTABLE;
fragment OTHER_PRINTABLE: '.' | '-' | '_';
fragment SINGLE_QUOTE: '\'';
fragment ALPHA: UPPER_LETTER | LOWER_LETTER;
fragment UPPER_LETTER: [A-Z];
fragment LOWER_LETTER: [a-z];
// the \n we care about for format statement separation, other new line characters don't matter.
fragment NEWLINE_CHAR: '\n';
fragment OTHER_NEWLINE_CHARS: [\r\f]+;

fragment DIGIT: [0-9];