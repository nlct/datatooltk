#!/usr/bin/perl -w

use strict;
use Tk;
use Tk::PNG;
use DatatoolTk;

my $db = DatatoolTk->new();

my $rowCount    = $db->rowCount;
my $columnCount = $db->columnCount;

my $selectedRow = $db->selectedRow();

my %colIndexes = ();

foreach my $key (qw/Label Name Text Parent Child Description Symbol
Long Short See SeeAlso Plural ShortPlural LongPlural Sort Location
CurrentLocation FirstId Used/)
{
   my $idx = $db->getColumnIndex($key);

   if ($idx == -1)
   {
      die $db->getDictWord('plugin.error.missing_column', $key), "\n";
   }

   $colIndexes{$key} = $idx;
}

my @row = ("") x $columnCount;

$row[$colIndexes{Used}] = "0";

foreach my $key (qw/Location CurrentLocation FirstId/)
{
   $row[$colIndexes{$key}] = "\\\@dtlnovalue";
}

if ($selectedRow > -1)
{
   @row = @{$db->getRow($selectedRow)};

   foreach my $key (qw/Parent See SeeAlso/)
   {
      $row[$colIndexes{$key}] = '' 
         if ($row[$colIndexes{$key}]=~/^\\\@dtlnovalue\s*$/);
   }
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
my @seealsoLabels = ();

my $theLabel = $row[$colIndexes{Label}];

for (my $idx = 0; $idx < $rowCount; $idx++)
{
   my $thisRow = $db->getRow($idx);

   my $thisLabel = $thisRow->[$colIndexes{Label}];

   unless ($thisLabel eq $theLabel)
   {
      push @labels, $thisLabel;
      push @seealsoLabels, $thisLabel;

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

my $crossrefFrame = $mw->Frame()->pack;

my $hasCrossref = 
 ($row[$colIndexes{See}] or $row[$colIndexes{SeeAlso}] ? 1 : 0);

my $crossrefCheckbutton = $crossrefFrame->Checkbutton
(
  -text=>&getWord('crossref'),
  -variable=>\$hasCrossref,
)->pack(-side=>'left', -expand=>1);

my $crossrefButtonFrame = $crossrefFrame->Frame();

$crossrefCheckbutton->configure
(
  '-command',
  sub
   {
     if ($hasCrossref)
     {
        $crossrefButtonFrame->pack;
     }
     else
     {
        $crossrefButtonFrame->packForget;
     }

     $mw->update
   }
);

my $crossrefVariable = 'see';

$crossrefButtonFrame->Radiobutton
(
  -text=>&getWord('See'),
  -variable=>\$crossrefVariable,
  -value=>'see',
  -command=>\&updateCrossRefWidgets
)->pack(-side=>'left', -expand=>1);

my $see = $row[$colIndexes{See}];

my $seeOptionmenu = $crossrefButtonFrame->Optionmenu
(
  -options=>\@labels,
  -variable=>\$see
)->pack(-side=>'left', -expand=>1);

$crossrefButtonFrame->Radiobutton
(
  -text=>&getWord('SeeAlso'),
  -variable=>\$crossrefVariable,
  -value=>'seealso',
  -command=>\&updateCrossRefWidgets
)->pack(-side=>'left', -expand=>1);

my $seealsoVariable = $row[$colIndexes{SeeAlso}];

my $seealsoLabel = $crossrefButtonFrame->Label
(
  -textvariable=>\$seealsoVariable,
  -width=>30,
  -anchor=>'w',
  -state=>'disabled'
)->pack(-side=>'left', -expand=>1);

my $seealsoOption='';

my $seealsoOptionmenu = $crossrefButtonFrame->Optionmenu
(
  -options=>\@seealsoLabels,
  -variable=>\$seealsoOption,
  -state=>'disabled'
)->pack(-side=>'left', -expand=>1);

my $crossrefActionFrame = $crossrefButtonFrame->Frame->pack;

my $addCrossrefButton;

my $shot;

my $imgFile = $db->getImageFile('add.png');

if ($imgFile and -e $imgFile)
{
   $shot = $mw->Photo(-file=>$imgFile);

   $addCrossrefButton = $crossrefActionFrame->Button
   (
     -text => &getWord('add_seealso'),
     -state=>'disabled',
     -command =>\&addCrossRef,
     -image => $shot,
     -compound=> 'left'
   )->pack(-side=>'top', -expand=>1);
}
else
{
   $addCrossrefButton = $crossrefActionFrame->Button
   (
     -text => &getWord('add_seealso'),
     -state=>'disabled',
     -command =>\&addCrossRef
   )->pack(-side=>'top', -expand=>1);
}

my $removeCrossrefButton;

$imgFile = $db->getImageFile('remove.png');

if ($imgFile and -e $imgFile)
{
   $shot = $mw->Photo(-file=>$imgFile);

   $removeCrossrefButton = $crossrefActionFrame->Button
   (
     -text => &getWord('remove_seealso'),
     -state=>'disabled',
     -command =>\&removeCrossRef,
     -image => $shot,
     -compound => 'left'
   )->pack(-side=>'top', -expand=>1);
}
else
{
   $removeCrossrefButton = $crossrefActionFrame->Button
   (
     -text => &getWord('remove_seealso'),
     -state=>'disabled',
     -command =>\&removeCrossRef
   )->pack(-side=>'top', -expand=>1);
}

if ($hasCrossref)
{
   $crossrefButtonFrame->pack;

   if ($row[$colIndexes{SeeAlso}])
   {
      $crossrefVariable = 'seealso';

      $seeOptionmenu->configure('-state', 'disabled');

      $seealsoLabel->configure('-state', 'normal');
      $seealsoOptionmenu->configure('-state', 'normal');
      $addCrossrefButton->configure('-state', 'normal');
      $removeCrossrefButton->configure('-state', 'normal');
   }
}
else
{
   $crossrefButtonFrame->packForget;
}

my $buttonFrame = $mw->Frame()->pack(-ipadx=>40);

$imgFile = $db->getImageFile('cancel.png');

if ($imgFile and -e $imgFile)
{
   $shot = $mw->Photo(-file=>$imgFile);

   $buttonFrame->Button(
       -text     => $db->getDictWord('button.cancel'),
       -command  => sub { $mw->destroy },
       -image    => $shot,
       -compound => 'left'
   )->pack(-side=>'left', -expand=>1);
}
else
{
   $buttonFrame->Button(
       -text    => $db->getDictWord('button.cancel'),
       -command => sub { $mw->destroy }
   )->pack(-side=>'left', -expand=>1);
}

$imgFile = $db->getImageFile('okay.png');

if ($imgFile and -e $imgFile)
{
   $shot = $mw->Photo(-file=>$imgFile);

   $buttonFrame->Button(
       -text     => $db->getDictWord('button.okay'),
       -command  => \&doDbUpdate,
       -image    => $shot,
       -compound => 'left'
   )->pack(-side=>'left', -expand=>1);
}
else
{
   $buttonFrame->Button(
       -text    => $db->getDictWord('button.okay'),
       -command => \&doDbUpdate
   )->pack(-side=>'left', -expand=>1);
}


if ($selectedRow > -1)
{
   $imgFile = $db->getImageFile('recycle.png');

   if ($imgFile and -e $imgFile)
   {
      $shot = $mw->Photo(-file=>$imgFile);

      $buttonFrame->Button(
          -text    => $db->getDictWord('plugin.remove_entry'),
          -command => \&doRemoveRow,
          -image    => $shot,
          -compound => 'left'
      )->pack(-side=>'left', -expand=>1);
   }
   else
   {
      $buttonFrame->Button(
          -text    => $db->getDictWord('plugin.remove_entry'),
          -command => \&doRemoveRow
      )->pack(-side=>'left', -expand=>1);
   }
}

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

   # Check against user accidentally pressing return in the
   # description box without noticing they've set the description to
   # a paragraph break

   $row[$colIndexes{Description}]=~s/^\s+$//;

   $row[$colIndexes{Description}]=~s/\n/<br\/>/sg;

   if ($hasCrossref)
   {
      if ($crossrefVariable eq 'see')
      {
         $row[$colIndexes{See}] = $see;
         $row[$colIndexes{SeeAlso}] = "\\\@dtlnovalue";
      }
      else
      {
         $row[$colIndexes{See}] = "\\\@dtlnovalue";
         $row[$colIndexes{SeeAlso}] = $seealsoVariable;
      }
   }
   else
   {
      $row[$colIndexes{See}] = "\\\@dtlnovalue";
      $row[$colIndexes{SeeAlso}] = "\\\@dtlnovalue";
   }

   $row[$colIndexes{Parent}] = 
     ($parent ? $parent : "\\\@dtlnovalue");

   $db->startModifications;

   if ($selectedRow > -1)
   {
      $db->replaceRow($selectedRow, \@row);
   }
   else
   {
      $db->appendRow(\@row);
   }

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

   $db->endModifications;

   $mw->destroy;
}

sub doRemoveRow{

   $db->startModifications;

   # Iterate through all the rows and remove any references to this
   # row

   my $theLabel = $row[$colIndexes{Label}];

   for (my $idx = 0; $idx < $rowCount; $idx++)
   {
      my $thisRow = $db->getRow($idx);

      next if ($thisRow->[$colIndexes{Label}] eq $theLabel);

      my $modified = 0;

      if ($thisRow->[$colIndexes{Parent}] eq $theLabel)
      {
         $thisRow->[$colIndexes{Parent}] = '';
         $modified = 1;
      }
      else
      {
         my $children = $thisRow->[$colIndexes{Child}];

         if ($children =~s/(^|,)$theLabel(,|$)/$1/)
         {
            $children =~s/,$//;

            $thisRow->[$colIndexes{Child}] = $children;

            $modified = 1;
         }
      }

      if ($thisRow->[$colIndexes{See}] eq $theLabel)
      {
         $thisRow->[$colIndexes{See}] = '';
         $modified = 1;
      }

      my $seealso = $thisRow->[$colIndexes{SeeAlso}];

      if ($seealso =~s/(^|,)$theLabel(,|$)/$1/)
      {
         $seealso =~s/,$//;

         $thisRow->[$colIndexes{SeeAlso}] = $seealso;

         $modified = 1;
      }

      if ($modified)
      {
         $db->replaceRow($idx, $thisRow);
      }
   }

   # remove row

   $db->removeRow($selectedRow);
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

sub updateCrossRefWidgets{

   my $active = $crossrefVariable eq 'see' ? 1 : 0;

   my $state = $active ? 'normal' : 'disabled';

   $seeOptionmenu->configure('-state', $state);

   $state = $active ? 'disabled' : 'normal';

   $seealsoLabel->configure('-state', $state);

   $seealsoOptionmenu->configure('-state', $state);

   $addCrossrefButton->configure('-state', $state);

   $removeCrossrefButton->configure('-state', $state);

   $mw->update
}

sub addCrossRef{

   if ($seealsoVariable)
   {
      $seealsoVariable .= ",$seealsoOption";
   }
   else
   {
      $seealsoVariable = $seealsoOption;
   }

   $mw->update
}

sub removeCrossRef{

   $seealsoVariable=~s/(^|,)$seealsoOption(,|$)/$1/;

   $seealsoVariable=~s/,$//;

   $mw->update
}

sub getWord{
  $db->getDictWord('plugin.datagidx.'.shift)
}

1;
