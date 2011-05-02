%{
/*
 * Linux IEEE 802.15.4 userspace tools
 *
 * Copyright (C) 2008, 2009 Siemens AG
 *
 * Written-by: Dmitry Eremin-Solenikov
 * Written-by: Sergey Lapin
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; version 2 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
#include "coord-config-parse.h"
#include "parser.h"
#define YY_NO_INPUT

#define YY_USER_ACTION {				\
		/* Step */				\
		yylloc->first_line = yylloc->last_line;	\
		yylloc->first_column = yylloc->last_column; \
		if (yytext[0] == '\n') {		\
			yylloc->last_line ++;		\
			yylloc->last_column = 0;	\
		} else {				\
			yylloc->last_column += yyleng;	\
		}					\
	}

%}

%option nounput noyywrap
%option reentrant
%option bison-bridge bison-locations
%option warn
%option nodefault

%%
[\{\}:;]			return yytext[0];
lease				return TOK_LEASE;
hwaddr				return TOK_HWADDR;
shortaddr			return TOK_SHORTADDR;
timestamp			return TOK_TIMESTAMP;
0x[0-9a-fA-F]+			yylval->number = strtol(yytext, NULL, 16); return TOK_NUMBER;
[0-9A-Fa-f][0-9a-fA-F]		yylval->number = strtol(yytext, NULL, 16); return TOK_NUMBER;
[ \t\n\r]+			;
.				return yytext[0];
%%

int addrdb_parser_init(yyscan_t *scanner, const char *fname)
{
	int rc;
	FILE *fin = fopen(fname, "r");
	if (!fin)
		return -1;

	rc = yylex_init(scanner);
	if (rc) {
		fclose(fin);
		return rc;
	}

	yyset_in(fin, *scanner);

	return 0;
}

void addrdb_parser_destroy(yyscan_t scanner)
{
	FILE *f = yyget_in(scanner);
	fclose(f);
	yylex_destroy(scanner);
}

