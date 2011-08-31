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

#include <epan/charsets.h>
#include <epan/dissectors/packet-data.h>
#include <epan/dissectors/packet-frame.h>


#include "cfile.h"
#include "capture_ui_utils.h"
#include "capture_ifinfo.h"
#include "capture-pcap-util.h"
#include "print.h"

void dissectCleanup(int ptr);
int dissectPacket(char *pHeader, char *pData, int encap);
char *wiresharkGet(int wfd_ptr, gchar *field);
void myoutput_fields_free(output_fields_t* fields);
void wireshark_get_fields(proto_node *node, gpointer data);
void wiresharkGetAll(int wfd_ptr);

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

static jmethodID nativeCrashed;

/*
 * The way the packet decode is to be written.
 */
typedef enum {
  WRITE_TEXT,   /* summary or detail text */
  WRITE_XML,    /* PDML or PSML */
  WRITE_FIELDS  /* User defined list of fields */
  /* Add CSV and the like here */
} output_action_e;

static JNIEnv *_env;
static jobject _obj;

static output_action_e output_action;

extern void proto_tree_get_node_field_values(proto_node *node, gpointer data);
extern void tshark_log_handler (const gchar *log_domain, GLogLevelFlags log_level, const gchar *message, gpointer user_data);
extern void open_failure_message(const char *filename, int err, gboolean for_writing);
extern void failure_message(const char *msg_format, va_list ap);
extern void read_failure_message(const char *filename, int err);
extern void write_failure_message(const char *filename, int err);

#define LOG_TAG "WiresharkDriver"
//#define VERBOSE

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
Java_com_gnychis_coexisyst_CoexiSyst_wiresharkTestGetAll(JNIEnv* env, jobject thiz, jstring jsFname)
{
  FILE *bf;
  unsigned char *data_buffer;
  int psize = 113;
  int z, y, i;
  struct pcap_file_header pfheader;
	char *fname;
	int parsed=0;

  _env=env;
  _obj=thiz;
	
	fname = (char *) (*env)->GetByteArrayElements(env, jsFname, NULL);

  bf = fopen("/sdcard/test.pcap", "rb");
  if(!bf) {
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Could not open %s - %s\n", jsFname, fname);
    return;
  }

  // Read the global pcap header
  if((z = fread(&pfheader, sizeof(struct pcap_file_header), 1, bf)) != 1) {
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Did not read past header: %d\n", z);
    return;
  }
#ifdef VERBOSE
  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "read link type: %d\n", pfheader.linktype);
#endif

	while(1) {
		int dissect_ptr;
		char *rval;
		struct pcap_pkthdr pheader;
	
		// Read in the first packet header
		if((z = fread(&pheader, sizeof(pheader), 1, bf)) != 1) {
			__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Could not read packet header\n");
			break;
		}
#ifdef VERBOSE
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "got the first packet, size: %d\n", pheader.caplen);
#endif

		// Read in the packet now
		data_buffer = malloc(pheader.caplen);
		if((z = fread(data_buffer, pheader.caplen, 1, bf)) != 1) {
			__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Could not read packet after %d packets\n", parsed);
			break;
		}
#ifdef VERBOSE
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Read in the packet\n", rval);
#endif
		
		dissect_ptr = dissectPacket((char *)&pheader, data_buffer, WTAP_ENCAP_IEEE_802_11_WLAN_RADIOTAP);
#ifdef VERBOSE
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Finished dissecting (%d)\n", parsed);
#endif
		wiresharkGetAll(dissect_ptr);
		dissectCleanup(dissect_ptr);
#ifdef VERBOSE
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Successful cleanup\n");
#endif
		free(data_buffer);
		parsed++;
	}

	(*env)->ReleaseByteArrayElements( env, jsFname, fname, 0);
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Succussfully passed wireshark test after %d packets\n", parsed);
}

