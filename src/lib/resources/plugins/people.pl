#!/usr/bin/env perl
#    Copyright (C) 2013 Nicola L.C. Talbot
#    www.dickimaw-books.com
#
#    This program is free software; you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation; either version 3 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with this program; if not, write to the Free Software
#    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

# people plugin used by datatooltk

use strict;
use Tk;
use Tk::PNG;
use DatatoolTk;

my $db = DatatoolTk->new({plugin_is_GPL_compatible => 1});

my $selectedRow = $db->selectedRow();

my %colIndexes = ();

foreach my $key (qw/ID Surname Forename Title Address Telephone
Email/)
{
   my $idx = $db->getColumnIndex($key);

   if ($idx == -1)
   {
      die $db->getDictWord('plugin.error.missing_column', $key),
"\n";
   }

   $colIndexes{$key} = $idx;
}

my @row = ("") x $db->columnCount;

if ($selectedRow > -1)
{
   @row = @{$db->getRow($selectedRow)};

   $row[$colIndexes{Address}]=~s/\\\\/\n/sg;
}

my $mw = MainWindow->new;

$mw->title($selectedRow > -1 ? 
  &getWord("edit_entry") : &getWord("new_entry"));

my $nameFrame = $mw->Frame()->pack;

$nameFrame->Label
  (
    -text => &getWord('Title')
  )->pack(-side=>'left', -expand=>1);

my $titleEntry = $nameFrame->Entry
  (
    -width=>4
  )->pack(-side=>'left', -expand=>1);

$titleEntry->insert(0, $row[$colIndexes{Title}]);

$nameFrame->Label
  (
    -text => &getWord('Forename')
  )->pack(-side=>'left', -expand=>1);

my $forenameEntry = $nameFrame->Entry()->pack(-side=>'left', -expand=>1);

$forenameEntry->insert(0, $row[$colIndexes{Forename}]);

$nameFrame->Label
  (
    -text => &getWord('Surname')
  )->pack(-side=>'left', -expand=>1);

my $surnameEntry = $nameFrame->Entry()->pack(-side=>'left', -expand=>1);

$surnameEntry->insert(0, $row[$colIndexes{Surname}]);

my $contactFrame = $mw->Frame()->pack;

$contactFrame->Label
  (
    -text=>&getWord('Email')
  )->pack(-side=>'left', -expand=>1);

my $emailEntry = $contactFrame->Entry()->pack(-side=>'left', -expand=>1);

$emailEntry->insert(0, $row[$colIndexes{Email}]);

$contactFrame->Label
  (
    -text=>&getWord('Telephone')
  )->pack(-side=>'left', -expand=>1);

my $telephoneEntry = $contactFrame->Entry()->pack(-side=>'left', -expand=>1);

$telephoneEntry->insert(0, $row[$colIndexes{Telephone}]);

my $addressFrame = $mw->Frame()->pack();

$addressFrame->Label
  (
    -text=>&getWord('Address')
  )->pack(-side=>'left', -expand=>1);

my $addressText = $addressFrame->Scrolled('Text', -width=>40, -height=>6)->pack();

$addressText->Contents($row[$colIndexes{Address}]);

my $buttonFrame = $mw->Frame()->pack;

$db->createCancelButton($buttonFrame, $mw);

$db->createOkayButton($buttonFrame, $mw, \&doDbUpdate);

$mw->iconify;
$mw->update;
$mw->deiconify;

$mw->state('withdrawn');

my $xpos = int(($mw->screenwidth-$mw->width)/2);
my $ypos = int(($mw->screenheight-$mw->height)/2);

$mw->geometry("+$xpos+$ypos");

$mw->state('normal');

#$mw->idletasks;
#$db->setIconImage($mw);

MainLoop;

sub doDbUpdate{

  $row[$colIndexes{Title}] = $titleEntry->get;

  $row[$colIndexes{Surname}] = $surnameEntry->get;

  $row[$colIndexes{Forename}] = $forenameEntry->get;

  $row[$colIndexes{Email}] = $emailEntry->get;

  $row[$colIndexes{Telephone}] = $telephoneEntry->get;

  $row[$colIndexes{Address}] = $addressText->Contents;

  $row[$colIndexes{Address}]=~s/\n\s*$//;

  $row[$colIndexes{Address}]=~s/\n/\\\\<br\/>/sg;

  $db->startModifications;

  if ($selectedRow > -1)
  {
     $db->replaceRow($selectedRow, \@row);
  }
  else
  {
     $row[$colIndexes{ID}] = $db->maxForColumn($colIndexes{ID})+1;

     $db->appendRow(\@row);
  }

  $db->endModifications;

  exit;
}

sub getWord{
  $db->getDictWord('plugin.people.'.shift)
}

1;
