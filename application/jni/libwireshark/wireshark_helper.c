/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <string.h>
#include <jni.h>
#include <usb.h>
#include <android/log.h>
#include "spectool_container.h" 
#include "spectool_net_client.h"
#include <errno.h>
#include <glib.h>
#define LOG_TAG "WiresharkDriver" // text for log tag 

#include <stdlib.h>
#include <stdio.h>
#include <ctype.h>
#include <locale.h>
#include <limits.h>
#include <unistd.h>
#include <glib.h>

#include "config.h"

#include <epan/packet.h>
#include <epan/epan.h>
#include <epan/dissectors/packet-radiotap.h>
#include <epan/timestamp.h>
#include <epan/crc32.h>
#include <epan/frequency-utils.h>
#include <epan/tap.h>
#include <epan/prefs.h>
#include <epan/addr_resolv.h>
#include "packet-radiotap-defs.h"
#include "log.h"

#include "cfile.h"
#include "capture_ui_utils.h"
#include "capture_ifinfo.h"
#include "capture-pcap-util.h"
#include "print.h"

capture_file cfile;
gchar               *cf_name = NULL, *rfilter = NULL;
static print_format_e print_format = PR_FMT_TEXT;
static output_fields_t* output_fields  = NULL;
static gboolean print_packet_info;      /* TRUE if we're to print packet information */
static capture_options global_capture_opts;
static gboolean do_dissection;  /* TRUE if we have to dissect each packet */

/*
 * The way the packet decode is to be written.
 */
typedef enum {
  WRITE_TEXT,   /* summary or detail text */
  WRITE_XML,    /* PDML or PSML */
  WRITE_FIELDS  /* User defined list of fields */
  /* Add CSV and the like here */
} output_action_e;

static output_action_e output_action;

extern void tshark_log_handler (const gchar *log_domain, GLogLevelFlags log_level, const gchar *message, gpointer user_data);
extern void open_failure_message(const char *filename, int err, gboolean for_writing);
extern void failure_message(const char *msg_format, va_list ap);
extern void read_failure_message(const char *filename, int err);
extern void write_failure_message(const char *filename, int err);

jstring
Java_com_gnychis_coexisyst_CoexiSyst_wiresharkGet(jstring param)
{

	return "something";
}