void
Java_com_gnychis_coexisyst_CoexiSyst_wiresharkTest(JNIEnv* env, jobject thiz, jstring jsFname)
{
  FILE *bf;
  unsigned char *data_buffer;
  int psize = 113;
  int z, y, i;
  struct pcap_file_header pfheader;
	char *fname;
	int parsed=0;
  
  _env=env;
  _obj=thiz;
	
	fname = (char *) (*env)->GetByteArrayElements(env, jsFname, NULL);

  bf = fopen("/sdcard/test.pcap", "rb");
  if(!bf) {
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Could not open %s - %s\n", jsFname, fname);
    return;
  }

  // Read the global pcap header
  if((z = fread(&pfheader, sizeof(struct pcap_file_header), 1, bf)) != 1) {
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Did not read past header: %d\n", z);
    return;
  }
#ifdef VERBOSE
  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "read link type: %d\n", pfheader.linktype);
#endif

	while(1) {
		int dissect_ptr;
		char *rval;
		struct pcap_pkthdr pheader;
	
		// Read in the first packet header
		if((z = fread(&pheader, sizeof(pheader), 1, bf)) != 1) {
			__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Could not read packet header\n");
			break;
		}
#ifdef VERBOSE
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "got the first packet, size: %d\n", pheader.caplen);
#endif

		// Read in the packet now
		data_buffer = malloc(pheader.caplen);
		if((z = fread(data_buffer, pheader.caplen, 1, bf)) != 1) {
			__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Could not read packet after %d packets\n", parsed);
			break;
		}
#ifdef VERBOSE
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Read in the packet\n", rval);
#endif
		
		dissect_ptr = dissectPacket((char *)&pheader, data_buffer, WTAP_ENCAP_IEEE_802_11_WLAN_RADIOTAP);
#ifdef VERBOSE
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Finished dissecting (%d)\n", parsed);
#endif
		rval = wiresharkGet(dissect_ptr, "wlan_mgt.ssid");
#ifdef VERBOSE
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Got result: %s\n", rval);
#endif
		free(rval);
		dissectCleanup(dissect_ptr);
		free(data_buffer);
		parsed++;
	}

	(*env)->ReleaseByteArrayElements( env, jsFname, fname, 0);
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Succussfully passed wireshark test after %d packets\n", parsed);
}


void
Java_com_gnychis_coexisyst_CoexiSyst_dissectCleanup(JNIEnv* env, jobject thiz, jint ptr)
{
  
  _env=env;
  _obj=thiz;
	dissectCleanup(ptr);
}
void
Java_com_gnychis_coexisyst_Packet_dissectCleanup(JNIEnv* env, jobject thiz, jint ptr)
{
  
  _env=env;
  _obj=thiz;
	dissectCleanup(ptr);
}

//#define VERBOSE_CLEAN
void
dissectCleanup(int ptr)
{
	write_field_data_t *dissection = (write_field_data_t *) ptr;
#ifdef VERBOSE_CLEAN
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "casted the pointer: %d - %d", ptr, (int) dissection->edt);
#endif
  if(dissection->edt!=NULL)
	  epan_dissect_cleanup(dissection->edt);
#ifdef VERBOSE_CLEAN
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "cleaned up dissection: %d", ptr);
#endif
  if(dissection->edt!=NULL)
    free(dissection->edt);
#ifdef VERBOSE_CLEAN
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "freed edt: %d", ptr);
#endif
  if(dissection!=NULL && dissection != -1)
    free(dissection);
#ifdef VERBOSE_CLEAN
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "freed dissection: %d", ptr);
#endif
}

