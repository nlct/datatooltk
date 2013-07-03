#!/usr/bin/perl -w

use strict;
use DatatoolTk;

my $db = DatatoolTk->new();

my $rowCount = $db->rowCount();
my $colCount = $db->columnCount();
my $selectedRow = $db->selectedRow();
my $selectedColumn = $db->selectedColumn();

my @row = ();

for (my $idx = 0; $idx < $rowCount; $idx++)
{
   push @row, "Entry $idx";
}

$db->insertRow($selectedRow+1, \@row);

1;
