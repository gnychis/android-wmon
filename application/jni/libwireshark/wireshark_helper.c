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

#include <stdlib.h>
#include <stdio.h>
#include <ctype.h>
#include <locale.h>
#include <limits.h>
#include <unistd.h>
#include <glib.h>

#include "config.h"

#include <epan/epan_dissect.h>
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
static gboolean print_packet_info;      /* TRUE if we're to print packet information */
static capture_options global_capture_opts;
static gboolean do_dissection;  /* TRUE if we have to dissect each packet */

static guint32 cum_bytes;
static nstime_t first_ts;
static nstime_t prev_dis_ts;
static nstime_t prev_cap_ts;

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

extern void proto_tree_get_node_field_values(proto_node *node, gpointer data);
extern void tshark_log_handler (const gchar *log_domain, GLogLevelFlags log_level, const gchar *message, gpointer user_data);
extern void open_failure_message(const char *filename, int err, gboolean for_writing);
extern void failure_message(const char *msg_format, va_list ap);
extern void read_failure_message(const char *filename, int err);
extern void write_failure_message(const char *filename, int err);

#define LOG_TAG "WiresharkDriver"
#define VERBOSE

struct _output_fields {
    gboolean print_header;
    gchar separator;
    gchar occurrence;
    gchar aggregator;
    GPtrArray* fields;
    GHashTable* field_indicies;
    emem_strbuf_t** field_values;
    gchar quote;
};

typedef struct {
    output_fields_t* fields;
	epan_dissect_t		*edt;
} write_field_data_t;

void
Java_com_gnychis_coexisyst_CoexiSyst_dissectCleanup(JNIEnv* env, jobject thiz, jint ptr)
{
	write_field_data_t *dissection = (write_field_data_t *) ptr;
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "casted the pointer");
	epan_dissect_cleanup(dissection->edt);
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "cleaned up dissection");
	free(dissection->edt);
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "freed edt");
	free(dissection);
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "freed dissection");
}

jint
Java_com_gnychis_coexisyst_CoexiSyst_dissectPacket(JNIEnv* env, jobject thiz, jbyteArray header, jbyteArray data, jint encap)
{

	// From the Java environment
	struct wtap_pkthdr whdr;
	int i;
	gsize z;
	char *pHeader;
	char *pData;

	// Wireshark related
	frame_data fdata;
	gboolean create_proto_tree = 1;
	gint64 offset = 0;
	union wtap_pseudo_header psh;

	// This structure is *critical*, it holds two points which we hold and pass back to
	// the Java code.  This way we don't have to dissect multiple times for a packet.
	write_field_data_t *dissection = malloc(sizeof(write_field_data_t));
	dissection->edt = malloc(sizeof(epan_dissect_t));
	memset(dissection->edt, '\0', sizeof(epan_dissect_t));

	// Translate the jbyteArrays to points for dissection
	pHeader = (char *) (*env)->GetByteArrayElements(env, header, NULL);
	pData = (char *) (*env)->GetByteArrayElements(env, data, NULL);

	// We are going to copy, not cast, in to the whdr because we need to set an additional value
	memcpy(&whdr, pHeader, sizeof(struct wtap_pkthdr));
	whdr.pkt_encap = encap;

	// Set up the frame data
	frame_data_init(&fdata, 0, &whdr, offset, cum_bytes);  // count is hardcoded 0, doesn't matter

	// Set up the dissection
	epan_dissect_init(dissection->edt, create_proto_tree, 1);
	tap_queue_init(dissection->edt);

	// Set some of the frame data
	memset(&cfile.elapsed_time, '\0', sizeof(nstime_t));
	memset(&first_ts, '\0', sizeof(nstime_t));
	memset(&prev_dis_ts, '\0', sizeof(nstime_t));
	memset(&prev_cap_ts, '\0', sizeof(nstime_t));
	frame_data_set_before_dissect(&fdata, &cfile.elapsed_time,
									&first_ts, &prev_dis_ts, &prev_cap_ts);
	fdata.file_off=0;

	// Run the actual dissection
	memset(&psh, '\0', sizeof(union wtap_pseudo_header));
	epan_dissect_run(dissection->edt, &psh, pData, &fdata, NULL);

	// Do some cleanup
	frame_data_cleanup(&fdata);
	(*env)->ReleaseByteArrayElements( env, header, pHeader, NULL);
	(*env)->ReleaseByteArrayElements( env, data, pData, NULL);


	return (jint)dissection;
}

jstring
Java_com_gnychis_coexisyst_CoexiSyst_wiresharkGet(JNIEnv* env, jobject thiz, jint wfd_ptr, jstring param)
{
	gchar *field;
	output_fields_t* output_fields  = NULL;
	int z = 1;
	jstring result;

	write_field_data_t *dissection = (write_field_data_t *) wfd_ptr;

	// Get a regular pointer to the field we are looking up, and add it to the output fields
	field = (gchar *) (*env)->GetStringUTFChars(env, param, 0);
	output_fields = output_fields_new();
	output_fields_add(output_fields, field);

	// Setup the output fields
	dissection->fields = output_fields;
	dissection->fields->field_indicies = g_hash_table_new(g_str_hash, g_str_equal);
	dissection->fields->fields->len = 1;
	g_hash_table_insert(dissection->fields->field_indicies, field, GUINT_TO_POINTER(z));
	dissection->fields->field_values = ep_alloc_array0(emem_strbuf_t*, dissection->fields->fields->len);

	// Run and get the value
	proto_tree_children_foreach(dissection->edt->tree, proto_tree_get_node_field_values, dissection);
	result = (*env)->NewStringUTF(env, dissection->fields->field_values[0]->str);

	output_fields_free(dissection->fields);
	(*env)->ReleaseStringUTFChars(env, param, field);

	return result;
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
  
  prefs_apply_all();
  start_requested_stats();
  
  capture_opts_trim_snaplen(&global_capture_opts, MIN_PACKET_SIZE);
  capture_opts_trim_ring_num_files(&global_capture_opts);
  
  cfile.rfcode = rfcode;
  
  do_dissection = 1;

  timestamp_set_precision(TS_PREC_AUTO_USEC);

	return 1;
}