int
dissectPacket(char *pHeader, char *pData, int encap)
{
	// From the Java environment
	struct wtap_pkthdr whdr;
	int i;
	gsize z;
	
	// Wireshark related
	frame_data fdata;
	gboolean create_proto_tree = 1;
	gint64 offset = 0;
	union wtap_pseudo_header psh;

#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "dissectPacket(hello!)");
#endif

	// This structure is *critical*, it holds two points which we hold and pass back to
	// the Java code.  This way we don't have to dissect multiple times for a packet.
	write_field_data_t *dissection = malloc(sizeof(write_field_data_t));
	dissection->edt = malloc(sizeof(epan_dissect_t));
	memset(dissection->edt, '\0', sizeof(epan_dissect_t));
#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "dissectPacket(created the dissection field)");
#endif

	// We are going to copy, not cast, in to the whdr because we need to set an additional value
	memcpy(&whdr, pHeader, sizeof(struct wtap_pkthdr));
	whdr.pkt_encap = encap;
#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "dissectPacket(set the encapsulation type)");
#endif

	// Set up the frame data
	frame_data_init(&fdata, 0, &whdr, offset, cum_bytes);  // count is hardcoded 0, doesn't matter
#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "dissectPacket(initialized the frame data)");
#endif

	// Set up the dissection
	epan_dissect_init(dissection->edt, create_proto_tree, 1);
	tap_queue_init(dissection->edt);
#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "dissectPacket(initialized the dissection)");
#endif

	// Set some of the frame data
	memset(&cfile.elapsed_time, '\0', sizeof(nstime_t));
	memset(&first_ts, '\0', sizeof(nstime_t));
	memset(&prev_dis_ts, '\0', sizeof(nstime_t));
	memset(&prev_cap_ts, '\0', sizeof(nstime_t));
	frame_data_set_before_dissect(&fdata, &cfile.elapsed_time,
									&first_ts, &prev_dis_ts, &prev_cap_ts);
	fdata.file_off=0;
#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "dissectPacket(setup additional frame data)");
#endif

	// Run the actual dissection
	memset(&psh, '\0', sizeof(union wtap_pseudo_header));
#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "dissectPacket(right before the dissection!)");
#endif
	epan_dissect_run(dissection->edt, &psh, pData, &fdata, NULL);
#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "dissectPacket(ran the dissection)");
#endif

	// Do some cleanup
	frame_data_cleanup(&fdata);
#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "dissectPacket(cleaned up the frame data)");
#endif

#ifdef VERBOSE_CLEAN
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "new dissection: %d - %d", (int)dissection, (int)dissection->edt);
#endif

	return (int)dissection;
}

jint
Java_com_gnychis_coexisyst_CoexiSyst_dissectPacket(JNIEnv* env, jobject thiz, jbyteArray header, jbyteArray data, jint encap)
{
	char *pHeader;
	char *pData;
	jint ret;
  
  _env=env;
  _obj=thiz;

	// Translate the jbyteArrays to points for dissection
	pHeader = (char *) (*env)->GetByteArrayElements(env, header, NULL);
	pData = (char *) (*env)->GetByteArrayElements(env, data, NULL);

	ret = dissectPacket(pHeader, pData, (int)encap);

	(*env)->ReleaseByteArrayElements( env, header, pHeader, 0);
	(*env)->ReleaseByteArrayElements( env, data, pData, 0);

	return ret;
}
jint
Java_com_gnychis_coexisyst_Packet_dissectPacket(JNIEnv* env, jobject thiz, jbyteArray header, jbyteArray data, jint encap)
{
	char *pHeader;
	char *pData;
	jint ret;
  
  _env=env;
  _obj=thiz;

#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Packet_dissectPacket(Hello!)");
#endif

	// Translate the jbyteArrays to points for dissection
	pHeader = (char *) (*env)->GetByteArrayElements(env, header, NULL);
	pData = (char *) (*env)->GetByteArrayElements(env, data, NULL);

	ret = dissectPacket(pHeader, pData, (int)encap);

	(*env)->ReleaseByteArrayElements( env, header, pHeader, 0);
	(*env)->ReleaseByteArrayElements( env, data, pData, 0);

	return ret;
}

struct gnychis_str_list_s {
  char *str;    // must be freed
  struct gnychis_str_list_s *next;
};
typedef struct gnychis_str_list_s str_list_item;

struct gnychis_field_data_s {
  int num_fields;
  epan_dissect_t *edt;
  struct gnychis_str_list_s *fields_head;
  struct gnychis_str_list_s *fields;
};

typedef struct gnychis_field_data_s gnychis_field_data;

