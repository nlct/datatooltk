#!/usr/bin/perl

package DatatoolTk;

use XML::Parser;
use LWP::Simple;

my @elements;
my @headers;
my @rows;

my $dbattrs;

sub new{
  my $class = shift;
  my $self = {};
  bless $self, $class;
  $self->_initialise();
  return $self;
}

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

sub _handle_end{
   my( $expat, $element ) = @_;

   my $currentElement = pop @elements;

   if ($element ne $currentElement)
   {
      die "Mismatched end $element tag. (</$currentElement>
expected)\n";
   }

}

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

sub _parseDictionary{
   my $self = shift;
   my $content = shift;

   while ($content=~/^([\w\.]+)=(.*)$/mg)
   {
      $self->{$1} = $2;
   }
}

sub getDictWord{
   my $self = shift;
   my $key = shift;

   my $word = $self->{$key};

   if ($#_ > -1)
   {
      $word=~s/\$(\d)/$_[$1-1]/g;
   }

   $word;
}

sub selectedRow{
  my $self = shift;

  return $dbattrs->{selectedrow};
}

sub selectedColumn{
  my $self = shift;

  return $dbattrs->{selectedcolumn};
}

sub rowCount{
  my $self = shift;

  return $#rows+1;
}

sub columnCount{
  my $self = shift;

  return $#headers+1;
}

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

sub getColumnLabel{
  my $self = shift;
  my $index = shift;

  return $headers[$index]->{label};
}

sub getColumnTitle{
  my $self = shift;
  my $index = shift;

  return $headers[$index]->{title};
}

sub getColumnType{
  my $self = shift;
  my $index = shift;

  return $headers[$index]->{type};
}

sub getEntry{
  my $self = shift;
  my $rowIndex = shift;
  my $colIndex = shift;

  my @row = @{$rows[$rowIndex]};

  return $row[$colIndex];
}

sub getRow{
  my $self = shift;
  my $rowIndex = shift;

  return $rows[$rowIndex];
}

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

sub startModifications{
   my $self = shift;

   print "<datatooltk>";
}

sub endModifications{
   my $self = shift;

   print "</datatooltk>";
}

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
