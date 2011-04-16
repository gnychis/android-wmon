#$Id: extract_asn1_from_spec.pl 36442 2011-04-04 14:35:21Z etxrab $
#!/usr/bin/perl
# 2011 Vincent Helfre and Erwan Yvin
# This script extracts the ASN1 definition from and TS 36.331/25.331 and generates asn files that can be processed by asn2wrs
# First download the specification from 3gpp.org as a word document and open it
# Then in "view" menu, select normal, draft or web layout (any kind that removes page header and footers)
# Finally save the document as a text file
# Example with TS 36.331: "perl extract_asn1_from_spec.pl 36331-xxx.txt"
# It should generate: EUTRA-RRC-Definitions.asn, EUTRA-UE-Variables.asn and EUTRA-InterNodeDefinitions
use warnings;
$input_file = $ARGV[0];
$version = 0;

sub extract_spec_version;
sub extract_asn1;

open(INPUT_FILE, "< $input_file") or die "Can not open file $input_file";

extract_spec_version();

extract_asn1();

close(INPUT_FILE);

# This subroutine extracts the version of the specification
sub extract_spec_version {
  my $line;
  while($line = <INPUT_FILE>){
    if($line =~ m/3GPP TS (25|36)\.331 V/){
      $version = $line;
      return;
    }
  }
}

# This subroutine copies the text delimited by -- ASN1START and -- ASN1STOP in INPUT_FILE
# and copies it into OUTPUT_FILE.
# The OUTPUT_FILE is opened on encounter of the keyword "DEFINITIONS AUTOMATIC TAGS"
# and closed on encounter of the keyword "END"
sub extract_asn1 {
  my $line;
  my $is_asn1 = 0;
  my $output_file_name = 0;

  while($line = <INPUT_FILE>){
    if ($line =~ m/-- ASN1STOP/) {
      $is_asn1 = 0;
    }

    if($line =~ m/DEFINITIONS AUTOMATIC TAGS ::=/){
      ($output_file_name) = ($line =~ m/^([a-zA-Z\-]+)\s+DEFINITIONS AUTOMATIC TAGS ::=/);
      $output_file_name = "$output_file_name".".asn";
      print  "generating $output_file_name\n";
      open(OUTPUT_FILE, "> $output_file_name") or die "Can not open file $output_file_name";
      syswrite OUTPUT_FILE,"-- $version-- \$Id: extract_asn1_from_spec.pl 36442 2011-04-04 14:35:21Z etxrab $output_file_name 32781 2010-05-12 05:51:54Z etxrab \$\n--\n";
      $is_asn1 = 1;
    }

    if (($line =~ /END$/) && (defined fileno OUTPUT_FILE)){
      syswrite OUTPUT_FILE,"$line";
      close(OUTPUT_FILE);
      $is_asn1 = 0;
    }

    if (($is_asn1 == 1) && (defined fileno OUTPUT_FILE)){
      syswrite OUTPUT_FILE,"$line";
    }
    if ($line =~ m/-- ASN1START/) {
      $is_asn1 = 1;
    }
  }
}