// Return a list of all the fields
void
wiresharkGetAll(int wfd_ptr)
{
	gnychis_field_data data;
	write_field_data_t *dissection = (write_field_data_t *) wfd_ptr;
  str_list_item *item;
  int x=0;

  /* Create the output */
	data.edt = dissection->edt;
  data.fields_head = NULL;
  data.fields = NULL;
  data.num_fields=0;

	proto_tree_children_foreach(dissection->edt->tree, wireshark_get_fields,
	    &data);

  // Go through the list
  item = data.fields_head;
#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "\n<packet>");
#endif
  while(item != NULL) {
		str_list_item *old;
#ifdef VERBOSE
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "%s", item->str);
#endif
		free(item->str);
		old = item;
    item = item->next;
		free(old);
    x++;
  }
#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "<packet>\n");
#endif
}

void
Java_com_gnychis_coexisyst_CoexiSyst_wiresharkGetAllTest(JNIEnv* env, jobject thiz, jint wfd_ptr)
{
  
  _env=env;
  _obj=thiz;
	wiresharkGetAll((int)wfd_ptr);
}

// Return a list of all the fields
jobjectArray
Java_com_gnychis_coexisyst_Packet_wiresharkGetAll(JNIEnv* env, jobject thiz, jint wfd_ptr)
{
  jobjectArray fields = 0;
	jstring      str;
	gnychis_field_data data;
	write_field_data_t *dissection = (write_field_data_t *) wfd_ptr;
  str_list_item *item;
  int x=0;
  
  _env=env;
  _obj=thiz;

  /* Create the output */
	data.edt = dissection->edt;
  data.fields_head = NULL;
  data.fields = NULL;
  data.num_fields=0;

	proto_tree_children_foreach(dissection->edt->tree, wireshark_get_fields,
	    &data);

  fields = (*env)->NewObjectArray(env, (jsize)data.num_fields, (*env)->FindClass(env, "java/lang/String"), 0);
#ifdef VERBOSE
  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Dissected with %d fields\n", data.num_fields);
#endif
  
  // Go through the list
  item = data.fields_head;
  while(item != NULL && x < data.num_fields) {
		str_list_item *old;
    jstring strt = (*env)->NewStringUTF( env, item->str );
    (*env)->SetObjectArrayElement(env, fields, x, strt);
		free(item->str);
		old = item;
    item = item->next;
		free(old);
    x++;
  }

  return fields;
}

// Return a list of all the fields
jobjectArray
Java_com_gnychis_coexisyst_CoexiSyst_wiresharkGetAll(JNIEnv* env, jobject thiz, jint wfd_ptr)
{
  jobjectArray fields = 0;
	jstring      str;
	gnychis_field_data data;
	write_field_data_t *dissection = (write_field_data_t *) wfd_ptr;
  str_list_item *item;
  int x=0;
  
  _env=env;
  _obj=thiz;

  /* Create the output */
	data.edt = dissection->edt;
  data.fields_head = NULL;
  data.fields = NULL;
  data.num_fields=0;

	proto_tree_children_foreach(dissection->edt->tree, wireshark_get_fields,
	    &data);

  fields = (*env)->NewObjectArray(env, (jsize)data.num_fields, (*env)->FindClass(env, "java/lang/String"), 0);
#ifdef VERBOSE
  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Dissected with %d fields\n", data.num_fields);
#endif
  
  // Go through the list
  item = data.fields_head;
  while(item != NULL && x < data.num_fields) {
		str_list_item *old;
    jstring strt = (*env)->NewStringUTF( env, item->str );
    (*env)->SetObjectArrayElement(env, fields, x, strt);
		free(item->str);
		old = item;
    item = item->next;
		free(old);
    x++;
  }

  return fields;
}

