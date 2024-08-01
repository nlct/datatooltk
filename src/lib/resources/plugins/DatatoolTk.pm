#!/usr/bin/env perl
#    Copyright (C) 2013-2024 Nicola L.C. Talbot
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

# Module used by datatooltk plugins
# Plugins must indicate that they are GPL compatible using
#    DatatoolTk->new({plugin_is_GPL_compatible => 1});

package DatatoolTk;

use XML::Parser;
use LWP::Simple;
use URI;

my @elements;
my @headers;
my @rows;

my $dbattrs;

# Constructor
sub new{
  my $class = shift;
  my $self = ($#_ == 0 ? $_[0] : {});

  unless ($self->{plugin_is_GPL_compatible})
  {
     die "Plugin not GPL compatible\n";
  }

  bless $self, $class;
  $self->_initialise();
  return $self;
}

# Read the XML data supplied by datatooltk, and load dictionary
# resources if provided
sub _initialise{
  my $self = shift;

  my $xml = '';

   while (<STDIN>)
   {
      chomp;

      last if (/^--EOF--$/);

      $xml .= $_;
   }

   @elements = ();

   my $parser = XML::Parser->new(
     Handlers =>
     {
        Start => \&_handle_start,
        End   => \&_handle_end,
        Char  => \&_handle_char
     });

   $parser->parse($xml);

   # Load dictionary if provided

   if ($dbattrs->{dict})
   {
      &_parseDictionary($self, get $dbattrs->{dict});
   }
}

# Handler for XML start tags
sub _handle_start{
   my( $expat, $element, %attrs ) = @_;

   if ($element eq 'datatooltkplugin')
   {
      unless ($#elements == -1)
      {
         die "'$element' must be at the top level\n";
      }

      $dbattrs = \%attrs;
   }
   elsif ($element eq 'headers')
   {
      if ($#elements == -1 or $elements[$#elements] ne 'datatooltkplugin')
      {
         die "'$element' must be contained in 'datatooltkplugin'\n";
      }

      @headers = ();
   }
   elsif ($element eq 'header')
   {
      if ($#elements == -1 or $elements[$#elements] ne 'headers')
      {
         die "'$element' must be contained in 'headers'\n";
      }

      my %header = 
      (
         label => '',
         title => '',
         type  => ''
      );

      push @headers, \%header;
   }
   elsif ($element eq 'label' or $element eq 'title'
       or $element eq 'type')
   {
      if ($#elements == -1 or $elements[$#elements] ne 'header')
      {
         die "'$element' must be contained in 'header'\n";
      }

   }
   elsif ($element eq 'rows')
   {
      if ($#elements == -1 or $elements[$#elements] ne 'datatooltkplugin')
      {
         die "'$element' must be contained in 'datatooltkplugin'\n";
      }

      @rows = ();
   }
   elsif ($element eq 'row')
   {
      if ($#elements == -1 or $elements[$#elements] ne 'rows')
      {
         die "'$element' must be contained in 'rows'\n";
      }

      my @row = ();

      push @rows, \@row;
   }
   elsif ($element eq 'entry')
   {
      if ($#elements == -1 or $elements[$#elements] ne 'row')
      {
         die "'$element' must be contained in 'row'\n";
      }

       push @{$rows[$#rows]}, '';
   }
   elsif ($element eq 'br')
   {
      if ($#elements == -1 or $elements[$#elements] ne 'entry')
      {
         die "'$element' must be contained in 'entry'\n";
      }

      my $row = $rows[$#rows];

      $row->[scalar(@$row)-1] .= "\n";
   }
   else
   {
      die "Unknown element '$element'\n";
   }

   push @elements, $element;
}

# Handler for XML end tags
sub _handle_end{
   my( $expat, $element ) = @_;

   my $currentElement = pop @elements;

   if ($element ne $currentElement)
   {
      die "Mismatched end $element tag. (</$currentElement>
expected)\n";
   }

}

# Handler for character read from XML input 
# (Current tag path given by @elements. The last item is the current
# tag element.)
sub _handle_char{
   my( $expat, $string ) = @_;

   if ($#elements > -1)
   {
      my $element = $elements[$#elements];

      if ($element eq 'label' or $element eq 'title'
       or $element eq 'type')
      {
         my $header = $headers[$#headers];

         $header->{$element} .= $string;
      }
      elsif ($element eq 'entry')
      {
         my $row = $rows[$#rows];

         $row->[scalar(@$row)-1] .= $string;
      }
   }
}

# Parse the dictionary resource file
sub _parseDictionary{
   my $self = shift;
   my $content = shift;

   while ($content=~/^([\w\.]+)=(.*)$/mg)
   {
      $self->{$1} = $2;
   }
}

# Get a word defined in the dictionary by the given key
sub getDictWord{
   my $self = shift;
   my $key = shift;

   my $word;

   if (exists $self->{$key})
   {
      $word = $self->{$key};
   }
   elsif (exists $self->{"plugin.$key"})
   {
      $word = $self->{"plugin.$key"};
   }
   else
   {
      $word = $key;
   }

   if ($#_ > -1)
   {
      $word=~s/\{(\d)\}/$_[$1-1]/g;
      $word=~s/''/'/g;
   }

   $word;
}

sub getDictMnemonicIndex{
   my $self = shift;
   my $key = shift;
   my $index = -1;

   if (exists $self->{"$key.mnemonicidx"})
   {
      $index = $self->{"$key.mnemonicidx"};
   }
   elsif (exists $self->{"plugin.$key.mnemonicidx"})
   {
      $index = $self->{"plugin.$key.mnemonicidx"};
   }

   $index;
}

# Get the location of datatooltk's resources directory (as a URI).
sub getResourcesUrl{
   my $self = shift;

   $dbattrs->{resources};
}

# Get the full path name of the given image file contained in
# datatooltk's resources/icons/ directory.
sub getImageFile{
   my $self = shift;
   my $filename = shift;

   $uri = URI->new($dbattrs->{resources}.'/icons/'.$filename);

   $uri->path;
}

# Set the iconimage for the main window if the image file is
# available
sub setIconImage{
   my $self = shift;
   my $mw = shift; # main window

   my $imgFile = $self->getImageFile('datatooltk-logosmall.png');

   if ($imgFile and -e $imgFile)
   {
      my $icon = $mw->Photo(-file=>$imgFile, -format=>'png');

      if ($icon)
      {
         $mw->iconimage($icon);
      }
   }
}

# Create cancel button
sub createCancelButton{
   my $self = shift;
   my $buttonFrame = shift;
   my $mw = shift; # main window

   my $widget = $self->createButton($buttonFrame, $mw,
    sub { exit }, "button.cancel", $self->getImageFile('cancel.png'));

   $widget;
}

# Create okay button
sub createOkayButton{
   my $self = shift;
   my $buttonFrame = shift;
   my $mw = shift;
   my $cmd = shift;

   my $widget = $self->createButton($buttonFrame, $mw,
    $cmd, "button.okay", $self->getImageFile('okay.png'));

   $mw->bind("<Shift-Return>", $cmd);

   $widget;
}

sub createButton{
   my $self = shift;
   my $buttonFrame = shift;
   my $mw = shift;
   my $cmd = shift;
   my $dictKey = shift;
   my $imgFile = shift;

   my $shot;

   my $widget;

   my $text = $self->getDictWord($dictKey);
   my $idx = $self->getDictMnemonicIndex($dictKey);

   if ($imgFile and -e $imgFile)
   {
      $shot = $mw->Photo(-file=>$imgFile);

      $widget = $buttonFrame->Button(
          -text     => $text,
          -command  => $cmd,
          -image    => $shot,
          -underline => $idx,
          -compound => 'left'
      )->pack(-side=>'left', -expand=>1, -padx =>20);
   }
   else
   {
      $widget = $buttonFrame->Button(
          -text    => $text,
          -command => $cmd,
          -underline => $idx
      )->pack(-side=>'left', -expand=>1, -padx =>20);
   }

   if ($idx > -1 and $idx < length($text))
   {
      $chr = lc(substr($text, $idx, 1));
      $mw->bind("<Alt-Key-$chr>", $cmd );
   }

   $widget;
}

# Return the index of the row that was currently selected in
# datatooltk when the plugin was invoked, or -1 if none selected.
sub selectedRow{
  my $self = shift;

  return $dbattrs->{selectedrow};
}

# Return the index of the column that was currently selected in
# datatooltk when the plugin was invoked, or -1 if none selected.
sub selectedColumn{
  my $self = shift;

  return $dbattrs->{selectedcolumn};
}

# Get the number of rows in the database
sub rowCount{
  my $self = shift;

  return $#rows+1;
}

# Get the number of columns in the database
sub columnCount{
  my $self = shift;

  return $#headers+1;
}

# Get the index of the column identified by the given label
sub getColumnIndex{
  my $self = shift;
  my $label = shift;

  for (my $idx = 0; $idx <= $#headers; $idx++)
  {
     if ($headers[$idx]->{label} eq $label)
     {
        return $idx;
     }
  }

  return -1;
}

# Get the identifying label for the column with the given index.
sub getColumnLabel{
  my $self = shift;
  my $index = shift;

  return $headers[$index]->{label};
}

# Get the title for the column with the given index.
sub getColumnTitle{
  my $self = shift;
  my $index = shift;

  return $headers[$index]->{title};
}

# Get the data type for the column with the given index.
sub getColumnType{
  my $self = shift;
  my $index = shift;

  return $headers[$index]->{type};
}

# Get the entry for the given row and column index.
sub getEntry{
  my $self = shift;
  my $rowIndex = shift;
  my $colIndex = shift;

  my @row = @{$rows[$rowIndex]};

  return $row[$colIndex];
}

# Get an array containing the data in the given row.
sub getRow{
  my $self = shift;
  my $rowIndex = shift;

  return $rows[$rowIndex];
}

# Get an array containing the data in the given column,
# excluding the given row index (use -1 if no row should be excluded).
sub getColumn{
  my $self = shift;
  my $colIndex = shift;
  my $excludeRowIndex = shift;

  my @column = ();

  for (my $idx = 0; $idx <= $#rows; $idx++)
  {
     unless ($idx == $excludeRowIndex)
     {
        push @column, $rows[$idx]->[$colIndex];
     }
  }

  @column
}

# Indicate that the database is about to be modified. Should only be
# used once per plugin call. Modifications to the database via
# functions like &insertRow must occur after this function.
sub startModifications{
   my $self = shift;

   print "<datatooltk>\n";
}

# Indicate that the database modifications have finished. Must occur
# after &startModifications and should not be used more than once
# per plugin call. No further modifications can be made to
# the database after this function.
sub endModifications{
   my $self = shift;

   print "</datatooltk>\n";
}

# Insert a row (array reference) into the database at the given row index.
sub insertRow{
  my $self = shift;
  my $rowIndex = shift;
  my $row = shift;

  splice @rows, $rowIndex, 0, $row;

  print "<row action=\"insert\" value=\"$rowIndex\">\n";

  foreach my $entry (@{$row})
  {
     print "<entry>$entry</entry>\n";
  }

  print "</row>\n";
}

# Append the given row (array reference) to the database.
sub appendRow{
  my $self = shift;
  my $row = shift;

  push @rows, $row;

  print "<row action=\"append\" >\n";

  foreach my $entry (@{$row})
  {
     print "<entry>$entry</entry>\n";
  }

  print "</row>\n";
}

# Replace the row at the given row index with the supplied array
# reference.
sub replaceRow{
  my $self = shift;
  my $rowIndex = shift;
  my $row = shift;

  $rows[$rowIndex] = $row;

  print "<row action=\"replace\" value=\"$rowIndex\">\n";

  foreach my $entry (@{$row})
  {
     print "<entry>$entry</entry>\n";
  }

  print "</row>\n";
}

# Remove the row at the given row index.
sub removeRow{
  my $self = shift;
  my $rowIndex = shift;

  print "<row action=\"remove\" value=\"$rowIndex\">\n";
  print "</row>\n";

}

# Get the maximum value in the given column.
# If the data is the string type, an alphabetical comparison is
# used.
sub maxForColumn{
   my $self = shift;
   my $colIdx = shift;
   my $max;

   # Is the column a string or numerical type?

   if ($headers[$colIdx]->{type} le 0)
   {
     # string

     $max = '';

     foreach my $row (@rows)
     {
        $max = $row->[$colIdx] if ($row->[$colIdx] gt $max);
     }
   }
   elsif ($headers[$colIdx]->{type} eq 3)
   {
     # currency

     $max = 0;

     foreach my $row (@rows)
     {
        my $value = $row->[$colIdx];

        if ($value=~/(\d+|(?:\d*\.?\d+))/)
        {
           $value = $1;

           $max = $value if ($value > $max);
        }
     }
   }
   else
   {
     $max = 0;

     foreach my $row (@rows)
     {
        $max = $row->[$colIdx] if ($row->[$colIdx] > $max);
     }
   }

   return $max
}

# Get the minimum value in the given column.
# If the data is the string type, an alphabetical comparison is
# used.
sub minForColumn{
   my $self = shift;
   my $colIdx = shift;
   my $min;

   # Is the column a string or numerical type?

   if ($headers[$colIdx]->{type} le 0)
   {
     # string

     $min = '';

     foreach my $row (@rows)
     {
        $min = $row->[$colIdx] if ($row->[$colIdx] lt $min);
     }
   }
   elsif ($headers[$colIdx]->{type} eq 3)
   {
     # currency

     $min = 0;

     foreach my $row (@rows)
     {
        my $value = $row->[$colIdx];

        if ($value=~/(\d+|(?:\d*\.?\d+))/)
        {
           $value = $1;

           $min = $value if ($value < $min);
        }
     }
   }
   else
   {
     $min = 0;

     foreach my $row (@rows)
     {
        $min = $row->[$colIdx] if ($row->[$colIdx] < $min);
     }
   }

   return $min
}

1;
