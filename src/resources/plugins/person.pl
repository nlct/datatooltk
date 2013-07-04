#!/usr/bin/perl -w

use strict;
use Tk;
use DatatoolTk;

my $db = DatatoolTk->new();

my $selectedRow = $db->selectedRow();

my %colIndexes = 
(
   'ID' => $db->getColumnIndex('ID'),
   'Surname' => $db->getColumnIndex('Surname'),
   'Forename' => $db->getColumnIndex('Forename'),
   'Title' => $db->getColumnIndex('Title'),
   'Address' => $db->getColumnIndex('Address'),
   'Telephone' => $db->getColumnIndex('Telephone'),
   'Email' => $db->getColumnIndex('Email')
);

my @row = ("") x $db->columnCount;

if ($selectedRow > -1)
{
   @row = @{$db->getRow($selectedRow)};

   $row[$colIndexes{Address}]=~s/\\\\/\n/sg;
}

my $mw = MainWindow->new;

my $nameFrame = $mw->Frame()->pack;

$nameFrame->Label(-text => 'Title:')->pack(-side=>'left', -expand=>1);

my $titleEntry = $nameFrame->Entry
  (
    -width=>4,
  )->pack(-side=>'left', -expand=>1);

$titleEntry->insert(0, $row[$colIndexes{Title}]);

$nameFrame->Label(-text => 'Forename:')->pack(-side=>'left', -expand=>1);

my $forenameEntry = $nameFrame->Entry()->pack(-side=>'left', -expand=>1);

$forenameEntry->insert(0, $row[$colIndexes{Forename}]);

$nameFrame->Label(-text => 'Surname:')->pack(-side=>'left', -expand=>1);

my $surnameEntry = $nameFrame->Entry()->pack(-side=>'left', -expand=>1);

$surnameEntry->insert(0, $row[$colIndexes{Surname}]);

my $contactFrame = $mw->Frame()->pack;

$contactFrame->Label(-text=>'Email')->pack(-side=>'left', -expand=>1);

my $emailEntry = $contactFrame->Entry()->pack(-side=>'left', -expand=>1);

$emailEntry->insert(0, $row[$colIndexes{Email}]);

$contactFrame->Label(-text=>'Telephone')->pack(-side=>'left', -expand=>1);

my $telephoneEntry = $contactFrame->Entry()->pack(-side=>'left', -expand=>1);

$telephoneEntry->insert(0, $row[$colIndexes{Telephone}]);

my $addressFrame = $mw->Frame()->pack();

$addressFrame->Label(-text=>'Address')->pack(-side=>'left', -expand=>1);
my $addressText = $addressFrame->Scrolled('Text', -width=>40, -height=>6)->pack();

$addressText->Contents($row[$colIndexes{Address}]);

my $buttonFrame = $mw->Frame()->pack;

$buttonFrame->Button(
    -text    => 'Cancel',
    -command => sub { exit },
)->pack(-side=>'left', -expand=>1);

$buttonFrame->Button(
    -text    => 'Okay',
    -command => \&doDbUpdate,
)->pack(-side=>'left', -expand=>1);

$mw->update;

my $xpos = int(($mw->screenwidth-$mw->width)/2);
my $ypos = int(($mw->screenheight-$mw->height)/2);

$mw->geometry("+$xpos+$ypos");

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

  if ($selectedRow > -1)
  {
     $db->replaceRow($selectedRow, \@row);
  }
  else
  {
     $row[$colIndexes{ID}] = $db->maxForColumn($colIndexes{ID})+1;

     $db->appendRow(\@row);
  }

  exit;
}

1;