void
wireshark_get_fields(proto_node *node, gpointer data)
{
	field_info	*fi = PNODE_FINFO(node);
	gnychis_field_data	*pdata = (gnychis_field_data*) data;
	const gchar	*label_ptr;
	gchar		label_str[ITEM_LABEL_LENGTH];
	char		*dfilter_string;
	size_t		chop_len;
	int		i;
  char item_buf[512];
  str_list_item *item;

	/* Text label. It's printed as a field with no name. */
	if (fi->hfinfo->id == hf_text_only) {
	}

	/* Uninterpreted data, i.e., the "Data" protocol, is
	 * printed as a field instead of a protocol. */
	else if (fi->hfinfo->id == proto_data) {

	}
	/* Normal protocols and fields */
	else {

		switch (fi->hfinfo->type)
		{
		case FT_PROTOCOL:
			break;
		case FT_NONE:
			break;
		default:

      
      item = malloc(sizeof(str_list_item));
      item->str = malloc(256);
      item->next = NULL;

      if(pdata->fields_head == NULL) {
        pdata->fields_head = item;
        pdata->fields = item;
      } else {
        pdata->fields->next = item;
        pdata->fields = item;
      }


			/* XXX - this is a hack until we can just call
			 * fvalue_to_string_repr() for *all* FT_* types. */
			dfilter_string = proto_construct_match_selected_string(fi,
			    pdata->edt);
			if (dfilter_string != NULL) {
				chop_len = strlen(fi->hfinfo->abbrev) + 4; /* for " == " */

				/* XXX - Remove double-quotes. Again, once we
				 * can call fvalue_to_string_repr(), we can
				 * ask it not to produce the version for
				 * display-filters, and thus, no
				 * double-quotes. */
				if (dfilter_string[strlen(dfilter_string)-1] == '"') {
					dfilter_string[strlen(dfilter_string)-1] = '\0';
					chop_len++;
				}

				//fputs("\" show=\"", pdata->fh);
				//print_escaped_xml(pdata->fh, &dfilter_string[chop_len]);
        snprintf(item->str, 256, "%s %s", fi->hfinfo->abbrev, &dfilter_string[chop_len]); 
#ifdef VERBOSE
//				__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "field: %s - %s", fi->hfinfo->abbrev, &dfilter_string[chop_len]);
#endif
        pdata->num_fields++;
			}
		}
	}

	/* We always print all levels for PDML. Recurse here. */
	if (node->first_child != NULL) {
		proto_tree_children_foreach(node,
				wireshark_get_fields, pdata);
	}

}


char *
wiresharkGet(int wfd_ptr, gchar *field)
{
	output_fields_t* output_fields  = NULL;
	int z = 1;
	jstring result;
	char *str_res = malloc(1024);	// assuming string result will be no more than 1024

	memset(str_res, '\0', sizeof(str_res));

	write_field_data_t *dissection = (write_field_data_t *) wfd_ptr;
#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "wiresharkGet(casted dissection pointer)");
#endif
	
	// Get a regular pointer to the field we are looking up, and add it to the output fields
	output_fields = output_fields_new();
	output_fields_add(output_fields, field);
#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "wiresharkGet(created the new output field)");
#endif

	// Setup the output fields
	dissection->fields = output_fields;
	dissection->fields->field_indicies = g_hash_table_new(g_str_hash, g_str_equal);
	dissection->fields->fields->len = 1;
	g_hash_table_insert(dissection->fields->field_indicies, field, GUINT_TO_POINTER(z));
	dissection->fields->field_values = ep_alloc_array0(emem_strbuf_t*, dissection->fields->fields->len);
#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "wiresharkGet(setup the output field)");
#endif

	// Run and get the value
	proto_tree_children_foreach(dissection->edt->tree, proto_tree_get_node_field_values, dissection);
	if(dissection->fields->field_values[0]!=NULL) {
		strncpy(str_res, dissection->fields->field_values[0]->str, 1024);
  } else {
    myoutput_fields_free(dissection->fields);
    free(str_res);
    return NULL;
  }

#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "wiresharkGet(traversed the tree, got result)");
#endif

	myoutput_fields_free(dissection->fields);
#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "wiresharkGet(freed up the output field)");
#endif

	return str_res;
}

