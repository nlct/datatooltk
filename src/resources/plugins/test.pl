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
   push @row, $db->getColumnLabel($idx);
}

if ($selectedRow eq -1)
{
   $db->appendRow(\@row);
}
else
{
   $db->insertRow($selectedRow+1, \@row);
   #$db->replaceRow($selectedRow, \@row);
}

1;
