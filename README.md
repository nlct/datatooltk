# datatooltk
Java application for use with the datatool LaTeX package.

The datatooltk Java application that can be used to create
[datatool.sty](https://ctan.org/pkg/datatool) databases in
datatool's internal format, which can be quickly imported into a
LaTeX document using `\DTLread` (for `datatool.sty` v3.0+ or 
`\input` for older versions). If your version of `datatool.sty`
is older than 3.0, be sure to set the format to `dbtex-2` or
`dtltex-2` (`--output-format` switch in batch mode or the
appropriate file filter selector in GUI mode).

The following applications are available:

 - DatatoolTk (`datatooltk.jar`): the primary application which can be run in batch
   mode or with a graphical user interface (GUI). That is, it can
   either be run from the command line with an input and output
   file, or it can open a window with menus and buttons.
   Preferred settings (which can be changed in GUI mode) are saved
   in a properties directory and picked up on the
   next run (`~/.datatooltk` on Unix-like systems
   or `datatooltk-settings` or the path identified by the
   environment variable `DATATOOLTK`).

   This application supports importing data from TeX files
   (containing `datatool.sty` database construction commands or
    `probsoln.sty` problem definitions), CSV/TSV files,
   ODS (Open Document SpreadSheet), FODS (Flat Open Document SpreadSheet),
   XLSX, and SQL. For database connections, a driver is required.
   For example, to provide support for MySQL, the
   `mysql-connector-java.jar` library needs to be on the class path.
   (Support for MySQL is automatically included in the Manifest.)

  ```bash
  datatooltk [options]
  ```
  The `datatooltk` script simply runs the `datatooltk.jar`
  application. You can run it directly where `path/to/lib` should be
  changed to the path to the `lib` directory:
  ```bash
  java -jar path/to/lib/datatooltk.jar [options]
  ```
  If run in batch mode (the default) the options must include an input
  file or input source and an output file. To run in GUI mode, the
  options list should include `--gui`.
  ```bash
  datatooltk-gui [options]
  ```
  The `datatooltk-gui` script simply runs the `datatooltk.jar`
  application with `--gui` and a splash image:
  ```bash
  java -splash:path/to/lib/datatooltk-splash.png -jar path/to/lib/datatooltk.jar --gui [options]
  ```

 - DatatoolTk Restricted (`datatooltk-restricted.jar`):
   a restricted version of the DatatoolTk application that only runs
   in batch mode. If you attempt to run this application in GUI mode
   it will trigger an "Unsupported option" error. The restricted
   application doesn't support SQL connections, but otherwise
   supports the import file formats that the primary DatatoolTk
   application supports.

 - DatatoolTk Extra (`datatooltk-extra.jar`):
   as the primary DatatoolTk application but additionally
   supports Microsoft's old binary Excel (`.xls`) file format.
   This requires the third party Apache POI library.

## Requirements

Requires at least Java 8 and the following libraries:

 - [TeX Parser Library](https://github.com/nlct/texparser)
   (GPL-3.0)
   to parse LaTeX files: the file `texparserlib.jar`
   should be placed in the `lib` directory. **This library is
   required by all DatatoolTk applications.**
 - [TeX Java Help](https://github.com/nlct/texjavahelp) (GPL-3.0)
   to convert the LaTeX manual with `texjavahelpmk`
   to the HTML and XML files required by `texjavahelplib.jar`
   which manages the application help system and localised messages:
   the file `texjavahelplib.jar` should be placed in the `lib` directory.
   **This library is required by all DatatoolTk applications.**
   (The restricted application only uses the library for localised
   messages. The other applications additionally use it for
   the in-application help in GUI mode.)

   (The `texjavahelpmk.jar` and `texjavahelp.sty` files are only required 
   when compiling the manual to PDF or HTML. The PDF file needs to be created first
   to ensure that the `toc`, `aux` and `glstex` files are available
   when `texjavahelpmk.jar` parses the document source.)

 - [mysql-connector-java.jar](https://dev.mysql.com/downloads/connector/j/)
   (GPL-2.0-only) 
   to pull data from MySQL databases: the file `mysql-connector-java.jar`
   should be placed in the `lib` directory.
   **This library is required for the primary DatatoolTk application
   and the DatatoolTK Extra application but not for the restricted
   application which doesn't support SQL imports.**

 - [Apache POI](https://poi.apache.org/) (Apache License, Version 2.0)
    to read Microsoft Excel spreadsheets (xls): the files `poi-`_version_`.jar`, 
   `poi-ooxml-`_version_`.jar` and `poi-ooxml-schema-`_version_`.jar` 
   should be placed in the `lib` directory. This library in turn
   requires Apache Commons IO, which requires Log4j, so those
   jar files are also required.
   **This library and its dependencies are only required for the 
   DatatoolTK Extra application.**
   
If any plugins are needed for the applications that have a GUI mode, 
Perl is also required.

---

Nicola Talbot [dickimaw-books.com](https://www.dickimaw-books.com/)

License GPLv3+: GNU GPL version 3 or later
http://gnu.org/licenses/gpl.html
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law.
