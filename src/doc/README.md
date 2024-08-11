# datatooltk

Java application to create and edit files in a format compatible
with the LaTeX datatool.sty package.

Author: Nicola Talbot (https://www.dickimaw-books.com/contact)

Application Home Page : https://www.dickimaw-books.com/software/datatooltk

## Licence

Copyright (C) 2013-2024 Nicola L. C. Talbot (dickimaw-books.com)

License GPLv3+: GNU GPL version 3 or later
http://gnu.org/licenses/gpl.html
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law.

## Requirements

  - Java Runtime Environment (at least Java 8) and, if plugins required, Perl;
  - TeX distribution that includes `datatool.sty`.

## Summary

A Java application that can be used to edit database files
that use `datatool.sty`'s internal format. It can also be used to
import data from CSV files, spreadsheets (ODS and XLSX), 
MySQL databases, and TeX files that define problems using commands
from `probsoln.sty`.

## Libraries

The `datatooltk.jar` file in the lib directory is the main
application. The other `.jar` files are required libraries
provided by other sources. The `texparserlib.jar` and
`texjavahelplib.jar` libraries
are by the same author and are also GPL 3.0. The source code
is available from https://github.com/nlct/texparser and
https://github.com/nlct/texjavahelp

The other `.jar` files are third party libraries:

  - MySQL connector (http://dev.mysql.com/downloads/connector/j/) GPL;
  - Apache POI (http://poi.apache.org/) Apache 2.0 License
   (which requires Apache Commons IO, which in turn requires Log4j)