// Mainly a rip-off of the main() function in tshark
jint
Java_com_gnychis_coexisyst_CoexiSyst_wiresharkInit( JNIEnv* env, jobject thiz )
{
  char                *init_progfile_dir_error;
  int                  opt;
  gboolean             arg_error = FALSE;

  char                *gpf_path, *pf_path;
  char                *gdp_path, *dp_path;
  int                  gpf_open_errno, gpf_read_errno;
  int                  pf_open_errno, pf_read_errno;
  int                  gdp_open_errno, gdp_read_errno;
  int                  dp_open_errno, dp_read_errno;
  int                  err;
  int                  exit_status = 0;
  
  gboolean             capture_option_specified = FALSE;

  dfilter_t           *rfcode = NULL;
  e_prefs             *prefs_p;
  char                 badopt;
  GLogLevelFlags       log_flags;
  int                  optind_initial;
  
  init_process_policies();
  
  opterr = 0;
  optind_initial = optind;
  
	optind = optind_initial;
  opterr = 1;



/** Send All g_log messages to our own handler **/

  log_flags =
                    G_LOG_LEVEL_ERROR|
                    G_LOG_LEVEL_CRITICAL|
                    G_LOG_LEVEL_WARNING|
                    G_LOG_LEVEL_MESSAGE|
                    G_LOG_LEVEL_INFO|
                    G_LOG_LEVEL_DEBUG|
                    G_LOG_FLAG_FATAL|G_LOG_FLAG_RECURSION;

  g_log_set_handler(NULL,
                    log_flags,
                    tshark_log_handler, NULL /* user_data */);
  g_log_set_handler(LOG_DOMAIN_MAIN,
                    log_flags,
                    tshark_log_handler, NULL /* user_data */);

  initialize_funnel_ops();
  capture_opts_init(&global_capture_opts, &cfile);
  
  timestamp_set_type(TS_RELATIVE);
  timestamp_set_precision(TS_PREC_AUTO);
  timestamp_set_seconds_type(TS_SECONDS_DEFAULT);
  
  /* Register all dissectors; we must do this before checking for the
     "-G" flag, as the "-G" flag dumps information registered by the
     dissectors, and we must do it before we read the preferences, in
     case any dissectors register preferences. */
  epan_init(register_all_protocols, register_all_protocol_handoffs, NULL, NULL,
            failure_message, open_failure_message, read_failure_message,
            write_failure_message);
  
  register_all_plugin_tap_listeners();
  register_all_tap_listeners();
  prefs_register_modules();

  setlocale(LC_ALL, "");

  prefs_p = read_prefs(&gpf_open_errno, &gpf_read_errno, &gpf_path,
                     &pf_open_errno, &pf_read_errno, &pf_path);
  if (gpf_path != NULL) {
    if (gpf_open_errno != 0) {
      cmdarg_err("Can't open global preferences file \"%s\": %s.",
              pf_path, strerror(gpf_open_errno));
    }
    if (gpf_read_errno != 0) {
      cmdarg_err("I/O error reading global preferences file \"%s\": %s.",
              pf_path, strerror(gpf_read_errno));
    }
  }
  if (pf_path != NULL) {
    if (pf_open_errno != 0) {
      cmdarg_err("Can't open your preferences file \"%s\": %s.", pf_path,
              strerror(pf_open_errno));
    }
    if (pf_read_errno != 0) {
      cmdarg_err("I/O error reading your preferences file \"%s\": %s.",
              pf_path, strerror(pf_read_errno));
    }
    g_free(pf_path);
    pf_path = NULL;
  }
  
  /* Set the name resolution code's flags from the preferences. */
  gbl_resolv_flags = prefs_p->name_resolve;

  /* Read the disabled protocols file. */
  read_disabled_protos_list(&gdp_path, &gdp_open_errno, &gdp_read_errno,
                            &dp_path, &dp_open_errno, &dp_read_errno);
  if (gdp_path != NULL) {
    if (gdp_open_errno != 0) {
      cmdarg_err("Could not open global disabled protocols file\n\"%s\": %s.",
                 gdp_path, strerror(gdp_open_errno));
    }
    if (gdp_read_errno != 0) {
      cmdarg_err("I/O error reading global disabled protocols file\n\"%s\": %s.",
                 gdp_path, strerror(gdp_read_errno));
    }
    g_free(gdp_path);
  }
  if (dp_path != NULL) {
    if (dp_open_errno != 0) {
      cmdarg_err(
        "Could not open your disabled protocols file\n\"%s\": %s.", dp_path,
        strerror(dp_open_errno));
    }
    if (dp_read_errno != 0) {
      cmdarg_err(
        "I/O error reading your disabled protocols file\n\"%s\": %s.", dp_path,
        strerror(dp_read_errno));
    }
    g_free(dp_path);
  }

  cap_file_init(&cfile);

  /* Print format defaults to this. */
  print_format = PR_FMT_TEXT;

  output_fields = output_fields_new();
  
  /* If we specified output fields, but not the output field type... */
  if(WRITE_FIELDS != output_action && 0 != output_fields_num_fields(output_fields)) {
        cmdarg_err("Output fields were specified with \"-e\", "
            "but \"-Tfields\" was not specified.");
        return -1;
  } else if(WRITE_FIELDS == output_action && 0 == output_fields_num_fields(output_fields)) {
        cmdarg_err("\"-Tfields\" was specified, but no fields were "
                    "specified with \"-e\".");

        return -1;
  }
  
  prefs_apply_all();
  start_requested_stats();
  
  capture_opts_trim_snaplen(&global_capture_opts, MIN_PACKET_SIZE);
  capture_opts_trim_ring_num_files(&global_capture_opts);
  
  cfile.rfcode = rfcode;
  
  do_dissection = 1;

  timestamp_set_precision(TS_PREC_AUTO_USEC);

	return 1;
}