void myoutput_fields_free(output_fields_t* fields)
{
    g_assert(fields);

    if(NULL != fields->field_indicies) {
        /* Keys are stored in fields->fields, values are
         * integers.
         */
        g_hash_table_destroy(fields->field_indicies);
#ifdef VERBOSE
				__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "myoutput_fields_free(destroyed the hash table)");
#endif
    }
    if(NULL != fields->fields) {
        gsize i;
        for(i = 0; i < fields->fields->len; ++i) {
            gchar* field = (gchar *)g_ptr_array_index(fields->fields,i);
#ifdef VERBOSE
						__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "myoutput_fields_free(freeing '%s')", field);
#endif
            g_free(field);
        }
        g_ptr_array_free(fields->fields, TRUE);
#ifdef VERBOSE
				__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "myoutput_fields_free(freed the fields array)");
#endif
    }

    g_free(fields);
#ifdef VERBOSE
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "myoutput_fields_free(freed fields entirely)");
#endif
}


jstring
Java_com_gnychis_coexisyst_CoexiSyst_wiresharkGet(JNIEnv* env, jobject thiz, jint wfd_ptr, jstring param)
{
	gchar *field;
	char *str_result;
	jstring result;
  
  _env=env;
  _obj=thiz;

	field = (gchar *) (*env)->GetStringUTFChars(env, param, 0);

	str_result = wiresharkGet((int)wfd_ptr, field);
	result = (*env)->NewStringUTF(env, str_result);
	free(str_result);

	(*env)->ReleaseStringUTFChars(env, param, field);

	return result;
}
jstring
Java_com_gnychis_coexisyst_Packet_wiresharkGet(JNIEnv* env, jobject thiz, jint wfd_ptr, jstring param)
{
	gchar *field;
	char *str_result;
	jstring result;
  
  _env=env;
  _obj=thiz;

	field = (gchar *) (*env)->GetStringUTFChars(env, param, 0);

	str_result = wiresharkGet((int)wfd_ptr, field);

  if(str_result == NULL)
    return NULL;

	result = (*env)->NewStringUTF(env, str_result);
	free(str_result);

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
  
  _env=env;
  _obj=thiz;
  
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
  /* disabled protocols as per configuration file */
	if (gdp_path == NULL && dp_path == NULL) {
		set_disabled_protos_list();
	}

	return 1;
}

static struct sigaction old_sa[NSIG];

void android_sigaction(int signal, siginfo_t *info, void *reserved)
{
  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "(JNIDEBUG) android_sigaction");
  (*_env)->CallVoidMethod(_env, _obj, nativeCrashed);
  old_sa[signal].sa_handler(signal);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved)
{

  JNIEnv  *env=NULL; 

  jclass cls=NULL;
  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "(JNIDEBUG) At JNI_OnLoad");

  if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) 
                  return -1; 

  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "(JNIDEBUG) Got the environment");
  _env = env;
  
  if(env==NULL)
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "(JNIDEBUG) Could not grab env");

  cls = (*env)->FindClass(env, "com/gnychis/coexisyst/Packet");

  if(cls==NULL)
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "(JNIDEBUG) Could not find class");

  nativeCrashed  = (*env)->GetMethodID(env, cls,  "nativeCrashed", "()V");

  if(nativeCrashed==NULL)
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "(JNIDEBUG) nativeCrashed was NULL");
    

  // Try to catch crashes...
  struct sigaction handler;
  memset(&handler, 0, sizeof(sigaction));
  handler.sa_sigaction = android_sigaction;
  handler.sa_flags = SA_RESETHAND;
#define CATCHSIG(X) sigaction(X, &handler, &old_sa[X])
  CATCHSIG(SIGILL);
  CATCHSIG(SIGABRT);
  CATCHSIG(SIGBUS);
  CATCHSIG(SIGFPE);
  CATCHSIG(SIGSEGV);
  CATCHSIG(SIGSTKFLT);
  CATCHSIG(SIGPIPE);

  return JNI_VERSION_1_2;
}
