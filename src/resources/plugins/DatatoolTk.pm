#!/usr/bin/perl

package DatatoolTk;

use XML::Parser;

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

      my $row = $rows[$#rows];

      push @{$row}, '';
   }
   else
   {
      die "Unknown element '$element'\n";
   }

   push @elements, $element;
}

sub _handle_end{
   my( $expat, $element ) = @_;

   pop @elements;
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
         my @row = @{$rows[$#rows]};

         $row[$#row] .= $string;
      }
   }
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

1;
