grammar decodes;

options {tokenVocab=decodesLexer;}

decodesScript: formatStatement* EOF;

formatStatement: formatLabel COLON decodesOperations redirect?;

formatLabel: FORMAT_LABEL;

redirect: SWITCH_FORMAT FORMAT_LABEL;

decodesOperations: decodesOperation (COMMA decodesOperation)*;

decodesOperation: repeat (NEXTLINE | function | IDENTIFIER);

function: IDENTIFIER '(' argument* ')';

argument: COMMA  (ALPHA|NUMBER|STRING|NUMBER_FORMAT);

repeat: NUMBER;


