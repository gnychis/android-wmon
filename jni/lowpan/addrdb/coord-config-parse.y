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
	#include <stdio.h>
	#include <unistd.h>
	#include <stdlib.h>
	#include <string.h>
	#include <stdint.h>
	#include <sys/types.h>
	#include <sys/stat.h>
	#include <fcntl.h>
	#include <errno.h>
	#include <libcommon.h>
	#include <time.h>
	#include <addrdb.h>
	#include "coord-config-parse.h"
	#include "parser.h"

	#define YYDEBUG 1

	static uint16_t short_addr;
	static uint8_t hwaddr[8];
	static time_t mystamp;
	static void yyerror(YYLTYPE * yylloc, yyscan_t yyscanner, char *s)
	{
		fprintf(stderr, "Error: %s at line %d\n", s, yylloc->first_line);
	}
	static void init_data(void)
	{
		memset(hwaddr, 0, 8);
		short_addr = 0;
		mystamp = 0;
	}
	static void dump_data(void)
	{
		int i;
		printf("HW addr: ");
		for(i = 0; i < 8; i++)
			printf("%02x", hwaddr[i]);
		printf(" short addr %#x", short_addr);
		printf("\n");
	}
#if 0
	static void dump_addr(unsigned char *s)
	{
		int i;
		printf("HW addr: ");
		for(i = 0; i < 8; i++)
			printf("%02x", s[i]);
	}
#endif
	static void set_hwaddr_octets(unsigned char *s, unsigned char a1,
				unsigned char a2,  unsigned char a3,  unsigned char a4,
				unsigned char a5,  unsigned char a6,  unsigned char a7,
				 unsigned char a8)
	{
		s[0] = a1;
		s[1] = a2;
		s[2] = a3;
		s[3] = a4;
		s[4] = a5;
		s[5] = a6;
		s[6] = a7;
		s[7] = a8;
	}
	static void do_set_hw_addr(unsigned char *s)
	{
		memcpy(hwaddr, s, 8);
	}
	static void do_set_short_addr(unsigned short addr)
	{
		short_addr = addr;
	}
	static void do_set_timestamp(time_t value)
	{
		mystamp = value;
	}
	static void do_commit_data()
	{
		addrdb_insert(hwaddr, short_addr, mystamp);
	}

%}

%union {
	unsigned long number;
	time_t timestamp;
	unsigned char hw_addr[8];
}

%error-verbose
%pure-parser
%locations
%lex-param	{ yyscan_t yyscanner }
%parse-param	{ yyscan_t yyscanner }

%type <hw_addr> hardaddr
%token <number> TOK_NUMBER
%type <hw_addr> cmd_hwaddr
%type <number> num_col cmd_shortaddr
%type <timestamp> cmd_timestamp
%type <number> num

%token TOK_LEASE
%token TOK_HWADDR
%token TOK_SHORTADDR
%token TOK_TIMESTAMP
%left '{' '}' ':' ';'

%%
input:  /* empty */
	| block input
	;

block:  lease_begin operators lease_end	{do_commit_data();}
	;

lease_begin: TOK_LEASE '{' {init_data();}
	;
lease_end: '}' ';' {dump_data();}
	;

operators: cmd ';'
	| cmd ';' operators
	;

cmd:      cmd_hwaddr				{do_set_hw_addr($1);}
	| cmd_shortaddr				{do_set_short_addr($1);}
	| cmd_timestamp				{do_set_timestamp($1);}
	;
cmd_hwaddr: TOK_HWADDR hardaddr			{memcpy($$, $2, 8);}
	;
cmd_shortaddr:  TOK_SHORTADDR TOK_NUMBER	{$$ = $2;}
	;
cmd_timestamp:  TOK_TIMESTAMP TOK_NUMBER	{$$ = (time_t) $2;}
	;

hardaddr: num_col num_col num_col num_col num_col num_col num_col num
				{set_hwaddr_octets($$, $1, $2, $3, $4, $5, $6, $7, $8);}
	;

num_col: num ':' {$$ = $1;}
	;
num: TOK_NUMBER
	;


%%

int addrdb_parse(const char *fname)
{
	yyscan_t scanner;
	int rc;
	FILE *fin = fopen(fname, "r");
	if (!fin) {
		if(errno == ENOENT) {
			int fd;
			/* Lease file does not exist. Tis is perfectly ok.
			   But if we will not be able to write this file,
			   we will not be able to dump leases, which
			   might have catastrophic consequiences. */
			fd = open(fname, O_CREAT | O_APPEND, S_IWUSR | S_IRUSR);
			if (fd < 0) {
				fprintf(stderr,
					"ERROR: We were unable to open lease file %s\n",
					fname);
				fprintf(stderr,
					"ERROR: Check that directory it locates in does exist\n");
				perror("fopen");
				exit(1);
			}
			close(fd);
			return -1;
		}
	}

	rc = addrdb_parser_init(&scanner, fname);
	if (rc) {
		perror("addrdb_parser_init");
		return 1;
	}

	yyparse(scanner);

	addrdb_parser_destroy(scanner);
	scanner = NULL;

	return 0;
}

