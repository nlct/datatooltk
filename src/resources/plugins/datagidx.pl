#!/usr/bin/perl -w

use strict;
use Tk;
use DatatoolTk;

my $db = DatatoolTk->new();

my $rowCount    = $db->rowCount;
my $columnCount = $db->columnCount;

my $selectedRow = $db->selectedRow();

my %colIndexes = ();

foreach my $key (qw/Label Name Text Parent Child Description Symbol
Long Short See SeeAlso Plural ShortPlural LongPlural Sort/)
{
   my $idx = $db->getColumnIndex($key);

   if ($idx == -1)
   {
      die $db->getDictWord('plugin.error.missing_column', $key), "\n";
   }

   $colIndexes{$key} = $idx;
}

my @row = ("") x $columnCount;

if ($selectedRow > -1)
{
   @row = @{$db->getRow($selectedRow)};
}

my $mw = MainWindow->new;

$mw->title($selectedRow > -1 ?
  &getWord("edit_entry") : &getWord("new_entry"));

my $frame = $mw->Frame()->pack;

my %entries = ();

foreach my $key (qw/Name Label Sort/)
{
   $frame->Label
    (
      -text => &getWord($key)
    )->pack(-side=>'left', -expand=>1);

   if ($key eq 'Name')
   {
      $entries{$key} = $frame->Entry
       (
         -validate=>'focusout',
         -validatecommand=>\&nameValidate
       )->pack(-side=>'left', -expand=>1);
   }
   else
   {
      $entries{$key} = $frame->Entry
       (
       )->pack(-side=>'left', -expand=>1);
   }

   $entries{$key}->insert(0, $row[$colIndexes{$key}]);
}

$frame = $mw->Frame()->pack;

$frame->Label
 (
   -text=>&getWord('Description')
 )->pack(-side=>'left', -expand=>1);

my $descriptionText = $frame->Scrolled('Text', -height=>6)->pack();

$descriptionText->Contents($row[$colIndexes{Description}]);

$frame = $mw->Frame()->pack;

my $columnIdx = 0;
my $rowIdx = 0;

foreach my $key (qw/Text Short Long Plural ShortPlural LongPlural/)
{
   $frame->Label
    (
      -text => &getWord($key)
    )->grid(-row=>$rowIdx, -column=>$columnIdx, -stick=>'nsew');

   $columnIdx++;

   $entries{$key} = $frame->Entry
    (
    )->grid(-row=>$rowIdx, -column=>$columnIdx, -stick=>'nsew');

   $entries{$key}->insert(0, $row[$colIndexes{$key}]);

   $columnIdx++;

   if ($columnIdx >= 6)
   {
      $columnIdx = 0;
      $rowIdx++;
   }
}

my $labels = $db->getColumn($colIndexes{Label}, $selectedRow);

$frame = $mw->Frame()->pack;

$frame->Label
 (
   -text=>&getWord('Symbol')
 )->pack(-side=>'left', -expand=>1);

$entries{'Symbol'} = $frame->Entry
 (
 )->pack(-side=>'left', -expand=>1);

$entries{'Symbol'}->insert(0, $row[$colIndexes{Symbol}]);

$frame->Label
 (
   -text=>&getWord('Parent'),
 )->pack(-side=>'left', -expand=>1);

my @allowedParents = ('');
my @labels = ('');

my $theLabel = $row[$colIndexes{Label}];

for (my $idx = 0; $idx < $rowCount; $idx++)
{
   my $thisRow = $db->getRow($idx);

   my $thisLabel = $thisRow->[$colIndexes{Label}];

   unless ($thisLabel eq $theLabel)
   {
      push @labels, $thisLabel;

      if (not $theLabel or ($thisRow->[$colIndexes{Parent}] ne $theLabel))
      {
         # Don't allow user to select an entry that has this as its parent

         push @allowedParents, $thisLabel;
      }
   }
}

my $parent = $row[$colIndexes{Parent}];

$frame->Optionmenu
(
  -options=>\@allowedParents,
  -variable=>\$parent
)->pack(-side=>'left', -expand=>1);

$frame->Label
 (
   -text=>&getWord('See')
 )->pack(-side=>'left', -expand=>1);

my $see = $row[$colIndexes{See}];

$frame->Optionmenu
(
  -options=>\@labels,
  -variable=>\$see
)->pack(-side=>'left', -expand=>1);

my $buttonFrame = $mw->Frame()->pack;

$buttonFrame->Button(
    -text    => $db->getDictWord('button.cancel'),
    -command => sub { $mw->destroy },
)->pack(-side=>'left', -expand=>1);

$buttonFrame->Button(
    -text    => $db->getDictWord('button.okay'),
    -command => \&doDbUpdate,
)->pack(-side=>'left', -expand=>1);

$mw->update;

my $xpos = int(($mw->screenwidth-$mw->width)/2);
my $ypos = int(($mw->screenheight-$mw->height)/2);

$mw->geometry("+$xpos+$ypos");

MainLoop;

sub doDbUpdate{

   foreach my $key (keys %entries)
   {
      $row[$colIndexes{$key}] = $entries{$key}->get;
   }

   $row[$colIndexes{Description}] = $descriptionText->Contents;

   $row[$colIndexes{Description}]=~s/\n/<br\/>/sg;

   $row[$colIndexes{See}] = $see;

   $row[$colIndexes{Parent}] = $parent;

   $db->startModifications;

   if ($parent)
   {
      # Find row corresponding to this value

      my $theLabel = $row[$colIndexes{Label}];

      for (my $idx = 0; $idx < $rowCount; $idx++)
      {
         if ($parent eq $db->getEntry($idx, $colIndexes{Label}))
         {
            my $parentRow = $db->getRow($idx);

            my $children = $parentRow->[$colIndexes{Child}];

            unless ($children=~/(^|,)$theLabel(,|$)/)
            {
               $parentRow->[$colIndexes{Child}] =
                   ($children ? "$children,$theLabel" : $theLabel);
            }

            $db->replaceRow($idx, $parentRow);

            last;
         }
      }
   }

   if ($selectedRow > -1)
   {
      $db->replaceRow($selectedRow, \@row);
   }
   else
   {
      $db->appendRow(\@row);
   }

   $db->endModifications;

   $mw->destroy;
}

sub nameValidate{

   my $value = $entries{Name}->get;

   foreach my $key (qw/Text Short Long/)
   {
      unless ($entries{$key}->get)
      {
         $entries{$key}->insert(0, $value);
      }
   }

   my $plural = $value.'s';

   foreach my $key (qw/Plural ShortPlural LongPlural/)
   {
      unless ($entries{$key}->get)
      {
         $entries{$key}->insert(0, $plural);
      }
   }


   $value=~s/\\[a-zA-Z]+\s*//g;
   $value=~s/[\{\}]//g;

   unless ($entries{'Sort'}->get)
   {
      $entries{'Sort'}->insert(0, $value);
   }

   unless ($entries{'Label'}->get)
   {
      $value=~s/\s+//g;

      $entries{'Label'}->insert(0, $value);
   }

   1;
}

sub getWord{
  $db->getDictWord('plugin.datagidx.'.shift)
}

1;
